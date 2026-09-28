package com.movie.app.best.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * Universal adaptive YouTube-style media thumbnail:
 * - Fits any aspect ratio (vertical 2:3, 9:16, square 1:1, or wide 16:9).
 * - Background layer: Heavily blurred poster stretched to fill with a dark overlay to provide
 *   dynamic, color-matched ambient side-pillars (just like YouTube Shorts in horizontal carousels).
 * - Center layer: Crisp, uncropped original poster rendered with ContentScale.Fit.
 * - Series overlay: YouTube Shorts-style outlined TV icon badge on the bottom-right if isSeries == true.
 * - Optional bottom progress indicator bar.
 */
@Composable
fun AdaptiveMediaThumbnail(
    posterUrl: String,
    title: String,
    isSeries: Boolean,
    modifier: Modifier = Modifier,
    shouldBlur: Boolean = false,
    contentModerationBlurRadius: Int = 30,
    progressPercent: Float = 0f,
    showTvBadge: Boolean = true
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF161616)),
        contentAlignment = Alignment.Center
    ) {
        // ── 1. Color-Matched Ambient Blurred Background ────────────────
        if (posterUrl.isNotEmpty()) {
            AsyncImage(
                model = posterUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(26.dp)
            )
            // Darkening scrim so background never clashes with foreground
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.58f))
            )
        }

        // ── 2. Foreground Center Uncropped Poster ─────────────────────
        BlurredContent(
            shouldBlur = shouldBlur,
            blurRadius = contentModerationBlurRadius,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = posterUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(6.dp))
                )
            }
        }

        // ── 3. Subtle Vignette / Contrast Gradient ────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.35f)
                        )
                    )
                )
        )

        // ── 4. YouTube-style Outlined TV Badge for Web Series ─────────
        if (isSeries && showTvBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.78f))
                    .padding(horizontal = 5.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Tv,
                    contentDescription = "TV Series",
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        // ── 5. Playback Progress Red Bar ──────────────────────────────
        if (progressPercent > 0f) {
            val displayProgress = when {
                progressPercent >= 0.98f -> 1.0f
                else -> maxOf(0.05f, progressPercent)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.BottomCenter)
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(displayProgress)
                        .background(Color(0xFFE50914))
                )
            }
        }
    }
}
