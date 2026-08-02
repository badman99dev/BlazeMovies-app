package com.movie.app.best.data.settings

import android.content.Context
import android.content.SharedPreferences

object ModerationSettings {
    private const val PREFS_NAME = "app_moderation"
    private const val KEY_ENABLED = "moderation_enabled"
    private const val KEY_MODE = "moderation_mode"

    const val MODE_BLUR = "blur"
    const val MODE_HIDE = "hide"

    var cachedEnabled: Boolean = true
    var cachedMode: String = MODE_BLUR

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_ENABLED, true)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
        cachedEnabled = enabled
    }

    fun getMode(context: Context): String {
        return prefs(context).getString(KEY_MODE, MODE_BLUR) ?: MODE_BLUR
    }

    fun setMode(context: Context, mode: String) {
        prefs(context).edit().putString(KEY_MODE, mode).apply()
        cachedMode = mode
    }

    fun initCache(context: Context) {
        cachedEnabled = isEnabled(context)
        cachedMode = getMode(context)
    }

    fun effectiveShouldBlur(movie: com.movie.app.best.data.model.Movie): Boolean {
        if (!cachedEnabled) return false
        if (cachedMode != MODE_BLUR) return false
        return movie.shouldBlurPoster
    }

    fun shouldHide(context: Context, movie: com.movie.app.best.data.model.Movie): Boolean {
        if (!isEnabled(context)) return false
        if (getMode(context) != MODE_HIDE) return false
        return movie.shouldBlurPoster
    }

    fun shouldBlur(context: Context, movie: com.movie.app.best.data.model.Movie): Boolean {
        if (!isEnabled(context)) return false
        if (getMode(context) != MODE_BLUR) return false
        return movie.shouldBlurPoster
    }

    fun shouldBlurDetail(context: Context, contentModeration: com.movie.app.best.data.model.ContentModeration?): Boolean {
        if (!isEnabled(context)) return false
        if (getMode(context) != MODE_BLUR) return false
        return contentModeration?.isPosterSexual == true
    }

    fun shouldHideDetail(context: Context, contentModeration: com.movie.app.best.data.model.ContentModeration?): Boolean {
        if (!isEnabled(context)) return false
        if (getMode(context) != MODE_HIDE) return false
        return contentModeration?.hasAnyFlag == true
    }

    fun shouldBlurScreenshots(context: Context, contentModeration: com.movie.app.best.data.model.ContentModeration?): Boolean {
        if (!isEnabled(context)) return false
        if (getMode(context) != MODE_BLUR) return false
        return contentModeration?.isScreenshotsSexual == true
    }

    fun shouldBlurStoryline(context: Context, contentModeration: com.movie.app.best.data.model.ContentModeration?): Boolean {
        if (!isEnabled(context)) return false
        if (getMode(context) != MODE_BLUR) return false
        return contentModeration?.isStorylineSexual == true
    }

    fun shouldBlur(context: Context, contentModeration: Map<String, String>?): Boolean {
        if (!isEnabled(context)) return false
        if (getMode(context) != MODE_BLUR) return false
        return contentModeration?.get("poster") == "sexual"
    }

    fun shouldHide(context: Context, contentModeration: Map<String, String>?): Boolean {
        if (!isEnabled(context)) return false
        if (getMode(context) != MODE_HIDE) return false
        return contentModeration?.get("poster") == "sexual" || contentModeration?.get("screenshots") == "sexual" || contentModeration?.get("storyline") == "sexual"
    }

    fun filterMovies(context: Context, movies: List<com.movie.app.best.data.model.Movie>): List<com.movie.app.best.data.model.Movie> {
        if (!isEnabled(context) || getMode(context) != MODE_HIDE) return movies
        return movies.filter { !it.shouldBlurPoster }
    }
}
