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

package org.asteroidos.sync.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import org.asteroidos.sync.MainActivity;
import org.asteroidos.sync.R;
import org.asteroidos.sync.asteroid.AsteroidBleManager;
import org.asteroidos.sync.asteroid.IAsteroidDevice;
import org.asteroidos.sync.connectivity.IConnectivityService;
import org.asteroidos.sync.connectivity.IService;
import org.asteroidos.sync.connectivity.IServiceCallback;
import org.asteroidos.sync.connectivity.MediaService;
import org.asteroidos.sync.connectivity.NotificationService;
import org.asteroidos.sync.connectivity.ScreenshotService;
import org.asteroidos.sync.connectivity.SilentModeService;
import org.asteroidos.sync.connectivity.TimeService;
import org.asteroidos.sync.connectivity.WeatherService;
import org.asteroidos.sync.viewmodel.base.SynchronizationServiceModel;
import org.asteroidos.sync.viewmodel.impl.SynchronizationServiceModelImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import no.nordicsemi.android.ble.observer.ConnectionObserver;

public class SynchronizationService extends Service implements IAsteroidDevice, ConnectionObserver {
    public static final String TAG = SynchronizationService.class.toString();
    public static final int MSG_CONNECT = 1;
    public static final int MSG_DISCONNECT = 2;
    public static final int MSG_SET_LOCAL_NAME = 3;
    public static final int MSG_SET_STATUS = 4;
    public static final int MSG_SET_BATTERY_PERCENTAGE = 5;
    public static final int MSG_REQUEST_BATTERY_LIFE = 6;
    public static final int MSG_SET_DEVICE = 7;
    public static final int MSG_UPDATE = 8;
    public static final int MSG_UNSET_DEVICE = 9;

    private static final String NOTIFICATION_CHANNEL_ID = "synchronizationservice_channel_id_01";
    private final int NOTIFICATION = 2725;
    public BluetoothDevice mDevice;
    public int batteryPercentage = 0;
    HashMap<UUID, IConnectivityService> bleServices;
    List<IService> nonBleServices;
    private NotificationManager mNM;
    private ConnectionState mState = ConnectionState.STATUS_DISCONNECTED;
    private SharedPreferences mPrefs;
    private AsteroidBleManager mBleMngr;

    private final SynchronizationServiceModel model = new SynchronizationServiceModelImpl();

