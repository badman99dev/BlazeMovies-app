package com.movie.app.best.ui.screens.player.engines.vlc

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.TrackGroup
import androidx.media3.common.Tracks
import org.videolan.libvlc.MediaPlayer

/** Result of mapping LibVLC's flat track descriptions into the media3 track model. */
internal class VlcTrackMapping(
    val tracks: Tracks,
    val audioIds: Map<TrackGroup, Int>,
    val textIds: Map<TrackGroup, Int>,
    val videoIds: Map<TrackGroup, Int>,
) {
    companion object {
        val EMPTY = VlcTrackMapping(Tracks.EMPTY, emptyMap(), emptyMap(), emptyMap())
    }
}

/**
 * Builds media3 [Tracks] out of LibVLC's `TrackDescription` arrays so the shared track selector UI
 * works unchanged. Brother of
 * [com.movie.app.best.ui.screens.player.engines.media3.Media3TrackMappers].
 */
internal object VlcTrackMappers {

    fun map(mediaPlayer: MediaPlayer): VlcTrackMapping {
        val groups = ArrayList<Tracks.Group>()
        val audioIds = LinkedHashMap<TrackGroup, Int>()
        val textIds = LinkedHashMap<TrackGroup, Int>()
        val videoIds = LinkedHashMap<TrackGroup, Int>()

        mediaPlayer.getAudioTracks()?.filter { it.isRealTrack() }?.forEach { track ->
            val group = TrackGroup(
                Format.Builder()
                    .setId("vlc-audio-${track.id}")
                    .setLabel(track.name)
                    .setSampleMimeType(MimeTypes.AUDIO_UNKNOWN)
                    .build(),
            )
            if (audioIds.put(group, track.id) == null) {
                groups += Tracks.Group(
                    group,
                    /* adaptiveSupported= */ false,
                    /* trackSupport= */ intArrayOf(C.FORMAT_HANDLED),
                    /* trackSelected= */ booleanArrayOf(track.id == mediaPlayer.getAudioTrack()),
                )
            }
        }

        mediaPlayer.getVideoTracks()?.filter { it.isRealTrack() }?.forEach { track ->
            val group = TrackGroup(
                Format.Builder()
                    .setId("vlc-video-${track.id}")
                    .setLabel(track.name)
                    .setSampleMimeType(MimeTypes.VIDEO_UNKNOWN)
                    .build(),
            )
            if (videoIds.put(group, track.id) == null) {
                groups += Tracks.Group(
                    group,
                    /* adaptiveSupported= */ false,
                    /* trackSupport= */ intArrayOf(C.FORMAT_HANDLED),
                    /* trackSelected= */ booleanArrayOf(track.id == mediaPlayer.getVideoTrack()),
                )
            }
        }

        mediaPlayer.getSpuTracks()?.filter { it.isRealTrack() }?.forEach { track ->
            val group = TrackGroup(
                Format.Builder()
                    .setId("vlc-text-${track.id}")
                    .setLabel(track.name)
                    .setSampleMimeType(MimeTypes.TEXT_UNKNOWN)
                    .build(),
            )
            if (textIds.put(group, track.id) == null) {
                groups += Tracks.Group(
                    group,
                    /* adaptiveSupported= */ false,
                    /* trackSupport= */ intArrayOf(C.FORMAT_HANDLED),
                    /* trackSelected= */ booleanArrayOf(track.id == mediaPlayer.getSpuTrack()),
                )
            }
        }

        return VlcTrackMapping(Tracks(groups), audioIds, textIds, videoIds)
    }

    /**
     * libVLC exposes a synthetic "Disable" pseudo-track (negative id) in its track description
     * arrays. It must stay out of the media3 track model: otherwise it reports `isSelected=true`
     * while subtitles are OFF, which breaks the CC single-tap toggle (it would always disable).
     */
    private fun MediaPlayer.TrackDescription.isRealTrack(): Boolean =
        id >= 0 && !name.equals("Disable", ignoreCase = true) && !name.equals("None", ignoreCase = true)
}
