package org.asteroidos.sync.connectivity

import org.asteroidos.sync.connectivity.IService
import java.util.*

/**
 * A connectivity service is a module that can exchange data with the watch. It has to implement [IService]
 * and additional functions regarding connectivity from [IConnectivityService].
 */
interface IConnectivityService : IService {
    enum class Direction {
        FROM_WATCH, TO_WATCH
    }

    val characteristicUUIDs: HashMap<UUID, Direction>
    val serviceUUID: UUID
}