package com.movie.app.best.ui.util

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
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
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Manages coordinated auto-hide / auto-show states for:
 * 1. Top Header: Directly synchronized 1:1 with content scroll offset and velocity.
 * 2. Bottom Navigation Bar:
 *    - Hides after content scrolls down ~2 inches (distance threshold, ~280dp).
 *    - Hides immediately if user flings/scrolls fast (~4 inches/sec velocity, < -1500 px/s).
 *    - Reappears immediately on any upward scroll.
 */
@Stable
class CollapsibleBarsState(
    val headerHeightPx: Float,
    val twoInchesScrollPx: Float,
    private val scope: CoroutineScope,
    initialVisible: Boolean = true
) {
    // Header offset: 0f (fully expanded) to -headerHeightPx (fully collapsed)
    private val _headerOffsetAnim = Animatable(if (initialVisible) 0f else -headerHeightPx)
    val headerOffset: Float get() = _headerOffsetAnim.value

    // Header visibility flag (used by AnimatedVisibility on screens with collapsing columns)
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

    fun show() {
        accumulatedDownScroll = 0f
        accumulatedUpScroll = 0f
        _isHeaderVisible.value = true
        _isBottomBarVisible.value = true
        scope.launch {
            _headerOffsetAnim.animateTo(
                0f,
                spring(
                    stiffness = Spring.StiffnessMediumLow,
                    dampingRatio = Spring.DampingRatioNoBouncy
                )
            )
        }
    }

    fun hide() {
        accumulatedDownScroll = 0f
        accumulatedUpScroll = 0f
        _isHeaderVisible.value = false
        _isBottomBarVisible.value = false
        scope.launch {
            _headerOffsetAnim.animateTo(
                -headerHeightPx,
                spring(
                    stiffness = Spring.StiffnessMediumLow,
                    dampingRatio = Spring.DampingRatioNoBouncy
                )
            )
        }
    }

    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val dy = available.y
            if (abs(dy) < 0.5f) return Offset.Zero

            if (dy < 0f) {
                // ── SCROLLING DOWN (Finger moving up, content moving up) ──
                accumulatedUpScroll = 0f
                accumulatedDownScroll += -dy

                // 1. Direct 1:1 Header Sync: Translate header upwards with scroll delta
                val newHeaderOffset = (headerOffset + dy).coerceIn(-headerHeightPx, 0f)
                scope.launch {
                    _headerOffsetAnim.snapTo(newHeaderOffset)
                }

                // If header moved more than half its height, consider it visually hidden
                if (newHeaderOffset <= -headerHeightPx * 0.5f && _isHeaderVisible.value) {
                    _isHeaderVisible.value = false
                }

                // 2. Bottom Navigation Bar: Only hide after user has scrolled ~2 inches of content
                if (accumulatedDownScroll >= twoInchesScrollPx && _isBottomBarVisible.value) {
                    _isBottomBarVisible.value = false
                }
            } else if (dy > 0f) {
                // ── SCROLLING UP (Finger moving down, content moving down) ──
                accumulatedDownScroll = 0f
                accumulatedUpScroll += dy

                // 1. Direct 1:1 Header Sync: Translate header downwards with scroll delta
                val newHeaderOffset = (headerOffset + dy).coerceIn(-headerHeightPx, 0f)
                scope.launch {
                    _headerOffsetAnim.snapTo(newHeaderOffset)
                }

                if (newHeaderOffset > -headerHeightPx * 0.5f && !_isHeaderVisible.value) {
                    _isHeaderVisible.value = true
                }

                // 2. Bottom Navigation Bar: Re-show quickly as soon as user scrolls up
                if (accumulatedUpScroll > 24f && !_isBottomBarVisible.value) {
                    _isBottomBarVisible.value = true
                }
            }

            return Offset.Zero
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            val vy = available.y

            // Fast flick downwards into content (> 4 inches/sec, i.e. velocity < -1500 px/sec)
            if (vy < -1500f) {
                _isBottomBarVisible.value = false
                _isHeaderVisible.value = false
                _headerOffsetAnim.animateTo(
                    -headerHeightPx,
                    spring(
                        stiffness = Spring.StiffnessMediumLow,
                        dampingRatio = Spring.DampingRatioNoBouncy
                    )
                )
            } else if (vy > 800f) {
                // Upward flick -> snap header and bottom bar back into view
                _isBottomBarVisible.value = true
                _isHeaderVisible.value = true
                _headerOffsetAnim.animateTo(
                    0f,
                    spring(
                        stiffness = Spring.StiffnessMediumLow,
                        dampingRatio = Spring.DampingRatioNoBouncy
                    )
                )
            } else {
                // Settle header to nearest boundary if partially collapsed
                if (headerOffset > -headerHeightPx * 0.5f) {
                    _headerOffsetAnim.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                    _isHeaderVisible.value = true
                } else {
                    _headerOffsetAnim.animateTo(-headerHeightPx, spring(stiffness = Spring.StiffnessMediumLow))
                    _isHeaderVisible.value = false
                }
            }

            return Velocity.Zero
        }
    }
}

val LocalCollapsibleBarsState = compositionLocalOf<CollapsibleBarsState?> { null }

@Composable
fun rememberCollapsibleBarsState(
    headerHeight: Dp = 64.dp,
    twoInchesScroll: Dp = 280.dp,
    initialVisible: Boolean = true
): CollapsibleBarsState {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val headerHeightPx = with(density) { headerHeight.toPx() }
    val twoInchesScrollPx = with(density) { twoInchesScroll.toPx() }

    return remember(headerHeightPx, twoInchesScrollPx) {
        CollapsibleBarsState(
            headerHeightPx = headerHeightPx,
            twoInchesScrollPx = twoInchesScrollPx,
            scope = scope,
            initialVisible = initialVisible
        )
    }
}
