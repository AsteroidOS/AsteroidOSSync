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
