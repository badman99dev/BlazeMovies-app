package com.movie.app.best.ui.screens.player

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * YouTube / Material style morphing Play-to-Pause icon.
 * Performs dual-polygon SVG path morphing between Play (▶) and Pause (⏸).
 * 
 * - When isPlaying is false (paused): displays Play ▶
 * - When isPlaying is true (playing): displays Pause ⏸
 * - Transitions smoothly via 250ms FastOutSlowInEasing path interpolation.
 */
@Composable
fun PlayPauseMorphIcon(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    tint: Color = Color.White,
) {
    val progress by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "PlayPauseMorphProgress",
    )

    Canvas(
        modifier = modifier
            .size(size)
            .semantics {
                contentDescription = if (isPlaying) "Pause" else "Play"
            }
    ) {
        val sx = this.size.width / 24f
        val sy = this.size.height / 24f

        fun lerp(start: Float, stop: Float, fraction: Float): Float =
            start + (stop - start) * fraction

        // Left Polygon (Left half of Play triangle -> Left Pause bar)
        val leftPath = Path().apply {
            moveTo(lerp(8f, 6f, progress) * sx, 19f * sy)
            lineTo(lerp(19f, 10f, progress) * sx, lerp(12f, 19f, progress) * sy)
            lineTo(lerp(19f, 10f, progress) * sx, lerp(12f, 5f, progress) * sy)
            lineTo(lerp(8f, 6f, progress) * sx, 5f * sy)
            close()
        }

        // Right Polygon (Right half of Play triangle -> Right Pause bar)
        val rightPath = Path().apply {
            moveTo(lerp(8f, 14f, progress) * sx, 19f * sy)
            lineTo(lerp(19f, 18f, progress) * sx, lerp(12f, 19f, progress) * sy)
            lineTo(lerp(19f, 18f, progress) * sx, lerp(12f, 5f, progress) * sy)
            lineTo(lerp(8f, 14f, progress) * sx, 5f * sy)
            close()
        }

        drawPath(path = leftPath, color = tint)
        drawPath(path = rightPath, color = tint)
    }
}
