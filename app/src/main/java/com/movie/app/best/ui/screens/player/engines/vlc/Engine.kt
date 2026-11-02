package com.movie.app.best.ui.screens.player.engines.vlc

import android.net.Uri
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.movie.app.best.ui.screens.player.engines.PlayerEngine
import com.movie.app.best.ui.screens.player.model.VideoContentScale
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

/**
 * LibVLC "Advance Decoder" engine — the brother of
 * [com.movie.app.best.ui.screens.player.engines.media3.Media3Engine].
 *
 * Stands on [SimpleBasePlayer] (only [getState] is strictly required) and republishes LibVLC's
 * state as a regular media3 [Player], which lets the entire shared player UI drive it unchanged.
 *
 * Phase 1: used for local/downloaded files only (see
 * [com.movie.app.best.ui.screens.player.engines.EngineRouter]). LibVLC seeks any MKV regardless of
 * broken/chained SeekHead structures, which fixes the "seek jumps back to 0:00" bug ExoPlayer has
 * on some files.
 */
@OptIn(UnstableApi::class)
class VlcEngine(
    private val libVlc: LibVLC,
    private val mediaPlayer: MediaPlayer,
) : SimpleBasePlayer(Looper.getMainLooper()), PlayerEngine {

    private companion object {
        const val SEEK_BACK_MS = 30_000L
        const val SEEK_FORWARD_MS = 30_000L
        const val BUFFERING_DONE = 100f

        val UID = Any()

        val AVAILABLE_COMMANDS: Player.Commands = Player.Commands.Builder()
            .addAll(
                Player.COMMAND_PLAY_PAUSE,
                Player.COMMAND_PREPARE,
                Player.COMMAND_STOP,
                Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
                Player.COMMAND_SEEK_BACK,
                Player.COMMAND_SEEK_FORWARD,
                Player.COMMAND_SEEK_TO_DEFAULT_POSITION,
                Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
                Player.COMMAND_GET_TIMELINE,
                Player.COMMAND_GET_METADATA,
                Player.COMMAND_GET_TRACKS,
                Player.COMMAND_SET_SPEED_AND_PITCH,
                Player.COMMAND_SET_TRACK_SELECTION_PARAMETERS,
                Player.COMMAND_SET_MEDIA_ITEM,
                Player.COMMAND_CHANGE_MEDIA_ITEMS,
                Player.COMMAND_SET_VOLUME,
            )
            .build()
    }

    internal val state = VlcPlayerState().apply {
        // Local files: subtitles off by default (matches the Media3 engine's disableSubtitles).
        trackSelectionParameters = TrackSelectionParameters.Builder()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .build()
    }

    private var trackMapping: VlcTrackMapping = VlcTrackMapping.EMPTY
    private var pendingUri: Uri? = null
    private var released = false
    private var userSelectedText = false

    private val eventListener = VlcPlayerEvents { event -> onVlcEvent(event) }

    init {
        mediaPlayer.setEventListener(eventListener)
    }

    // ---------------------------------------------------------------------------------------------
    // Shared surface attach/detach
    // ---------------------------------------------------------------------------------------------

    fun attachSurface(layout: VLCVideoLayout) {
        if (released) return
        mediaPlayer.attachViews(layout, null, true, false)
    }

    fun detachSurface() {
        if (released) return
        runCatching { mediaPlayer.detachViews() }
    }

    fun setContentScale(scale: VideoContentScale) {
        if (released) return
        val type = when (scale) {
            VideoContentScale.STRETCH, VideoContentScale.CROP -> MediaPlayer.ScaleType.SURFACE_FILL
            VideoContentScale.BEST_FIT, VideoContentScale.HUNDRED_PERCENT ->
                MediaPlayer.ScaleType.SURFACE_BEST_FIT
        }
        runCatching { mediaPlayer.setVideoScale(type) }
    }

    // ---------------------------------------------------------------------------------------------
    // State
    // ---------------------------------------------------------------------------------------------

    override fun getState(): State {
        val s = state
        val builder = State.Builder()
            .setAvailableCommands(AVAILABLE_COMMANDS)
            .setPlayWhenReady(s.playWhenReady, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            .setPlaybackState(s.playbackState)
            .setPlaybackSuppressionReason(Player.PLAYBACK_SUPPRESSION_REASON_NONE)
            .setPlayerError(s.playerError)
            .setRepeatMode(s.repeatMode)
            .setShuffleModeEnabled(s.shuffleModeEnabled)
            .setIsLoading(s.isLoading)
            .setSeekBackIncrementMs(SEEK_BACK_MS)
            .setSeekForwardIncrementMs(SEEK_FORWARD_MS)
            .setMaxSeekToPreviousPositionMs(SEEK_BACK_MS)
            .setPlaybackParameters(s.playbackParameters)
            .setTrackSelectionParameters(s.trackSelectionParameters)
            .setVolume(s.volume)
            .setVideoSize(s.videoSize)

        val item = s.mediaItem
        if (item != null) {
            val durationUs =
                if (s.durationMs == C.TIME_UNSET) C.TIME_UNSET else Util.msToUs(s.durationMs)
            val period = PeriodData.Builder(UID)
                .setDurationUs(durationUs)
                .build()
            val mediaItemData = MediaItemData.Builder(UID)
                .setMediaItem(item)
                .setDurationUs(durationUs)
                .setIsSeekable(s.isSeekable)
                .setIsDynamic(false)
                .setTracks(s.currentTracks)
                .setPeriods(listOf(period))
                .build()
            builder.setPlaylist(listOf(mediaItemData))
            builder.setCurrentMediaItemIndex(0)
            builder.setContentPositionMs(s.positionMs)
        } else {
            builder.setPlaylist(emptyList<SimpleBasePlayer.MediaItemData>())
        }
        return builder.build()
    }

    // ---------------------------------------------------------------------------------------------
    // Command handlers
    // ---------------------------------------------------------------------------------------------

    override fun handleSetMediaItems(
        mediaItems: List<MediaItem>,
        startIndex: Int,
        startPositionMs: Long,
    ): ListenableFuture<*> {
        val item = mediaItems.getOrNull(startIndex)
        state.mediaItem = item
        state.durationMs = C.TIME_UNSET
        state.positionMs = if (startPositionMs == C.TIME_UNSET) 0L else startPositionMs
        state.isSeekable = false
        state.playbackState = Player.STATE_IDLE
        state.isLoading = false
        state.playerError = null
        userSelectedText = false
        pendingUri = item?.localConfiguration?.uri
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handlePrepare(): ListenableFuture<*> {
        val uri = pendingUri
        if (uri != null && !released) {
            runCatching {
                val media = Media(libVlc, uri)
                media.setHWDecoderEnabled(true, false)
                mediaPlayer.setMedia(media)
                media.release()
                applyTrackSelection(state.trackSelectionParameters)
                state.playbackState = Player.STATE_BUFFERING
                state.isLoading = true
                mediaPlayer.play()
            }.onFailure { error ->
                state.playerError = PlaybackException(
                    error.message ?: "LibVLC error",
                    error,
                    PlaybackException.ERROR_CODE_UNSPECIFIED,
                )
                state.playbackState = Player.STATE_IDLE
                state.isLoading = false
            }
        }
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        state.playWhenReady = playWhenReady
        if (released) return Futures.immediateVoidFuture()
        runCatching {
            if (playWhenReady) {
                mediaPlayer.play()
            } else {
                mediaPlayer.pause()
            }
        }
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleSeek(
        mediaItemIndex: Int,
        positionMs: Long,
        seekCommand: Int,
    ): ListenableFuture<*> {
        if (released) return Futures.immediateVoidFuture()
        if (positionMs == C.TIME_UNSET) {
            state.positionMs = 0L
            runCatching { mediaPlayer.setTime(0L) }
        } else {
            state.positionMs = positionMs
            runCatching { mediaPlayer.setTime(positionMs) }
        }
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleSetPlaybackParameters(
        playbackParameters: PlaybackParameters,
    ): ListenableFuture<*> {
        state.playbackParameters = playbackParameters
        if (!released) {
            runCatching { mediaPlayer.setRate(playbackParameters.speed) }
        }
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleSetTrackSelectionParameters(
        trackSelectionParameters: TrackSelectionParameters,
    ): ListenableFuture<*> {
        state.trackSelectionParameters = trackSelectionParameters
        if (trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT) ||
            trackSelectionParameters.overrides.values.any { it.type == C.TRACK_TYPE_TEXT }
        ) {
            userSelectedText = true
        }
        if (!released) {
            applyTrackSelection(trackSelectionParameters)
        }
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleSetVolume(volume: Float): ListenableFuture<*> {
        // The app drives the system volume stream, so the player's own gain stays at unity.
        state.volume = volume
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleSetRepeatMode(repeatMode: Int): ListenableFuture<*> {
        state.repeatMode = repeatMode
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleSetShuffleModeEnabled(shuffleModeEnabled: Boolean): ListenableFuture<*> {
        state.shuffleModeEnabled = shuffleModeEnabled
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleStop(): ListenableFuture<*> {
        if (!released) runCatching { mediaPlayer.stop() }
        state.playbackState = Player.STATE_IDLE
        state.isLoading = false
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleRelease(): ListenableFuture<*> {
        if (released) return Futures.immediateVoidFuture()
        released = true
        runCatching { mediaPlayer.setEventListener(null) }
        runCatching { mediaPlayer.detachViews() }
        runCatching { mediaPlayer.stop() }
        runCatching { mediaPlayer.release() }
        runCatching { libVlc.release() }
        return Futures.immediateVoidFuture()
    }

    // ---------------------------------------------------------------------------------------------
    // LibVLC -> media3 bridge
    // ---------------------------------------------------------------------------------------------

    private fun onVlcEvent(event: MediaPlayer.Event) {
        if (released) return
        when (event.type) {
            MediaPlayer.Event.Opening -> {
                state.playbackState = Player.STATE_BUFFERING
                state.isLoading = true
            }
            MediaPlayer.Event.Playing -> {
                refreshTracks()
                refreshVideoSize()
                state.durationMs = mediaPlayer.getLength()
                state.isSeekable = mediaPlayer.isSeekable
                state.positionMs = mediaPlayer.getTime()
                state.playbackState = Player.STATE_READY
                state.isLoading = false
                state.playerError = null
            }
            MediaPlayer.Event.Paused -> {
                state.playbackState = Player.STATE_READY
                state.playWhenReady = false
                state.positionMs = mediaPlayer.getTime()
            }
            MediaPlayer.Event.Stopped -> {
                state.playbackState = Player.STATE_IDLE
                state.isLoading = false
            }
            MediaPlayer.Event.EndReached -> {
                state.playbackState = Player.STATE_ENDED
                state.isLoading = false
            }
            MediaPlayer.Event.EncounteredError -> {
                state.playbackState = Player.STATE_IDLE
                state.isLoading = false
                state.playerError = PlaybackException(
                    "LibVLC playback error",
                    null,
                    PlaybackException.ERROR_CODE_UNSPECIFIED,
                )
            }
            MediaPlayer.Event.TimeChanged -> {
                state.positionMs = event.timeChanged
            }
            MediaPlayer.Event.LengthChanged -> {
                state.durationMs = event.lengthChanged
                state.isSeekable = mediaPlayer.isSeekable
            }
            MediaPlayer.Event.SeekableChanged -> {
                state.isSeekable = event.seekable
            }
            MediaPlayer.Event.Buffering -> {
                val percent = event.buffering
                state.playbackState =
                    if (percent >= BUFFERING_DONE) Player.STATE_READY else Player.STATE_BUFFERING
                state.isLoading = percent < BUFFERING_DONE
            }
            MediaPlayer.Event.Vout -> {
                refreshTracks()
                refreshVideoSize()
            }
            MediaPlayer.Event.ESAdded,
            MediaPlayer.Event.ESDeleted,
            MediaPlayer.Event.ESSelected,
            -> {
                refreshTracks()
            }
            else -> return
        }
        invalidateState()
    }

    private fun refreshTracks() {
        trackMapping = VlcTrackMappers.map(mediaPlayer)
        state.currentTracks = trackMapping.tracks
        if (!userSelectedText && mediaPlayer.getSpuTracksCount() > 0 && mediaPlayer.getSpuTrack() != -1) {
            runCatching { mediaPlayer.setSpuTrack(-1) }
        }
    }

    private fun refreshVideoSize() {
        val videoTrack = mediaPlayer.getCurrentVideoTrack()
        if (videoTrack != null && videoTrack.width > 0 && videoTrack.height > 0) {
            state.videoSize = VideoSize(videoTrack.width, videoTrack.height)
        }
    }

    private fun applyTrackSelection(params: TrackSelectionParameters) {
        val audioOverride = params.overrides.values.firstOrNull { it.type == C.TRACK_TYPE_AUDIO }
        val videoOverride = params.overrides.values.firstOrNull { it.type == C.TRACK_TYPE_VIDEO }
        val textOverride = params.overrides.values.firstOrNull { it.type == C.TRACK_TYPE_TEXT }

        when {
            params.disabledTrackTypes.contains(C.TRACK_TYPE_AUDIO) ->
                runCatching { mediaPlayer.setAudioTrack(-1) }
            audioOverride != null ->
                trackMapping.audioIds[audioOverride.mediaTrackGroup]?.let {
                    runCatching { mediaPlayer.setAudioTrack(it) }
                }
        }
        when {
            params.disabledTrackTypes.contains(C.TRACK_TYPE_VIDEO) ->
                runCatching { mediaPlayer.setVideoTrack(-1) }
            videoOverride != null ->
                trackMapping.videoIds[videoOverride.mediaTrackGroup]?.let {
                    runCatching { mediaPlayer.setVideoTrack(it) }
                }
        }
        when {
            params.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT) ->
                runCatching { mediaPlayer.setSpuTrack(-1) }
            textOverride != null ->
                trackMapping.textIds[textOverride.mediaTrackGroup]?.let {
                    runCatching { mediaPlayer.setSpuTrack(it) }
                }
        }
        refreshTracks()
    }
}
