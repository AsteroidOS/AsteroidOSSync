/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus.test;

import org.freedesktop.dbus.Position;
import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.Variant;
import org.freedesktop.dbus.exceptions.DBusException;

import java.util.List;

public final class TestStruct2 extends Struct
{
   @Position(0)
   public final List<String> a;
   @Position(1)
   public final Variant<? extends Object> b;
   public TestStruct2(List<String> a, Variant<? extends Object> b) throws DBusException
   {
      this.a = a;
      this.b = b;
   }
}
