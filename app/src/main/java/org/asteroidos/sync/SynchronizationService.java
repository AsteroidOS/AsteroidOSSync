package org.asteroidos.sync;

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

import java.util.UUID;

import static com.idevicesinc.sweetblue.BleManager.get;

public class SynchronizationService extends Service implements BleDevice.StateListener {
    private NotificationManager mNM;
    private int NOTIFICATION = R.string.local_service_started;
    private BleManager mBleMngr;
    private BleDevice mDevice;
    private int mState = STATUS_DISCONNECTED;

    private Messenger replyTo;

    public static final UUID batteryLevelCharac = UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb");

    static final int MSG_CONNECT = 1;
    static final int MSG_DISCONNECT = 2;

    static final int MSG_SET_LOCAL_NAME = 3;
    static final int MSG_SET_STATUS = 4;
    static final int MSG_SET_BATTERY_PERCENTAGE = 5;

    static final int STATUS_CONNECTED = 1;
    static final int STATUS_DISCONNECTED = 2;
    static final int STATUS_CONNECTING = 3;

    private WeatherService mWeatherService;
    private NotificationService mNotificationService;
    private MediaService mMediaService;

    class SynchronizationHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_CONNECT:
                    mDevice = mBleMngr.getDevice((String)msg.obj);
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

                    try {
                        Message answer = Message.obtain(null, MSG_SET_LOCAL_NAME);
                        answer.obj = mDevice.getName_normalized();
                        replyTo.send(answer);
                    } catch (RemoteException ignored) {}

                    mDevice.connect();
                    break;
                case MSG_DISCONNECT:
                    if (mWeatherService != null)
                        mWeatherService.unsync();
                    if (mNotificationService != null)
                        mNotificationService.unsync();
                    if (mMediaService != null)
                        mMediaService.unsync();

                    mDevice.disconnect();
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

    private void updateNotification() {
        String status = getString(R.string.disconnected);
        if(mDevice != null) {
            if (mState == STATUS_CONNECTING)
                status = getString(R.string.connecting_formatted, mDevice.getName_normalized());
            else if (mState == STATUS_CONNECTED)
                status = getString(R.string.connected_formatted, mDevice.getName_normalized());
        }

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, DeviceDetailActivity.class), 0);

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

    @Override
    public void onEvent(StateEvent event) {
        if (event.didEnter(BleDeviceState.INITIALIZED)) {
            mState = STATUS_CONNECTED;
            updateNotification();
            try {
                replyTo.send(Message.obtain(null, MSG_SET_STATUS, STATUS_CONNECTED, 0));
            } catch (RemoteException ignored) {}

            event.device().read(batteryLevelCharac, new BleDevice.ReadWriteListener()
            {
                @Override public void onEvent(ReadWriteEvent result)
                {
                    if(result.wasSuccess()) try {
                        replyTo.send(Message.obtain(null, MSG_SET_BATTERY_PERCENTAGE, result.data()[0], 0));
                    } catch (RemoteException ignored) {}
                }
            });

            if (mWeatherService != null)
                mWeatherService.sync();
            if (mNotificationService != null)
                mNotificationService.sync();
            if (mMediaService != null)
                mMediaService.sync();
        } else if (event.didEnter(BleDeviceState.DISCONNECTED)) {
            mState = STATUS_DISCONNECTED;
            updateNotification();
            try {
                replyTo.send(Message.obtain(null, MSG_SET_STATUS, STATUS_DISCONNECTED, 0));
            } catch (RemoteException ignored) {}

            if (mWeatherService != null)
                mWeatherService.unsync();
            if (mNotificationService != null)
                mNotificationService.unsync();
            if (mMediaService != null)
                mMediaService.unsync();
        } else if(event.didEnter(BleDeviceState.CONNECTING)) {
            mState = STATUS_CONNECTING;
            updateNotification();
            try {
                replyTo.send(Message.obtain(null, MSG_SET_STATUS, STATUS_CONNECTING, 0));
            } catch (RemoteException ignored) {}
        }
    }
}