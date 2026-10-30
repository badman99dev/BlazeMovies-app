package com.movie.app.best.ui.screens.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp

private val SheetPanelDark = Color(0xFF0C0C0E)

/** Keeps the last row of a sheet above the system gesture bar. */
fun Modifier.sheetBottomInset(): Modifier = this
    .padding(bottom = 20.dp)
    .navigationBarsPadding()

@Composable
fun BoxScope.OverlayView(
    modifier: Modifier = Modifier,
    show: Boolean,
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    val configuration = LocalConfiguration.current
    val layoutDirection = LocalLayoutDirection.current
    val endPadding = WindowInsets.safeDrawing.asPaddingValues().calculateEndPadding(layoutDirection)
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT

    AnimatedVisibility(
        modifier = Modifier.align(if (isPortrait) Alignment.BottomCenter else Alignment.CenterEnd),
        visible = show,
        enter = if (isPortrait) slideInVertically { it } else slideInHorizontally { it },
        exit = if (isPortrait) slideOutVertically { it } else slideOutHorizontally { it },
    ) {
        Surface(
            shape = if (isPortrait) {
                RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
            } else {
                RoundedCornerShape(16.dp)
            },
            color = if (isPortrait) SheetPanelDark else Color.Transparent,
            shadowElevation = if (isPortrait) 10.dp else 0.dp,
            modifier = modifier.then(
                if (isPortrait) {
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.48f)
                        .padding(horizontal = 12.dp)
                } else {
                    Modifier.fillMaxWidth(0.45f).fillMaxHeight()
                },
            ),
        ) {
            if (!isPortrait) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colorStops = arrayOf(
                                    0f to Color.Transparent,
                                    0.5f to SheetPanelDark.copy(alpha = 0.79f),
                                    1f to SheetPanelDark.copy(alpha = 1f),
                                )
                            )
                        )
                )
            }
            Column(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .padding(start = if (isPortrait) 0.dp else 18.dp)
                    .padding(end = endPadding),
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(modifier = Modifier.size(8.dp))
                content()
            }
        }
    }
}

enum class OverlayViewType {
    AUDIO_SELECTOR,
    QUALITY_SELECTOR,
    PLAYBACK_SPEED,
    VIDEO_CONTENT_SCALE,
    SUBTITLE_SELECTOR,
    SERVER_SELECTOR,
}
