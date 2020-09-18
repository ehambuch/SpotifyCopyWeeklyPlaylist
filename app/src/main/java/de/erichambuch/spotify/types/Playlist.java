package de.erichambuch.spotify.types;

/**
 * Wrapper for JSON object of Spotify API.
 *
 * @link https://developer.spotify.com/documentation/web-api/reference/object-model/#user-object-private
 */
public class Playlist {

    public static class PlaylistTracks {
         public String href;
         public int total;
    }
    public static class ExternalUrls {
        public String spotify;
    }

    public Boolean collaborative;
    public String description;
    public String href;
    public String id;
    public String name;
    public String snapshot_id;
    public PlaylistTracks tracks;
    public String type;
    public String uri;
    public ExternalUrls external_urls;
}
