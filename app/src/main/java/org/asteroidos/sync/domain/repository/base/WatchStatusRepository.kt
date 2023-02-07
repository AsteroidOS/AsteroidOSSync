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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.asteroidos.sync.asteroid.IAsteroidDevice
import org.asteroidos.sync.asteroid.IAsteroidDevice.ConnectionState
import org.asteroidos.sync.domain.repository.impl.WatchStatusRepositoryImpl

/**
 * Source of truth on watch status
 */
interface WatchStatusRepository {
    /**
     * Battery percentage state
     */
    val batteryPercentage: StateFlow<Int>

    fun setBatteryPercentage(batteryPercentage: Int)

    /**
     * Connection state
     */
    val connectionState: StateFlow<ConnectionState>
    fun setConnectionState(state: ConnectionState)

    /**
     * Device name
     */
    val deviceName: StateFlow<String>
    fun setDeviceName(name: String)

    companion object {
        private var repo: WatchStatusRepository? = null

        fun get(): WatchStatusRepository {
            if (repo == null)
                repo = WatchStatusRepositoryImpl()

            return repo!!
        }
    }
}