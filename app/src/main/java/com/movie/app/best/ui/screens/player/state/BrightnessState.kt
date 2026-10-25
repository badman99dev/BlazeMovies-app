package com.movie.app.best.ui.screens.player.state

import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.movie.app.best.ui.screens.player.extensions.brightnessPercentage
import com.movie.app.best.ui.screens.player.extensions.currentBrightness
import com.movie.app.best.ui.screens.player.extensions.resetScreenBrightness

@Composable
fun rememberBrightnessState(enabled: Boolean = true): BrightnessState {
    val activity = LocalActivity.current
    val state = remember { BrightnessState(activity) }
    // Brightness override is only allowed inside the fullscreen player. Leaving
    // fullscreen (or any other screen) restores the system brightness.
    LaunchedEffect(enabled) {
        state.enabled = enabled
        if (!enabled) activity?.resetScreenBrightness()
    }
    return state
}

@Stable
class BrightnessState(private val activity: android.app.Activity?) {
    /** Only the fullscreen player may override the window brightness. */
    var enabled: Boolean = true

    val maxBrightness: Float = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL

    var currentBrightness: Float by mutableFloatStateOf(activity?.currentBrightness ?: 0.5f)
    var brightnessPercentage: Int by mutableIntStateOf(activity?.brightnessPercentage ?: 50)

    fun updateBrightnessPercentage(percentage: Int) {
        setBrightness(brightness = percentage.coerceIn(0, 100) * maxBrightness / 100)
    }

    fun setBrightness(brightness: Float) {
        val a = activity ?: return
        if (!enabled) return
        val windowAttributes = a.window.attributes
        windowAttributes.screenBrightness = brightness.coerceIn(0f, maxBrightness)
        a.window.attributes = windowAttributes
        currentBrightness = brightness.coerceIn(0f, maxBrightness)
        brightnessPercentage = (currentBrightness / maxBrightness * 100).toInt()
    }
}
