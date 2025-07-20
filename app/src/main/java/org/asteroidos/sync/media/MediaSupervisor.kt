/*
 * AsteroidOSSync
 * Copyright (c) 2024 AsteroidOS
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
package org.asteroidos.sync.media

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.media.session.MediaSessionManager
import android.media.session.MediaSessionManager.OnActiveSessionsChangedListener
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import org.asteroidos.sync.connectivity.IService
import org.asteroidos.sync.dbus.DBusNotificationListenerService

class MediaSupervisor(private val mCtx: Context) : IService, OnActiveSessionsChangedListener {

    var mMediaCallback: IMediaService? = null

    var mediaController: MediaController? = null
        private set
    private var mMediaSessionManager: MediaSessionManager? = null

    var mediaControllerPackageName: String? = null
        private set

    private val mSettings: SharedPreferences = mCtx.getSharedPreferences(PREFS_NAME, 0)

    val scope = CoroutineScope(Job() + Dispatchers.Main)

    @OptIn(UnstableApi::class) override fun onActiveSessionsChanged(controllers: List<android.media.session.MediaController>?) {
        if (!controllers.isNullOrEmpty()) {
            mediaController?.removeListener(mMediaCallback!!)
            mediaController = null
            mMediaCallback!!.onReset()

            scope.launch {
                val mediaController = MediaController.Builder(mCtx, SessionToken.createSessionToken(mCtx, controllers.first().sessionToken).await()).buildAsync().await()
                mediaController.addListener(mMediaCallback!!)

                this@MediaSupervisor.mediaController = mediaController

                mMediaCallback!!.onMediaMetadataChanged(mediaController.mediaMetadata)
                mMediaCallback!!.onPlaybackStateChanged(mediaController.playbackState)
                Log.d(TAG, "MediaController set: " + mediaController.connectedToken?.packageName)
                val editor = mSettings.edit()
                editor.putString(PREFS_MEDIA_CONTROLLER_PACKAGE, mediaController.connectedToken?.packageName)
                editor.apply()
                mediaControllerPackageName = mediaController.connectedToken?.packageName
            }
        } else {
            this@MediaSupervisor.mediaController?.removeListener(mMediaCallback!!)
            mediaControllerPackageName = null
            mediaController = null
            mMediaCallback!!.onReset()
        }
    }

    override fun sync() {
        if (mMediaSessionManager == null) {
            try {
                mMediaSessionManager = mCtx.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
                val controllers = mMediaSessionManager?.getActiveSessions(ComponentName(mCtx, DBusNotificationListenerService::class.java))
                val handler = Handler(Looper.getMainLooper())
                handler.post {
                    onActiveSessionsChanged(controllers)
                    mMediaSessionManager?.addOnActiveSessionsChangedListener(this, ComponentName(mCtx, DBusNotificationListenerService::class.java))
                }
            } catch (e: SecurityException) {
                Log.w(TAG, "No Notification Access")
            }
        }
    }

    override fun unsync() {
        if (mMediaSessionManager != null) {
            mMediaSessionManager?.removeOnActiveSessionsChangedListener(this)
            mMediaSessionManager = null
        }
        if (mediaController != null) {
            try {
                mediaController?.removeListener(mMediaCallback!!)
            } catch (ignored: IllegalArgumentException) {
            }
            mediaController = null
        }
    }

    companion object {
        val TAG = MediaSupervisor::class.java.toString()
//        private const val MEDIA_COMMAND_PREVIOUS: Byte = 0x0
//        private const val MEDIA_COMMAND_NEXT: Byte = 0x1
//        private const val MEDIA_COMMAND_PLAY: Byte = 0x2
//        private const val MEDIA_COMMAND_PAUSE: Byte = 0x3
//        private const val MEDIA_COMMAND_VOLUME: Byte = 0x4
        const val PREFS_NAME = "MediaPreferences"
        const val PREFS_MEDIA_CONTROLLER_PACKAGE = "media_controller_package"
        const val PREFS_MEDIA_CONTROLLER_PACKAGE_DEFAULT = "default"
    }
}