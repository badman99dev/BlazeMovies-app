package com.movie.app.best.ui.screens.home.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import coil.compose.AsyncImage
import com.movie.app.best.data.model.Movie
import com.movie.app.best.data.settings.ModerationSettings
import com.movie.app.best.ui.components.BlurredContent
import com.movie.app.best.ui.theme.Amber
import com.movie.app.best.ui.theme.AppRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HeroCarousel(
    movies: List<Movie>,
    isLoading: Boolean,
    onPlayClick: (slug: String, isSeries: Boolean) -> Unit,
    onInfoClick: (slug: String, isSeries: Boolean) -> Unit
) {
    if (isLoading && movies.isEmpty()) {
        HeroSkeleton()
        return
    }
    if (movies.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { movies.size })
    val scope = rememberCoroutineScope()

    // Auto-scroll every 6 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(6.seconds)
            val next = (pagerState.currentPage + 1) % movies.size
            pagerState.animateScrollToPage(
                page = next,
                animationSpec = tween(700, easing = FastOutSlowInEasing)
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(580.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val pageOffset = ((pagerState.currentPage - page) +
                    pagerState.currentPageOffsetFraction).absoluteValue

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Slight scale + alpha parallax
                        val scale = lerp(0.92f, 1f, 1f - pageOffset.coerceIn(0f, 1f))
                        scaleX = scale; scaleY = scale
                        alpha = lerp(0.7f, 1f, 1f - pageOffset.coerceIn(0f, 0.6f))
                    }
            ) {
                HeroSlide(
                    movie = movies[page],
                    onPlayClick = onPlayClick,
                    onInfoClick = onInfoClick
                )
            }
        }

        // ── Pager Dots ────────────────────────────────
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(movies.size) { index ->
                val selected = pagerState.currentPage == index
                val width by animateDpAsState(
                    targetValue = if (selected) 20.dp else 6.dp,
                    animationSpec = tween(250),
                    label = "dot_width_$index"
                )
                Box(
                    modifier = Modifier
                        .size(width = width, height = 6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (selected) AppRed else Color.White.copy(alpha = 0.4f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            scope.launch {
                                pagerState.animateScrollToPage(
                                    page = index,
                                    animationSpec = tween(400)
                                )
                            }
                        }
                )
            }
        }
    }
}

// ── Single hero slide ─────────────────────────────────────
@Composable
private fun HeroSlide(
    movie: Movie,
    onPlayClick: (String, Boolean) -> Unit,
    onInfoClick: (String, Boolean) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Backdrop image
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color.Black.copy(alpha = 0.25f),
                            0.40f to Color.Transparent,
                            0.65f to Color.Black.copy(alpha = 0.55f),
                            0.85f to Color.Black.copy(alpha = 0.85f),
                            1.00f to Color.Black
                        )
                    )
                )
        )

        // Content overlay at bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Text(
                text = movie.title.uppercase(),
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                letterSpacing = 1.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 30.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Meta row
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (movie.releaseYear.isNotBlank()) {
                    Text(movie.releaseYear, color = Color.White.copy(0.75f), fontSize = 13.sp)
                    MetaDot()
                }
                if (movie.rating.isNotBlank()) {
                    Icon(
                        Icons.Default.Star, null,
                        tint = Amber,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(movie.rating, color = Color.White.copy(0.75f), fontSize = 13.sp)
                    MetaDot()
                }
                if (movie.qualityLabel.isNotBlank()) {
                    Spacer(Modifier.width(4.dp))
                    QualityBadge(label = movie.qualityLabel)
                }
                if (movie.hasStream) {
                    Spacer(Modifier.width(4.dp))
                    StreamBadge()
                }
                if (movie.isSeries) {
                    Spacer(Modifier.width(4.dp))
                    SeriesBadge()
                }
                if (movie.contentModeration?.hasAnyFlag == true) {
                    Spacer(Modifier.width(4.dp))
                    AgeBadge()
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                val watchBg = Brush.linearGradient(colors = listOf(Color(0xFFE50914), Color(0xFFB71C1C)))
                val watchBorder = Brush.linearGradient(colors = listOf(Color(0xFFFF5252), Color(0xFFFFD700), Color(0xFFFF5252)))

                Box(
                    modifier = Modifier
                        .weight(1.3f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(23.dp))
                        .background(watchBg)
                        .border(width = 1.dp, brush = watchBorder, shape = RoundedCornerShape(23.dp))
                        .clickable { onPlayClick(movie.slug, movie.isSeries) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(22.dp), tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text("Watch Now", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                    }
                }

                val infoBg = Brush.linearGradient(colors = listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.06f)))
                val infoBorder = Brush.linearGradient(colors = listOf(Color.White.copy(alpha = 0.5f), Color(0xFFFFD700).copy(alpha = 0.3f), Color.White.copy(alpha = 0.5f)))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(23.dp))
                        .background(infoBg)
                        .border(width = 1.dp, brush = infoBorder, shape = RoundedCornerShape(23.dp))
                        .clickable { onInfoClick(movie.slug, movie.isSeries) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, modifier = Modifier.size(18.dp), tint = Color.White)
                        Spacer(Modifier.width(5.dp))
                        Text("More Info", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
                    }
                }
            }
        }

        // Top-right badge
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(14.dp),
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
    }
}

@Composable
private fun MetaDot() {
    Text("  •  ", color = Color.White.copy(0.4f), fontSize = 13.sp)
}

// ── Hero skeleton ─────────────────────────────────────────
@Composable
private fun HeroSkeleton() {
    val shimmer = rememberInfiniteTransition(label = "hero_shimmer")
    val alpha by shimmer.animateFloat(
        initialValue = 0.2f, targetValue = 0.5f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "alpha"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(580.dp)
            .background(Color.White.copy(alpha = alpha))
    )
}
