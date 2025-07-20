package org.freedesktop;

import java.util.List;
import java.util.Map;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;

/**
 * Auto-generated class.
 */
public interface Notifications extends DBusInterface {


    public List<String> GetCapabilities();
    public UInt32 Notify(String appName, UInt32 replacesId, String appIcon, String summary, String body, List<String> actions, Map<String, Variant<?>> hints, int expireTimeout);
    public void CloseNotification(UInt32 id);
    public GetServerInformationTuple GetServerInformation();
    public List<GetNotificationsStruct> GetNotifications(String appName);


    public static class NotificationClosed extends DBusSignal {

        private final UInt32 id;
        private final UInt32 reason;

        public NotificationClosed(String _path, UInt32 _id, UInt32 _reason) throws DBusException {
            super(_path, _id, _reason);
            this.id = _id;
            this.reason = _reason;
        }


        public UInt32 getId() {
            return id;
        }

        public UInt32 getReason() {
            return reason;
        }


    }

    public static class ActionInvoked extends DBusSignal {

        private final UInt32 id;
        private final String actionKey;

        public ActionInvoked(String _path, UInt32 _id, String _actionKey) throws DBusException {
            super(_path, _id, _actionKey);
            this.id = _id;
            this.actionKey = _actionKey;
        }


        public UInt32 getId() {
            return id;
        }

        public String getActionKey() {
            return actionKey;
        }


    }
}
