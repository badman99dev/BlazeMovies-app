package com.movie.app.best.ui.screens.moviedetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.movie.app.best.data.model.ContentModeration
import com.movie.app.best.data.model.MovieDetails
import com.movie.app.best.data.settings.ModerationSettings
import com.movie.app.best.ui.components.BlurredContent
import com.movie.app.best.ui.theme.Amber
import com.movie.app.best.ui.theme.AppRed

/**
 * Full-bleed hero image with back button, share, title and meta info overlay.
 */
@Composable
fun DetailHeroSection(
    movie: MovieDetails,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit = {},
    onReportClick: () -> Unit = {},
    forceBlur: Boolean = false,
    minimal: Boolean = false,
    overrideRating: String? = null,
    overrideVotes: String? = null
) {
    val context = LocalContext.current
    val shouldBlur = forceBlur || ModerationSettings.shouldBlurDetail(context, movie.contentModeration)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(580.dp)
    ) {
        // Backdrop / poster
        BlurredContent(
            shouldBlur = shouldBlur,
            modifier = Modifier.fillMaxSize()
        ) {
            AsyncImage(
                model              = movie.backdropUrl.ifEmpty { movie.posterUrl },
                contentDescription = movie.title,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.fillMaxSize()
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color.Black.copy(alpha = 0.20f),
                            0.45f to Color.Transparent,
                            0.68f to Color.Black.copy(alpha = 0.60f),
                            0.84f to Color.Black.copy(alpha = 0.90f),
                            1.00f to Color.Black
                        )
                    )
                )
        )


        // ── Top bar ───────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            // Back
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = onBackClick, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector  = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint         = Color.White,
                        modifier     = Modifier.size(22.dp)
                    )
                }
            }

            // Share
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = onShareClick, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector  = Icons.Default.Share,
                        contentDescription = "Share",
                        tint         = Color.White,
                        modifier     = Modifier.size(20.dp)
                    )
                }
            }

            // Report
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE50914).copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = onReportClick, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector  = Icons.Default.Flag,
                        contentDescription = "Report",
                        tint         = Color.White,
                        modifier     = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Quality badge (top-right below back button)
        if (!minimal && movie.qualityLabel.isNotEmpty()) {
            val bgColor = when {
                movie.qualityLabel.contains("4K", ignoreCase = true) -> Color(0xFFFFD700).copy(alpha = 0.25f)
                movie.qualityLabel.contains("UHD", ignoreCase = true) -> Color(0xFF00E5FF).copy(alpha = 0.2f)
                movie.qualityLabel.contains("HD", ignoreCase = true) -> Color(0xFF4FC3F7).copy(alpha = 0.15f)
                else -> Color.White.copy(alpha = 0.15f)
            }
            val textColor = when {
                movie.qualityLabel.contains("4K", ignoreCase = true) -> Color(0xFFFFD700)
                movie.qualityLabel.contains("UHD", ignoreCase = true) -> Color(0xFF00E5FF)
                movie.qualityLabel.contains("HD", ignoreCase = true) -> Color(0xFF4FC3F7)
                else -> Color.White
            }
            val borderColor = textColor.copy(alpha = 0.3f)

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 56.dp, end = 14.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.linearGradient(colors = listOf(bgColor, Color.White.copy(alpha = 0.08f), bgColor)))
                    .border(0.5.dp, Brush.linearGradient(colors = listOf(borderColor, Color.White.copy(alpha = 0.1f), borderColor)), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text       = movie.qualityLabel,
                    color      = textColor,
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // ── Bottom title block ────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp)
        ) {
            // Title
            Text(
                text       = movie.title,
                color      = Color.White,
                fontSize   = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 30.sp
            )

            // Original title
            if (!minimal && movie.originalTitle.isNotEmpty() && movie.originalTitle != movie.title) {
                Text(
                    text     = movie.originalTitle,
                    color    = Color.White.copy(alpha = 0.45f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            if (!minimal) {
                Spacer(Modifier.height(8.dp))

                // Meta row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (movie.releaseYear.isNotEmpty()) {
                        Text(movie.releaseYear, color = Color.White.copy(0.75f), fontSize = 13.sp)
                        Text("  •  ", color = Color.White.copy(0.4f), fontSize = 13.sp)
                    }
                    Icon(Icons.Default.Star, null, tint = Amber, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(overrideRating ?: movie.rating, color = Color.White.copy(0.75f), fontSize = 13.sp)
                    if (overrideVotes != null) {
                        Text(overrideVotes, color = Color.White.copy(0.5f), fontSize = 12.sp)
                    }
                    if (movie.runtime.isNotEmpty()) {
                        Text("  •  ", color = Color.White.copy(0.4f), fontSize = 13.sp)
                        Text(movie.runtime, color = Color.White.copy(0.75f), fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
