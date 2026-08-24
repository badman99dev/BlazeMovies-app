package com.movie.app.best.ui.screens.home.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.movie.app.best.data.model.Movie
import com.movie.app.best.data.settings.ModerationSettings
import com.movie.app.best.ui.components.BlurredContent
import com.movie.app.best.ui.screens.home.components.StreamBadge
import com.movie.app.best.ui.screens.home.components.SeriesBadge
import com.movie.app.best.ui.screens.home.components.AgeBadge

/**
 * Wide landscape (16:9) cards row — used for "Because You Watched" section.
 */
@Composable
fun WideMovieRowSection(
    title: String,
    movies: List<Movie>,
    onMovieClick: (String, Boolean, String) -> Unit
) {
    Column {
        SectionHeader(title = title)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(movies, key = { it.slug }) { movie ->
                WideMovieCard(
                    movie   = movie,
                    onClick = { onMovieClick(movie.slug, movie.isSeries, movie.imdbId) }
                )
            }
        }
    }
}

@Composable
fun WideMovieCard(
    movie: Movie,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "wide_scale"
    )

    Box(
        modifier = Modifier
            .width(200.dp)
            .height(115.dp)
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1A1A1A))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        BlurredContent(
            shouldBlur = ModerationSettings.effectiveShouldBlur(movie),
            modifier = Modifier.fillMaxSize()
        ) {
            AsyncImage(
                model            = movie.posterUrl,
                contentDescription = movie.title,
                contentScale     = ContentScale.Crop,
                modifier         = Modifier.fillMaxSize()
            )
        }

        // Gradient for title legibility
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(56.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                    )
                )
        )

        // Quality badge
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(5.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (movie.qualityLabel.isNotBlank()) {
                    QualityBadge(label = movie.qualityLabel)
                }
                if (movie.hasStream) {
                    StreamBadge()
                }
            }
            if (movie.isSeries) {
                SeriesBadge()
            }
            if (movie.contentModeration?.hasAnyFlag == true) {
                AgeBadge()
            }
        }



        // Title at bottom
        Text(
            text     = movie.title,
            color    = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}
