/* AsteroidOS UUID collection for ble characteristics and watch filtering */
package org.asteroidos.sync.utils;

import java.util.UUID;

public class AsteroidUUIDS {
    // AsteroidOS Service Watch Filter UUID
    public static final UUID SERVICE_UUID               = UUID.fromString("00000000-0000-0000-0000-00A57E401D05");

    // Battery level
    public static final UUID BATTERY_SERVICE_UUID       = UUID.fromString("0000180F-0000-1000-8000-00805F9B34FB");
    public static final UUID BATTERY_UUID               = UUID.fromString("00002A19-0000-1000-8000-00805F9B34FB");

    // Time
    public static final UUID TIME_SET_CHAR              = UUID.fromString("00005001-0000-0000-0000-00A57E401D05");

    // ScreenshotService
    public static final UUID SCREENSHOT_REQUEST         = UUID.fromString("00006001-0000-0000-0000-00A57E401D05");
    public static final UUID SCREENSHOT_CONTENT         = UUID.fromString("00006002-0000-0000-0000-00A57E401D05");

    // MediaService
    public static final UUID MEDIA_TITLE_CHAR           = UUID.fromString("00007001-0000-0000-0000-00A57E401D05");
    public static final UUID MEDIA_ALBUM_CHAR           = UUID.fromString("00007002-0000-0000-0000-00A57E401D05");
    public static final UUID MEDIA_ARTIST_CHAR          = UUID.fromString("00007003-0000-0000-0000-00A57E401D05");
    public static final UUID MEDIA_PLAYING_CHAR         = UUID.fromString("00007004-0000-0000-0000-00A57E401D05");
    public static final UUID MEDIA_COMMANDS_CHAR        = UUID.fromString("00007005-0000-0000-0000-00A57E401D05");
    public static final UUID MEDIA_VOLUME_CHAR          = UUID.fromString("00007006-0000-0000-0000-00A57E401D05");

    // WeatherService
    public static final UUID WEATHER_CITY_CHAR          = UUID.fromString("00008001-0000-0000-0000-00A57E401D05");
    public static final UUID WEATHER_IDS_CHAR           = UUID.fromString("00008002-0000-0000-0000-00A57E401D05");
    public static final UUID WEATHER_MIN_TEMPS_CHAR     = UUID.fromString("00008003-0000-0000-0000-00A57E401D05");
    public static final UUID WEATHER_MAX_TEMPS_CHAR     = UUID.fromString("00008004-0000-0000-0000-00A57E401D05");

    // Notification Service
    public static final UUID NOTIFICATION_SERVICE_UUID  = UUID.fromString("00009071-0000-0000-0000-00A57E401D05");
    public static final UUID NOTIFICATION_UPDATE_CHAR   = UUID.fromString("00009001-0000-0000-0000-00A57E401D05");
    public static final UUID NOTIFICATION_FEEDBACK_CHAR = UUID.fromString("00009002-0000-0000-0000-00A57E401D05");
}
