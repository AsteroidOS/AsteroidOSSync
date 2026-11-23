/*
 * AsteroidOSSync
 * Copyright (c) 2026 AsteroidOS
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package org.asteroidos.sync.connectivity;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;

import org.asteroidos.sync.asteroid.IAsteroidDevice;
import org.asteroidos.sync.utils.AsteroidUUIDS;

import java.util.HashMap;
import java.util.UUID;

public class FindPhoneService implements IConnectivityService {
    public static final String TAG = "FindPhoneService";
    private final Context mCtx;
    private final IAsteroidDevice mDevice;
    private Ringtone mRingtone;

    public FindPhoneService(Context ctx, IAsteroidDevice device) {
        this.mCtx = ctx;
        this.mDevice = device;

        device.registerCallback(AsteroidUUIDS.FINDPHONE_RING_CHAR, data -> {
            if (data == null || data.length < 1) {
                return;
            }
            int value = data[0] & 0xFF;
            Log.d(TAG, "Find phone characteristic value: " + value);
            if (value == 1) {
                startRinging();
            } else if (value == 0) {
                stopRinging();
            }
        });
    }

    private synchronized void startRinging() {
        try {
            if (mRingtone == null) {
                Uri alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                if (alertUri == null) {
                    alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                }
                mRingtone = RingtoneManager.getRingtone(mCtx, alertUri);
                if (mRingtone != null) {
                    mRingtone.setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build());
                }
            }
            if (mRingtone != null && !mRingtone.isPlaying()) {
                mRingtone.play();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing ringtone for find phone", e);
        }
    }

    private synchronized void stopRinging() {
        if (mRingtone != null && mRingtone.isPlaying()) {
            mRingtone.stop();
        }
    }

    @Override
    public void sync() {}

    @Override
    public void unsync() {
        stopRinging();
    }

    @Override
    public HashMap<UUID, Direction> getCharacteristicUUIDs() {
        HashMap<UUID, Direction> chars = new HashMap<>();
        chars.put(AsteroidUUIDS.FINDPHONE_RING_CHAR, Direction.FROM_WATCH);
        return chars;
    }

    @Override
    public UUID getServiceUUID() {
        return AsteroidUUIDS.FINDPHONE_SERVICE_UUID;
    }
}
