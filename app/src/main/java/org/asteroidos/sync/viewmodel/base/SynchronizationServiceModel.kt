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
import androidx.core.util.Consumer
import kotlinx.coroutines.Runnable
import org.asteroidos.sync.asteroid.IAsteroidDevice
import org.asteroidos.sync.asteroid.IAsteroidDevice.ConnectionState

/**
 * Model for the synchronization service to bind to kotlin flows.
 * In the future this can be removed.
 */
interface SynchronizationServiceModel {

    /**
     * Listen to connection requests
     */
    fun onConnectRequested(consumer: Runnable)

    /**
     * Listen to disconnect requests
     */
    fun onDisconnectRequested(consumer: Runnable)

    /**
     * Listen to battery life requests
     */
    fun onBatteryLifeRequested(consumer: Runnable)

    /**
     * Listen to device set requests
     */
    fun onDeviceSetRequested(consumer: Consumer<BluetoothDevice>)

    /**
     * Listen to device unset requests
     */
    fun onDeviceUnsetRequested(consumer: Runnable)

    /**
     * Listen to device update requests
     */
    fun onDeviceUpdateRequested(consumer: Runnable)

    /**
     * Set the local name
     */
    fun setLocalName(name: String)

    /**
     * Set the device connection state
     */
    fun setConnectionState(state: ConnectionState)

    /**
     * Set the battery percentage
     */
    fun setBatteryPercentage(percentage: Int)
}