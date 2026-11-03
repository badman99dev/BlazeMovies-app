package com.movie.app.best.data.settings

import android.content.Context
import java.security.MessageDigest

/**
 * Local (downloaded / device) playback ke decoder modes.
 *
 * Teeno modes libVLC pe chalte hain (demuxer VLC ka hi rehta hai, isliye
 * chained-SeekHead waale MKV me bhi seek bar sahi chalta hai):
 *
 * - [Mode.HW_PLUS] (default) hardware decode + software fallback
 *                  (`:codec=mediacodec,all`)
 * - [Mode.HW]      force MediaCodec only — koi software fallback nahi
 * - [Mode.SW]      pure software (ffmpeg), sabse compatible
 *
 * Preference per-file cache hoti hai — jo last time chala wahi agli baar khulega.
 */
object LocalPlaybackSettings {

    private const val PREFS_NAME = "app_local_playback"
    private const val KEY_PREFIX = "mode:"

    enum class Mode(val key: String, val label: String) {
        HW("hw", "HW"),
        HW_PLUS("hw_plus", "HW+"),
        SW("sw", "SW"),
        ;

        companion object {
            fun fromKey(key: String?): Mode = entries.firstOrNull { it.key == key } ?: HW_PLUS
        }
    }

    /** LibVLC instance level options for the chosen decoder. */
    fun libVlcOptions(mode: Mode): List<String> = when (mode) {
        Mode.HW -> listOf("--avcodec-hw=mediacodec")
        Mode.SW -> listOf("--avcodec-hw=none")
        else -> emptyList()
    }

    fun modeFor(context: Context, fileUrl: String): Mode {
        if (fileUrl.isEmpty()) return Mode.HW_PLUS
        val raw = prefs(context).getString(KEY_PREFIX + fileKey(fileUrl), null)
        return Mode.fromKey(raw)
    }

    fun setMode(context: Context, fileUrl: String, mode: Mode) {
        if (fileUrl.isEmpty()) return
        prefs(context).edit().putString(KEY_PREFIX + fileKey(fileUrl), mode.key).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Stable per-file key (path/URL hashed, prefs clean rehta hai). */
    private fun fileKey(fileUrl: String): String {
        val digest = MessageDigest.getInstance("MD5").digest(fileUrl.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}