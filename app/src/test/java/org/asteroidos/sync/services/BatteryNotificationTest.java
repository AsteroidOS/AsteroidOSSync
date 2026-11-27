package org.asteroidos.sync.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Messenger;
import org.asteroidos.sync.asteroid.AsteroidBleManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, qualifiers = "en")
public class BatteryNotificationTest {
    private SynchronizationService service;
    private NotificationManager manager;
    private BluetoothDevice device;

    @Before public void prepare() {
        service = Robolectric.buildService(SynchronizationService.class).get();
        manager = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(new NotificationChannel("synchronizationservice_channel_id_01", "Sync", NotificationManager.IMPORTANCE_LOW));
        ReflectionHelpers.setField(service, "mNM", manager);
        ReflectionHelpers.setField(service, "bleServices", new HashMap<>());
        ReflectionHelpers.setField(service, "nonBleServices", new ArrayList<>());
        device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice("00:11:22:33:44:55");
        Shadows.shadowOf(device).setName("dory");
        service.mDevice = device;
        service.onDeviceConnecting(device);
        service.onDeviceConnected(device);
    }

    private void battery(int value) {
        AsteroidBleManager.BatteryLevelEvent event = new AsteroidBleManager.BatteryLevelEvent();
        event.battery = value;
        service.handleUpdateBatteryPercentage(event);
    }
    private Notification notification() { return Shadows.shadowOf(manager).getNotification(2725); }
    private String text() { return notification().extras.getCharSequence(Notification.EXTRA_TEXT).toString(); }

    @Test public void unknownBatteryDoesNotDisplayZero() { assertEquals("Connected to dory", text()); }
    @Test public void batteryUpdatesExistingNotification() {
        battery(82);
        assertEquals("Connected to dory (battery 82%)", text());
        battery(81);
        assertEquals("Connected to dory (battery 81%)", text());
        assertEquals(1, Shadows.shadowOf(manager).getAllNotifications().size());
        assertTrue((notification().flags & Notification.FLAG_ONLY_ALERT_ONCE) != 0);
    }
    @Test public void zeroAndHundredAreValid() {
        battery(0);
        assertEquals("Connected to dory (battery 0%)", text());
        battery(100);
        assertEquals("Connected to dory (battery 100%)", text());
    }
    @Test public void invalidValuesAreIgnored() {
        battery(42); battery(-1); battery(101); battery(255);
        assertEquals("Connected to dory (battery 42%)", text());
    }
    @Test public void reconnectDoesNotReuseOldBattery() {
        battery(64);
        service.onDeviceDisconnected(device, 0);
        assertEquals(-1, service.batteryPercentage);
        service.onDeviceConnecting(device);
        service.onDeviceConnected(device);
        assertEquals("Connected to dory", text());
    }
    @Test public void batteryUpdateDoesNotSendConnectionStatus() {
        List<Integer> messages = new ArrayList<>();
        ReflectionHelpers.setField(service, "replyTo", new Messenger(new Handler(Looper.getMainLooper(), message -> {
            messages.add(message.what); return true;
        })));
        battery(55);
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertEquals(java.util.Collections.singletonList(SynchronizationService.MSG_SET_BATTERY_PERCENTAGE), messages);
    }
    @Test @Config(qualifiers = "it") public void italianText() {
        battery(73);
        assertEquals("Connesso a dory (batteria 73%)", text());
    }
}
