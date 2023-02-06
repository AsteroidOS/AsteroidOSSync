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

package org.asteroidos.sync;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class NotificationPreferences {
    public enum NotificationOption {
        DEFAULT(0),
        NO_NOTIFICATIONS(1),
        SILENT_NOTIFICATION(2),
        NORMAL_VIBRATION(3),
        STRONG_VIBRATION(4),
        RINGTONE_VIBRATION(5);

        private int value;
        NotificationOption(int value) {
            this.value = value;
        }
        public int asInt() {
            return this.value;
        }

        public static NotificationOption fromInt(int x) {
            switch (x) {
                case 0: return DEFAULT;
                case 1: return NO_NOTIFICATIONS;
                case 2: return SILENT_NOTIFICATION;
                case 3: return NORMAL_VIBRATION;
                case 4: return STRONG_VIBRATION;
                case 5: return RINGTONE_VIBRATION;
            }
            throw new IllegalArgumentException("No such NotificationOption: " + x);
        }
    }

    private static final String PREFS_NAME = "NotificationPreferences";
    private static final String PREFS_NOTIFICATIONS = "notifications";
    private static final String PREFS_SEEN_PACKAGES = "seenPackages";

    private static Map<String, NotificationOption> getOptionMap(Context context) {
        SharedPreferences prefs = getPrefs(context);
        String notificationPrefsAsString = prefs.getString(PREFS_NOTIFICATIONS, "{}");
        Gson gson = new Gson();
        Type notificationPrefs = new TypeToken<Map<String, NotificationOption>>(){}.getType();
        return gson.fromJson(notificationPrefsAsString, notificationPrefs);
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static NotificationOption getNotificationPreferenceForApp(Context context, String packageName) {
        NotificationOption value = getOptionMap(context).get(packageName);
        return value == null ? NotificationOption.DEFAULT : value;
    }

    public static void saveNotificationPreferenceForApp(Context context, String packageName, int value) {
        Map<String,NotificationOption> map = getOptionMap(context);
        NotificationOption option = NotificationOption.fromInt(value);

        // this function gets fired a lot on scroll, don't save defaults if there's nothing set
        if (map.get(packageName) == null && option == NotificationOption.DEFAULT)
            return;

        map.put(packageName, option);
        SharedPreferences.Editor editor = getPrefs(context).edit();
        String jsonString = new Gson().toJson(map);
        editor.putString(PREFS_NOTIFICATIONS, jsonString);
        editor.apply();
    }

    public static List<String> seenPackageNames(Context context) {
        String asString = getPrefs(context).getString(PREFS_SEEN_PACKAGES, "[]");
        String[] asArray = new Gson().fromJson(asString, String[].class);
        return Arrays.asList(asArray);
    }

    public static void putPackageToSeen(Context context, String packageName) {
        List<String> list = seenPackageNames(context);
        if (list.contains(packageName))
            return;
        ArrayList<String> array = new ArrayList<>(list);
        array.add(packageName);

        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putString(PREFS_SEEN_PACKAGES, new Gson().toJson(array));
        editor.apply();
    }
}
