package org.asteroidos.sync.connectivity;

import java.util.HashMap;
import java.util.UUID;

/**
 * A connectivity service is a module that can exchange data with the watch. It has to implement {@link IService}
 * and additional functions regarding connectivity from {@link IConnectivityService}.
 */
public interface IConnectivityService extends IService {
    enum Direction{
        FROM_WATCH,
        TO_WATCH
    }

    HashMap<UUID, Direction> getCharacteristicUUIDs();

    UUID getServiceUUID();
}
