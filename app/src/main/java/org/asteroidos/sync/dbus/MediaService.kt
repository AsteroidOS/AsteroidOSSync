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
package org.asteroidos.sync.dbus

import android.content.Context
import android.os.Handler
import androidx.media3.common.MediaItem
import androidx.media3.common.Player.*
import com.google.common.collect.Lists
import com.google.common.hash.Hashing
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import org.asteroidos.sync.media.IMediaService
import org.asteroidos.sync.media.MediaSupervisor
import org.freedesktop.dbus.DBusPath
import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.interfaces.Properties.PropertiesChanged
import org.freedesktop.dbus.types.Variant
import org.mpris.MediaPlayer2
import org.mpris.mediaplayer2.Player
import java.nio.charset.Charset
import java.util.Collections
import java.util.Date

class MediaService(private val mCtx: Context, private val supervisor: MediaSupervisor, private val connectionProvider: IDBusConnectionProvider) : IMediaService, MediaPlayer2, Player {
    private val mNReceiver: NotificationService.NotificationReceiver? = null
    private val hashing = Hashing.goodFastHash(64)

    private val busSuffix = Hashing.murmur3_32_fixed(42).hashLong(Date().time).toString()

    override fun sync() {
        connectionProvider.acquireDBusConnection { connection: DBusConnection ->
            connection.requestBusName("org.mpris.MediaPlayer2.x$busSuffix")
            connection.exportObject("/org/mpris/MediaPlayer2", this@MediaService)
        }
    }

    override fun unsync() {
        connectionProvider.acquireDBusConnection { connection: DBusConnection ->
            connection.unExportObject("/org/mpris/MediaPlayer2")
            connection.releaseBusName("org.mpris.MediaPlayer2.x$busSuffix")
        }
    }

    override fun onReset() {
        connectionProvider.acquireDBusConnection { connection: DBusConnection ->
            connection.sendMessage(PropertiesChanged(objectPath, "org.mpris.MediaPlayer2.Player", Collections.singletonMap("Metadata", Variant(metadata, "a{sv}")) as Map<String, Variant<*>>?, emptyList<String>()))
        }
    }

    override fun canQuit(): Boolean {
        return false
    }

    override fun isFullscreen(): Boolean {
        return false
    }

    override fun setFullscreen(_property: Boolean) {
    }

    override fun canSetFullscreen(): Boolean {
        return false
    }

    override fun canRaise(): Boolean {
        return false
    }

    override fun hasTrackList(): Boolean {
        // TODO: Track List!!! :grin:
        return false
    }

    override fun getIdentity(): String {
        return "Android"
    }

    override fun getSupportedUriSchemes(): List<String> = emptyList<String>()

    override fun getSupportedMimeTypes(): List<String> = emptyList<String>()

    override fun Raise() {}
    override fun Quit() {}
    override fun getPlaybackStatus(): String {
        val controller = supervisor.mediaController ?: return "Stopped"
        return runBlocking(Handler(controller.applicationLooper).asCoroutineDispatcher()) {
            if (controller.isPlaying) return@runBlocking "Playing" else if (controller.playbackState == STATE_READY && !controller.playWhenReady) return@runBlocking "Paused" else return@runBlocking "Stopped"
        }
    }

    override fun getLoopStatus(): String {
        val controller = supervisor.mediaController ?: return "None"
        return runBlocking(Handler(controller.applicationLooper).asCoroutineDispatcher()) {
            return@runBlocking when (controller.repeatMode) {
                REPEAT_MODE_ALL -> "Playlist"
                REPEAT_MODE_ONE -> "Track"
                REPEAT_MODE_OFF -> "None"
                else -> throw IllegalStateException("Unexpected value: ${controller.repeatMode}")
            }
        }
    }

    override fun setLoopStatus(_property: String) {
        val controller = supervisor.mediaController ?: return
        runBlocking(Handler(controller.applicationLooper).asCoroutineDispatcher()) {
            if (controller.isCommandAvailable(COMMAND_SET_REPEAT_MODE)) {
                controller.repeatMode = when (_property) {
                    "None" -> REPEAT_MODE_OFF
                    "Track" -> REPEAT_MODE_ONE
                    "Playlist" -> REPEAT_MODE_ALL
                    else -> throw IllegalStateException("Unexpected value: $_property")
                }
            }
        }
    }

    override fun getRate(): Double {
        val controller = supervisor.mediaController ?: return 1.0
        return runBlocking(Handler(controller.applicationLooper).asCoroutineDispatcher()) {
            return@runBlocking controller.playbackParameters.speed.toDouble()
        }
    }

    override fun setRate(_property: Double) {
        val controller = supervisor.mediaController ?: return
        runBlocking(Handler(controller.applicationLooper).asCoroutineDispatcher()) {
            if (controller.isCommandAvailable(COMMAND_SET_SPEED_AND_PITCH)) {
                controller.setPlaybackSpeed(_property.toFloat())
            }
        }
    }

