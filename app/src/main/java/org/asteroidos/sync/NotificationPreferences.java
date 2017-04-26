package org.asteroidos.sync;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Map;

public class NotificationPreferences {
    public enum NotificationOption {
        DEFAULT(0),
        NO_NOTIFICATIONS(1),
        SILENT_NOTIFICATION(2),
        NORMAL_VIBRATION(3),
        STRONG_VIBRATION(4);

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
            }
            throw new IllegalArgumentException("No such NotificationOption: " + x);
        }
    }

    public static final String PREFS_NAME = "NotificationPreferences";
    public static final String PREFS_NOTIFICATIONS = "notifications";



    private static Map<String, NotificationOption> getOptionMap(Context context) {
        SharedPreferences prefs = getPrefs(context);
        String notificationPrefsAsString = prefs.getString(PREFS_NOTIFICATIONS, "{}");
        Gson gson = new Gson();
        Type notificationPrefs = new TypeToken<Map<String, NotificationOption>>(){}.getType();
        Map<String, NotificationOption> map = gson.fromJson(notificationPrefsAsString, notificationPrefs);
        return map;
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
}
