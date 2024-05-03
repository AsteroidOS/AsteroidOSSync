package org.mpris;

import java.util.List;
import org.freedesktop.dbus.TypeRef;
import org.freedesktop.dbus.annotations.DBusBoundProperty;
import org.freedesktop.dbus.annotations.DBusProperty;
import org.freedesktop.dbus.annotations.DBusProperty.Access;
import org.freedesktop.dbus.interfaces.DBusInterface;

/**
 * Auto-generated class.
 */
public interface MediaPlayer2 extends DBusInterface {

    @DBusBoundProperty(name = "CanQuit", access = Access.READ)
    public boolean canQuit();

    @DBusBoundProperty(name = "Fullscreen", access = Access.READ)
    public boolean isFullscreen();
    @DBusBoundProperty(name = "Fullscreen", access = Access.WRITE)
    public void setFullscreen(boolean _property);

    @DBusBoundProperty(name = "CanSetFullscreen", access = Access.READ)
    public boolean canSetFullscreen();

    @DBusBoundProperty(name = "CanRaise", access = Access.READ)
    public boolean canRaise();

    @DBusBoundProperty(name = "HasTrackList", access = Access.READ)
    public boolean hasTrackList();

    @DBusBoundProperty(name = "Identity", access = Access.READ)
    public String getIdentity();

    @DBusBoundProperty(name = "SupportedUriSchemes", access = Access.READ)
    public List<String> getSupportedUriSchemes();

    @DBusBoundProperty(name = "SupportedMimeTypes", access = Access.READ)
    public List<String> getSupportedMimeTypes();



    public void Raise();
    public void Quit();


    public static interface PropertySupportedUriSchemesType extends TypeRef<List<String>> {




    }

    public static interface PropertySupportedMimeTypesType extends TypeRef<List<String>> {




    }
}
