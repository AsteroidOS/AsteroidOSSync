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

import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import com.google.common.base.Stopwatch
import org.freedesktop.dbus.connections.impl.AndroidDBusConnectionBuilder
import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.exceptions.DBusException
import java.io.IOException

class DBusConnector(val address: String) : IDBusConnectionProvider {

    private val dBusHandlerThread: HandlerThread = HandlerThread("D-Bus connection")

    private val dBusHandler: Handler

    private var dBusConnection: DBusConnection? = null

    private var dBusCbId: Long = 0

    init {
        dBusHandlerThread.start()
        dBusHandler = object: Handler(dBusHandlerThread.looper) {
            override fun handleMessage(msg: Message) {
                when (msg.arg1) {
                    DBUS_HANDLER_MSG_CLOSE -> {
                        if (dBusConnection != null) {
                            dBusConnection!!.disconnect()
                            Log.i("SlirpService", "Closed the D-Bus connection")
                        }
                    }

                    DBUS_HANDLER_MSG_OPEN -> {
                        Log.i("SlirpService", "(Re-)opening a D-Bus connection")
                        val timer = Stopwatch.createStarted()
                        if (dBusConnection != null) {
                            try {
                                dBusConnection!!.connect()
                            } catch (e: IOException) {
                                Log.e("SlirpService", "Failed to establish a D-Bus connection", e)
                            }
                            Log.i("SlirpService", "Done re-opening the D-Bus connection; took " + timer.stop())
                            return
                        }
                        try {
                            dBusConnection = AndroidDBusConnectionBuilder
                                    .forAddress(msg.obj as String).build()
                        } catch (e: DBusException) {
                            Log.e("SlirpService", "Failed to establish a D-Bus connection", e)
                        }
                        Log.i("SlirpService", "Done opening a D-Bus connection; took " + timer.stop())
                    }
                }
            }
        }
    }

    private fun acquireDBusConnectionImpl(dBusConnectionConsumer: (connection: DBusConnection) -> Unit) {
        if (dBusConnection == null) {
            Log.w("SlirpService", "D-Bus connection not set up yet")
            return
        }

        val cbId: Long
        synchronized(this) { cbId = dBusCbId++ }
        val timer = Stopwatch.createStarted()
        Log.d("SlirpService", "D-Bus callback start: $cbId")
        try {
            dBusConnectionConsumer(dBusConnection!!)
        } catch (e: DBusException) {
            Log.e("SlirpService", "D-Bus error", e)
        } catch (e: Throwable) {
            Log.e("SlirpService", "Runtime error in D-Bus callback", e)
        }
        Log.d("SlirpService", "D-Bus callback end: " + cbId + "; took " + timer.stop())
    }

    override fun acquireDBusConnection(dBusConnectionConsumer: (connection: DBusConnection) -> Unit) {
        dBusHandler.post { acquireDBusConnectionImpl(dBusConnectionConsumer) }
    }

    override fun acquireDBusConnectionLater(dBusConnectionConsumer: (connection: DBusConnection) -> Unit, delay: Long) {
        dBusHandler.postDelayed({ acquireDBusConnectionImpl(dBusConnectionConsumer) }, delay)
    }

    override fun sync() {
        val msg = Message()
        msg.arg1 = DBUS_HANDLER_MSG_OPEN
        msg.obj = address
        dBusHandler.sendMessage(msg)
    }

    override fun unsync() {
        val msg = Message()
        msg.arg1 = DBUS_HANDLER_MSG_CLOSE
        dBusHandler.sendMessage(msg)
    }

    companion object {
        private const val DBUS_HANDLER_MSG_CLOSE = 0
        private const val DBUS_HANDLER_MSG_OPEN = 1
    }
}