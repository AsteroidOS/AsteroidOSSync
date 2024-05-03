package org.mpris.mediaplayer2;

import java.util.Map;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.TypeRef;
import org.freedesktop.dbus.annotations.DBusBoundProperty;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusProperty;
import org.freedesktop.dbus.annotations.DBusProperty.Access;
import org.freedesktop.dbus.annotations.PropertiesEmitsChangedSignal;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.types.Variant;

/**
 * Auto-generated class.
 */
@DBusInterfaceName("org.mpris.MediaPlayer2.Player")
public interface Player extends DBusInterface {

    @DBusBoundProperty(name = "PlaybackStatus", access = Access.READ)
    public String getPlaybackStatus();

    @DBusBoundProperty(name = "LoopStatus", access = Access.READ)
    public String getLoopStatus();
    @DBusBoundProperty(name = "LoopStatus", access = Access.WRITE)
    public void setLoopStatus(String _property);

    @DBusBoundProperty(name = "Rate", access = Access.READ)
    public double getRate();
    @DBusBoundProperty(name = "Rate", access = Access.WRITE)
    public void setRate(double _property);

    @DBusBoundProperty(name = "Shuffle", access = Access.READ)
    public boolean isShuffle();
    @DBusBoundProperty(name = "Shuffle", access = Access.WRITE)
    public void setShuffle(boolean _property);

    @DBusBoundProperty(name = "Metadata", access = Access.READ)
    public Map<String, Variant<?>> getMetadata();

    @DBusBoundProperty(name = "Volume", access = Access.READ)
    public double getVolume();
    @DBusBoundProperty(name = "Volume", access = Access.WRITE)
    public void setVolume(double _property);

    @DBusBoundProperty(name = "Position", access = Access.READ)
    public long getPosition();

    @DBusBoundProperty(name = "MinimumRate", access = Access.READ)
    public double getMinimumRate();

    @DBusBoundProperty(name = "MaximumRate", access = Access.READ)
    public double getMaximumRate();

    @DBusBoundProperty(name = "CanGoNext", access = Access.READ)
    public boolean canGoNext();

    @DBusBoundProperty(name = "CanGoPrevious", access = Access.READ)
    public boolean canGoPrevious();

    @DBusBoundProperty(name = "CanPlay", access = Access.READ)
    public boolean canPlay();

    @DBusBoundProperty(name = "CanPause", access = Access.READ)
    public boolean canPause();

    @DBusBoundProperty(name = "CanSeek", access = Access.READ)
    public boolean canSeek();

    @DBusBoundProperty(name = "CanControl", access = Access.READ)
    public boolean canControl();



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
