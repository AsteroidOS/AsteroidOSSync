/*
 * AsteroidOSSync
 * Copyright (c) 2024 AsteroidOS
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

package org.asteroidos.sync.connectivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import org.asteroidos.sync.NotificationPreferences;
import org.asteroidos.sync.asteroid.IAsteroidDevice;
import org.asteroidos.sync.dataobjects.Notification;
import org.asteroidos.sync.services.INotificationHandler;
import org.asteroidos.sync.utils.AsteroidUUIDS;

import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

public class NotificationService implements IConnectivityService, INotificationHandler {

    public static final String TAG = NotificationService.class.toString();
    private final Context mCtx;
    private final IAsteroidDevice mDevice;
    private NotificationReceiver mNReceiver;

    public NotificationService(Context ctx, IAsteroidDevice device) {
        this.mDevice = device;
        this.mCtx = ctx;
    }

    @Override
    public void sync() {
        if (mNReceiver == null) {
            IntentFilter filter = new IntentFilter();
            filter.addAction("org.asteroidos.sync.NOTIFICATION_LISTENER");
            mNReceiver = new NotificationReceiver();
            mCtx.registerReceiver(mNReceiver, filter);

            Intent i = new Intent("org.asteroidos.sync.NOTIFICATION_LISTENER_SERVICE");
            i.putExtra("command", "refresh");
            mCtx.sendBroadcast(i);
        }
    }

    @Override
    public void unsync() {
        if (mNReceiver != null) {
            try {
                mCtx.unregisterReceiver(mNReceiver);
            } catch (IllegalArgumentException ignored) {
            }
            mNReceiver = null;
        }
    }

    @Override
    public final HashMap<UUID, Direction> getCharacteristicUUIDs() {
        HashMap<UUID, Direction> chars = new HashMap<>();
        chars.put(AsteroidUUIDS.NOTIFICATION_UPDATE_CHAR, Direction.TO_WATCH);
        chars.put(AsteroidUUIDS.NOTIFICATION_FEEDBACK_CHAR, Direction.FROM_WATCH);
        return chars;
    }

    @Override
    public final UUID getServiceUUID() {
        return AsteroidUUIDS.NOTIFICATION_SERVICE_UUID;
    }

    @Override
    public void postNotification(Context context, Intent intent) {
        String event = intent.getStringExtra("event");
        if (Objects.equals(event, "posted")) {
            String packageName = intent.getStringExtra("packageName");
            NotificationPreferences.putPackageToSeen(context, packageName);
            NotificationPreferences.NotificationOption notificationOption =
                    NotificationPreferences.getNotificationPreferenceForApp(context, packageName);
            if (notificationOption == NotificationPreferences.NotificationOption.NO_NOTIFICATIONS)
                return;

            int id = intent.getIntExtra("id", 0);
            String appName = intent.getStringExtra("appName");
            String appIcon = intent.getStringExtra("appIcon");
            String summary = intent.getStringExtra("summary");
            String body = intent.getStringExtra("body");
            String vibration;
            if (notificationOption == NotificationPreferences.NotificationOption.SILENT_NOTIFICATION)
                vibration = "none";
            else if (notificationOption == null
                    || notificationOption == NotificationPreferences.NotificationOption.NORMAL_VIBRATION
                    || notificationOption == NotificationPreferences.NotificationOption.DEFAULT)
                vibration = "normal";
            else if (notificationOption == NotificationPreferences.NotificationOption.STRONG_VIBRATION)
                vibration = "strong";
            else if(notificationOption == NotificationPreferences.NotificationOption.RINGTONE_VIBRATION)
                vibration = "ringtone";
            else
                throw new IllegalArgumentException("Not all options handled");

            if(intent.hasExtra("vibration"))
                vibration = intent.getStringExtra("vibration");

            Notification notification = new Notification(
                    Notification.MsgType.POSTED,
                    packageName,
                    id,
                    appName,
                    appIcon,
                    summary,
                    body,
                    vibration);

            mDevice.send(AsteroidUUIDS.NOTIFICATION_UPDATE_CHAR, notification.toBytes(), NotificationService.this);
        } else if (Objects.equals(event, "removed")) {
            int id = intent.getIntExtra("id", 0);

            mDevice.send(AsteroidUUIDS.NOTIFICATION_UPDATE_CHAR, new Notification(Notification.MsgType.REMOVED, id).toBytes(), NotificationService.this);
        }
    }

    class NotificationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            postNotification(context, intent);
        }
    }
}
