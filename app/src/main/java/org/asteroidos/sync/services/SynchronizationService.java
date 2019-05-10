/*
 * Copyright (C) 2016 - Florent Revest <revestflo@gmail.com>
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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;

import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleDeviceConfig;
import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerConfig;
import com.idevicesinc.sweetblue.BleNodeConfig;
import com.idevicesinc.sweetblue.BleTask;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Uuids;

import org.asteroidos.sync.BuildConfig;
import org.asteroidos.sync.MainActivity;
import org.asteroidos.sync.R;
import org.asteroidos.sync.ble.MediaService;
import org.asteroidos.sync.ble.NotificationService;
import org.asteroidos.sync.ble.ScreenshotService;
import org.asteroidos.sync.ble.SilentModeService;
import org.asteroidos.sync.ble.TimeService;
import org.asteroidos.sync.ble.WeatherService;

import java.util.UUID;

import static com.idevicesinc.sweetblue.BleManager.get;

@SuppressWarnings( "deprecation" ) // Before upgrading to SweetBlue 3.0, we don't have an alternative to the deprecated StateListener
public class SynchronizationService extends Service implements BleDevice.StateListener {
    private static final String NOTIFICATION_CHANNEL_ID = "synchronizationservice_channel_id_01";
    private NotificationManager mNM;
    private int NOTIFICATION = 2725;
    private BleManager mBleMngr;
    private BleDevice mDevice;
    private int mState = STATUS_DISCONNECTED;

    private Messenger replyTo;

    public static final int MSG_CONNECT = 1;
    public static final int MSG_DISCONNECT = 2;

    public static final int MSG_SET_LOCAL_NAME = 3;
    public static final int MSG_SET_STATUS = 4;
    public static final int MSG_SET_BATTERY_PERCENTAGE = 5;
    public static final int MSG_REQUEST_BATTERY_LIFE = 6;
    public static final int MSG_SET_DEVICE = 7;
    public static final int MSG_UPDATE = 8;

    public static final int STATUS_CONNECTED = 1;
    public static final int STATUS_DISCONNECTED = 2;
    public static final int STATUS_CONNECTING = 3;

    private ScreenshotService mScreenshotService;
    private WeatherService mWeatherService;
    private NotificationService mNotificationService;
    private MediaService mMediaService;
    private TimeService mTimeService;

    private SilentModeService silentModeService;
    private SharedPreferences mPrefs;

    void handleConnect() {
        if(mDevice == null) return;
        if(mState == STATUS_CONNECTED || mState == STATUS_CONNECTING) return;
        mDevice.setListener_State(SynchronizationService.this);

        mWeatherService = new WeatherService(getApplicationContext(), mDevice);
        mNotificationService = new NotificationService(getApplicationContext(), mDevice);
        mMediaService = new MediaService(getApplicationContext(), mDevice);
        mScreenshotService = new ScreenshotService(getApplicationContext(), mDevice);
        mTimeService = new TimeService(getApplicationContext(), mDevice);
        silentModeService = new SilentModeService(getApplicationContext());

        mDevice.connect();
    }

    void handleDisconnect() {
        if(mDevice == null) return;
        if(mState == STATUS_DISCONNECTED) return;
        mScreenshotService.unsync();
        mWeatherService.unsync();
        mNotificationService.unsync();
        mMediaService.unsync();
        mTimeService.unsync();
        mDevice.disconnect();
        silentModeService.onDisconnect();
    }

    void handleReqBattery() {
        if(mDevice == null) return;
        if(mState == STATUS_DISCONNECTED) return;
        mDevice.read(Uuids.BATTERY_LEVEL, new BleDevice.ReadWriteListener()
        {
            @Override public void onEvent(ReadWriteEvent result)
            {
                if(result.wasSuccess())
                    try {
                        replyTo.send(Message.obtain(null, MSG_SET_BATTERY_PERCENTAGE, result.data()[0], 0));
                    } catch (RemoteException | NullPointerException ignored) {}
            }
        });
    }

    void handleSetDevice(String macAddress) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(MainActivity.PREFS_DEFAULT_MAC_ADDR, macAddress);

        if(macAddress.isEmpty()) {
            if(mState != STATUS_DISCONNECTED) {
                mScreenshotService.unsync();
                mWeatherService.unsync();
                mNotificationService.unsync();
                mMediaService.unsync();
                mTimeService.unsync();
                mDevice.disconnect();
                mDevice.unbond();
            }
            mDevice = null;
            editor.putString(MainActivity.PREFS_DEFAULT_LOC_NAME, "");
        } else {
            mDevice = mBleMngr.getDevice(macAddress);

            String name = mDevice.getName_normalized();
            try {
                Message answer = Message.obtain(null, MSG_SET_LOCAL_NAME);
                answer.obj = name;
                replyTo.send(answer);

                replyTo.send(Message.obtain(null, MSG_SET_STATUS, mState, 0));
            } catch (RemoteException | NullPointerException ignored) {}

            editor.putString(MainActivity.PREFS_DEFAULT_LOC_NAME, name);
        }
        editor.apply();
    }

    void handleUpdate() {
        if(mDevice != null) {
            try {
                Message answer = Message.obtain(null, MSG_SET_LOCAL_NAME);
                answer.obj = mDevice.getName_normalized();
                replyTo.send(answer);

                replyTo.send(Message.obtain(null, MSG_SET_STATUS, mState, 0));

                mDevice.read(Uuids.BATTERY_LEVEL, new BleDevice.ReadWriteListener()
                {
                    @Override public void onEvent(ReadWriteEvent result)
                    {
                        if(result.wasSuccess())
                            try {
                                replyTo.send(Message.obtain(null, MSG_SET_BATTERY_PERCENTAGE, result.data()[0], 0));
                            } catch (RemoteException | NullPointerException ignored) {}
                    }
                });
            } catch (RemoteException | NullPointerException ignored) {}
        }
    }

    static private class SynchronizationHandler extends Handler {
        private SynchronizationService mService;

        SynchronizationHandler(SynchronizationService service) {
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
                    mService.handleReqBattery();
                    break;
                case MSG_SET_DEVICE:
                    mService.handleSetDevice((String)msg.obj);
                    break;
                case MSG_UPDATE:
                    mService.handleUpdate();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
    final Messenger mMessenger = new Messenger(new SynchronizationHandler(this));

    @Override
    public void onCreate() {
        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "Synchronization Service", NotificationManager.IMPORTANCE_LOW);
            notificationChannel.setDescription("Connection status");
            notificationChannel.setVibrationPattern(new long[]{0L});
            notificationChannel.setShowBadge(false);
            mNM.createNotificationChannel(notificationChannel);
        }

        mBleMngr = get(getApplication());
        BleManagerConfig cfg = new BleManagerConfig();
        cfg.forceBondDialog = true;
        cfg.taskTimeoutRequestFilter = new TaskTimeoutRequestFilter();
        cfg.defaultScanFilter = new WatchesFilter();
        cfg.enableCrashResolver = true;
        cfg.bondFilter = new BondFilter();
        cfg.alwaysUseAutoConnect = true;
        if (BuildConfig.DEBUG)
            cfg.loggingEnabled = true;
        mBleMngr.setConfig(cfg);

        mPrefs = getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        String defaultDevMacAddr = mPrefs.getString(MainActivity.PREFS_DEFAULT_MAC_ADDR, "");
        String defaultLocalName = mPrefs.getString(MainActivity.PREFS_DEFAULT_LOC_NAME, "");

        if(!defaultDevMacAddr.isEmpty()) {
            if(!mBleMngr.hasDevice(defaultDevMacAddr))
                mBleMngr.newDevice(defaultDevMacAddr, defaultLocalName);

            mDevice = mBleMngr.getDevice(defaultDevMacAddr);
            mDevice.setListener_State(SynchronizationService.this);

            mWeatherService = new WeatherService(getApplicationContext(), mDevice);
            mNotificationService = new NotificationService(getApplicationContext(), mDevice);
            mMediaService = new MediaService(getApplicationContext(), mDevice);
            mScreenshotService = new ScreenshotService(getApplicationContext(), mDevice);
            mTimeService = new TimeService(getApplicationContext(), mDevice);

            silentModeService = new SilentModeService(getApplicationContext());

            mDevice.connect();
        }

        updateNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private void updateNotification() {
        String status = getString(R.string.disconnected);
        if(mDevice != null) {
            if (mState == STATUS_CONNECTING)
                status = getString(R.string.connecting_formatted, mDevice.getName_normalized());
            else if (mState == STATUS_CONNECTED)
                status = getString(R.string.connected_formatted, mDevice.getName_normalized());
        }

        if(mDevice != null) {
            Intent intent = new Intent(this, MainActivity.class);
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

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
        if(mDevice != null)
            mDevice.disconnect();
        mNM.cancel(NOTIFICATION);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    /* Bluetooth events handling */
    @Override
    public void onEvent(StateEvent event) {
        if (event.didEnter(BleDeviceState.INITIALIZED)) {
            mState = STATUS_CONNECTED;
            updateNotification();
            try {
                replyTo.send(Message.obtain(null, MSG_SET_STATUS, STATUS_CONNECTED, 0));
            } catch (RemoteException | NullPointerException ignored) {}
            mDevice.setMtu(256);

            event.device().enableNotify(Uuids.BATTERY_LEVEL, new BleDevice.ReadWriteListener() {
                @Override
                public void onEvent(ReadWriteEvent e) {
                    try {
                        if (e.isNotification() && e.charUuid().equals(Uuids.BATTERY_LEVEL)) {
                            byte data[] = e.data();
                            replyTo.send(Message.obtain(null, MSG_SET_BATTERY_PERCENTAGE, data[0], 0));
                        }
                    } catch(RemoteException | NullPointerException ignored) {}
                }
            });

            if(mScreenshotService != null)
                mScreenshotService.sync();
            if (mWeatherService != null)
                mWeatherService.sync();
            if (mNotificationService != null)
                mNotificationService.sync();
            if (mMediaService != null)
                mMediaService.sync();
            if (mTimeService != null)
                mTimeService.sync();
            if (silentModeService != null)
                silentModeService.onConnect();
        } else if (event.didEnter(BleDeviceState.DISCONNECTED)) {
            mState = STATUS_DISCONNECTED;
            updateNotification();
            try {
                replyTo.send(Message.obtain(null, MSG_SET_STATUS, STATUS_DISCONNECTED, 0));
            } catch (RemoteException | NullPointerException ignored) {}

            if(mScreenshotService != null)
                mScreenshotService.sync();
            if (mWeatherService != null)
                mWeatherService.unsync();
            if (mNotificationService != null)
                mNotificationService.unsync();
            if (mMediaService != null)
                mMediaService.unsync();
            if (mTimeService != null)
                mTimeService.unsync();
            if (silentModeService != null)
                silentModeService.onDisconnect();
        } else if(event.didEnter(BleDeviceState.CONNECTING)) {
            mState = STATUS_CONNECTING;
            updateNotification();
            try {
                replyTo.send(Message.obtain(null, MSG_SET_STATUS, STATUS_CONNECTING, 0));
            } catch (RemoteException | NullPointerException ignored) {}
        }
    }

    private final class WatchesFilter implements BleManagerConfig.ScanFilter
    {
        @Override
        public Please onEvent(ScanEvent e)
        {
            return Please.acknowledgeIf(e.advertisedServices().contains(UUID.fromString("00000000-0000-0000-0000-00a57e401d05")));
        }
    }

    private static class TaskTimeoutRequestFilter implements BleNodeConfig.TaskTimeoutRequestFilter
    {
        static final double DEFAULT_TASK_TIMEOUT					= 12.5;
        static final double BOND_TASK_TIMEOUT					= 60.0;
        static final double DEFAULT_CRASH_RESOLVER_TIMEOUT		= 50.0;

        private static final Please DEFAULT_RETURN_VALUE = Please.setTimeoutFor(Interval.secs(DEFAULT_TASK_TIMEOUT));

        @Override public Please onEvent(TaskTimeoutRequestEvent e)
        {
            if(e.task() == BleTask.RESOLVE_CRASHES)
                return Please.setTimeoutFor(Interval.secs(DEFAULT_CRASH_RESOLVER_TIMEOUT));
            else if(e.task() == BleTask.BOND)
                return Please.setTimeoutFor(Interval.secs(BOND_TASK_TIMEOUT));
            else
                return DEFAULT_RETURN_VALUE;
        }
    }

    private static class BondFilter implements BleDeviceConfig.BondFilter
    {
        @Override public Please onEvent(StateChangeEvent e)    { return Please.doNothing(); }
        @Override public Please onEvent(CharacteristicEvent e)
        {
            return Please.doNothing();
        }
    }
}
