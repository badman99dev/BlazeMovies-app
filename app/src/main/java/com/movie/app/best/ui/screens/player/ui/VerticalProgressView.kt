package com.movie.app.best.ui.screens.player.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private val LabelShadow = Shadow(
    color = Color.Black.copy(alpha = 0.65f),
    offset = Offset(0f, 1f),
    blurRadius = 6f,
)

@Composable
fun VerticalGestureIndicator(
    percent: Float,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    barHeight: Dp = 170.dp,
    labelOnRight: Boolean = false,
) {
    val clampedPercent = percent.coerceIn(0f, 100f)
    val fraction = clampedPercent / 100f
    val animatedFill by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(70, easing = LinearEasing),
        label = "gestureFill",
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top,
    ) {
        if (!labelOnRight) {
            GestureLabel(
                text = label,
                modifier = Modifier.padding(top = 24.dp + barHeight * 0.50f, end = 12.dp),
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            Box(
                modifier = Modifier.width(44.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${clampedPercent.roundToInt()}%",
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    style = TextStyle(shadow = LabelShadow),
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.28f)),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(animatedFill.coerceIn(0f, 1f))
                        .background(Color.White),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.95f),
                modifier = Modifier.size(20.dp),
            )
        }

        if (labelOnRight) {
            GestureLabel(
                text = label,
                modifier = Modifier.padding(top = 24.dp + barHeight * 0.50f, start = 12.dp),
            )
        }
    }
}

@Composable
private fun GestureLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        color = Color.White.copy(alpha = 0.85f),
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        style = TextStyle(shadow = LabelShadow),
    )
}
