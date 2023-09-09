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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;

import androidx.annotation.NonNull;

import com.maxmpz.poweramp.player.PowerampAPI;
import com.maxmpz.poweramp.player.PowerampAPIHelper;

import org.asteroidos.sync.asteroid.IAsteroidDevice;
import org.asteroidos.sync.services.NLService;
import org.asteroidos.sync.utils.AsteroidUUIDS;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class MediaService implements IConnectivityService,  MediaSessionManager.OnActiveSessionsChangedListener {

    public static final String TAG = MediaService.class.toString();

    private static final byte MEDIA_COMMAND_PREVIOUS = 0x0;
    private static final byte MEDIA_COMMAND_NEXT     = 0x1;
    private static final byte MEDIA_COMMAND_PLAY     = 0x2;
    private static final byte MEDIA_COMMAND_PAUSE    = 0x3;
    private static final byte MEDIA_COMMAND_VOLUME   = 0x4;

    public static final String PREFS_NAME = "MediaPreferences";
    public static final String PREFS_MEDIA_CONTROLLER_PACKAGE = "media_controller_package";
    public static final String PREFS_MEDIA_CONTROLLER_PACKAGE_DEFAULT = "default";

    private final Context mCtx;
    private final IAsteroidDevice mDevice;
    private SharedPreferences mSettings;

    private MediaController mMediaController = null;
    private MediaSessionManager mMediaSessionManager;

    private int mVolume;

    public MediaService(Context ctx, IAsteroidDevice device) {
        mDevice = device;
        mCtx = ctx;
        device.registerCallback(AsteroidUUIDS.MEDIA_COMMANDS_CHAR, (data) -> {
            if (data == null) return;
            if (mMediaController != null) {
                boolean isPoweramp = mSettings.getString(PREFS_MEDIA_CONTROLLER_PACKAGE, PREFS_MEDIA_CONTROLLER_PACKAGE_DEFAULT)
                        .equals(PowerampAPI.PACKAGE_NAME);

                switch (data[0]) {
                    case MEDIA_COMMAND_PREVIOUS:
                        if(isPoweramp) {
                            PowerampAPIHelper.startPAService(mCtx, new Intent(PowerampAPI.ACTION_API_COMMAND)
                                    .putExtra(PowerampAPI.COMMAND, PowerampAPI.Commands.PREVIOUS));
                        } else {
                            mMediaController.getTransportControls().skipToPrevious();
                        }
                        break;
                    case MEDIA_COMMAND_NEXT:
                        if(isPoweramp) {
                            PowerampAPIHelper.startPAService(mCtx, new Intent(PowerampAPI.ACTION_API_COMMAND)
                                    .putExtra(PowerampAPI.COMMAND, PowerampAPI.Commands.NEXT));
                        } else {
                            mMediaController.getTransportControls().skipToNext();
                        }
                        break;
                    case MEDIA_COMMAND_PLAY:
                        if(isPoweramp) {
                            PowerampAPIHelper.startPAService(mCtx, new Intent(PowerampAPI.ACTION_API_COMMAND)
                                    .putExtra(PowerampAPI.COMMAND, PowerampAPI.Commands.RESUME));
                        } else {
                            mMediaController.getTransportControls().play();
                        }
                        break;
                    case MEDIA_COMMAND_PAUSE:
                        if (isPoweramp) {
                            PowerampAPIHelper.startPAService(mCtx, new Intent(PowerampAPI.ACTION_API_COMMAND)
                                    .putExtra(PowerampAPI.COMMAND, PowerampAPI.Commands.PAUSE));
                        } else {
                            mMediaController.getTransportControls().pause();
                        }
                        break;
                    case MEDIA_COMMAND_VOLUME:
                        if (mMediaController.getPlaybackInfo() != null) {
                            if (data[1] != mVolume) {
                                int delta = Math.abs(mVolume - data[1]);
                                int deviceDelta = 100 / mMediaController.getPlaybackInfo().getMaxVolume();
                                // Change in volume is smaller than the device volume step (i.e. volume won't change)
                                // Increase or decrease the volume by one step anyway to improve UX.
                                if (delta < deviceDelta) {
                                    if (data[1] > mVolume) {
                                        mMediaController.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                                    } else if (data[1] < mVolume) {
                                        mMediaController.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                                    }
                                } else {
                                    // Convert volume range (0-100) to Android device range(0-?).
                                    int volume = (int) (mMediaController.getPlaybackInfo().getMaxVolume() * (data[1] / 100.0));
                                    mMediaController.setVolumeTo(volume, AudioManager.FLAG_SHOW_UI);
                                }
                                // Set theoretical volume.
                                mVolume = data[1];
                            }
                        }
                        break;
                }

            } else if (data[0] != MEDIA_COMMAND_VOLUME){
                Log.d(TAG, "No active media session, starting playback...");

                try {
                    Runtime runtime = Runtime.getRuntime();
                    runtime.exec("input keyevent " + KeyEvent.KEYCODE_MEDIA_PLAY);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        mSettings = mCtx.getSharedPreferences(PREFS_NAME, 0);
    }

    @Override
    public void sync() {
        if (mMediaSessionManager == null) {
            mCtx.getContentResolver().registerContentObserver(android.provider.Settings.System.CONTENT_URI, true, mVolumeChangeObserver);
            try {
                mMediaSessionManager = (MediaSessionManager) mCtx.getSystemService(Context.MEDIA_SESSION_SERVICE);
                List<MediaController> controllers = mMediaSessionManager.getActiveSessions(new ComponentName(mCtx, NLService.class));
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(() -> {
                    onActiveSessionsChanged(controllers);
                    mMediaSessionManager.addOnActiveSessionsChangedListener(this, new ComponentName(mCtx, NLService.class));
                });
            } catch (SecurityException e) {
                Log.w(TAG, "No Notification Access");
            }
        }
    }

    @Override
    public final void unsync() {
        if (mMediaSessionManager != null) {
            mCtx.getContentResolver().unregisterContentObserver(mVolumeChangeObserver);

            mMediaSessionManager.removeOnActiveSessionsChangedListener(this);
            mMediaSessionManager = null;
        }
        if (mMediaController != null) {
            try {
                mMediaController.unregisterCallback(mMediaCallback);
            } catch (IllegalArgumentException ignored) {
            }
            mMediaController = null;
        }
    }

    private void sendVolume(int volume) {
        // Set real volume.
        mVolume = volume;

        byte[] data = new byte[1];
        data[0] = (byte) mVolume;
        mDevice.send(AsteroidUUIDS.MEDIA_VOLUME_CHAR, data, MediaService.this);
    }

    private final ContentObserver mVolumeChangeObserver = new ContentObserver(new Handler()) {
        // The last value of volume send to the watch.
        private int reportedVolume;

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            if (mMediaController != null && mMediaController.getPlaybackInfo() != null) {
                int vol = (100 * mMediaController.getPlaybackInfo().getCurrentVolume()) / mMediaController.getPlaybackInfo().getMaxVolume();

                if (reportedVolume != vol) {
                    reportedVolume = vol;
                    sendVolume(reportedVolume);
                }
            }
        }
    };

    /**
     * Callback for the MediaController.
     */
    private final MediaController.Callback mMediaCallback = new MediaController.Callback() {

        @Override
        public void onAudioInfoChanged(MediaController.PlaybackInfo playbackInfo) {
            super.onAudioInfoChanged(playbackInfo);
        }

        /**
         * Helper method to safely get a text value from a {@link MediaMetadata} as a byte array
         * (UTF-8 encoded).
         *
         * <p>If the field is null, a zero length byte array will be returned.</p>
         *
         * @param metadata the MediaMetadata (assumed to be non-null)
         * @param fieldName the field name
         * @return the field value as a byte array
         */
        private byte[] getTextAsBytes(MediaMetadata metadata, String fieldName) {
            byte[] result;

            CharSequence text = metadata.getText(fieldName);

            if (text != null) {
                result = text.toString().getBytes(StandardCharsets.UTF_8);
            } else {
                result = new byte[]{0};
            }

            return result;
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            super.onMetadataChanged(metadata);

            if (metadata != null) {
                mDevice.send(AsteroidUUIDS.MEDIA_ARTIST_CHAR,
                        getTextAsBytes(metadata, MediaMetadata.METADATA_KEY_ARTIST),
                        MediaService.this);

                mDevice.send(AsteroidUUIDS.MEDIA_ALBUM_CHAR,
                        getTextAsBytes(metadata, MediaMetadata.METADATA_KEY_ALBUM),
                        MediaService.this);

                mDevice.send(AsteroidUUIDS.MEDIA_TITLE_CHAR,
                        getTextAsBytes(metadata, MediaMetadata.METADATA_KEY_TITLE),
                        MediaService.this);

                mVolume = (100 * mMediaController.getPlaybackInfo().getCurrentVolume()) / mMediaController.getPlaybackInfo().getMaxVolume();
                sendVolume(mVolume);
            }
        }

        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackState state) {
            super.onPlaybackStateChanged(state);
            byte[] data = new byte[1];
            data[0] = (byte)(state.getState() == PlaybackState.STATE_PLAYING ?  1 : 0);
            mDevice.send(AsteroidUUIDS.MEDIA_PLAYING_CHAR, data, MediaService.this);
        }

        @Override
        public void onQueueChanged(List<MediaSession.QueueItem> queue) {
            super.onQueueChanged(queue);
        }

        @Override
        public void onQueueTitleChanged(CharSequence title) {
            super.onQueueTitleChanged(title);
        }
    };
    @Override
    public void onActiveSessionsChanged(List<MediaController> controllers) {
        if (controllers.size() > 0) {
            if (mMediaController != null && !controllers.get(0).getSessionToken().equals(mMediaController.getSessionToken())) {
                // Detach current controller
                mMediaController.unregisterCallback(mMediaCallback);
                Log.d(TAG, "MediaController removed");
                mMediaController = null;
            }

            if(mMediaController == null) {
                // Attach new controller
                mMediaController = controllers.get(0);
                mMediaController.registerCallback(mMediaCallback);
                mMediaCallback.onMetadataChanged(mMediaController.getMetadata());
                if (mMediaController.getPlaybackState() != null)
                    mMediaCallback.onPlaybackStateChanged(mMediaController.getPlaybackState());
                Log.d(TAG, "MediaController set: " + mMediaController.getPackageName());
                SharedPreferences.Editor editor = mSettings.edit();
                editor.putString(PREFS_MEDIA_CONTROLLER_PACKAGE, mMediaController.getPackageName());
                editor.apply();
            }
        } else {
            byte[] data = new byte[]{0};
            mDevice.send(AsteroidUUIDS.MEDIA_ARTIST_CHAR, data, MediaService.this);
            mDevice.send(AsteroidUUIDS.MEDIA_ALBUM_CHAR, data, MediaService.this);
            mDevice.send(AsteroidUUIDS.MEDIA_TITLE_CHAR, data, MediaService.this);
        }
    }

    @Override
    public HashMap<UUID, Direction> getCharacteristicUUIDs() {
        HashMap<UUID, Direction> chars = new HashMap<>();
        chars.put(AsteroidUUIDS.MEDIA_TITLE_CHAR, Direction.TO_WATCH);
        chars.put(AsteroidUUIDS.MEDIA_ALBUM_CHAR, Direction.TO_WATCH);
        chars.put(AsteroidUUIDS.MEDIA_ARTIST_CHAR, Direction.TO_WATCH);
        chars.put(AsteroidUUIDS.MEDIA_PLAYING_CHAR, Direction.TO_WATCH);
        chars.put(AsteroidUUIDS.MEDIA_COMMANDS_CHAR, Direction.FROM_WATCH);
        chars.put(AsteroidUUIDS.MEDIA_VOLUME_CHAR, Direction.TO_WATCH);
        return chars;
    }

    @Override
    public final UUID getServiceUUID() {
        return AsteroidUUIDS.MEDIA_SERVICE_UUID;
    }
}
