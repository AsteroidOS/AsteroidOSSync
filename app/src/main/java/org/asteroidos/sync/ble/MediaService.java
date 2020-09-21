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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import androidx.annotation.NonNull;
import android.util.Log;

import com.idevicesinc.sweetblue.BleDevice;
import com.maxmpz.poweramp.player.PowerampAPI;
import com.maxmpz.poweramp.player.PowerampAPIHelper;

import org.asteroidos.sync.services.NLService;
import org.asteroidos.sync.utils.AsteroidUUIDS;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@SuppressWarnings( "deprecation" ) // Before upgrading to SweetBlue 3.0, we don't have an alternative to the deprecated ReadWriteListener
public class MediaService implements BleDevice.ReadWriteListener,  MediaSessionManager.OnActiveSessionsChangedListener {

    private static final byte MEDIA_COMMAND_PREVIOUS = 0x0;
    private static final byte MEDIA_COMMAND_NEXT     = 0x1;
    private static final byte MEDIA_COMMAND_PLAY     = 0x2;
    private static final byte MEDIA_COMMAND_PAUSE    = 0x3;

    public static final String PREFS_NAME = "MediaPreferences";
    public static final String PREFS_MEDIA_CONTROLLER_PACKAGE = "media_controller_package";
    public static final String PREFS_MEDIA_CONTROLLER_PACKAGE_DEFAULT = "default";

    private Context mCtx;
    private BleDevice mDevice;
    private SharedPreferences mSettings;

    private MediaController mMediaController = null;
    private MediaSessionManager mMediaSessionManager;

    public MediaService(Context ctx, BleDevice device)
    {
        mDevice = device;
        mCtx = ctx;

        mSettings = mCtx.getSharedPreferences(PREFS_NAME, 0);
    }

    public void sync() {
        mDevice.enableNotify(AsteroidUUIDS.MEDIA_COMMANDS_CHAR, commandsListener);
        try {
            mMediaSessionManager = (MediaSessionManager) mCtx.getSystemService(Context.MEDIA_SESSION_SERVICE);
            List<MediaController> controllers = mMediaSessionManager.getActiveSessions(new ComponentName(mCtx, NLService.class));
            onActiveSessionsChanged(controllers);
            mMediaSessionManager.addOnActiveSessionsChangedListener(this, new ComponentName(mCtx, NLService.class));
        } catch (SecurityException e) {
            Log.w("MediaService", "No Notification Access");
        }
    }

    public void unsync() {
        mDevice.disableNotify(AsteroidUUIDS.MEDIA_COMMANDS_CHAR);

        if(mMediaSessionManager != null)
            mMediaSessionManager.removeOnActiveSessionsChangedListener(this);
        if (mMediaController != null) {
            try {
                mMediaController.unregisterCallback(mMediaCallback);
            } catch(IllegalArgumentException ignored) {}
            Log.d("MediaService", "MediaController removed");
        }
    }

    private BleDevice.ReadWriteListener commandsListener = new BleDevice.ReadWriteListener() {
        @Override
        public void onEvent(ReadWriteEvent e) {
            if(e.isNotification() && e.charUuid().equals(AsteroidUUIDS.MEDIA_COMMANDS_CHAR)) {
                if (mMediaController != null) {
                    byte data[] = e.data();
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
                    }
                } else {
                    Intent mediaIntent = new Intent(Intent.ACTION_MAIN);
                    mediaIntent.addCategory(Intent.CATEGORY_APP_MUSIC);
                    mediaIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mCtx.startActivity(mediaIntent);
                }
            }
        }
    };

    @Override
    public void onEvent(ReadWriteEvent e) {
        if(!e.wasSuccess())
            Log.e("MediaService", e.status().toString());
    }

    /**
     * Callback for the MediaController.
     */
    private MediaController.Callback mMediaCallback = new MediaController.Callback() {

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
            byte [] result;

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
                mDevice.write(AsteroidUUIDS.MEDIA_ARTIST_CHAR,
                        getTextAsBytes(metadata, MediaMetadata.METADATA_KEY_ARTIST),
                        MediaService.this);

                mDevice.write(AsteroidUUIDS.MEDIA_ALBUM_CHAR,
                        getTextAsBytes(metadata, MediaMetadata.METADATA_KEY_ALBUM),
                        MediaService.this);

                mDevice.write(AsteroidUUIDS.MEDIA_TITLE_CHAR,
                        getTextAsBytes(metadata, MediaMetadata.METADATA_KEY_TITLE),
                        MediaService.this);
            }
        }

        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackState state) {
            super.onPlaybackStateChanged(state);
            byte[] data = new byte[1];
            data[0] = (byte)(state.getState() == PlaybackState.STATE_PLAYING ?  1 : 0);
            mDevice.write(AsteroidUUIDS.MEDIA_PLAYING_CHAR, data, MediaService.this);
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
                Log.d("MediaService", "MediaController removed");
                mMediaController = null;
            }

            if(mMediaController == null) {
                // Attach new controller
                mMediaController = controllers.get(0);
                mMediaController.registerCallback(mMediaCallback);
                mMediaCallback.onMetadataChanged(mMediaController.getMetadata());
                if (mMediaController.getPlaybackState() != null)
                    mMediaCallback.onPlaybackStateChanged(mMediaController.getPlaybackState());
                Log.d("MediaService", "MediaController set: " + mMediaController.getPackageName());
                SharedPreferences.Editor editor = mSettings.edit();
                editor.putString(PREFS_MEDIA_CONTROLLER_PACKAGE, mMediaController.getPackageName());
                editor.apply();
            }
        } else {
            byte[] data = new byte[]{0};
            mDevice.write(AsteroidUUIDS.MEDIA_ARTIST_CHAR, data, MediaService.this);
            mDevice.write(AsteroidUUIDS.MEDIA_ALBUM_CHAR, data, MediaService.this);
            mDevice.write(AsteroidUUIDS.MEDIA_TITLE_CHAR, data, MediaService.this);
        }
    }
}
