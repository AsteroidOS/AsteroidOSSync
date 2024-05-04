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
package org.asteroidos.sync.dbus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import org.asteroidos.sync.NotificationPreferences
import org.asteroidos.sync.NotificationPreferences.NotificationOption
import org.asteroidos.sync.services.INotificationHandler
import org.freedesktop.Notifications
import org.freedesktop.Notifications.NotificationClosed
import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.interfaces.DBusSigHandler
import org.freedesktop.dbus.types.UInt32
import org.freedesktop.dbus.types.Variant
import java.util.Arrays
import java.util.Map
import java.util.Objects

class NotificationService(private val mCtx: Context, private val connectionProvider: IDBusConnectionProvider) : INotificationHandler, DBusSigHandler<NotificationClosed?> {
    private val mapping: BiMap<String, UInt32> = HashBiMap.create()
    private var mNReceiver: NotificationReceiver? = null

    override fun postNotification(context: Context, intent: Intent) {
        val event = intent.getStringExtra("event")
        if (event == "posted") {
            val packageName = intent.getStringExtra("packageName")
            NotificationPreferences.putPackageToSeen(context, packageName)
            val notificationOption = NotificationPreferences.getNotificationPreferenceForApp(context, packageName)
            if (notificationOption == NotificationOption.NO_NOTIFICATIONS) return
            val key = intent.getStringExtra("key")!!
            val appName = intent.getStringExtra("appName")
            val appIcon = intent.getStringExtra("appIcon")
            val summary = intent.getStringExtra("summary")
            val body = intent.getStringExtra("body")
            val vibration: String
            vibration = if (notificationOption == NotificationOption.SILENT_NOTIFICATION) "notif_silent" else if (notificationOption == null || notificationOption == NotificationOption.NORMAL_VIBRATION || notificationOption == NotificationOption.DEFAULT) "notif_normal" else if (notificationOption == NotificationOption.STRONG_VIBRATION) "notif_strong" else if (notificationOption == NotificationOption.RINGTONE_VIBRATION) "ringtone" else throw IllegalArgumentException("Not all options handled")
            connectionProvider.acquireDBusConnection { notify: DBusConnection ->
                synchronized(mapping) {
                    mapping[key] = notify.getRemoteObject("org.freedesktop.Notifications", "/org/freedesktop/Notifications", Notifications::class.java)
                            .Notify(appName, mapping.getOrDefault(key, UInt32(0)), appIcon, summary, body, emptyList(),
                                    Map.of<String, Variant<*>>(
                                            "x-nemo-feedback", Variant(vibration),
                                            "x-nemo-preview-body", Variant(body),
                                            "x-nemo-preview-summary", Variant(summary),
                                            "urgency", Variant(3.toByte())), 0)
                }
            }
        } else if (event == "removed") {
            val key = Objects.requireNonNull(intent.getStringExtra("key"))
            // Avoid an infinite loop when the user dismisses the notification on the watch
            if (mapping.containsKey(key)) {
                var id: UInt32?
                synchronized(mapping) {
                    id = mapping[key]
                    mapping.remove(key)
                }
                connectionProvider.acquireDBusConnection { notify: DBusConnection ->
                    notify.getRemoteObject("org.freedesktop.Notifications", "/org/freedesktop/Notifications", Notifications::class.java)
                            .CloseNotification(id)
                }
            }
        }
    }

    override fun handle(s: NotificationClosed?) {
        synchronized(mapping) {
            if (s != null
                    && s.reason.toInt() == 2
                    && mapping.containsValue(s.id)) {
                val dismiss = Intent("org.asteroidos.sync.NOTIFICATION_LISTENER_SERVICE")
                dismiss.putExtra("command", "dismiss")
                dismiss.putExtra("key", mapping.inverse()[s.id])
                mapping.inverse().remove(s.id)
                mCtx.sendBroadcast(dismiss)
            }
        }
    }

    override fun sync() {
        if (mNReceiver == null) {
            val filter = IntentFilter()
            filter.addAction("org.asteroidos.sync.NOTIFICATION_LISTENER")
            mNReceiver = NotificationReceiver()
            mCtx.registerReceiver(mNReceiver, filter)
            val i = Intent("org.asteroidos.sync.NOTIFICATION_LISTENER_SERVICE")
            i.putExtra("command", "refresh")
            mCtx.sendBroadcast(i)
        }
        connectionProvider.acquireDBusConnection { notify: DBusConnection -> notify.addSigHandler(NotificationClosed::class.java, notify.getRemoteObject("org.freedesktop.Notifications", "/org/freedesktop/Notifications", Notifications::class.java), this@NotificationService) }
    }

    override fun unsync() {
        connectionProvider.acquireDBusConnection { notify: DBusConnection -> notify.removeSigHandler(NotificationClosed::class.java, notify.getRemoteObject("org.freedesktop.Notifications", "/org/freedesktop/Notifications", Notifications::class.java), this@NotificationService) }
        if (mNReceiver != null) {
            try {
                mCtx.unregisterReceiver(mNReceiver)
            } catch (ignored: IllegalArgumentException) {
            }
            mNReceiver = null
        }
    }

    internal inner class NotificationReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            postNotification(context, intent)
        }
    }
}