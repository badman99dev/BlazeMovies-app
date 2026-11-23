package com.movie.app.best.data.settings

import android.content.Context
import android.content.SharedPreferences

/**
 * Visual / performance preferences.
 *
 * Both flags default to `false` (i.e. the effects are enabled). They are meant
 * for low-end devices where the frosted navigation bar and the hero-slider
 * backdrop blur cost too much battery / CPU.
 *
 * Values are persisted immediately, but the in-memory cache is only refreshed in
 * [initCache] (called from the Application). That means a change never applies to
 * the running process — it takes effect on the next app start, exactly as the UI
 * tells the user.
 */
object VisualSettings {
    private const val PREFS_NAME = "app_visual"
    private const val KEY_NAV_GLASS_DISABLED = "nav_glass_disabled"
    private const val KEY_SLIDER_BLUR_DISABLED = "slider_blur_disabled"

    /** True when the frosted-glass bottom nav bar is turned off. */
    var navGlassDisabled: Boolean = false
        private set

    /** True when the hero-slider background blur is turned off. */
    var sliderBlurDisabled: Boolean = false
        private set

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isNavGlassDisabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NAV_GLASS_DISABLED, false)

    fun isSliderBlurDisabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SLIDER_BLUR_DISABLED, false)

    /**
     * Persists the choice. Deliberately does NOT update [navGlassDisabled] so the
     * running composition keeps the current look until the next app restart.
     */
    fun setNavGlassDisabled(context: Context, disabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_NAV_GLASS_DISABLED, disabled).apply()
    }

    /**
     * Persists the choice. Deliberately does NOT update [sliderBlurDisabled] so the
     * running composition keeps the current look until the next app restart.
     */
    fun setSliderBlurDisabled(context: Context, disabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_SLIDER_BLUR_DISABLED, disabled).apply()
    }

    fun initCache(context: Context) {
        navGlassDisabled = isNavGlassDisabled(context)
        sliderBlurDisabled = isSliderBlurDisabled(context)
    }
}