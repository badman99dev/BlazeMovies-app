package com.movie.app.best.data.settings

import android.content.Context
import java.security.MessageDigest

/**
 * Local (downloaded / device) playback ke decoder modes.
 *
 * - [Mode.HW_PLUS] (default) libVLC — hardware decode + software fallback
 *                  (`:codec=mediacodec,all`). Broken-SeekHead MKV bhi seek karte hain.
 * - [Mode.HW]      Media3 (ExoPlayer) — sirf MediaCodec, koi software fallback nahi.
 *                  Sirf tab jab user khud sheet se force kare.
 * - [Mode.SW]      libVLC — pure software (ffmpeg), sabse compatible.
 *
 * Preference per-file cache hoti hai — jo last time chala wahi agli baar khulega.
 * HW me seek/codec problem aaye to UI dialog dikha kar HW+ offer karta hai.
 */
object LocalPlaybackSettings {

    private const val PREFS_NAME = "app_local_playback"
    private const val KEY_PREFIX = "mode:"

    enum class Mode(val key: String, val label: String, val description: String) {
        HW("hw", "HW", "Hardware only · Media3 (may break seeking)"),
        HW_PLUS("hw_plus", "HW+", "Hardware + software fallback · VLC"),
        SW("sw", "SW", "Software decoder · VLC (most compatible)"),
        ;

        companion object {
            fun fromKey(key: String?): Mode = entries.firstOrNull { it.key == key } ?: HW_PLUS
        }
    }

    /** HW = Media3, baaki dono libVLC. */
    fun usesVlc(mode: Mode): Boolean = mode != Mode.HW

    /** LibVLC instance level options for a VLC mode. */
    fun libVlcOptions(mode: Mode): List<String> = when (mode) {
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