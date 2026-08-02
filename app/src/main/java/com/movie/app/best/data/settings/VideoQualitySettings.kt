package com.movie.app.best.data.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector

object VideoQualitySettings {
    private const val PREFS_NAME = "app_video_quality"
    private const val KEY_MODE = "video_quality_mode"

    const val MODE_AUTO = "auto"
    const val MODE_HIGH = "high"
    const val MODE_DATA_SAVING = "data_saving"

    var cachedMode: String = MODE_AUTO

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getMode(context: Context): String {
        return prefs(context).getString(KEY_MODE, MODE_AUTO) ?: MODE_AUTO
    }

    fun setMode(context: Context, mode: String) {
        prefs(context).edit().putString(KEY_MODE, mode).apply()
        cachedMode = mode
    }

    fun initCache(context: Context) {
        cachedMode = getMode(context)
    }

    fun isAuto(): Boolean = cachedMode == MODE_AUTO
    fun isHigh(): Boolean = cachedMode == MODE_HIGH
    fun isDataSaving(): Boolean = cachedMode == MODE_DATA_SAVING

    fun applyTo(builder: DefaultTrackSelector.Parameters.Builder): DefaultTrackSelector.Parameters.Builder {
        return when (cachedMode) {
            MODE_HIGH -> builder
                .setMaxVideoSize(1920, 1080)
                .setMinVideoSize(0, 0)
                .setForceLowestBitrate(false)
            MODE_DATA_SAVING -> builder
                .setMinVideoSize(0, 0)
                .setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
                .setForceLowestBitrate(true)
            else -> builder
                .setMinVideoSize(0, 0)
                .setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
                .setForceLowestBitrate(false)
        }
    }
}