    override fun isShuffle(): Boolean {
        val controller = supervisor.mediaController ?: return false
        return runBlocking(Handler(controller.applicationLooper).asCoroutineDispatcher()) {
            if (controller.isCommandAvailable(COMMAND_SET_SHUFFLE_MODE)) {
                return@runBlocking controller.shuffleModeEnabled
            } else {
                return@runBlocking false
            }
        }
    }

    override fun setShuffle(_property: Boolean) {
        val controller = supervisor.mediaController ?: return
        runBlocking(Handler(controller.applicationLooper).asCoroutineDispatcher()) {
            if (controller.isCommandAvailable(COMMAND_SET_SHUFFLE_MODE)) {
                controller.shuffleModeEnabled = _property
            }
        }
    }

    private fun mediaToPath(packageName: String, item: MediaItem?): DBusPath {
        val mediaId = Hashing.combineOrdered(Lists.newArrayList(
                hashing.hashString(item?.mediaMetadata?.title ?: "", Charset.defaultCharset()),
                hashing.hashString(item?.mediaId ?: "", Charset.defaultCharset())))
        return DBusPath("/" + packageName.replace('.', '/') + "/" + mediaId)
    }

    private val currentMediaIdObjectPath get() = mediaToPath(supervisor.mediaController?.connectedToken?.packageName ?: "", supervisor.mediaController?.currentMediaItem)

    override fun getMetadata(): Map<String, Variant<*>> {
        val dummy = Collections.singletonMap<String, Variant<*>>("mpris:trackid", Variant(DBusPath("/org/mpris/MediaPlayer2/TrackList/NoTrack")))

        val controller = supervisor.mediaController ?: return dummy
        return runBlocking(Handler(controller.applicationLooper).asCoroutineDispatcher()) {
            if (controller.currentMediaItem != null) {
                return@runBlocking java.util.Map.of(
                        "mpris:trackid", Variant(currentMediaIdObjectPath),
                        "mpris:length", Variant(controller.contentDuration * 1000)
                )
            } else {
                return@runBlocking dummy
            }
        }
    }

    override fun getVolume(): Double {
        // TODO:XXX:
        val controller = supervisor.mediaController ?: return 0.0
        return runBlocking(Handler(controller.applicationLooper).asCoroutineDispatcher()) {
            return@runBlocking controller.volume.toDouble()
        }
    }

    override fun setVolume(_property: Double) {
        val controller = supervisor.mediaController ?: return
        runBlocking(Handler(controller.applicationLooper).asCoroutineDispatcher()) {
            if (controller.isCommandAvailable(COMMAND_SET_VOLUME)) {
                controller.volume = _property.toFloat()
            }
        }
    }

    override fun getPosition(): Long {
        val controller = supervisor.mediaController ?: return 0L
        return runBlocking(Handler(controller.applicationLooper).asCoroutineDispatcher()) {
            if (controller.isCommandAvailable(COMMAND_GET_CURRENT_MEDIA_ITEM)) {
                return@runBlocking controller.currentPosition * 1000L
            } else {
                return@runBlocking 0L
            }
        }
    }

    override fun getMinimumRate(): Double {
        return .25
    }

    override fun getMaximumRate(): Double {
        return 2.0
    }

    override fun canGoNext(): Boolean {
        val controller = supervisor.mediaController ?: return false
        return runBlocking(Handler(controller.applicationLooper).asCoroutineDispatcher()) {
            return@runBlocking controller.isCommandAvailable(COMMAND_SEEK_TO_NEXT_MEDIA_ITEM) && controller.hasNextMediaItem()
        }
    }

    override fun canGoPrevious(): Boolean {
        val controller = supervisor.mediaController ?: return false
        return runBlocking(Handler(controller.applicationLooper).asCoroutineDispatcher()) {
            return@runBlocking controller.isCommandAvailable(COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM) && controller.hasPreviousMediaItem()
        }
    }

    override fun canPlay(): Boolean {
        val controller = supervisor.mediaController ?: return false
        return runBlocking(Handler(controller.applicationLooper).asCoroutineDispatcher()) {
            return@runBlocking controller.isCommandAvailable(COMMAND_PLAY_PAUSE)
        }
    }

    override fun canPause(): Boolean {
        val controller = supervisor.mediaController ?: return false
        return runBlocking(Handler(controller.applicationLooper).asCoroutineDispatcher()) {
            return@runBlocking controller.isCommandAvailable(COMMAND_PLAY_PAUSE)
        }
    }

    override fun canSeek(): Boolean {
        val controller = supervisor.mediaController ?: return false
        return runBlocking(Handler(controller.applicationLooper).asCoroutineDispatcher()) {
            return@runBlocking controller.isCommandAvailable(COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM) && controller.isCurrentMediaItemSeekable
        }
    }

    override fun canControl(): Boolean = supervisor.mediaController != null

    override fun Next() {
        val controller = supervisor.mediaController ?: return
        runBlocking(Handler(controller.applicationLooper).asCoroutineDispatcher()) {
            if (controller.isCommandAvailable(COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)) {
                controller.seekToNextMediaItem()
            }
        }
    }

