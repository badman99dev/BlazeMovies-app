package com.movie.app.best.data.settings

import android.content.Context

object PlayerRotationSettings {
    private const val PREFS_NAME = "app_player_rotation"
    private const val KEY_AUTO_ROTATE = "auto_rotate"

    private const val DEFAULT_AUTO_ROTATE = true

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isAutoRotate(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AUTO_ROTATE, DEFAULT_AUTO_ROTATE)

    fun setAutoRotate(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_AUTO_ROTATE, enabled).apply()
    }
}
