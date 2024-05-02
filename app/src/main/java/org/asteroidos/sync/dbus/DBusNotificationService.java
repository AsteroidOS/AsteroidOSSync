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

package org.asteroidos.sync.dbus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import org.asteroidos.sync.NotificationPreferences;
import org.freedesktop.Notifications;
import org.asteroidos.sync.services.INotificationHandler;
import org.freedesktop.dbus.DBusSigHandler;
import org.freedesktop.dbus.UInt32;
import org.freedesktop.dbus.Variant;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DBusNotificationService implements INotificationHandler, DBusSigHandler<Notifications.NotificationClosed> {
    private final IDBusConnectionProvider connectionProvider;

    private static final HashFunction murmur32 = Hashing.murmur3_32_fixed(0);

    private final BiMap<String, UInt32> mapping = HashBiMap.create();

    private Context mCtx;

    private NotificationReceiver mNReceiver;

    public DBusNotificationService(Context ctx, IDBusConnectionProvider connectionProvider) {
        this.mCtx = ctx;
        this.connectionProvider = connectionProvider;

        connectionProvider.acquireDBusConnectionLater(notify -> Log.w("DBusNotificationService", Arrays.toString(notify.getNames())), 1000);
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

            String key = Objects.requireNonNull(intent.getStringExtra("key"));
            String appName = intent.getStringExtra("appName");
            String appIcon = intent.getStringExtra("appIcon");
            String summary = intent.getStringExtra("summary");
            String body = intent.getStringExtra("body");
            String vibration;
            if (notificationOption == NotificationPreferences.NotificationOption.SILENT_NOTIFICATION)
                vibration = "notif_silent";
            else if (notificationOption == null
                    || notificationOption == NotificationPreferences.NotificationOption.NORMAL_VIBRATION
                    || notificationOption == NotificationPreferences.NotificationOption.DEFAULT)
                vibration = "notif_normal";
            else if (notificationOption == NotificationPreferences.NotificationOption.STRONG_VIBRATION)
                vibration = "notif_strong";
            else if (notificationOption == NotificationPreferences.NotificationOption.RINGTONE_VIBRATION)
                vibration = "ringtone";
            else
                throw new IllegalArgumentException("Not all options handled");

            connectionProvider.acquireDBusConnectionLater(notify -> {
                synchronized (mapping) {
                    mapping.put(key, notify.getRemoteObject("org.freedesktop.Notifications", "/org/freedesktop/Notifications", Notifications.class)
                            .Notify(appName, mapping.getOrDefault(key, new UInt32(0)), appIcon, summary, body, Collections.emptyList(),
                                    Map.of(
                                            "x-nemo-feedback", new Variant<>(vibration),
                                            "x-nemo-preview-body", new Variant<>(body),
                                            "x-nemo-preview-summary", new Variant<>(summary),
                                            "urgency", new Variant<>((byte) 3)), 0));
                }
            }, 500);
        } else if (Objects.equals(event, "removed")) {
            String key = Objects.requireNonNull(intent.getStringExtra("key"));
            // Avoid an infinite loop when the user dismisses the notification on the watch
            if (mapping.containsKey(key)) {
                UInt32 id;
                synchronized (mapping) {
                    id = mapping.get(key);
                    mapping.remove(key);
                }
                connectionProvider.acquireDBusConnectionLater(notify -> notify.getRemoteObject("org.freedesktop.Notifications", "/org/freedesktop/Notifications", Notifications.class)
                        .CloseNotification(id), 500);
            }
        }
    }

    @Override
    public void handle(Notifications.NotificationClosed s) {
        synchronized (mapping) {
            if (s.reason == Notifications.NotificationClosed.REASON_DISMISSED_BY_USER
                    && mapping.containsValue(s.id)) {
                Intent dismiss = new Intent("org.asteroidos.sync.NOTIFICATION_LISTENER_SERVICE");
                dismiss.putExtra("command", "dismiss");
                dismiss.putExtra("key", mapping.inverse().get(s.id));

                mapping.inverse().remove(s.id);
                mCtx.sendBroadcast(dismiss);
            }
        }
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
        connectionProvider.acquireDBusConnection(notify -> notify.addSigHandler(Notifications.NotificationClosed.class, notify.getRemoteObject("org.freedesktop.Notifications", "/org/freedesktop/Notifications", Notifications.class), DBusNotificationService.this));
    }

    @Override
    public void unsync() {
        connectionProvider.acquireDBusConnection(notify -> notify.removeSigHandler(Notifications.NotificationClosed.class, notify.getRemoteObject("org.freedesktop.Notifications", "/org/freedesktop/Notifications", Notifications.class), DBusNotificationService.this));
        if (mNReceiver != null) {
            try {
                mCtx.unregisterReceiver(mNReceiver);
            } catch (IllegalArgumentException ignored) {
            }
            mNReceiver = null;
        }
    }

    class NotificationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            postNotification(context, intent);
        }
    }
}