    final void handleConnect() {
        if (mBleMngr == null) {
            mBleMngr = new AsteroidBleManager(getApplicationContext(), SynchronizationService.this);
            mBleMngr.setConnectionObserver(this);
        }
        if (mState == ConnectionState.STATUS_CONNECTED || mState == ConnectionState.STATUS_CONNECTING)
            return;

        mPrefs = getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        String defaultDevMacAddr = mPrefs.getString(MainActivity.PREFS_DEFAULT_MAC_ADDR, "");
        if (defaultDevMacAddr.equals("")) return;
        String defaultLocalName = mPrefs.getString(MainActivity.PREFS_DEFAULT_LOC_NAME, "");
        BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(defaultDevMacAddr);
        try {
            device.createBond();
            mBleMngr.connect(device)
                    .useAutoConnect(true)
                    .timeout(100 * 1000)
                    .retry(3, 200)
                    .done(device1 -> {
                        Log.d(TAG, "Connected to " + device1.getName());
                        // Now we read the current values of the GATT characteristics,
                        // _after_ the connection has been fully established, to avoid
                        // connection failures on Android 12 and later.
                        mBleMngr.readCharacteristics();
                    })
                    .fail((device2, error) -> Log.e(TAG, "Failed to connect to " + device.getName() +
                            " with error code: " + error))
                    .enqueue();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    final void handleDisconnect() {
        if (mBleMngr == null) return;
        if (mState == ConnectionState.STATUS_DISCONNECTED) return;

        bleServices.values().forEach(IService::unsync);
        mBleMngr.abort();
        mBleMngr.disconnect().enqueue();
    }

    final void handleSetDevice(BluetoothDevice device) {
        SharedPreferences.Editor editor = mPrefs.edit();
        Log.d(TAG, "handleSetDevice: " + device.toString());
        editor.putString(MainActivity.PREFS_DEFAULT_MAC_ADDR, device.getAddress());
        mDevice = device;
        try {
            String name = mDevice.getName();
            model.setLocalName(name);
        } catch (SecurityException | NullPointerException ignored) {
        }
        editor.putString(MainActivity.PREFS_DEFAULT_LOC_NAME, name);
        editor.apply();
    }

    final void handleUpdateConnectionStatus() {
        if (mDevice != null) {
            model.setConnectionState(mState);
        }
    }

    final public void unsyncServices() {
        bleServices.values().forEach(IService::unsync);
        nonBleServices.forEach(IService::unsync);
    }

    final public void syncServices() {
        bleServices.values().forEach(IService::sync);
        nonBleServices.forEach(IService::sync);
    }

    @Override
    public final ConnectionState getConnectionState() {
        return mState;
    }

    @Override
    public final void send(UUID characteristic, byte[] data, IConnectivityService service) {
        mBleMngr.send(characteristic, data);
        Log.d(TAG, characteristic.toString() + " " + Arrays.toString(data));
    }

    @Override
    public final void registerBleService(IConnectivityService service) {
        bleServices.put(service.getServiceUUID(), service);
        Log.d(TAG, "BLE Service registered: " + service.getServiceUUID());
    }

    @Override
    public final void unregisterBleService(UUID serviceUUID) {
        bleServices.remove(getServiceByUUID(serviceUUID));
        Log.d(TAG, "BLE Service unregistered: " + serviceUUID);
    }

    @Override
    public final void registerCallback(UUID characteristicUUID, IServiceCallback callback) {
        mBleMngr.recvCallbacks.putIfAbsent(characteristicUUID, callback);
    }

    @Override
    public final void unregisterCallback(UUID characteristicUUID) {
        mBleMngr.recvCallbacks.remove(characteristicUUID);
    }

    @Override
    public final IConnectivityService getServiceByUUID(UUID uuid) {
        return bleServices.get(uuid);
    }

    @Override
    public final HashMap<UUID, IConnectivityService> getServices() {
        return bleServices;
    }

    @Override
    public final void onDeviceConnected(@NonNull BluetoothDevice device) {
        mState = ConnectionState.STATUS_CONNECTED;
        updateNotification();
    }

    @Override
    public void onDeviceFailedToConnect(@NonNull BluetoothDevice device, int reason) {
        try {
            Log.d(TAG, "Failed to connect to " + device.getName() + ": " + reason);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    public final void onDeviceReady(@NonNull BluetoothDevice device) {
        mState = ConnectionState.STATUS_CONNECTED;
        updateNotification();
        syncServices();
        AsteroidBleManager.BatteryLevelEvent bevent = new AsteroidBleManager.BatteryLevelEvent();
        bevent.battery = batteryPercentage;
        handleUpdateBatteryPercentage(bevent);
    }

    @Override
    public final void onDeviceDisconnecting(@NonNull BluetoothDevice device) {
        mState = ConnectionState.STATUS_CONNECTED;
        updateNotification();
    }

    @Override
    public final void onDeviceDisconnected(@NonNull BluetoothDevice device, int reason) {
        mState = ConnectionState.STATUS_DISCONNECTED;
        updateNotification();
        unsyncServices();
    }

    @Override
    public void onCreate() {
        bleServices = new HashMap<>();
        nonBleServices = new ArrayList<>();

        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "Synchronization Service", NotificationManager.IMPORTANCE_LOW);
            notificationChannel.setDescription("Connection status");
            notificationChannel.setVibrationPattern(new long[]{0L});
            notificationChannel.setShowBadge(false);
            mNM.createNotificationChannel(notificationChannel);
        }


        mPrefs = getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        String defaultDevMacAddr = mPrefs.getString(MainActivity.PREFS_DEFAULT_MAC_ADDR, "");
        String defaultLocalName = mPrefs.getString(MainActivity.PREFS_DEFAULT_LOC_NAME, "");

        if (mBleMngr == null) {
            mBleMngr = new AsteroidBleManager(getApplicationContext(), SynchronizationService.this);
            mBleMngr.setConnectionObserver(this);
        }

        if (!(defaultDevMacAddr.equals(""))) {
            mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(defaultDevMacAddr);
        }

        if (nonBleServices.isEmpty())
            nonBleServices.add(new SilentModeService(getApplicationContext()));

        if (bleServices.isEmpty()) {
            // Register Services
            registerBleService(new MediaService(getApplicationContext(), this));
            registerBleService(new NotificationService(getApplicationContext(), this));
            registerBleService(new WeatherService(getApplicationContext(), this));
            registerBleService(new ScreenshotService(getApplicationContext(), this));
            registerBleService(new TimeService(getApplicationContext(), this));
        }

        handleConnect();
        updateNotification();

        model.onConnectRequested(this::handleConnect);
        model.onDisconnectRequested(this::handleDisconnect);
        model.onBatteryLifeRequested(this::handleUpdateBatteryPercentageRequest);
        model.onDeviceSetRequested(this::handleSetDevice);
        model.onDeviceUnsetRequested(this::handleUnSetDevice);
        model.onDeviceUpdateRequested(this::handleUpdateConnectionStatus);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private void updateNotification() {
        handleUpdateConnectionStatus();
        String status = getString(R.string.disconnected);
        if (mDevice != null) {
            try {
                if (mState == ConnectionState.STATUS_CONNECTING)
                    status = getString(R.string.connecting_formatted, mDevice.getName());
                else if (mState == ConnectionState.STATUS_CONNECTED)
                    status = getString(R.string.connected_formatted, mDevice.getName());
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }

        if (mDevice != null) {
            Intent intent = new Intent(this, MainActivity.class);
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                    intent, PendingIntent.FLAG_UPDATE_CURRENT + PendingIntent.FLAG_IMMUTABLE);

            Notification notification = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_stat_name)
                    .setContentTitle(getText(R.string.app_name))
                    .setContentText(status)
                    .setContentIntent(contentIntent)
                    .setOngoing(true)
                    .setPriority(Notification.PRIORITY_MIN)
                    .setShowWhen(false)
                    .build();

            mNM.notify(NOTIFICATION, notification);
            startForeground(NOTIFICATION, notification);
        }
    }

    @Override
    public void onDestroy() {
        mBleMngr.disconnect();
        mNM.cancel(NOTIFICATION);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDeviceConnecting(@NonNull BluetoothDevice device) {
        mState = ConnectionState.STATUS_CONNECTING;
        updateNotification();
    }

    private void handleUnSetDevice() {
        SharedPreferences.Editor editor = mPrefs.edit();
        if (mState != ConnectionState.STATUS_DISCONNECTED) {
            mBleMngr.disconnect().enqueue();
        }
        mDevice = null;
        editor.putString(MainActivity.PREFS_DEFAULT_LOC_NAME, "");
        editor.putString(MainActivity.PREFS_DEFAULT_MAC_ADDR, "");
        editor.putString(MainActivity.PREFS_NAME, "");
        editor.apply();
    }

    public void handleUpdateBatteryPercentageRequest() {
        AsteroidBleManager.BatteryLevelEvent batteryLevelEvent = new AsteroidBleManager.BatteryLevelEvent();
        batteryLevelEvent.battery = batteryPercentage;
        handleUpdateBatteryPercentage(batteryLevelEvent);
    }

    public void handleUpdateBatteryPercentage(AsteroidBleManager.BatteryLevelEvent battery) {
        Log.d(TAG, "handleBattery: " + battery.battery + "%");
        batteryPercentage = battery.battery;
        model.setBatteryPercentage(batteryPercentage);
    }
}
