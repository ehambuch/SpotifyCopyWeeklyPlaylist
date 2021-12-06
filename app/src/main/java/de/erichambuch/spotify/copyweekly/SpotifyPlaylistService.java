package de.erichambuch.spotify.copyweekly;

import static android.content.Context.CONNECTIVITY_SERVICE;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.gson.Gson;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import de.erichambuch.spotify.types.Playlist;
import de.erichambuch.spotify.types.PlaylistPaging;
import de.erichambuch.spotify.types.ReplacePlaylistTracks;
import de.erichambuch.spotify.types.Track;
import de.erichambuch.spotify.types.TrackPaging;
import de.erichambuch.spotify.types.User;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Hintergrund-Service, der das Kopieren der Spotify Playlist übernimmt.
 * TODO: Rückmeldung nicht mehr per Broadcast sondern Result
 */
public class SpotifyPlaylistService extends Worker {

    /**
     * Fester Name der wöchentlichen Playlist.
     */
    private static final String WEEKLY_PLAY_LIST_NAME = "Discover Weekly";

    public SpotifyPlaylistService(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String token = getInputData().getString("access_token");
        if (!checkInternet()) {
            showError(R.string.text_error_no_internet);
            return Result.failure();
        }
        sendStatus(R.string.status_start, 10);
        String user = getUser(token);
        if (user == null) {
            showError(R.string.text_error_kein_user);
            return Result.failure();
        }
        sendStatus(R.string.status_searchplaylist, 20);
        Playlist weeklyPlaylist = findWeeklyPlaylist(token);
        if (weeklyPlaylist == null) {
            showError(R.string.text_error_keine_weekly_playlist);
            return Result.failure();
        }
        sendStatus(R.string.status_gettracks, 40);
        List<Track> tracks = readTracks(weeklyPlaylist.tracks.href, token);
        if (tracks == null ) {
            showError(R.string.text_error_keine_tracks);
            return Result.failure();
        }
        Calendar playlistDate = GregorianCalendar.getInstance();
        playlistDate.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        Date date = playlistDate.getTime();
        if ( !tracks.isEmpty() && tracks.get(0).createdDate != null )
            date = tracks.get(0).createdDate;
        sendStatus(R.string.status_createplaylist, 60);
        Playlist newPlaylist = createPlaylist(user, token, getApplicationContext().getString(R.string.text_copyof) + " " +
                DateFormat.getDateInstance(DateFormat.SHORT).format(date));
        if (newPlaylist == null) {
            showError(R.string.text_error_no_create_playlist);
            return Result.failure();
        }
        sendStatus(R.string.status_copytracks, 70);
        boolean okay = overwriteTracks(newPlaylist.id, token, tracks);
        if(!okay) {
            showError(R.string.text_error_no_copy_tracks);
            return Result.failure();
        }
        sendStatus(R.string.status_finish, 100);
        // and send URI of new Playlist to Activity
        Intent finishIntent = new Intent(MainActivity.ACTION_PLAYLIST_FINISHED);
        finishIntent.putExtra("uri", newPlaylist.uri).putExtra("url", newPlaylist.external_urls.spotify);
        getApplicationContext().sendBroadcast(finishIntent);
        return Result.success();
    }

