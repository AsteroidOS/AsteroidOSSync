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
import android.os.Build
import android.os.Handler
import androidx.media3.common.MediaItem
import androidx.media3.common.Player.*
import com.google.common.base.Optional
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
import java.time.format.DateTimeFormatter
import java.util.Collections
import java.util.Date
import java.util.GregorianCalendar
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

class MediaService(private val mCtx: Context, private val supervisor: MediaSupervisor, private val connectionProvider: IDBusConnectionProvider) : IMediaService, MediaPlayer2, Player {
    private val mNReceiver: NotificationService.NotificationReceiver? = null
    private val hashing = Hashing.goodFastHash(64)

    private lateinit var busSuffix: String

    override fun sync() {
        busSuffix = "x" + Hashing.murmur3_32_fixed(42).hashLong(Date().time).toString()
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

    @MyDBusProperty("org.mpris.MediaPlayer2")
    val CanQuit get() = false

    @MyDBusProperty("org.mpris.MediaPlayer2")
    var Fullscreen: Boolean
        get() = false
        set(_property) {}

    @MyDBusProperty("org.mpris.MediaPlayer2")
    val CanSetFullscreen get() = false

    @MyDBusProperty("org.mpris.MediaPlayer2")
    val CanRaise get() = false

    // TODO: Track List!!! :grin:
    @MyDBusProperty("org.mpris.MediaPlayer2")
    val HasTrackList get() = false

    @MyDBusProperty("org.mpris.MediaPlayer2")
    val Identity get() = "Android"

    @MyDBusProperty("org.mpris.MediaPlayer2", "as")
    val SupportedUriSchemes: List<String> get() = listOf()

    @MyDBusProperty("org.mpris.MediaPlayer2", "as")
    val SupportedMimeTypes: List<String> get() = listOf()

    override fun Raise() {}

    override fun Quit() {}

    @MyDBusProperty("org.mpris.MediaPlayer2.Player")
    val PlaybackStatus: String get() {
        val controller = supervisor.mediaController ?: return "Stopped"
        return runBlocking(Handler(controller.applicationLooper).asCoroutineDispatcher()) {
            if (controller.isPlaying) return@runBlocking "Playing" else if (controller.playbackState == STATE_READY && !controller.playWhenReady) return@runBlocking "Paused" else return@runBlocking "Stopped"
        }
    }

    @MyDBusProperty("org.mpris.MediaPlayer2.Player")
    var LoopStatus: String get() {
        val controller = supervisor.mediaController ?: return "None"
        return runBlocking(Handler(controller.applicationLooper).asCoroutineDispatcher()) {
            return@runBlocking when (controller.repeatMode) {
                REPEAT_MODE_ALL -> "Playlist"
                REPEAT_MODE_ONE -> "Track"
                REPEAT_MODE_OFF -> "None"
                else -> throw IllegalStateException("Unexpected value: ${controller.repeatMode}")
            }
        }
    } set(_property) {
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

    @MyDBusProperty("org.mpris.MediaPlayer2.Player")
    var Rate: Double get() {
        val controller = supervisor.mediaController ?: return 1.0
        return runBlocking(Handler(controller.applicationLooper).asCoroutineDispatcher()) {
            return@runBlocking controller.playbackParameters.speed.toDouble()
        }
    } set(_property) {
        val controller = supervisor.mediaController ?: return
        runBlocking(Handler(controller.applicationLooper).asCoroutineDispatcher()) {
            if (controller.isCommandAvailable(COMMAND_SET_SPEED_AND_PITCH)) {
                controller.setPlaybackSpeed(_property.toFloat())
            }
        }
    }

    @MyDBusProperty("org.mpris.MediaPlayer2.Player")
    var Shuffle: Boolean get() {
        val controller = supervisor.mediaController ?: return false
        return runBlocking(Handler(controller.applicationLooper).asCoroutineDispatcher()) {
            if (controller.isCommandAvailable(COMMAND_SET_SHUFFLE_MODE)) {
                return@runBlocking controller.shuffleModeEnabled
            } else {
                return@runBlocking false
            }
        }
    } set(_property) {
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

    @MyDBusProperty("org.mpris.MediaPlayer2.Player", "a{sv}")
    val Metadata: Map<String, Variant<*>> get() {
        val dummy = Collections.singletonMap<String, Variant<*>>("mpris:trackid", Variant(DBusPath("/org/mpris/MediaPlayer2/TrackList/NoTrack")))

        val controller = supervisor.mediaController ?: return dummy
        return runBlocking(Handler(controller.applicationLooper).asCoroutineDispatcher()) {
            if (controller.currentMediaItem != null) {
                val metadata = mutableMapOf<String, Variant<*>>(
                        "mpris:trackid" to Variant(currentMediaIdObjectPath),
                        "mpris:length" to Variant(controller.contentDuration * 1000L),
                )

                controller.mediaMetadata.albumTitle?.let { metadata["xesam:album"] = Variant(it) }
                controller.mediaMetadata.artist?.let { metadata["xesam:artist"] = Variant(listOf(it), "as") }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    controller.mediaMetadata.recordingYear?.let { metadata["xesam:contentCreated"] = Variant(GregorianCalendar(it, 0, 1).toZonedDateTime().format(DateTimeFormatter.ISO_INSTANT)) }
                }
                controller.mediaMetadata.discNumber?.let { metadata["xesam:discNumber"] = Variant(it) }
                controller.mediaMetadata.genre?.let { metadata["xesam:genre"] = Variant(it) }
                controller.mediaMetadata.title?.let { metadata["xesam:title"] = Variant(it) }
                controller.mediaMetadata.trackNumber?.let { metadata["xesam:trackNumber"] = Variant(it) }

                return@runBlocking metadata
            } else {
                return@runBlocking dummy
            }
        }
    }

    @MyDBusProperty("org.mpris.MediaPlayer2.Player")
    var Volume: Double get() {
        // TODO:XXX:
        val controller = supervisor.mediaController ?: return 0.0
        return runBlocking(Handler(controller.applicationLooper).asCoroutineDispatcher()) {
            return@runBlocking controller.volume.toDouble()
        }
    } set(_property) {
        val controller = supervisor.mediaController ?: return
        runBlocking(Handler(controller.applicationLooper).asCoroutineDispatcher()) {
            if (controller.isCommandAvailable(COMMAND_SET_VOLUME)) {
                controller.volume = _property.toFloat()
            }
        }
    }

    @MyDBusProperty("org.mpris.MediaPlayer2.Player")
    val Position: Long get() {
        val controller = supervisor.mediaController ?: return 0L
        return runBlocking(Handler(controller.applicationLooper).asCoroutineDispatcher()) {
            if (controller.isCommandAvailable(COMMAND_GET_CURRENT_MEDIA_ITEM)) {
                return@runBlocking controller.currentPosition * 1000L
            } else {
                return@runBlocking 0L
            }
        }
    }

    @MyDBusProperty("org.mpris.MediaPlayer2.Player")
    val MinimumRate get() = .25

    @MyDBusProperty("org.mpris.MediaPlayer2.Player")
    val MaximumRate get() = 2.0

    @MyDBusProperty("org.mpris.MediaPlayer2.Player")
    val CanGoNext: Boolean get() {
        val controller = supervisor.mediaController ?: return false
        return runBlocking(Handler(controller.applicationLooper).asCoroutineDispatcher()) {
            return@runBlocking controller.isCommandAvailable(COMMAND_SEEK_TO_NEXT_MEDIA_ITEM) && controller.hasNextMediaItem()
        }
    }

    @MyDBusProperty("org.mpris.MediaPlayer2.Player")
    val CanGoPrevious: Boolean get() {
        val controller = supervisor.mediaController ?: return false
        return runBlocking(Handler(controller.applicationLooper).asCoroutineDispatcher()) {
            return@runBlocking controller.isCommandAvailable(COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM) && controller.hasPreviousMediaItem()
        }
    }

    @MyDBusProperty("org.mpris.MediaPlayer2.Player")
    val CanPlay: Boolean get() {
        val controller = supervisor.mediaController ?: return false
        return runBlocking(Handler(controller.applicationLooper).asCoroutineDispatcher()) {
            return@runBlocking controller.isCommandAvailable(COMMAND_PLAY_PAUSE)
        }
    }

    @MyDBusProperty("org.mpris.MediaPlayer2.Player")
    val CanPause: Boolean get() {
        val controller = supervisor.mediaController ?: return false
        return runBlocking(Handler(controller.applicationLooper).asCoroutineDispatcher()) {
            return@runBlocking controller.isCommandAvailable(COMMAND_PLAY_PAUSE)
        }
    }

    @MyDBusProperty("org.mpris.MediaPlayer2.Player")
    val CanSeek: Boolean get() {
        val controller = supervisor.mediaController ?: return false
        return runBlocking(Handler(controller.applicationLooper).asCoroutineDispatcher()) {
            return@runBlocking controller.isCommandAvailable(COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM) && controller.isCurrentMediaItemSeekable
        }
    }

    @MyDBusProperty("org.mpris.MediaPlayer2.Player")
    val CanControl get() = supervisor.mediaController != null

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

    @Suppress("UNCHECKED_CAST")
    override fun <A : Any?> Get(p0: String?, p1: String?): A =
            MediaService::class.memberProperties
                    .find { it.findAnnotation<MyDBusProperty>()?.dBusInterface == p0 && it.name == p1 }
                    ?.getter?.call(this) as A

    override fun <A : Any?> Set(p0: String?, p1: String?, p2: A) {
        val property = MediaService::class.memberProperties.find { it.findAnnotation<MyDBusProperty>()?.dBusInterface == p0 && it.name == p1 } as? KMutableProperty<*>
        property!!.setter.call(this, p2);
    }

    override fun GetAll(p0: String?): MutableMap<String, Variant<*>> = getProperties(p0!!, Optional.absent())

    private fun getProperties(dBusInterface: String, names: Optional<List<String>> = Optional.absent()): MutableMap<String, Variant<*>> =
            MediaService::class.memberProperties
                    .filter { it.findAnnotation<MyDBusProperty>()?.dBusInterface == dBusInterface && (if (names.isPresent) it.name in names.get() else true) }
                    .associate { it.name to (if (it.findAnnotation<MyDBusProperty>()!!.dBusTypeSig.isNotEmpty())
                        Variant(it.getter.call(this),
                                it.findAnnotation<MyDBusProperty>()!!.dBusTypeSig)
                    else
                        Variant(it.getter.call(this))) }
                    .toMutableMap()

    override fun isRemote(): Boolean = false

    override fun onPositionDiscontinuity(oldPosition: PositionInfo, newPosition: PositionInfo, reason: Int) {
        connectionProvider.acquireDBusConnection { connection ->
            connection.sendMessage(Player.Seeked(objectPath, newPosition.positionMs / 1000L))
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        connectionProvider.acquireDBusConnection { connection ->
            connection.sendMessage(PropertiesChanged(objectPath, "org.mpris.MediaPlayer2.Player", getProperties("org.mpris.MediaPlayer2.Player", Optional.of(listOf("PlaybackStatus"))), listOf()))
        }
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        connectionProvider.acquireDBusConnection { connection ->
            connection.sendMessage(PropertiesChanged(objectPath, "org.mpris.MediaPlayer2.Player", getProperties("org.mpris.MediaPlayer2.Player", Optional.of(listOf("PlaybackStatus"))), listOf()))
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        connectionProvider.acquireDBusConnection { connection ->
            connection.sendMessage(PropertiesChanged(objectPath, "org.mpris.MediaPlayer2.Player", getProperties("org.mpris.MediaPlayer2.Player", Optional.of(listOf("Metadata", "PlaybackStatus"))), listOf()))
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        connectionProvider.acquireDBusConnection { connection ->
            connection.sendMessage(PropertiesChanged(objectPath, "org.mpris.MediaPlayer2.Player", getProperties("org.mpris.MediaPlayer2.Player", Optional.of(listOf("PlaybackStatus"))), listOf()))
        }
    }

    override fun onVolumeChanged(volume: Float) {
        connectionProvider.acquireDBusConnection { connection ->
            connection.sendMessage(PropertiesChanged(objectPath, "org.mpris.MediaPlayer2.Player", getProperties("org.mpris.MediaPlayer2.Player", Optional.of(listOf("Volume"))), listOf()))
        }
    }

    override fun onAvailableCommandsChanged(availableCommands: Commands) {
        connectionProvider.acquireDBusConnection { connection ->
            connection.sendMessage(PropertiesChanged(objectPath, "org.mpris.MediaPlayer2.Player", getProperties("org.mpris.MediaPlayer2.Player", Optional.of(listOf(
                    "CanGoNext",
                    "CanGoPrevious",
                    "CanPlay",
                    "CanPause",
                    "CanSeek",
                    "CanControl",
            ))), listOf()))
        }
    }

    override fun onReset() {
        connectionProvider.acquireDBusConnection { connection: DBusConnection ->
            connection.sendMessage(PropertiesChanged(objectPath, "org.mpris.MediaPlayer2.Player", getProperties("org.mpris.MediaPlayer2.Player", Optional.absent()), listOf()))
        }
    }
}