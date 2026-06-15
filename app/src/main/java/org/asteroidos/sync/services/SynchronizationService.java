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
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
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
    final Messenger mMessenger = new Messenger(new SynchronizationHandler(this));
    private final int NOTIFICATION = 2725;
    public BluetoothDevice mDevice;
    public int batteryPercentage = 0;
    HashMap<UUID, IConnectivityService> bleServices;
    List<IService> nonBleServices;
    private NotificationManager mNM;
    private volatile ConnectionState mState = ConnectionState.STATUS_DISCONNECTED;
    private Messenger replyTo;
    private SharedPreferences mPrefs;
    private AsteroidBleManager mBleMngr;
    // Set when the user (or app teardown) explicitly asked to disconnect, so we
    // do not fight that intent by automatically reconnecting.
    private volatile boolean mUserInitiatedDisconnect = false;
    private final Handler mReconnectHandler = new Handler(Looper.getMainLooper());
    private static final long RECONNECT_DELAY_MS = 3000;

    final void handleConnect() {
        if (mBleMngr == null) {
            mBleMngr = new AsteroidBleManager(getApplicationContext(), SynchronizationService.this);
            mBleMngr.setConnectionObserver(this);
        }
        if (mState == ConnectionState.STATUS_CONNECTED || mState == ConnectionState.STATUS_CONNECTING) return;

        // A new connection attempt overrides any previously requested disconnect.
        mUserInitiatedDisconnect = false;
        mReconnectHandler.removeCallbacksAndMessages(null);

        mPrefs = getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        String defaultDevMacAddr = mPrefs.getString(MainActivity.PREFS_DEFAULT_MAC_ADDR, "");
        if (defaultDevMacAddr.equals("")) return;
        BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(defaultDevMacAddr);
        try {
            // Only initiate bonding when the device is not already bonded. Calling
            // createBond() on every connect races the bonding state machine against
            // the GATT connection and is a known cause of error 133 / failed service
            // discovery (the "I have to forget and re-pair" symptom). When already
            // bonded, encryption is re-established automatically on connect.
            if (device.getBondState() == BluetoothDevice.BOND_NONE)
                device.createBond();
            mBleMngr.connect(device)
                    // autoConnect lets the OS reconnect in the background when the
                    // watch comes back into range (power efficient, survives Doze).
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

    // Re-establish the connection after an unexpected link loss. Gated on the
    // user-initiated flag and the current state so we never reconnect against the
    // user's wishes or double-connect while the stack is already (re)connecting.
    private void scheduleReconnect() {
        if (mUserInitiatedDisconnect || mDevice == null) return;
        mReconnectHandler.removeCallbacksAndMessages(null);
        mReconnectHandler.postDelayed(() -> {
            if (mUserInitiatedDisconnect) return;
            if (mState == ConnectionState.STATUS_CONNECTED || mState == ConnectionState.STATUS_CONNECTING)
                return;
            Log.d(TAG, "Attempting to reconnect after link loss");
            handleConnect();
        }, RECONNECT_DELAY_MS);
    }

    final void handleDisconnect() {
        if (mBleMngr == null) return;
        if (mState == ConnectionState.STATUS_DISCONNECTED) return;

        mUserInitiatedDisconnect = true;
        mReconnectHandler.removeCallbacksAndMessages(null);
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
            Message answer = Message.obtain(null, MSG_SET_LOCAL_NAME);
            answer.obj = name;
            replyTo.send(answer);
            replyTo.send(Message.obtain(null, MSG_SET_STATUS, mState));
        } catch (RemoteException | SecurityException | NullPointerException ignored) {
        }
        editor.putString(MainActivity.PREFS_DEFAULT_LOC_NAME, name);
        editor.apply();
    }

    final void handleUpdateConnectionStatus() {
        if (mDevice != null) {
            try {
                replyTo.send(Message.obtain(null, MSG_SET_STATUS, mState));
            } catch (RemoteException | NullPointerException ignored) {
            }
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
        // Services are driven by broadcasts, observers, alarms and network
        // callbacks that fire independently of the BLE link. Dropping sends while
        // disconnected avoids crashing in the BLE layer when there is no
        // characteristic / connection to write to.
        if (mBleMngr == null || mState != ConnectionState.STATUS_CONNECTED) {
            Log.w(TAG, "Dropping send to " + characteristic + ": not connected");
            return;
        }
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
        // Only a clean, locally requested disconnect should be left alone; any
        // other reason (link loss, timeout, peer terminated) means we lost the
        // watch unexpectedly and should try to get it back.
        if (reason != ConnectionObserver.REASON_SUCCESS
                && reason != ConnectionObserver.REASON_TERMINATE_LOCAL_HOST)
            scheduleReconnect();
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

        // Always promote to a foreground service immediately. When the service is
        // launched with startForegroundService() (e.g. from boot autostart on
        // Android 8+), startForeground() must be called within a few seconds or
        // the system kills the process with an ANR, so this must not be gated on
        // a device being set.
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

    @Override
    public void onDestroy() {
        mUserInitiatedDisconnect = true;
        mReconnectHandler.removeCallbacksAndMessages(null);
        // disconnect() only queues a request; it must be enqueued to actually run,
        // otherwise the GATT connection is leaked when the service is destroyed.
        if (mBleMngr != null)
            mBleMngr.disconnect().enqueue();
        mNM.cancel(NOTIFICATION);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    @Override
    public void onDeviceConnecting(@NonNull BluetoothDevice device) {
        mState = ConnectionState.STATUS_CONNECTING;
        updateNotification();
    }

    private void handleUnSetDevice() {
        mUserInitiatedDisconnect = true;
        mReconnectHandler.removeCallbacksAndMessages(null);
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

    public void handleUpdateBatteryPercentage(AsteroidBleManager.BatteryLevelEvent battery) {
        Log.d(TAG, "handleBattery: " + battery.battery + "%");
        batteryPercentage = battery.battery;
        try {
            if (replyTo != null)
                replyTo.send(Message.obtain(null, MSG_SET_BATTERY_PERCENTAGE, batteryPercentage, 0));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    static private class SynchronizationHandler extends Handler {
        private final SynchronizationService mService;

        SynchronizationHandler(SynchronizationService service) {
            super(Looper.getMainLooper());
            mService = service;
        }

        @Override
        public void handleMessage(Message msg) {
            mService.replyTo = msg.replyTo;

            switch (msg.what) {
                case MSG_CONNECT:
                    mService.handleConnect();
                    break;
                case MSG_DISCONNECT:
                    mService.handleDisconnect();
                    break;
                case MSG_REQUEST_BATTERY_LIFE:
                    AsteroidBleManager.BatteryLevelEvent batteryLevelEvent = new AsteroidBleManager.BatteryLevelEvent();
                    batteryLevelEvent.battery = mService.batteryPercentage;
                    mService.handleUpdateBatteryPercentage(batteryLevelEvent);
                    break;
                case MSG_SET_DEVICE:
                    mService.handleSetDevice((BluetoothDevice) msg.obj);
                    break;
                case MSG_UNSET_DEVICE:
                    mService.handleUnSetDevice();
                    break;
                case MSG_UPDATE:
                    mService.handleUpdateConnectionStatus();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
}
