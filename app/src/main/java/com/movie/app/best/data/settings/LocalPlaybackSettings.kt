package com.movie.app.best.data.settings

import android.content.Context
import java.security.MessageDigest

/**
 * Local (downloaded / device) playback ke decoder modes.
 *
 * - [Mode.HW]      Media3 (ExoPlayer) — sirf MediaCodec, koi software fallback nahi.
 *                  Battery/performance ke liye best.
 * - [Mode.HW_PLUS] libVLC — hardware decode + software fallback (`:codec=mediacodec,all`).
 * - [Mode.SW]      libVLC — pure software (ffmpeg), sabse compatible.
 *
 * HW (Media3) pe ek file chal na paaye (unseekable MKV / unsupported codec) to player
 * automatically HW+ (libVLC) pe switch karta hai aur us file ke liye HW+ pin ho jata hai.
 * Preference per-file cache hoti hai — jo last time chala wahi agli baar khulega.
 */
object LocalPlaybackSettings {

    private const val PREFS_NAME = "app_local_playback"
    private const val KEY_PREFIX = "mode:"

    enum class Mode(val key: String, val label: String, val description: String) {
        HW("hw", "HW", "Hardware only · Media3 (battery saver)"),
        HW_PLUS("hw_plus", "HW+", "Hardware + software fallback · VLC"),
        SW("sw", "SW", "Software decoder · VLC (most compatible)"),
        ;

        companion object {
            fun fromKey(key: String?): Mode = entries.firstOrNull { it.key == key } ?: HW
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
        if (fileUrl.isEmpty()) return Mode.HW
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