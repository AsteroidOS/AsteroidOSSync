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

package org.asteroidos.sync.viewmodel.impl

import android.bluetooth.BluetoothDevice
import androidx.core.util.Consumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import org.asteroidos.sync.asteroid.IAsteroidDevice.ConnectionState
import org.asteroidos.sync.domain.repository.base.MessageRepository
import org.asteroidos.sync.domain.repository.base.WatchStatusRepository
import org.asteroidos.sync.viewmodel.base.SynchronizationServiceModel

class SynchronizationServiceModelImpl(
    private val messageRepo: MessageRepository = MessageRepository.get(),
    private val watchStatusRepo: WatchStatusRepository = WatchStatusRepository.get()
) : SynchronizationServiceModel {
    private val modelScope = CoroutineScope(Dispatchers.IO)

    override fun onConnectRequested(consumer: Runnable) {
        modelScope.launch {
            messageRepo.requestConnectFlow.collect {
                consumer.run()
            }
        }
    }

    override fun onDisconnectRequested(consumer: Runnable) {
        modelScope.launch {
            messageRepo.requestDisconnectFlow.collect {
                consumer.run()
            }
        }
    }

    override fun onBatteryLifeRequested(consumer: Runnable) {
        modelScope.launch {
            messageRepo.requestBatteryLifeFlow.collect {
                consumer.run()
            }
        }
    }

    override fun onDeviceSetRequested(consumer: Consumer<BluetoothDevice>) {
        modelScope.launch {
            messageRepo.requestSetDeviceFlow.collect {
                consumer.accept(it)
            }
        }
    }

    override fun onDeviceUnsetRequested(consumer: Runnable) {
        modelScope.launch {
            messageRepo.requestUnsetDeviceFlow.collect {
                consumer.run()
            }
        }
    }

    override fun onDeviceUpdateRequested(consumer: Runnable) {
        modelScope.launch {
            messageRepo.requestUpdateDeviceFlow.collect {
                consumer.run()
            }
        }
    }

    override fun setLocalName(name: String) {
        watchStatusRepo.setDeviceName(name)
    }

    override fun setConnectionState(state: ConnectionState) {
        watchStatusRepo.setConnectionState(state)
    }

    override fun setBatteryPercentage(percentage: Int) {
        watchStatusRepo.setBatteryPercentage(percentage)
    }
}