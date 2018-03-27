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
