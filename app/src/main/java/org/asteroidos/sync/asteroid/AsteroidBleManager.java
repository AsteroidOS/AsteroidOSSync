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

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.asteroidos.sync.connectivity.IConnectivityService;
import org.asteroidos.sync.connectivity.IServiceCallback;
import org.asteroidos.sync.services.SynchronizationService;
import org.asteroidos.sync.utils.AsteroidUUIDS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.ble.data.Data;
import no.nordicsemi.android.ble.data.DataSplitter;

public class AsteroidBleManager extends BleManager {
    // Android's BluetoothGatt.writeCharacteristic() rejects any value longer
    // than the maximum GATT attribute length (512 bytes) since Android 13,
    // throwing IllegalArgumentException. The default MTU-based splitter chunks
    // at MTU - 3 bytes, which is 514 when the system negotiates the maximum MTU
    // of 517 (as on the Pixel 8), exceeding the limit and crashing the app on
    // the first long notification. Cap each chunk at 512 bytes regardless of
    // the negotiated MTU. The watch reassembles the message from the chunks, so
    // a smaller chunk size has no effect other than splitting into more writes.
    private static final int MAX_ATTRIBUTE_LENGTH = 512;

    // Request the largest ATT_MTU the BLE spec allows (517 bytes) at connection
    // time. A bigger MTU means fewer, larger GATT writes and notifications, so
    // syncs (notifications, screenshots, weather) transfer faster. This is only
    // safe because CAPPED_SPLITTER above clamps every write to the 512-byte
    // attribute limit no matter what MTU the system negotiates; the peer always
    // falls back to a smaller MTU if it can't support the maximum.
    private static final int GATT_MAX_MTU = 517;

    private static final DataSplitter CAPPED_SPLITTER = (message, index, maxLength) -> {
        final int size = Math.min(maxLength, MAX_ATTRIBUTE_LENGTH);
        final int offset = index * size;
        if (offset >= message.length)
            return null;
        final int length = Math.min(size, message.length - offset);
        final byte[] chunk = new byte[length];
        System.arraycopy(message, offset, chunk, 0, length);
        return chunk;
    };

    public static final String TAG = AsteroidBleManager.class.toString();
    @Nullable
    public BluetoothGattCharacteristic batteryCharacteristic;
    final SynchronizationService mSynchronizationService;
    final ArrayList<BluetoothGattService> mGattServices;
    public final HashMap<UUID, IServiceCallback> recvCallbacks;
    public HashMap<UUID, BluetoothGattCharacteristic> sendingCharacteristics;

    public AsteroidBleManager(@NonNull final Context context, SynchronizationService syncService) {
        super(context);
        mSynchronizationService = syncService;
        mGattServices = new ArrayList<>();
        recvCallbacks = new HashMap<>();
    }

    public final void send(UUID characteristic, byte[] data) {
        writeCharacteristic(sendingCharacteristics.get(characteristic), data,
                Objects.requireNonNull(sendingCharacteristics.get(characteristic)).getWriteType()).split(CAPPED_SPLITTER).enqueue();
    }

    @NonNull
    @Override
    protected final BleManagerGattCallback getGattCallback() {
        return new AsteroidBleManagerGattCallback() {
            @Override
            protected void onServicesInvalidated() {
                mSynchronizationService.unsyncServices();
                batteryCharacteristic = null;
                mGattServices.clear();
            }
        };
    }

    public final void abort() {
        cancelQueue();
    }

    public final void setBatteryLevel(Data data) {
        BatteryLevelEvent batteryLevelEvent = new BatteryLevelEvent();
        batteryLevelEvent.battery = Objects.requireNonNull(data.getByte(0)).intValue();
        mSynchronizationService.handleUpdateBatteryPercentage(batteryLevelEvent);
    }

    public static class BatteryLevelEvent {
        public int battery = 0;
    }

    public final void readCharacteristics() {
        readCharacteristic(batteryCharacteristic).with(((device, data) -> setBatteryLevel(data))).enqueue();
    }

    private abstract class AsteroidBleManagerGattCallback extends BleManagerGattCallback {

        /* It is a constraint of the Bluetooth library that it is required to initialize
          the characteristics in the isRequiredServiceSupported() function. */
        @Override
        public final boolean isRequiredServiceSupported(@NonNull final BluetoothGatt gatt) {
            final BluetoothGattService batteryService = gatt.getService(AsteroidUUIDS.BATTERY_SERVICE_UUID);


            boolean notify = false;
            if (batteryService != null) {
                batteryCharacteristic = batteryService.getCharacteristic(AsteroidUUIDS.BATTERY_UUID);

                if (batteryCharacteristic != null) {
                    final int properties = batteryCharacteristic.getProperties();
                    notify = (properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;
                }
            }
            if (sendingCharacteristics == null)
                sendingCharacteristics = new HashMap<>();

            for (IConnectivityService service : mSynchronizationService.getServices().values()) {
                BluetoothGattService bluetoothGattService = gatt.getService(service.getServiceUUID());
                List<UUID> sendUuids = new ArrayList<>();
                service.getCharacteristicUUIDs().forEach((uuid, direction) -> {
                    if (direction == IConnectivityService.Direction.TO_WATCH)
                        sendUuids.add(uuid);
                });
                Log.d(TAG, "UUID " + sendUuids);

                for (UUID uuid : sendUuids) {
                    BluetoothGattCharacteristic characteristic = bluetoothGattService.getCharacteristic(uuid);
                    sendingCharacteristics.put(uuid, characteristic);
                    bluetoothGattService.addCharacteristic(characteristic);
                }
                recvCallbacks.forEach((characteristic, callback) -> {
                    BluetoothGattCharacteristic characteristic1 = bluetoothGattService.getCharacteristic(characteristic);
                    removeNotificationCallback(characteristic1);
                    setNotificationCallback(characteristic1).with((device, data) -> callback.call(data.getValue()));
                    enableNotifications(characteristic1).enqueue();
                });
            }

            return (batteryCharacteristic != null && notify);
        }

        @Override
        protected final void initialize() {
            beginAtomicRequestQueue()
                    .add(requestMtu(GATT_MAX_MTU)
                            .with((device, mtu) -> log(Log.INFO, "MTU set to " + mtu))
                            .fail((device, status) -> log(Log.WARN, "Requested MTU not supported: " + status)))
                    .done(device -> log(Log.INFO, "Target initialized"))
                    .fail((device, status) -> Log.e("Init", device.getAddress() + " not initialized with error: " + status))
                    .enqueue();

            setNotificationCallback(batteryCharacteristic).with(((device, data) -> setBatteryLevel(data)));
            // Do not call readCharacteristic(batteryCharacteristic) here.
            // Otherwise, on Android 12 and later, the BLE bond to the watch
            // is lost, and communication no longer works. Arguably, reading
            // and writing should not be done in initialize(), since it is
            // part of the BLE manager's connect() request. Instead, do those
            // IO operations _after_ that request finishes (-> read the
            // characteristic in the SynchronizationService class, which is
            // where the BLE manager's connect() function is called).
            enableNotifications(batteryCharacteristic).enqueue();

            // Let all services know that we are connected.
            try {
                mSynchronizationService.syncServices();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}