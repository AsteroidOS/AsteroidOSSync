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

package org.asteroidos.sync.connectivity;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;

public class SilentModeService implements SharedPreferences.OnSharedPreferenceChangeListener, IService {

    public static final String PREFS_NAME = "AppPreferences";
    public static final String PREF_RINGER = "PhoneRingModeOnConnection";
    private static final String PREF_ORIG_RINGER = "OriginalRingMode";
    private final SharedPreferences prefs;
    private Boolean notificationPref;
    private final AudioManager am;

    public SilentModeService(Context con) {
        prefs = con.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE);
        prefs.registerOnSharedPreferenceChangeListener(this);
        am = (AudioManager) con.getSystemService(Context.AUDIO_SERVICE);

    }

    @Override
    public final void sync() {
        if (notificationPref == null) {
            notificationPref = prefs.getBoolean(PREF_RINGER, false);

            int currentRinger = am.getRingerMode();
            // If silence-on-connect is enabled and the phone is already silent,
            // a previous session was almost certainly killed before unsync()
            // could restore the ringer. Recording SILENT as the "original" mode
            // would leave the user permanently muted, so fall back to NORMAL.
            if (notificationPref && currentRinger == AudioManager.RINGER_MODE_SILENT) {
                currentRinger = AudioManager.RINGER_MODE_NORMAL;
            }

            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(PREF_ORIG_RINGER, currentRinger);
            editor.apply();

            if (notificationPref) {
                am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            }
        }
    }

    @Override
    public final void unsync() {
        if (notificationPref != null) {
            notificationPref = prefs.getBoolean(PREF_RINGER, false);
            if (notificationPref) {
                int origRingerMode = prefs.getInt(PREF_ORIG_RINGER, AudioManager.RINGER_MODE_NORMAL);
                am.setRingerMode(origRingerMode);
            }
	    notificationPref = null;
        }
    }

    @Override
    public final void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        notificationPref = prefs.getBoolean(PREF_RINGER, false);
        if (notificationPref) {
            am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
        } else {
            am.setRingerMode(prefs.getInt(PREF_ORIG_RINGER, am.getRingerMode()));
        }
    }
}
