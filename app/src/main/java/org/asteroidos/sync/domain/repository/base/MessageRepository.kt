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

package org.asteroidos.sync.domain.repository.base

import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.flow.Flow
import org.asteroidos.sync.domain.repository.impl.MessageRepositoryImpl
import org.asteroidos.sync.domain.repository.impl.WatchStatusRepositoryImpl

/**
 * Source of truth for all inter app message communication using flows.
 */
interface MessageRepository {
    /**
     * Emits when a device is selected
     */
    val requestSetDeviceFlow: Flow<BluetoothDevice>

    fun requestSetDevice(device: BluetoothDevice)

    /**
     * Emits when device should be unset
     */
    val requestUnsetDeviceFlow: Flow<Int>

    fun requestUnsetDevice()

    /**
     * Emits when a device update is requested
     */
    val requestUpdateDeviceFlow: Flow<Int>

    fun requestUpdateDevice()

    /**
     * Emits when a device should be connected to
     */
    val requestConnectFlow: Flow<Int>

    fun requestConnect()

    /**
     * Emits when a device should be disconnected from
     */
    val requestDisconnectFlow: Flow<Int>

    fun requestDisconnect()

    /**
     * Emits when a battery life update is requested
     */
    val requestBatteryLifeFlow: Flow<Int>

    fun requestBatteryLife()

    companion object {
        private var repo: MessageRepository? = null

        fun get(): MessageRepository {
            if (repo == null)
                repo = MessageRepositoryImpl()

            return repo!!
        }
    }
}