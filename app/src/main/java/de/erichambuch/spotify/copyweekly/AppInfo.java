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
 * V1.2 (11): Update Android 12, Einsatz WorkManager neu5
 * V1.3.0 (12): Update Libs, Anzeige OpenSource Libs, Hide Client API Key
 * V1.4.0 (13): Update for Fehler bei Anmeldung Spotify (Web Flow alternativ)
 * V1.5.0 (14): Bugfix for Login problems
 * Idee: Liste mit Checkmarks für Fortschritt
 */
public class AppInfo {

    /**
     * Preferences: Notification on/off.
     */
    public static final String PREFS_NOTIFY_ME = "notify_me";

    public static final String PREFS_AUTHENTICATE = "authenticate_me";

    public static final String APP_NAME = "CopyWeeklyPlaylistForSpotify";
}
