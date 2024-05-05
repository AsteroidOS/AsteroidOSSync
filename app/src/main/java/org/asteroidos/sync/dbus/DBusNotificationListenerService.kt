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

import android.app.Notification
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import org.asteroidos.sync.NotificationPreferences
import org.asteroidos.sync.NotificationPreferences.NotificationOption.DEFAULT
import org.asteroidos.sync.NotificationPreferences.NotificationOption.NORMAL_VIBRATION
import org.asteroidos.sync.NotificationPreferences.NotificationOption.RINGTONE_VIBRATION
import org.asteroidos.sync.NotificationPreferences.NotificationOption.SILENT_NOTIFICATION
import org.asteroidos.sync.NotificationPreferences.NotificationOption.STRONG_VIBRATION
import org.asteroidos.sync.connectivity.SlirpService
import org.asteroidos.sync.utils.IconToPackageMapper
import org.asteroidos.sync.utils.NotificationParser
import org.freedesktop.Notifications
import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.interfaces.DBusSigHandler
import org.freedesktop.dbus.types.UInt32
import org.freedesktop.dbus.types.Variant

class DBusNotificationListenerService : NotificationListenerService(), DBusSigHandler<Notifications.NotificationClosed?> {
    @Volatile
    private var listenerConnected = false

    private lateinit var connector: DBusConnector

    private lateinit var iconMapper: IconToPackageMapper

    private val mapping: BiMap<String, UInt32> = HashBiMap.create()

    override fun onCreate() {
        iconMapper = IconToPackageMapper(baseContext)
        connector = DBusConnector(SlirpService.SLIRP_DBUS_ADDRESS) // hardcoded (for now)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        if (sbn == null)
            return

        val notification = sbn.notification
        val packageName = sbn.packageName

        val appName: String
        try {
            val pm = applicationContext.packageManager
            val ai = pm.getApplicationInfo(packageName, 0)
            appName = pm.getApplicationLabel(ai).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            return
        }
        if (notification.priority < Notification.PRIORITY_DEFAULT ||
                (notification.flags and Notification.FLAG_ONGOING_EVENT != 0
                        && !allowedOngoingApps.contains(packageName)) ||
                NotificationCompat.getLocalOnly(notification) ||
                NotificationCompat.isGroupSummary(notification)) return

        NotificationPreferences.putPackageToSeen(applicationContext, packageName)
        val notificationOption = NotificationPreferences.getNotificationPreferenceForApp(applicationContext, packageName)
        if (notificationOption == NotificationPreferences.NotificationOption.NO_NOTIFICATIONS) return

        val notifParser = NotificationParser(notification)
        val summary = notifParser.summary
        val body = notifParser.body
        val vibration: String = when (notificationOption) {
            SILENT_NOTIFICATION -> "notif_silent"
            NORMAL_VIBRATION, DEFAULT, null -> "notif_normal"
            STRONG_VIBRATION -> "notif_strong"
            RINGTONE_VIBRATION -> "ringtone"
            else -> throw IllegalArgumentException("Not all options handled")
        }

        connector.acquireDBusConnection { notify: DBusConnection ->
            synchronized(mapping) {
                mapping[sbn.key] = notify.getRemoteObject("org.freedesktop.Notifications", "/org/freedesktop/Notifications", Notifications::class.java)
                        .Notify(appName, mapping.getOrDefault(sbn.key, UInt32(0)), iconMapper.iconForPackage(packageName), summary, body, emptyList(),
                                mutableMapOf(
                                        "x-nemo-feedback" to Variant(vibration),
                                        "x-nemo-preview-body" to Variant(body),
                                        "x-nemo-preview-summary" to Variant(summary),
                                        "urgency" to Variant(3.toByte())) as Map<String, Variant<*>>?, 0)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)

        val key = sbn?.key ?: return
        // Avoid an infinite loop when the user dismisses the notification on the watch
        if (mapping.containsKey(key)) {
            var id: UInt32?
            synchronized(mapping) {
                id = mapping[key]
                mapping.remove(key)
            }
            connector.acquireDBusConnection { notify: DBusConnection ->
                notify.getRemoteObject("org.freedesktop.Notifications", "/org/freedesktop/Notifications", Notifications::class.java)
                        .CloseNotification(id)
            }
        }
    }

    override fun onListenerDisconnected() {
        listenerConnected = false
        connector.unsync()
        // Notification listener disconnected - requesting rebind
        requestRebind(ComponentName(this, NotificationListenerService::class.java))
    }

    override fun onListenerConnected() {
        listenerConnected = true
        connector.sync()

        connector.acquireDBusConnection { notify: DBusConnection -> notify.addSigHandler(Notifications.NotificationClosed::class.java, notify.getRemoteObject("org.freedesktop.Notifications", "/org/freedesktop/Notifications", Notifications::class.java), this@DBusNotificationListenerService) }
    }

    override fun handle(s: Notifications.NotificationClosed?) {
        synchronized(mapping) {
            if (s != null
                    && s.reason.toInt() == 2
                    && mapping.containsValue(s.id)) {
                val key = mapping.inverse()[s.id]
                mapping.inverse().remove(s.id)
                Handler(Looper.getMainLooper()).post { cancelNotification(key) }
            }
        }
    }

    companion object {

        private val allowedOngoingApps = listOf("com.google.android.apps.maps", "org.thoughtcrime.securesms")
    }
}