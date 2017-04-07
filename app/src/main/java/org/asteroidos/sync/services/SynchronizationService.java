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
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.widget.Toast;

import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleNode;
import com.idevicesinc.sweetblue.utils.Uuids;

import org.asteroidos.sync.MainActivity;
import org.asteroidos.sync.R;
import org.asteroidos.sync.ble.MediaService;
import org.asteroidos.sync.ble.NotificationService;
import org.asteroidos.sync.ble.ScreenshotService;
import org.asteroidos.sync.ble.TimeService;
import org.asteroidos.sync.ble.WeatherService;

import static com.idevicesinc.sweetblue.BleManager.get;

public class SynchronizationService extends Service implements BleDevice.StateListener {
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

    public static final int STATUS_CONNECTED = 1;
    public static final int STATUS_DISCONNECTED = 2;
    public static final int STATUS_CONNECTING = 3;

    private ScreenshotService mScreenshotService;
    private WeatherService mWeatherService;
    private NotificationService mNotificationService;
    private MediaService mMediaService;
    private TimeService mTimeService;

    class SynchronizationHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_CONNECT:
                    if(mDevice == null) return;
                    if(mState == STATUS_CONNECTED || mState == STATUS_CONNECTING) return;
                    replyTo = msg.replyTo;
                    mDevice.setListener_State(SynchronizationService.this);
                    mDevice.setListener_ConnectionFail(new BleDevice.DefaultConnectionFailListener() {
                        @Override public BleNode.ConnectionFailListener.Please onEvent(BleDevice.ConnectionFailListener.ConnectionFailEvent event)
                        {
                            BleNode.ConnectionFailListener.Please please = super.onEvent(event);

                            if(!please.isRetry())
                            {
                                final String toast = event.device().getName_debug() + " connection failed with " + event.failureCountSoFar() + " retries - " + event.status();
                                if(getApplicationContext() != null)
                                    Toast.makeText(getApplicationContext(), toast, Toast.LENGTH_LONG).show();
                            }

                            return please;
                        }
                    });

                    mWeatherService = new WeatherService(getApplicationContext(), mDevice);
                    mNotificationService = new NotificationService(getApplicationContext(), mDevice);
                    mMediaService = new MediaService(getApplicationContext(), mDevice);
                    mScreenshotService = new ScreenshotService(getApplicationContext(), mDevice);
                    mTimeService = new TimeService(getApplicationContext(), mDevice);

                    mDevice.connect();
                    break;
                case MSG_DISCONNECT:
                    if(mDevice == null) return;
                    if(mState == STATUS_DISCONNECTED) return;
                    mScreenshotService.unsync();
                    mWeatherService.unsync();
                    mNotificationService.unsync();
                    mMediaService.unsync();
                    mTimeService.unsync();
                    mDevice.disconnect();
                    break;
                case MSG_REQUEST_BATTERY_LIFE:
                    if(mDevice == null) return;
                    if(mState == STATUS_DISCONNECTED) return;
                    replyTo = msg.replyTo;
                    mDevice.read(Uuids.BATTERY_LEVEL, new BleDevice.ReadWriteListener()
                    {
                        @Override public void onEvent(ReadWriteEvent result)
                        {
                            if(result.wasSuccess())
                                try {
                                    replyTo.send(Message.obtain(null, MSG_SET_BATTERY_PERCENTAGE, result.data()[0], 0));
                                } catch (RemoteException ignored) {}
                        }
                    });
                    break;
                case MSG_SET_DEVICE:
                    String macAddress = (String)msg.obj;
                    if(macAddress.isEmpty()) {
                        if(mState != STATUS_DISCONNECTED) {
                            mScreenshotService.unsync();
                            mWeatherService.unsync();
                            mNotificationService.unsync();
                            mMediaService.unsync();
                            mTimeService.unsync();
                            mDevice.disconnect();
                        }
                        mDevice = null;
                    } else {
                        mDevice = mBleMngr.getDevice(macAddress);
                        replyTo = msg.replyTo;

                        try {
                            Message answer = Message.obtain(null, MSG_SET_LOCAL_NAME);
                            answer.obj = mDevice.getName_normalized();
                            replyTo.send(answer);

                            replyTo.send(Message.obtain(null, MSG_SET_STATUS, mState, 0));
                        } catch (RemoteException ignored) {}
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
    final Messenger mMessenger = new Messenger(new SynchronizationHandler());

    @Override
    public void onCreate() {
        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mBleMngr = get(getApplication());
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

            Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getText(R.string.app_name))
                .setContentText(status)
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_MIN)
                .setShowWhen(false)
                .build();

            mNM.notify(NOTIFICATION, notification);
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
            } catch (RemoteException ignored) {}
            mDevice.setMtu(256);

            event.device().enableNotify(Uuids.BATTERY_LEVEL, new BleDevice.ReadWriteListener() {
                @Override
                public void onEvent(ReadWriteEvent e) {
                    try {
                        if (e.isNotification() && e.charUuid().equals(Uuids.BATTERY_LEVEL)) {
                            byte data[] = e.data();
                            if (replyTo != null)
                                replyTo.send(Message.obtain(null, MSG_SET_BATTERY_PERCENTAGE, data[0], 0));
                        }
                    } catch(RemoteException ignored) {}
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
        } else if (event.didEnter(BleDeviceState.DISCONNECTED)) {
            mState = STATUS_DISCONNECTED;
            updateNotification();
            try {
                replyTo.send(Message.obtain(null, MSG_SET_STATUS, STATUS_DISCONNECTED, 0));
            } catch (RemoteException ignored) {}

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
        } else if(event.didEnter(BleDeviceState.CONNECTING)) {
            mState = STATUS_CONNECTING;
            updateNotification();
            try {
                replyTo.send(Message.obtain(null, MSG_SET_STATUS, STATUS_CONNECTING, 0));
            } catch (RemoteException ignored) {}
        }
    }
}
