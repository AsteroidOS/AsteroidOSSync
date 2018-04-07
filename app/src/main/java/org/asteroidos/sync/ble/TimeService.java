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

package org.asteroidos.sync.ble;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;

import com.idevicesinc.sweetblue.BleDevice;

import java.util.Calendar;
import java.util.UUID;

@SuppressWarnings( "deprecation" ) // Before upgrading to SweetBlue 3.0, we don't have an alternative to the deprecated ReadWriteListener
public class TimeService implements BleDevice.ReadWriteListener, SharedPreferences.OnSharedPreferenceChangeListener {
    private static final UUID timeSetCharac = UUID.fromString("00005001-0000-0000-0000-00a57e401d05");

    public static final String PREFS_NAME = "TimePreference";
    public static final String PREFS_SYNC_TIME = "syncTime";
    public static final boolean PREFS_SYNC_TIME_DEFAULT = true;

    private BleDevice mDevice;

    private SharedPreferences mTimeSyncSettings;

    public TimeService(Context ctx, BleDevice device) {
        mDevice = device;
        mTimeSyncSettings = ctx.getSharedPreferences(PREFS_NAME, 0);
        mTimeSyncSettings.registerOnSharedPreferenceChangeListener(this);
    }

    public void sync() {

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateTime();
            }
        }, 500);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updateTime();
    }

    private void updateTime() {
        if(mTimeSyncSettings.getBoolean(PREFS_SYNC_TIME, PREFS_SYNC_TIME_DEFAULT)) {
            byte[] data = new byte[6];
            Calendar c =  Calendar.getInstance();
            data[0] = (byte)(c.get(Calendar.YEAR) - 1900);
            data[1] = (byte)(c.get(Calendar.MONTH));
            data[2] = (byte)(c.get(Calendar.DAY_OF_MONTH));
            data[3] = (byte)(c.get(Calendar.HOUR_OF_DAY));
            data[4] = (byte)(c.get(Calendar.MINUTE));
            data[5] = (byte)(c.get(Calendar.SECOND));
            mDevice.write(timeSetCharac, data, TimeService.this);
        }
    }

    public void unsync() { }

    @Override
    public void onEvent(ReadWriteEvent e) {
        if(!e.wasSuccess())
            Log.e("TimeService", e.status().toString());
    }
}
