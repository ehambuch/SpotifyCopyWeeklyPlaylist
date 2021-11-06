package de.erichambuch.spotify.copyweekly;

/**
 * V0.1 (1): Initiale Version
 * V0.2 (2): Open Playlist button
 * V0.3 (3): API Level 24
 * V0.4 (4): bugfixes
 * V0.5 (5): check internet connection, Client-ID verschluesselt
 * V0.6 (6): Nach LocalBroadcast-Receiver deprecated: UI/Service-Kommunikation umgebaut, neue Spotify API!
 * v0.7 (7): fehlende Permission für Wake-Lock
 * v1.0 (8): Target Android 11
 * V1.1 (10): Updated Updated libs
 * Idee: Liste mit Checkmarks für Fortschritt
 */
public class AppInfo {

    /**
     * Preferences: Notification on/off.
     */
    public static final String PREFS_NOTIFY_ME = "notify_me";

    public static final String APP_NAME = "CopyWeeklyPlaylistForSpotify";
}