    /**
     *  GET https://api.spotify.com/v1/me
     *
     * @param accessToken das Token
     * @return user name or null
     */
    private String getUser(String accessToken) {
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().get().url("https://api.spotify.com/v1/me").
                    addHeader("Authorization", "Bearer "+accessToken).
                    build();
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful())
                return null;
            User user = new Gson().fromJson(response.body().string(), User.class);
            return user.id;
        } catch(IOException e) {
            showError(e);
            return null;
        }
    }

    /**
     * GET https://api.spotify.com/v1/users/{user_id}/playlists
     * bzw. nur GET https://api.spotify.com/v1/me/playlists für mich.
     *
     * @param accessToken
     * @return playlist or null
     */
    private Playlist findWeeklyPlaylist(String accessToken) {
        try {
            OkHttpClient client = new OkHttpClient();
            PlaylistPaging paging = new PlaylistPaging();
            // erste Seite
            paging.next = "https://api.spotify.com/v1/me/playlists?limit=50";
            do {
                Request request = new Request.Builder().get().url(paging.next).addHeader("Authorization", "Bearer "+accessToken).build();
                Response response = client.newCall(request).execute();
                if (!response.isSuccessful())
                    return null;
                paging = new Gson().fromJson(response.body().string(), PlaylistPaging.class);
                if (paging != null && paging.items != null ) {
                    for(int i=0;i<paging.items.length;i++) {
                        Playlist playlist = paging.items[i];
                        if ( WEEKLY_PLAY_LIST_NAME.equals(playlist.name)) {
                            return playlist;
                        }
                    }
                }
            } while(paging != null && paging.next != null); // nächste Seiten holen
            return null; // not found

        } catch(IOException e) {
            showError(e);
            return null;
        }
    }

    /**
     *  GET https://api.spotify.com/v1/playlists/{playlist_id}/tracks
     * @param url
     * @param accessToken
     * @return List of tracks or null
     */
    private List<Track> readTracks(String url, String accessToken) {
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().get().url(url+"?market=from_token").  //&fields=id,href,uri"
                    addHeader("Authorization", "Bearer "+accessToken).
                    build();
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful())
                return null;
            String rep =response.body().string();
            TrackPaging paging = new Gson().fromJson(rep, TrackPaging.class);
            if (paging != null && paging.items != null ) {
                ArrayList<Track> list = new ArrayList<>(paging.items.length);
                for(int i=0;i<paging.items.length;i++) {
                    if (paging.items[i].track != null) {
                        try {
                            paging.items[i].track.createdDate = paging.items[i].getAddedAt();
                        } catch(ParseException e) { // ignore
                        }
                        list.add(paging.items[i].track);
                    }
                }
                return list;
            }
            return null;
        } catch(IOException e) {
            showError(e);
            return null;
        }
    }

    /**
     * PUT https://api.spotify.com/v1/playlists/{playlist_id}/tracks
     * @param playlistId
     * @param accessToken
     * @param tracks
     * @return true if succcessful
     */
    private boolean overwriteTracks(String playlistId, String accessToken, List<Track> tracks) {
        try {
            OkHttpClient client = new OkHttpClient();
            List<String> trackURIs = new ArrayList<>(tracks.size());
            for(Track track : tracks) {
               if(track.uri != null)
                   trackURIs.add(track.uri);
            }
            RequestBody body = RequestBody.create(new Gson().toJson(new ReplacePlaylistTracks(trackURIs)), MediaType.get("application/json"));
            Request request = new Request.Builder().put(body).url("https://api.spotify.com/v1/playlists/"+playlistId+"/tracks").
                    addHeader("Authorization", "Bearer "+accessToken).
                    build();
            Response response = client.newCall(request).execute();
            return response.isSuccessful();
        } catch(IOException e) {
            showError(e);
            return false;
        }
    }

    /**
     * POST https://api.spotify.com/v1/users/{user_id}/playlists
     * @param user
     * @param accessToken
     * @param name
     * @return the new playlist or null
     */
    private Playlist createPlaylist(String user, String accessToken, String name) {
        try {
            OkHttpClient client = new OkHttpClient();
            Playlist playlist = new Playlist();
            playlist.name = name;
            playlist.description = "Kopie der Discover Weekly von Spotify";
            RequestBody body = RequestBody.create(new Gson().toJson(playlist), MediaType.get("application/json"));
            Request request = new Request.Builder().post(body).url("https://api.spotify.com/v1/users/"+user+"/playlists").
                    addHeader("Authorization", "Bearer "+accessToken).
                    build();
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful())
                return null;
            return new Gson().fromJson(response.body().string(), Playlist.class);
        } catch(IOException e) {
            showError(e);
            return null;
        }
    }

    private void showError(Exception e) {
        Log.e(AppInfo.APP_NAME, "Error in using Spotify API", e);
        getApplicationContext().sendBroadcast(new Intent(MainActivity.ACTION_ERROR).
                putExtra("text", getApplicationContext().getString(R.string.text_error_msg)+": "+e.getLocalizedMessage()));
    }

    private void showError(int resId) {
        getApplicationContext().sendBroadcast(new Intent(MainActivity.ACTION_ERROR).
                putExtra("text", getApplicationContext().getString(resId)));
    }

    private void sendStatus(int resId, int percent) {
        getApplicationContext().sendBroadcast(new Intent(MainActivity.ACTION_UPDATE_PROGRESS).
                putExtra("text", getApplicationContext().getString(resId)).
                putExtra("percent", percent));
    }

    private boolean checkInternet() {
        ConnectivityManager connMgr = (ConnectivityManager)
                getApplicationContext().getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }
}
