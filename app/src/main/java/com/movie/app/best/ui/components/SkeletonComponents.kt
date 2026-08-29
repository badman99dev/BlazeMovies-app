package com.movie.app.best.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.movie.app.best.ui.screens.home.components.CardSize

// Smooth shimmer brush — no lag, pure Compose, no external lib needed
@Composable
fun shimmerBrush(baseColor: Color = Color(0xFF1C1C1C)): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shimmerX by transition.animateFloat(
        initialValue = -600f,
        targetValue = 1400f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerX"
    )
    return Brush.linearGradient(
        colors = listOf(
            baseColor,
            Color(0xFF2E2E2E),
            Color(0xFF3A3A3A),
            Color(0xFF2E2E2E),
            baseColor
        ),
        start = Offset(shimmerX, 0f),
        end = Offset(shimmerX + 600f, 300f)
    )
}

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp)
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(shimmerBrush())
    )
}

@Composable
fun SkeletonLine(
    width: Dp = 120.dp,
    height: Dp = 14.dp
) {
    SkeletonBox(
        modifier = Modifier.width(width).height(height),
        shape = RoundedCornerShape(4.dp)
    )
}

@Composable
fun SkeletonCircle(size: Dp = 40.dp) {
    SkeletonBox(
        modifier = Modifier.size(size),
        shape = CircleShape
    )
}

@Composable
fun SkeletonPosterCard(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        SkeletonBox(
            modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f),
            shape = RoundedCornerShape(8.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        SkeletonLine(width = 90.dp, height = 11.dp)
        Spacer(modifier = Modifier.height(3.dp))
        SkeletonLine(width = 55.dp, height = 10.dp)
    }
}

@Composable
private fun SkeletonHomeMovieRow(cardSize: CardSize, count: Int) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(count) {
            SkeletonPosterCard(modifier = Modifier.width(cardSize.width))
        }
    }
}

@Composable
private fun SkeletonHomeSectionHeader(showSeeAll: Boolean = true) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 10.dp, top = 24.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SkeletonLine(width = 160.dp, height = 18.dp)
        if (showSeeAll) {
            SkeletonLine(width = 60.dp, height = 14.dp)
        }
    }
}

@Composable
private fun SkeletonHomePoster(cardWidth: Dp, cardHeight: Dp) {
    Box(
        modifier = Modifier
            .width(cardWidth)
            .wrapContentHeight()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Box(
                modifier = Modifier
                    .width(cardWidth)
                    .height(cardHeight)
            ) {
                SkeletonBox(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(10.dp)
                )
                SkeletonBox(
                    modifier = Modifier
                        .padding(7.dp)
                        .width(56.dp)
                        .height(22.dp)
                        .align(Alignment.TopStart),
                    shape = RoundedCornerShape(50)
                )
                SkeletonCircle(size = 28.dp)
            }
            SkeletonLine(width = cardWidth * 0.85f, height = 12.dp)
            SkeletonLine(width = cardWidth * 0.65f, height = 12.dp)
        }
    }
}

@Composable
private fun SkeletonHomePosterRow(cardWidth: Dp, cardHeight: Dp) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SkeletonHomeSectionHeader()
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(5) {
                SkeletonHomePoster(cardWidth = cardWidth, cardHeight = cardHeight)
            }
        }
    }
}

@Composable
private fun SkeletonHomeGridSection(showSeeAll: Boolean = true) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SkeletonHomeSectionHeader(showSeeAll = showSeeAll)
        val cardWidth = (LocalConfiguration.current.screenWidthDp.dp - 48.dp) / 3
        val cardHeight = cardWidth * 1.5f
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            repeat(2) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .width(cardWidth)
                                .wrapContentHeight()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .width(cardWidth)
                                        .height(cardHeight)
                                ) {
                                    SkeletonBox(
                                        modifier = Modifier.fillMaxSize(),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    SkeletonBox(
                                        modifier = Modifier
                                            .padding(5.dp)
                                            .width(44.dp)
                                            .height(18.dp)
                                            .align(Alignment.TopStart),
                                        shape = RoundedCornerShape(50)
                                    )
                                }
                                SkeletonLine(width = cardWidth * 0.9f, height = 11.dp)
                                SkeletonLine(width = cardWidth * 0.7f, height = 11.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SkeletonCategoryCard(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        SkeletonBox(
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            shape = RoundedCornerShape(10.dp)
        )
        Spacer(modifier = Modifier.height(5.dp))
        SkeletonLine(width = 100.dp, height = 12.dp)
    }
}

@Composable
fun SkeletonDetailPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
    ) {
        SkeletonHeroPoster()
        Spacer(modifier = Modifier.height(22.dp))
        SkeletonActionButtons()
        Spacer(modifier = Modifier.height(18.dp))
        SkeletonTagChips()
        Spacer(modifier = Modifier.height(16.dp))
        SkeletonDescriptionLines()
        Spacer(modifier = Modifier.height(28.dp))
        SkeletonCastCrewSection()
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SkeletonHeroPoster() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2f / 3f)
    ) {
        SkeletonBox(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(0.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .align(Alignment.TopStart),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonCircle(size = 42.dp)
            SkeletonCircle(size = 42.dp)
            SkeletonBox(
                modifier = Modifier
                    .width(72.dp)
                    .height(36.dp),
                shape = RoundedCornerShape(10.dp)
            )
        }
    }
}

@Composable
private fun SkeletonActionButtons() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SkeletonBox(
            modifier = Modifier
                .width(158.dp)
                .height(50.dp),
            shape = RoundedCornerShape(50)
        )
        repeat(3) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                SkeletonCircle(size = 28.dp)
                SkeletonLine(width = 46.dp, height = 11.dp)
            }
        }
    }
}

@Composable
private fun SkeletonTagChips() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(62.dp, 70.dp, 70.dp, 66.dp).forEach { chipWidth ->
            SkeletonBox(
                modifier = Modifier
                    .width(chipWidth)
                    .height(30.dp),
                shape = RoundedCornerShape(50)
            )
        }
    }
}

@Composable
private fun SkeletonDescriptionLines() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SkeletonLine(width = 340.dp, height = 13.dp)
        SkeletonLine(width = 320.dp, height = 13.dp)
        SkeletonLine(width = 200.dp, height = 13.dp)
        Spacer(modifier = Modifier.height(4.dp))
        SkeletonLine(width = 90.dp, height = 13.dp)
    }
}

@Composable
private fun SkeletonCastCrewSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SkeletonLine(width = 110.dp, height = 18.dp)
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            repeat(4) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    SkeletonCircle(size = 64.dp)
                    SkeletonLine(width = 56.dp, height = 11.dp)
                    SkeletonLine(width = 44.dp, height = 10.dp)
                }
            }
        }
    }
}

@Composable
fun SkeletonLibraryPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonLine(width = 100.dp, height = 28.dp)
            Spacer(modifier = Modifier.weight(1f))
            SkeletonCircle(size = 28.dp)
        }
        SkeletonHomePosterRow(cardWidth = 155.dp, cardHeight = 220.dp)
        Spacer(modifier = Modifier.height(20.dp))
        SkeletonHomePosterRow(cardWidth = 155.dp, cardHeight = 220.dp)
        Spacer(modifier = Modifier.height(20.dp))
        SkeletonHomePosterRow(cardWidth = 155.dp, cardHeight = 220.dp)
    }
}
