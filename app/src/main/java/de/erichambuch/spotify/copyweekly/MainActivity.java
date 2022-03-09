package de.erichambuch.spotify.copyweekly;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.google.android.gms.oss.licenses.OssLicensesMenuActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationResponse;

import java.util.LinkedHashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    /**
     * Zur Kommunikationn Service and MainActivity (UI).
     */
    public class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ( ACTION_ERROR.equals(intent.getAction()) ) {
                new MaterialAlertDialogBuilder(MainActivity.this)
                        .setTitle("Fehler")
                        .setMessage(intent.getStringExtra("text"))
                        .setNeutralButton(android.R.string.ok, null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            } else if ( ACTION_PLAYLIST_FINISHED.equals(intent.getAction()) ) {
                newPlaylistUri = Uri.parse(intent.getStringExtra("uri"));
                newPlaylistHttpUrl = Uri.parse(intent.getStringExtra("url"));
                final Button open = findViewById(R.id.button_openplaylist);
                open.setEnabled(true);
            } else if (ACTION_UPDATE_PROGRESS.equals(intent.getAction())) {
                String text = intent.getStringExtra("text");
                int progress = intent.getIntExtra("percent", 0);
                ((TextView)findViewById(R.id.id_maintext)).setText(text);
                ((ProgressBar)findViewById(R.id.progressBar)).setProgress(progress);
            }
        }
    }

    private static final int REQUEST_SPOTIFY_AUTH_CODE = 1337;

    static final String ACTION_UPDATE_PROGRESS = "de.erichambuch.spotify.copyweekly.updateprogress";
    static final String ACTION_ERROR = "de.erichambuch.spotify.copyweekly.error";
    static final String ACTION_PLAYLIST_FINISHED = "de.erichambuch.spotify.copyweekly.playlistfinished";

    /**
     * Spotify Access Token.
     */
    private String accessToken;

    /**
     * URI der neuen Playlist.
     */
    private Uri newPlaylistUri;
    /**
     * Web URL der neuen Playlist.
     */
    private Uri newPlaylistHttpUrl;

    /**
     * Broadcast für Kommunikation zwischen Activity und Service.
     */
    private BroadcastReceiver thisReceiver;

    /**
     * Fragment für Hauptscreen.
     */
    public static class MainFragment extends Fragment {

        /**
         * Callback to activity.
         */
        private MainActivity listener;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
            // Defines the xml file for the fragment
            return inflater.inflate(R.layout.fragment_main, parent, false);
        }

        @Override
        public void onAttach(@NonNull Context context) {
            super.onAttach(context);
            if (context instanceof MainActivity) {
                listener = (MainActivity) context;
            } else {
                throw new ClassCastException(context
                        + " must implement MainActivity");
            }
        }

        @Override
        public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            final Button fab = view.findViewById(R.id.button);
            fab.setOnClickListener(view1 -> {
                listener.startCopy(); // callback to Activity
            });
            final Button open = view.findViewById(R.id.button_openplaylist);
            open.setOnClickListener(view12 -> {
                listener.openPlaylist(); // callback to Activity
            });
        }

    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // das Konzept ist einfacher als das ganze Geraffel mit ViewModel & Co, ServiceConnection etc.
        thisReceiver = new MyBroadcastReceiver();
        IntentFilter broadCastIntentFilter = new IntentFilter();
        broadCastIntentFilter.addAction(ACTION_ERROR);
        broadCastIntentFilter.addAction(ACTION_PLAYLIST_FINISHED);
        broadCastIntentFilter.addAction(ACTION_UPDATE_PROGRESS);
        registerReceiver(thisReceiver, broadCastIntentFilter);

        // und Alarm registrieren
        BootUpAlarmScheduleReceiver.registerNotificationAlarm(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        final String action = getIntent().getAction();
        if(Intent.ACTION_MAIN.equals(action)) {
            // Check internet
            ConnectivityManager connMgr = (ConnectivityManager)
                    getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {
                // Melde dich bei Spotify an
                boolean authenticateApp = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(AppInfo.PREFS_AUTHENTICATE, true);
                final AuthorizationRequest request = new AuthorizationRequest.Builder(getClientId(), AuthorizationResponse.Type.TOKEN,
                        "app://de.erichambuch.spotify.copyweekly")
                        .setScopes(new String[]{"user-read-private", "playlist-read", "playlist-read-private", "playlist-modify-private", "playlist-modify-public"})
                        .setShowDialog(false)
                        .build();
                if (authenticateApp)
                    AuthorizationClient.openLoginActivity(this, REQUEST_SPOTIFY_AUTH_CODE, request);
                else
                    AuthorizationClient.openLoginInBrowser(this, request);
            } else {
                ((TextView) findViewById(R.id.id_maintext)).setText(R.string.text_error_no_internet);
            }
        } else if( Intent.ACTION_VIEW.equals(action)) { // for Web Authentication Flow
            Uri uri = getIntent().getData();
            final String fragment = uri.getFragment() != null ? uri.getFragment() : "";
            accessToken = splitQuery(fragment).get("access_token");
            if(accessToken != null)
                ((TextView)findViewById(R.id.id_maintext)).setText(R.string.text_info_authokay);
            else
                ((TextView)findViewById(R.id.id_maintext)).setText(R.string.text_error_authenticate);
        }
    }

    public static Map<String, String> splitQuery(String query) {
        Map<String, String> query_pairs = new LinkedHashMap<>();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            query_pairs.put(pair.substring(0, idx), pair.substring(idx + 1));
        }
        return query_pairs;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (thisReceiver != null)
            unregisterReceiver(thisReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            // clear caches - this may help
            AuthorizationClient.clearCookies(this);

            startActivity(new Intent(this, MyPreferencesActivity.class));
            return true;
        }
        else if (id == R.id.action_dataprotection) {
            startActivity(
                    new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_dataprotection))).
                    setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY));
            return true;
        } else if (id == R.id.action_about) {
            showAboutDialog();
            return true;
        } else if (id == R.id.action_showosslicenses) {
            showOSSLicences();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_SPOTIFY_AUTH_CODE) {
            AuthorizationResponse response = AuthorizationClient.getResponse(resultCode, intent);
            switch (response.getType()) {
                // Response was successful and contains auth token
                case TOKEN:
                    // Handle successful response
                    accessToken = response.getAccessToken();
                    ((TextView)findViewById(R.id.id_maintext)).setText(R.string.text_info_authokay);
                    break;
                // Auth flow returned an error
                case ERROR:
                    // Handle error response
                    ((TextView)findViewById(R.id.id_maintext)).setText(response.getError());
                    break;
                // Most likely auth flow was cancelled
                default:
                    // Handle other cases
            }
        }
    }

    /**
     * Starte die Kopieraktion der Playlist.
     * Dazu nutzen wir anstatt JobScheduler nun den WorkManager.
     */
    void startCopy() {
        if (accessToken != null ) {
            // and start in background
            Data data = new Data.Builder().putString("access_token", accessToken).build();
            WorkRequest uploadWorkRequest =
                    new OneTimeWorkRequest.Builder(SpotifyPlaylistService.class)
                            .setInputData(data).setConstraints(
                                    new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).
                            build();
            WorkManager
                    .getInstance(getApplicationContext())
                    .enqueue(uploadWorkRequest);
        } else {
            ((TextView)findViewById(R.id.id_maintext)).setText(R.string.text_error_authenticate);
        }
    }

    /**
     * Versuche Playlist zu öffnen: entweder direkt in Spotify App oder über Browser.
     */
    void openPlaylist() {
        if(newPlaylistUri != null) {
            final Intent spotifyIntent = new Intent(Intent.ACTION_VIEW, newPlaylistUri);
            if (spotifyIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(spotifyIntent);
            } else if ( newPlaylistHttpUrl != null ) {
                final Intent webIntent = new Intent(Intent.ACTION_VIEW, newPlaylistHttpUrl);
                if (webIntent.resolveActivity(getPackageManager()) != null)
                    startActivity(webIntent);
            }
        }
    }

    /**
     * Zeige den About-Dialog an.
     */
    private void showAboutDialog() {
        final MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(MainActivity.this);
        builder.setTitle(R.string.app_name);
        builder.setMessage(Html.fromHtml(getString(R.string.text_license), Html.FROM_HTML_MODE_COMPACT));
        builder.setPositiveButton(android.R.string.ok, null);
        AlertDialog dialog = builder.show();
        TextView view = (TextView)dialog.findViewById(android.R.id.message);
        if (view != null ) view.setMovementMethod(LinkMovementMethod.getInstance()); // make links clickable
    }

    private void showOSSLicences() {
        startActivity(new Intent(this, OssLicensesMenuActivity.class));
    }

    /**
     * Client ID aus https://developer.spotify.com/dashboard/applications/xxx.
     * xxx als base64
     * Außerdem muss der SHA1-Hash der App bei Spotify registriert werden.
     * Vorsicht: bei App Bundes ist dies nicht der "upload" key, sondern der zum publishing. Dieser laesst sich
     * in der Google Developer Console einsehen unter App-Signatur.
     * @return clientId
     */
    private String getClientId() {
        return BuildConfig.SPOTIFY_CLIENT_ID; // compiled by build.gradle into BuildConfig
    }
}
