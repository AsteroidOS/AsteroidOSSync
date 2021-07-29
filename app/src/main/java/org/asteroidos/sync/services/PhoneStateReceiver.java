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
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import org.asteroidos.sync.R;
import java.util.Objects;

import static android.telephony.TelephonyManager.ACTION_PHONE_STATE_CHANGED;

public class PhoneStateReceiver extends BroadcastReceiver {

    TelephonyManager telephony;
    public static final String PREFS_NAME = "PhoneStatePreference";
    public static final String PREF_SEND_CALL_STATE = "PhoneCallNotificationForwarding";

    public void onReceive(Context context, Intent intent) {
        if (Objects.equals(intent.getAction(), ACTION_PHONE_STATE_CHANGED)){
            CallStateService callStateService = new CallStateService(context);
            telephony = (TelephonyManager) context
                    .getSystemService(Context.TELEPHONY_SERVICE);
            assert telephony != null;
            telephony.listen(callStateService, PhoneStateListener.LISTEN_CALL_STATE);
        }
    }

    static class CallStateService extends PhoneStateListener {
        private Context context;
        private SharedPreferences prefs;

        CallStateService(Context con) {
            context = con;
            prefs = con.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE);
        }

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            switch (state) {
                case TelephonyManager.CALL_STATE_IDLE:
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    stopRinging();
                    break;
                case TelephonyManager.CALL_STATE_RINGING:
                    startRinging(incomingNumber);
                    break;
            }
        }

        private String getContact(String number) {
            String contact = null;
            ContentResolver cr = context.getContentResolver();
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
            Cursor cursor = cr.query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);
            if (cursor != null) {
                if(cursor.moveToFirst()) {
                    try {
                        contact = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME));
                    } catch (IllegalArgumentException e){
                        e.printStackTrace();
                    }
                }
                cursor.close();
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
