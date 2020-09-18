package de.erichambuch.spotify.types;

import java.util.Date;

public class Track {

    public String id;
    public String href;
    public String name;
    public String uri;
    public String type;

    public volatile Date createdDate;
}
