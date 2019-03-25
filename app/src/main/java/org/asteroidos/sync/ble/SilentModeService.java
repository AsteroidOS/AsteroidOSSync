/*
 * Copyright (C) 2019 - Justus Tartz <git@jrtberlin.de>
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

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;

public class SilentModeService implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String PREFS_NAME = "AppPreferences";
    public static final String PREF_RINGER = "PhoneRingModeOnConnection";
    private static final String PREF_ORIG_RINGER = "OriginalRingMode";
    SharedPreferences prefs;
    Boolean notificationPref;
    Context context;

    public SilentModeService(Context con) {
        prefs = con.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE);
        context = con;
        prefs.registerOnSharedPreferenceChangeListener(this);

    }
    public void onConnect() {
        notificationPref = prefs.getBoolean(PREF_RINGER, false);
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        int origRingerMode = am.getRingerMode();
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PREF_ORIG_RINGER, origRingerMode);
        editor.apply();

        if (notificationPref){
            am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
        }
    }

    public void onDisconnect(){
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int origRingerMode = prefs.getInt(PREF_ORIG_RINGER, AudioManager.MODE_NORMAL);
        am.setRingerMode(origRingerMode);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        notificationPref = prefs.getBoolean(PREF_RINGER, false);
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (notificationPref){
            am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
        } else {
            am.setRingerMode(prefs.getInt(PREF_ORIG_RINGER, AudioManager.MODE_NORMAL));
        }
    }
}
