package org.mpris;

import java.util.List;
import org.freedesktop.dbus.TypeRef;
import org.freedesktop.dbus.annotations.DBusProperty;
import org.freedesktop.dbus.annotations.DBusProperty.Access;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.Properties;

/**
 * Auto-generated class.
 */
@DBusProperty(name = "CanQuit", type = Boolean.class, access = Access.READ)
@DBusProperty(name = "Fullscreen", type = Boolean.class, access = Access.READ_WRITE)
@DBusProperty(name = "CanSetFullscreen", type = Boolean.class, access = Access.READ)
@DBusProperty(name = "CanRaise", type = Boolean.class, access = Access.READ)
@DBusProperty(name = "HasTrackList", type = Boolean.class, access = Access.READ)
@DBusProperty(name = "Identity", type = String.class, access = Access.READ)
@DBusProperty(name = "DesktopEntry", type = String.class, access = Access.READ)
@DBusProperty(name = "SupportedUriSchemes", type = MediaPlayer2.PropertySupportedUriSchemesType.class, access = Access.READ)
@DBusProperty(name = "SupportedMimeTypes", type = MediaPlayer2.PropertySupportedMimeTypesType.class, access = Access.READ)
public interface MediaPlayer2 extends DBusInterface, Properties {


    public void Raise();
    public void Quit();


    public static interface PropertySupportedUriSchemesType extends TypeRef<List<String>> {




    }

    public static interface PropertySupportedMimeTypesType extends TypeRef<List<String>> {




    }
}
