package org.asteroidos.sync.asteroid

import org.asteroidos.sync.connectivity.IConnectivityService
import org.asteroidos.sync.connectivity.IServiceCallback
import java.util.*

interface IAsteroidDevice {
    enum class ConnectionState {
        STATUS_CONNECTED, STATUS_CONNECTING, STATUS_DISCONNECTED
    }

    val connectionState: ConnectionState
    fun send(characteristic: UUID?, data: ByteArray?, service: IConnectivityService?)
    fun registerBleService(service: IConnectivityService?)
    fun unregisterBleService(serviceUUID: UUID?)
    fun registerCallback(characteristicUUID: UUID?, callback: IServiceCallback?)
    fun unregisterCallback(characteristicUUID: UUID?)
    fun getServiceByUUID(uuid: UUID?): IConnectivityService?
    val services: HashMap<UUID?, IConnectivityService?>?

    companion object {
        const val name = ""
        const val macAddress = ""
        const val batteryPercentage = 0
        const val bonded = false
    }
}