    override fun Previous() {
        val controller = supervisor.mediaController ?: return
        runBlocking(Handler(controller.applicationLooper).asCoroutineDispatcher()) {
            if (controller.isCommandAvailable(COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)) {
                controller.seekToPreviousMediaItem()
            }
        }
    }

    override fun Pause() {
        val controller = supervisor.mediaController ?: return
        runBlocking(Handler(controller.applicationLooper).asCoroutineDispatcher()) {
            if (controller.isCommandAvailable(COMMAND_PLAY_PAUSE)) {
                controller.pause()
            }
        }
    }

    override fun PlayPause() {
        val controller = supervisor.mediaController ?: return
        runBlocking(Handler(controller.applicationLooper).asCoroutineDispatcher()) {
            if (controller.isCommandAvailable(COMMAND_PLAY_PAUSE)) {
                if (controller.isPlaying) {
                    controller.pause()
                } else {
                    controller.play()
                }
            }
        }
    }

    override fun Stop() {
        val controller = supervisor.mediaController ?: return
        runBlocking(Handler(controller.applicationLooper).asCoroutineDispatcher()) {
            if (controller.isCommandAvailable(COMMAND_STOP)) {
                controller.stop()
            } else {
                controller.pause()
            }
        }
    }

    override fun Play() {
        val controller = supervisor.mediaController ?: return
        runBlocking(Handler(controller.applicationLooper).asCoroutineDispatcher()) {
            if (controller.isCommandAvailable(COMMAND_PLAY_PAUSE)) {
                controller.play()
            }
        }
    }

    override fun Seek(Offset: Long) {
        val controller = supervisor.mediaController ?: return
        runBlocking(Handler(controller.applicationLooper).asCoroutineDispatcher()) {
            if (controller.isCommandAvailable(COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)) {
                controller.seekTo(controller.currentPosition + Offset / 1000L)
            }
        }
    }

    override fun SetPosition(TrackId: DBusPath, Position: Long) {
        val controller = supervisor.mediaController ?: return
        runBlocking(Handler(controller.applicationLooper).asCoroutineDispatcher()) {
            if (controller.isCommandAvailable(COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)) {
                controller.seekTo(Position / 1000L)
            }
        }
    }

    override fun OpenUri(Uri: String) {}

    override fun getObjectPath() = "/org/mpris/MediaPlayer2"

    override fun isRemote(): Boolean = false

    override fun onPositionDiscontinuity(oldPosition: PositionInfo, newPosition: PositionInfo, reason: Int) {
        connectionProvider.acquireDBusConnection { connection ->
            connection.sendMessage(Player.Seeked(objectPath, newPosition.positionMs / 1000L))
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        connectionProvider.acquireDBusConnection { connection ->
            connection.sendMessage(PropertiesChanged(objectPath, "org.mpris.MediaPlayer2.Player", Collections.singletonMap("PlaybackStatus", Variant(playbackStatus)) as Map<String, Variant<*>>?, emptyList()))
        }
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        connectionProvider.acquireDBusConnection { connection ->
            connection.sendMessage(PropertiesChanged(objectPath, "org.mpris.MediaPlayer2.Player", Collections.singletonMap("PlaybackStatus", Variant(playbackStatus)) as Map<String, Variant<*>>?, emptyList()))
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        connectionProvider.acquireDBusConnection { connection ->
            connection.sendMessage(PropertiesChanged(objectPath, "org.mpris.MediaPlayer2.Player", Collections.singletonMap("Metadata", Variant(metadata, "a{sv}")) as Map<String, Variant<*>>?, emptyList()))
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        connectionProvider.acquireDBusConnection { connection ->
            connection.sendMessage(PropertiesChanged(objectPath, "org.mpris.MediaPlayer2.Player", Collections.singletonMap("PlaybackStatus", Variant(playbackStatus)) as Map<String, Variant<*>>?, emptyList()))
        }
    }

    override fun onVolumeChanged(volume: Float) {
        connectionProvider.acquireDBusConnection { connection ->
            connection.sendMessage(PropertiesChanged(objectPath, "org.mpris.MediaPlayer2.Player", Collections.singletonMap("Volume", Variant(playbackStatus)) as Map<String, Variant<*>>?, emptyList()))
        }
    }

    override fun onAvailableCommandsChanged(availableCommands: Commands) {
        connectionProvider.acquireDBusConnection { connection ->
            val map: Map<String, Variant<*>> = java.util.Map.of(
                    "CanGoNext", Variant(canGoNext()),
                    "CanGoPrevious", Variant(canGoPrevious()),
                    "CanPlay", Variant(canPlay()),
                    "CanPause", Variant(canPause()),
                    "CanSeek", Variant(canSeek()),
                    "CanControl", Variant(canControl()),
            )
            connection.sendMessage(PropertiesChanged(objectPath, "org.mpris.MediaPlayer2.Player", map, emptyList()))
        }
    }
}