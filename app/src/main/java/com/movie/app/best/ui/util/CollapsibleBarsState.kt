package com.movie.app.best.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import kotlin.math.abs

/**
 * Manages coordinated auto-hide / auto-show states for the Top Header and Bottom Navigation Bar
 * on vertical scroll gestures (similar to YouTube, Twitter, Instagram and Chrome).
 */
@Stable
class CollapsibleBarsState(
    initialVisible: Boolean = true,
    private val scrollThreshold: Float = 24f
) {
    private val _isBarsVisible = mutableStateOf(initialVisible)
    val isBarsVisible: State<Boolean> = _isBarsVisible

    private var accumulatedScroll = 0f

    fun show() {
        accumulatedScroll = 0f
        _isBarsVisible.value = true
    }

    fun hide() {
        accumulatedScroll = 0f
        _isBarsVisible.value = false
    }

    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val dy = available.y
            // dy < 0 means finger dragged UP (user scrolling DOWN into content) -> HIDE BARS
            // dy > 0 means finger dragged DOWN (user scrolling UP towards header) -> SHOW BARS
            if (abs(dy) > 0.5f) {
                if (dy < 0) {
                    if (accumulatedScroll > 0) accumulatedScroll = 0f
                    accumulatedScroll += dy
                    if (accumulatedScroll < -scrollThreshold && _isBarsVisible.value) {
                        _isBarsVisible.value = false
                    }
                } else if (dy > 0) {
                    if (accumulatedScroll < 0) accumulatedScroll = 0f
                    accumulatedScroll += dy
                    if (accumulatedScroll > scrollThreshold && !_isBarsVisible.value) {
                        _isBarsVisible.value = true
                    }
                }
            }
            return Offset.Zero
        }
    }
}

val LocalCollapsibleBarsState = compositionLocalOf<CollapsibleBarsState?> { null }

@Composable
fun rememberCollapsibleBarsState(
    initialVisible: Boolean = true,
    scrollThreshold: Float = 24f
): CollapsibleBarsState {
    return remember {
        CollapsibleBarsState(
            initialVisible = initialVisible,
            scrollThreshold = scrollThreshold
        )
    }
}
