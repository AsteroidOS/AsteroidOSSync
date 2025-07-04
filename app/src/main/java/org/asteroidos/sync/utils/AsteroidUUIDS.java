/*
 * AsteroidOSSync
 * Copyright (c) 2023 AsteroidOS
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

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
    public static final UUID TIME_SERVICE_UUID          = UUID.fromString("00005071-0000-0000-0000-00A57E401D05");
    public static final UUID TIME_SET_CHAR              = UUID.fromString("00005001-0000-0000-0000-00A57E401D05");

    // ScreenshotService
    public static final UUID SCREENSHOT_SERVICE_UUID    = UUID.fromString("00006071-0000-0000-0000-00A57E401D05");
    public static final UUID SCREENSHOT_REQUEST         = UUID.fromString("00006001-0000-0000-0000-00A57E401D05");
    public static final UUID SCREENSHOT_CONTENT         = UUID.fromString("00006002-0000-0000-0000-00A57E401D05");

    // MediaService
    public static final UUID MEDIA_SERVICE_UUID         = UUID.fromString("00007071-0000-0000-0000-00A57E401D05");
    public static final UUID MEDIA_TITLE_CHAR           = UUID.fromString("00007001-0000-0000-0000-00A57E401D05");
    public static final UUID MEDIA_ALBUM_CHAR           = UUID.fromString("00007002-0000-0000-0000-00A57E401D05");
    public static final UUID MEDIA_ARTIST_CHAR          = UUID.fromString("00007003-0000-0000-0000-00A57E401D05");
    public static final UUID MEDIA_PLAYING_CHAR         = UUID.fromString("00007004-0000-0000-0000-00A57E401D05");
    public static final UUID MEDIA_COMMANDS_CHAR        = UUID.fromString("00007005-0000-0000-0000-00A57E401D05");
    public static final UUID MEDIA_VOLUME_CHAR          = UUID.fromString("00007006-0000-0000-0000-00A57E401D05");

    // WeatherService
    public static final UUID WEATHER_SERVICE_UUID       = UUID.fromString("00008071-0000-0000-0000-00A57E401D05");
    public static final UUID WEATHER_CITY_CHAR          = UUID.fromString("00008001-0000-0000-0000-00A57E401D05");
    public static final UUID WEATHER_IDS_CHAR           = UUID.fromString("00008002-0000-0000-0000-00A57E401D05");
    public static final UUID WEATHER_MIN_TEMPS_CHAR     = UUID.fromString("00008003-0000-0000-0000-00A57E401D05");
    public static final UUID WEATHER_MAX_TEMPS_CHAR     = UUID.fromString("00008004-0000-0000-0000-00A57E401D05");

    // Notification Service
    public static final UUID NOTIFICATION_SERVICE_UUID  = UUID.fromString("00009071-0000-0000-0000-00A57E401D05");
    public static final UUID NOTIFICATION_UPDATE_CHAR   = UUID.fromString("00009001-0000-0000-0000-00A57E401D05");
    public static final UUID NOTIFICATION_FEEDBACK_CHAR = UUID.fromString("00009002-0000-0000-0000-00A57E401D05");

    // SlirpService
    public static final UUID SLIRP_SERVICE_UUID         = UUID.fromString("0000A071-0000-0000-0000-00A57E401D05");
    public static final UUID SLIRP_OUTGOING_CHAR        = UUID.fromString("0000A001-0000-0000-0000-00A57E401D05");
    public static final UUID SLIRP_INCOMING_CHAR        = UUID.fromString("0000A002-0000-0000-0000-00A57E401D05");
}
