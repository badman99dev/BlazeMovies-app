package com.movie.app.best.ui.screens.player.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration

@Composable
fun PlayerVignette(modifier: Modifier = Modifier) {
    val isPortrait =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT

    val topAlpha = if (isPortrait) 0.55f else 0.65f
    val bottomAlpha = if (isPortrait) 0.30f else 0.72f
    val topFraction = if (isPortrait) 0.30f else 0.28f
    val bottomFraction = if (isPortrait) 0.24f else 0.40f

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val topHeight = maxHeight * topFraction
        val bottomHeight = maxHeight * bottomFraction

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(topHeight)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = topAlpha),
                            Color.Transparent,
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(bottomHeight)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = bottomAlpha),
                        )
                    )
                )
        )
    }
}
