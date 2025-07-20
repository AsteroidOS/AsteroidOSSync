package org.mpris.mediaplayer2;

import java.util.Map;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.TypeRef;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusProperty;
import org.freedesktop.dbus.annotations.DBusProperty.Access;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.types.Variant;

/**
 * Auto-generated class.
 */
@DBusInterfaceName("org.mpris.MediaPlayer2.Player")
@DBusProperty(name = "PlaybackStatus", type = String.class, access = Access.READ)
@DBusProperty(name = "LoopStatus", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "Rate", type = Double.class, access = Access.READ_WRITE)
@DBusProperty(name = "Shuffle", type = Boolean.class, access = Access.READ_WRITE)
@DBusProperty(name = "Metadata", type = Player.PropertyMetadataType.class, access = Access.READ)
@DBusProperty(name = "Volume", type = Double.class, access = Access.READ_WRITE)
@DBusProperty(name = "Position", type = Long.class, access = Access.READ)
@DBusProperty(name = "MinimumRate", type = Double.class, access = Access.READ)
@DBusProperty(name = "MaximumRate", type = Double.class, access = Access.READ)
@DBusProperty(name = "CanGoNext", type = Boolean.class, access = Access.READ)
@DBusProperty(name = "CanGoPrevious", type = Boolean.class, access = Access.READ)
@DBusProperty(name = "CanPlay", type = Boolean.class, access = Access.READ)
@DBusProperty(name = "CanPause", type = Boolean.class, access = Access.READ)
@DBusProperty(name = "CanSeek", type = Boolean.class, access = Access.READ)
@DBusProperty(name = "CanControl", type = Boolean.class, access = Access.READ)
public interface Player extends DBusInterface, Properties {


    public void Next();
    public void Previous();
    public void Pause();
    public void PlayPause();
    public void Stop();
    public void Play();
    public void Seek(long Offset);
    public void SetPosition(DBusPath TrackId, long Position);
    public void OpenUri(String Uri);


    public static class Seeked extends DBusSignal {

        private final long Position;

        public Seeked(String _path, long _Position) throws DBusException {
            super(_path, _Position);
            this.Position = _Position;
        }


        public long getPosition() {
            return Position;
        }


    }

    public static interface PropertyMetadataType extends TypeRef<Map<String, Variant>> {




    }
}
