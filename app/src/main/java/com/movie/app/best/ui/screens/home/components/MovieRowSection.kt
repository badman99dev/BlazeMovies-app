package com.movie.app.best.ui.screens.home.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.movie.app.best.data.model.Movie
import com.movie.app.best.data.settings.ModerationSettings
import com.movie.app.best.ui.components.BlurredContent
import com.movie.app.best.ui.theme.SuccessGreen
import com.movie.app.best.ui.theme.AppRed

// ── Poster sizes ──────────────────────────────────────────
enum class CardSize(val width: Dp, val height: Dp) {
    SMALL(100.dp, 150.dp),
    NORMAL(120.dp, 180.dp),
    LARGE(140.dp, 210.dp)
}

/**
 * Full horizontal scrollable movie row with title.
 */
@Composable
fun MovieRowSection(
    title: String,
    movies: List<Movie>,
    isLoading: Boolean = false,
    cardSize: CardSize = CardSize.NORMAL,
    onMovieClick: (String, Boolean, String) -> Unit,
    onSeeAllClick: () -> Unit = {}
) {
    Column {
        SectionHeader(title = title, onSeeAllClick = onSeeAllClick)

        if (isLoading) {
            SkeletonMovieRow(cardSize = cardSize)
        } else if (movies.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(movies) { _, movie ->
                    MoviePosterCard(
                        movie = movie,
                        size = cardSize,
                        onClick = { onMovieClick(movie.slug, movie.isSeries, movie.imdbId) }
                    )
                }
            }
        }
    }
}

/**
 * Single poster card with quality badge + pressed animation.
 */
@Composable
fun MoviePosterCard(
    movie: Movie,
    size: CardSize = CardSize.NORMAL,
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
        label = "card_scale"
    )

    Column(
        modifier = Modifier
            .width(size.width)
            .scale(scale)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .width(size.width)
                .height(size.height)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1A1A1A))
        ) {
            // Poster image
            BlurredContent(
                shouldBlur = ModerationSettings.effectiveShouldBlur(movie),
                modifier = Modifier.fillMaxSize()
            ) {
                AsyncImage(
                    model = movie.posterUrl,
                    contentDescription = movie.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Gradient overlay at bottom for title
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                        )
                    )
            )

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
                if (movie.contentModeration?.hasAnyFlag == true) {
                    AgeBadge()
                }
                if (movie.isSeries) {
                    SeriesBadge()
                }
            }


        }

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = movie.title,
            color = Color.White.copy(alpha = 0.88f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 14.sp,
            modifier = Modifier.width(size.width)
        )
    }
}

@Composable
 fun QualityBadge(label: String, modifier: Modifier = Modifier) {
     val bgColor = when {
         label.contains("4K", ignoreCase = true) -> Color(0xFFD4A017).copy(alpha = 0.35f)
         label.contains("UHD", ignoreCase = true) -> Color(0xFFD4A017).copy(alpha = 0.3f)
         label.contains("HD", ignoreCase = true) -> Color(0xFF4FC3F7).copy(alpha = 0.2f)
         label.contains("CAM", ignoreCase = true) -> Color(0xFFFF5252).copy(alpha = 0.25f)
         else -> Color.White.copy(alpha = 0.18f)
     }
     val textColor = when {
         label.contains("4K", ignoreCase = true) -> Color(0xFFFFD700)
         label.contains("UHD", ignoreCase = true) -> Color(0xFFF0C040)
         label.contains("HD", ignoreCase = true) -> Color(0xFF4FC3F7)
         label.contains("CAM", ignoreCase = true) -> Color(0xFFFF5252)
         else -> Color.White
     }
     val borderColor = textColor.copy(alpha = 0.4f)

     Box(
         modifier = modifier
             .height(21.dp)
             .clip(RoundedCornerShape(50))
             .background(
                 Brush.linearGradient(
                     colors = listOf(bgColor, Color.White.copy(alpha = 0.14f), bgColor)
                 )
             )
             .border(
                 width = 0.5.dp,
                 brush = Brush.linearGradient(
                     colors = listOf(borderColor, Color.White.copy(alpha = 0.18f), borderColor)
                 ),
                 shape = RoundedCornerShape(50)
             )
             .padding(horizontal = 7.dp, vertical = 3.dp),
         contentAlignment = Alignment.Center
     ) {
         Text(
             text = label,
             color = textColor,
             fontSize = 9.sp,
             fontWeight = FontWeight.ExtraBold,
             letterSpacing = 0.5.sp
         )
     }
 }

@Composable
fun StreamBadge(modifier: Modifier = Modifier) {
    val bgColor = Color(0xFFFFD700).copy(alpha = 0.25f)
    val borderColor = Color(0xFFFFD700).copy(alpha = 0.4f)

    Box(
        modifier = modifier
            .size(21.dp)
            .clip(RoundedCornerShape(50))
            .background(
                Brush.linearGradient(
                    colors = listOf(bgColor, Color.White.copy(alpha = 0.14f), bgColor)
                )
            )
            .border(
                width = 0.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(borderColor, Color.White.copy(alpha = 0.18f), borderColor)
                ),
                shape = RoundedCornerShape(50)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = Color(0xFFFFD700).copy(alpha = 0.9f),
            modifier = Modifier.size(11.dp)
        )
    }
}

@Composable
fun AgeBadge(modifier: Modifier = Modifier) {
    val bgColor = Color(0xFFFF1744).copy(alpha = 0.25f)
    val borderColor = Color(0xFFFF1744).copy(alpha = 0.4f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(
                Brush.linearGradient(
                    colors = listOf(bgColor, Color.White.copy(alpha = 0.14f), bgColor)
                )
            )
            .border(
                width = 0.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(borderColor, Color.White.copy(alpha = 0.18f), borderColor)
                ),
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text = "18+",
            color = Color(0xFFFF1744),
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun SeriesBadge(modifier: Modifier = Modifier) {
    val bgColor = Color(0xFF7C4DFF).copy(alpha = 0.25f)
    val borderColor = Color(0xFF7C4DFF).copy(alpha = 0.4f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(
                Brush.linearGradient(
                    colors = listOf(bgColor, Color.White.copy(alpha = 0.14f), bgColor)
                )
            )
            .border(
                width = 0.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(borderColor, Color.White.copy(alpha = 0.18f), borderColor)
                ),
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text = "Series",
            color = Color(0xFF7C4DFF),
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp
        )
    }
}

// ── Skeleton ──────────────────────────────────────────────
@Composable
fun SkeletonMovieRow(cardSize: CardSize = CardSize.NORMAL) {
    val shimmer = rememberInfiniteTransition(label = "shimmer")
    val alpha by shimmer.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "alpha"
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(6) {
            Box(
                modifier = Modifier
                    .width(cardSize.width)
                    .height(cardSize.height)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = alpha))
            )
        }
    }
}
