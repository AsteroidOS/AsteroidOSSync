/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus.test;

import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.exceptions.DBusException;

public interface TwoPartInterface extends DBusInterface
{
   public TwoPartObject getNew();
   public class TwoPartSignal extends DBusSignal
   {
      public final TwoPartObject o;
      public TwoPartSignal(String path, TwoPartObject o) throws DBusException
      {
         super (path, o);
         this.o = o;
      }
   }
}
