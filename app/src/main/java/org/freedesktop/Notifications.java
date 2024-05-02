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

package org.freedesktop;

import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.UInt32;
import org.freedesktop.dbus.Variant;
import org.freedesktop.dbus.exceptions.DBusException;

import java.util.List;
import java.util.Map;

public interface Notifications extends DBusInterface {

    public static class NotificationClosed extends DBusSignal {
        public static final int REASON_EXPIRED = 1;
        public static final int REASON_DISMISSED_BY_USER = 2;
        public static final int REASON_CLOSE_NOTIFICATION_CALLED = 3;
        public static final int REASON_RESERVED = 4;

        public final UInt32 id;
        public final int reason;
        public NotificationClosed(String path, UInt32 id, UInt32 reason) throws DBusException {
            super(path, id, reason);
            this.id = id;
            this.reason = reason.intValue();
        }
    }

    public List<String> GetCapabilities();

    public UInt32 Notify(String app_name, UInt32 replaces_id, String app_icon, String summary, String body, List<String> actions, Map<String, Variant> hints, int expire_timeout);

    public void CloseNotification(UInt32 id);
}
