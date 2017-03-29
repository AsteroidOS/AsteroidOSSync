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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.idevicesinc.sweetblue.BleDevice;

import java.util.Date;
import java.util.UUID;

public class TimeService implements BleDevice.ReadWriteListener {
    public static final UUID timeSetCharac = UUID.fromString("00005001-0000-0000-0000-00a57e401d05");

    private Context mCtx;
    private BleDevice mDevice;

    private TimeSyncReceiver mTReceiver;

    public TimeService(Context ctx, BleDevice device)
    {
        mDevice = device;
        mCtx = ctx;
    }

    public void sync() {
        mTReceiver = new TimeSyncReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("org.asteroidos.sync.TIME_SYNC_LISTENER");
        mCtx.registerReceiver(mTReceiver, filter);
    }

    public void unsync() {
        try {
            mCtx.unregisterReceiver(mTReceiver);
        } catch (IllegalArgumentException ignored) {}
    }

    @Override
    public void onEvent(ReadWriteEvent e) {
        if(!e.wasSuccess())
            Log.e("TimeService", e.status().toString());
    }

    class TimeSyncReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Date dt = new Date();
            byte[] data = new byte[6];
            data[0] = (byte)dt.getYear();
            data[1] = (byte)dt.getMonth();
            data[2] = (byte)dt.getDate();
            data[3] = (byte)dt.getHours();
            data[4] = (byte)dt.getMinutes();
            data[5] = (byte)dt.getSeconds();
            mDevice.write(timeSetCharac, data, TimeService.this);
        }
    }
}
