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
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.asteroidos.sync.asteroid.IAsteroidDevice
import org.asteroidos.sync.common.ext.logV
import org.asteroidos.sync.domain.repository.base.MessageRepository
import org.asteroidos.sync.domain.repository.base.WatchStatusRepository
import org.asteroidos.sync.viewmodel.base.MainActivityViewModel
import java.util.function.Consumer

/**
 * Implementation of the main activity view model
 *
 * TODO use android DI framework
 */
class MainActivityViewModelImpl(
    private val messageRepo: MessageRepository = MessageRepository.get(),
    private val watchStatusRepo: WatchStatusRepository = WatchStatusRepository.get()
) : MainActivityViewModel() {

    override fun onWatchLocalNameChanged(consumer: Consumer<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            watchStatusRepo.deviceName.collect {
                logV("onWatchLocalNameChanged")
                viewModelScope.launch {
                    consumer.accept(it)
                }
            }
        }
    }

    override fun onWatchConnectionStateChanged(consumer: Consumer<IAsteroidDevice.ConnectionState>) {
        viewModelScope.launch(Dispatchers.IO) {
            watchStatusRepo.connectionState.collect {
                logV("onWatchConnectionStateChanged")
                viewModelScope.launch {
                    consumer.accept(it)
                }
            }
        }
    }

    override fun onWatchBatteryPercentageChanged(consumer: Consumer<Int>) {
        viewModelScope.launch(Dispatchers.IO) {
            watchStatusRepo.batteryPercentage.collect {
                logV("onWatchBatteryPercentageChanged")
                viewModelScope.launch {
                    consumer.accept(it)
                }
            }
        }
    }

    override fun requestDisconnect() {
        logV("")
        messageRepo.requestDisconnect()
    }

    override fun requestConnect() {
        logV("")
        messageRepo.requestConnect()
    }

    override fun requestUpdate() {
        logV("")
        messageRepo.requestUpdateDevice()
    }

    override fun requestUnsetDevice() {
        logV("")
        messageRepo.requestUnsetDevice()
    }

    override fun onDefaultDeviceSelected(mDevice: BluetoothDevice) {
        logV("")
        messageRepo.requestSetDevice(mDevice)
    }

    override fun requestBatteryLevel() {
        logV("")
        messageRepo.requestBatteryLife()
    }
}