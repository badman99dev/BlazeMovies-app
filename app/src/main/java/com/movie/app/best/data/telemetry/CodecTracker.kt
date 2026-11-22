package com.movie.app.best.data.telemetry

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector

/**
 * Captures the decoder ExoPlayer actually selects at runtime.
 *
 * Media3 has no public "current decoder" getter, but every decoder query funnels
 * through a [MediaCodecSelector]. We wrap the default one (pure delegation — the
 * returned list is unchanged, so playback behaviour is identical) and remember the
 * first decoder it offers for a video/audio MIME. ExoPlayer instantiates the first
 * usable entry, so this is the decoder in use.
 *
 * Complements [DeviceProfile.videoDecoders] (the full static list).
 */
@OptIn(UnstableApi::class)
object CodecTracker {

    @Volatile
    private var videoDecoder: String? = null

    @Volatile
    private var videoHardware: Boolean = false

    @Volatile
    private var videoSecure: Boolean = false

    @Volatile
    private var audioDecoder: String? = null

    val selector: MediaCodecSelector = MediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
        val infos: List<MediaCodecInfo> =
            MediaCodecSelector.DEFAULT.getDecoderInfos(mimeType, requiresSecureDecoder, requiresTunnelingDecoder)
        runCatching {
            val first = infos.firstOrNull()
            if (first != null) {
                if (mimeType.startsWith("video/")) {
                    videoDecoder = first.name
                    videoHardware = first.hardwareAccelerated
                    videoSecure = requiresSecureDecoder
                } else if (mimeType.startsWith("audio/")) {
                    audioDecoder = first.name
                }
            }
        }
        infos
    }

    fun snapshot(): Map<String, Any?> = linkedMapOf(
        "video_decoder" to videoDecoder,
        "video_hardware" to videoHardware,
        "video_secure" to videoSecure,
        "audio_decoder" to audioDecoder
    )
}
