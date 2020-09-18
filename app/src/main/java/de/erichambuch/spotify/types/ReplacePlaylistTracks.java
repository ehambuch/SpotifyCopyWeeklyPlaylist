package de.erichambuch.spotify.types;

import java.util.List;

/**
 * Wrapper for JSON object of Spotify API.
 *
 * @link https://developer.spotify.com/documentation/web-api/reference/object-model/#user-object-private
 */
public class ReplacePlaylistTracks {

    public ReplacePlaylistTracks() {
    }

    public ReplacePlaylistTracks(List<String> uris) {
        this.uris = uris.toArray(new String[0]);
    }

    public String[] uris;
}
