package com.movie.app.best.ui.util

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Manages coordinated auto-hide / auto-show states for:
 * 1. Top Header: Directly synchronized 1:1 with content scroll offset and velocity without coroutine dispatch lag.
 * 2. Bottom Navigation Bar:
 *    - Hides after content scrolls down ~2 inches (distance threshold, ~280dp).
 *    - Hides immediately if user flings/scrolls fast (~4 inches/sec velocity, < -1500 px/s).
 *    - Reappears immediately on any upward scroll.
 */
@Stable
class CollapsibleBarsState(
    val defaultHeaderHeightPx: Float,
    val twoInchesScrollPx: Float,
    private val scope: CoroutineScope,
    initialVisible: Boolean = true
) {
    // Current active maximum collapse height (can be updated dynamically by screens with larger headers)
    private val _headerHeightPx = mutableFloatStateOf(defaultHeaderHeightPx)
    val headerHeightPx: Float get() = _headerHeightPx.floatValue

    fun updateHeaderHeight(newHeightPx: Float) {
        if (newHeightPx > 0f && _headerHeightPx.floatValue != newHeightPx) {
            _headerHeightPx.floatValue = newHeightPx
            _headerOffset.floatValue = _headerOffset.floatValue.coerceIn(-newHeightPx, 0f)
        }
    }

    fun resetToDefaultHeaderHeight() {
        updateHeaderHeight(defaultHeaderHeightPx)
    }

    // Synchronous direct header offset: 0f (fully expanded) to -headerHeightPx (fully collapsed)
    // Updated instantaneously on the UI thread without coroutine scheduling delay!
    private val _headerOffset = mutableFloatStateOf(if (initialVisible) 0f else -headerHeightPx)
    val headerOffset: Float get() = _headerOffset.floatValue

    // Header visibility flag
    private val _isHeaderVisible = mutableStateOf(initialVisible)
    val isHeaderVisible: State<Boolean> = _isHeaderVisible

    // Bottom Navigation Bar visibility flag
    private val _isBottomBarVisible = mutableStateOf(initialVisible)
    val isBottomBarVisible: State<Boolean> = _isBottomBarVisible

    // Backwards compatibility alias for screens expecting isBarsVisible
    val isBarsVisible: State<Boolean> = _isHeaderVisible

    // Tracks cumulative content scrolled down / up since last direction change
    private var accumulatedDownScroll = 0f
    private var accumulatedUpScroll = 0f

    private var animJob: Job? = null

    private fun animateHeaderTo(target: Float) {
        animJob?.cancel()
        animJob = scope.launch {
            Animatable(_headerOffset.floatValue).animateTo(
                targetValue = target,
                animationSpec = spring(
                    stiffness = Spring.StiffnessMedium,
                    dampingRatio = Spring.DampingRatioNoBouncy
                )
            ) {
                _headerOffset.floatValue = value
            }
        }
    }

    fun show() {
        accumulatedDownScroll = 0f
        accumulatedUpScroll = 0f
        _isHeaderVisible.value = true
        _isBottomBarVisible.value = true
        animateHeaderTo(0f)
    }

    fun hide() {
        accumulatedDownScroll = 0f
        accumulatedUpScroll = 0f
        _isHeaderVisible.value = false
        _isBottomBarVisible.value = false
        animateHeaderTo(-headerHeightPx)
    }

    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val dy = available.y
            if (abs(dy) < 0.5f) return Offset.Zero

            // Cancel any ongoing settle animation so drag is 100% responsive
            animJob?.cancel()

            val maxCollapse = headerHeightPx

            if (dy < 0f) {
                // ── SCROLLING DOWN (Finger moving up, content moving up) ──
                accumulatedUpScroll = 0f
                accumulatedDownScroll += -dy

                // Direct 1:1 synchronous header translation (0 coroutine lag)
                val newHeaderOffset = (_headerOffset.floatValue + dy).coerceIn(-maxCollapse, 0f)
                _headerOffset.floatValue = newHeaderOffset

                if (newHeaderOffset <= -maxCollapse * 0.5f && _isHeaderVisible.value) {
                    _isHeaderVisible.value = false
                }

                // Bottom Navigation Bar: Only hide after user has scrolled ~2 inches of content
                if (accumulatedDownScroll >= twoInchesScrollPx && _isBottomBarVisible.value) {
                    _isBottomBarVisible.value = false
                }
            } else if (dy > 0f) {
                // ── SCROLLING UP (Finger moving down, content moving down) ──
                accumulatedDownScroll = 0f
                accumulatedUpScroll += dy

                // Direct 1:1 synchronous header translation
                val newHeaderOffset = (_headerOffset.floatValue + dy).coerceIn(-maxCollapse, 0f)
                _headerOffset.floatValue = newHeaderOffset

                if (newHeaderOffset > -maxCollapse * 0.5f && !_isHeaderVisible.value) {
                    _isHeaderVisible.value = true
                }

                // Bottom Navigation Bar: Re-show quickly as soon as user scrolls up
                if (accumulatedUpScroll > 24f && !_isBottomBarVisible.value) {
                    _isBottomBarVisible.value = true
                }
            }

            return Offset.Zero
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            val vy = available.y
            val maxCollapse = headerHeightPx

            if (vy < -1200f) {
                // Fast flick downwards into content -> snap header and bottom bar away
                _isBottomBarVisible.value = false
                _isHeaderVisible.value = false
                animateHeaderTo(-maxCollapse)
            } else if (vy > 600f) {
                // Upward flick -> snap header and bottom bar back into view
                _isBottomBarVisible.value = true
                _isHeaderVisible.value = true
                animateHeaderTo(0f)
            } else {
                // Settle header to nearest boundary if partially collapsed
                if (_headerOffset.floatValue > -maxCollapse * 0.5f) {
                    show()
                } else {
                    hide()
                }
            }

            return Velocity.Zero
        }
    }
}

val LocalCollapsibleBarsState = compositionLocalOf<CollapsibleBarsState?> { null }

@Composable
fun rememberCollapsibleBarsState(
    headerContentHeight: Dp = 48.dp,
    twoInchesScroll: Dp = 280.dp,
    initialVisible: Boolean = true
): CollapsibleBarsState {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val totalHeaderHeight = statusBarTop + headerContentHeight
    val headerHeightPx = with(density) { totalHeaderHeight.toPx() }
    val twoInchesScrollPx = with(density) { twoInchesScroll.toPx() }

    return remember(headerHeightPx, twoInchesScrollPx) {
        CollapsibleBarsState(
            defaultHeaderHeightPx = headerHeightPx,
            twoInchesScrollPx = twoInchesScrollPx,
            scope = scope,
            initialVisible = initialVisible
        )
    }
}
