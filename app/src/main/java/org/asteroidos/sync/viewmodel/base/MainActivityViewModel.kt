/*
 * AsteroidOSSync
 * Copyright (c) 2023 AsteroidOS
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

package org.asteroidos.sync.viewmodel.base

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import org.asteroidos.sync.asteroid.IAsteroidDevice
import java.util.function.Consumer

/**
 * View model for the main activity
 */
abstract class MainActivityViewModel : ViewModel() {

    /**
     * Listen to changes to local name
     */
    abstract fun onWatchLocalNameChanged(consumer: Consumer<String>)

    /**
     * Listen to connection state changes
     */
    abstract fun onWatchConnectionStateChanged(consumer: Consumer<IAsteroidDevice.ConnectionState>)

    /**
     * Listen to battery percentage changes
     */
    abstract fun onWatchBatteryPercentageChanged(consumer: Consumer<Int>)

    /**
     * Request the device to be disconnected
     */
    abstract fun requestDisconnect()

    /**
     * Request to connect to the device
     */
    abstract fun requestConnect()

    /**
     * Request an update from the device
     */
    abstract fun requestUpdate()

    /**
     * Request the device to be unset
     */
    abstract fun requestUnsetDevice()

    /**
     * Select device
     */
    abstract fun onDefaultDeviceSelected(mDevice: BluetoothDevice)

    /**
     * Request battery level of the device
     */
    abstract fun requestBatteryLevel()
}