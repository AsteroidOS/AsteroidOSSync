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

package org.asteroidos.sync.asteroid;

import org.asteroidos.sync.connectivity.IConnectivityService;
import org.asteroidos.sync.connectivity.IServiceCallback;

import java.util.HashMap;
import java.util.UUID;

public interface IAsteroidDevice {
    String name = "";
    String macAddress = "";
    int batteryPercentage = 0;
    boolean bonded = false;

    enum ConnectionState {
        STATUS_CONNECTED,
        STATUS_CONNECTING,
        STATUS_DISCONNECTED
    }

    ConnectionState getConnectionState();

    void send(UUID characteristic, byte[] data, IConnectivityService service);
    void registerBleService(IConnectivityService service);
    void unregisterBleService(UUID serviceUUID);
    void registerCallback(UUID characteristicUUID, IServiceCallback callback);
    void unregisterCallback(UUID characteristicUUID);

    IConnectivityService getServiceByUUID(UUID uuid);
    HashMap<UUID, IConnectivityService> getServices();
}
