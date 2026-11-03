package com.movie.app.best.data.settings

import android.content.Context

/**
 * Local (libVLC) playback ke decoder modes — MX Player jaisa hi model.
 *
 * [Mode.HW_PLUS] LibVLC ka default hybrid hai: hardware (MediaCodec) try karo,
 * codec/device support na kare to software (FFmpeg) pe fallback. Option
 * `:codec=mediacodec,all` (Media.setHWDecoderEnabled(true, false)) yahi karta hai.
 */
object VlcDecoderSettings {

    private const val PREFS_NAME = "app_vlc_decoder"
    private const val KEY_MODE = "decoder_mode"

    enum class Mode(val key: String, val label: String, val description: String) {
        HW_PLUS("hw_plus", "HW+", "Hardware decode, software fallback"),
        HW("hw", "HW", "Force hardware decoder"),
        SW("sw", "SW", "Software decoder (most compatible)"),
        ;

        companion object {
            fun fromKey(key: String?): Mode =
                entries.firstOrNull { it.key == key } ?: HW_PLUS
        }
    }

    fun getMode(context: Context): Mode =
        Mode.fromKey(
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_MODE, Mode.HW_PLUS.key)
        )

    fun setMode(context: Context, mode: Mode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MODE, mode.key)
            .apply()
    }

    /** LibVLC instance level options for the chosen mode. */
    fun libVlcOptions(mode: Mode): List<String> = when (mode) {
        Mode.HW_PLUS -> emptyList()
        Mode.HW -> listOf("--avcodec-hw=mediacodec")
        Mode.SW -> listOf("--avcodec-hw=none")
    }
}