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
