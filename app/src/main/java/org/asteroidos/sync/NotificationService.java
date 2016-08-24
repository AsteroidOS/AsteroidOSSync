package org.asteroidos.sync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.idevicesinc.sweetblue.BleDevice;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

public class NotificationService implements BleDevice.ReadWriteListener {
    public static final UUID notificationUpdateCharac   = UUID.fromString("00009001-0000-0000-0000-00a57e401d05");
    public static final UUID notificationFeedbackCharac = UUID.fromString("00009002-0000-0000-0000-00a57e401d05");

    private Context mCtx;
    private BleDevice mDevice;

    public NotificationService(Context ctx, BleDevice device)
    {
        mDevice = device;
        mCtx = ctx;
    }

    public void sync() {
        mDevice.enableNotify(notificationFeedbackCharac);

        NotificationReceiver nReceiver = new NotificationReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("org.asteroidos.sync.NOTIFICATION_LISTENER");
        mCtx.registerReceiver(nReceiver, filter);
    }

    @Override
    public void onEvent(ReadWriteEvent e) {
        if(!e.wasSuccess())
            Log.e("WeatherService", e.status().toString());
    }

    class NotificationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String event = intent.getStringExtra("event");
            if (Objects.equals(event, "posted")) {
                String title = intent.getStringExtra("title");
                byte[] data = title.getBytes(StandardCharsets.UTF_8);
                mDevice.write(notificationUpdateCharac, data, NotificationService.this);
            }
        }
    }
}
