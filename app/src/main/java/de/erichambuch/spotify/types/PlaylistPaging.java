package de.erichambuch.spotify.types;

/**
 * Wrapper for JSON object of Spotify API.
 *
 * @link https://developer.spotify.com/documentation/web-api/reference/object-model/#user-object-private
 */
public class PlaylistPaging {

    public String href;
    public Playlist[] items;
    public int limit;
    public String next;
    public  String previous;
    public int total;

}
