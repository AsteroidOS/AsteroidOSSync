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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;

import org.asteroidos.sync.R;
import java.util.Objects;

import static android.telephony.TelephonyManager.ACTION_PHONE_STATE_CHANGED;

public class PhoneStateReceiver extends BroadcastReceiver {

    public static final String PREFS_NAME = "PhoneStatePreference";
    public static final String PREF_SEND_CALL_STATE = "PhoneCallNotificationForwarding";

    public void onReceive(Context context, Intent intent) {
        if (Objects.equals(intent.getAction(), ACTION_PHONE_STATE_CHANGED)) {
            // Read the call state straight from the broadcast extras. The
            // previous implementation created a new PhoneStateListener on every
            // PHONE_STATE broadcast and registered it via telephony.listen()
            // without ever unregistering it. Because PHONE_STATE fires several
            // times per call (ringing/offhook/idle) and the receiver instance is
            // discarded after onReceive(), listeners accumulated, leaking and
            // delivering duplicate ring notifications to the watch.
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            if (state == null) return;

            CallStateService handler = new CallStateService(context);
            if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
                handler.startRinging(intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER));
            } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)
                    || TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
                handler.stopRinging();
            }
        }
    }

    static class CallStateService {
        private final Context context;
        private final SharedPreferences prefs;

        CallStateService(Context con) {
            context = con;
            prefs = con.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE);
        }

        private String getContact(String number) {
            if (number == null) return null;
            String contact = null;
            ContentResolver cr = context.getContentResolver();
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
            Cursor cursor = cr.query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        contact = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME));
                    }
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } finally {
                    cursor.close();
                }
            }
            return contact;
        }

        private void startRinging(String number) {
            boolean notificationPref = prefs.getBoolean(PREF_SEND_CALL_STATE, true);
            if (notificationPref) {
                String contact = getContact(number);
                if (contact == null) {
                    contact = number;
                }
                Intent i = new  Intent("org.asteroidos.sync.NOTIFICATION_LISTENER");
                i.putExtra("event", "posted");
                i.putExtra("packageName", "org.asteroidos.generic.dialer");
                i.putExtra("id", 56345);
                i.putExtra("appName", context.getResources().getString(R.string.dialer));
                i.putExtra("appIcon", "ios-call");
                i.putExtra("summary", contact);
                i.putExtra("body", number);
                i.putExtra("vibration", "ringtone");

                context.sendBroadcast(i);
            }
        }

        private void stopRinging(){
            Intent i = new Intent("org.asteroidos.sync.NOTIFICATION_LISTENER");
            i.putExtra("event", "removed");
            i.putExtra("id", 56345);
            context.sendBroadcast(i);
        }
    }
}
