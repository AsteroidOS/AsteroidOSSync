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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import org.asteroidos.sync.MainActivity;

import java.util.Objects;

public class AutostartService extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent)
    {
        if(Objects.equals(intent.getAction(), Intent.ACTION_BOOT_COMPLETED)) {
            SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
            String defaultDevMacAddr = prefs.getString(MainActivity.PREFS_DEFAULT_MAC_ADDR, "");

            if (defaultDevMacAddr.length() > 0) {
                Intent mSyncServiceIntent = new Intent(context, SynchronizationService.class);
                context.startService(mSyncServiceIntent);
            }
        }
    }
}
