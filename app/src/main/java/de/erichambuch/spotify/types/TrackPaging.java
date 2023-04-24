package de.erichambuch.spotify.types;

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;

/**
 * Wrapper for JSON object of Spotify API.
 *
 * @link https://developer.spotify.com/documentation/web-api/reference/object-model/#user-object-private
 */
public class TrackPaging {

    public static class TrackContainer {
        public Boolean is_local;
        public String added_at;
        public Track track;

        public Date getAddedAt() throws ParseException {
            if (added_at == null)
                return null;
            return Date.from(Instant.parse(added_at));
        }
    }

    public String href;
    public TrackContainer[] items;
    public int limit;
    public String next;
    public  String previous;
    public int total;

}
