package com.movie.app.best.ui.screens.home.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.movie.app.best.data.model.SliderItem
import com.movie.app.best.util.SliderKind
import com.movie.app.best.util.SliderLink
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Big home hero slider — sits between the Live TV row and Trending.
 *
 * Portrait poster drawn with ContentScale.Crop (fixed card, zoom + centre-crop) so any
 * image resolution fits without stretching. Right-side glass actions and the watch
 * control depend on the item's deep link:
 *   movie/series   -> Share + My List + white Play
 *   sports         -> "Watch Live" pill with a red dot
 *   anything else  -> plain tappable card
 * Optional title is overlaid bottom-left on the poster; middle/small text render under
 * the card, then the dot indicator.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeSliderSection(
    items: List<SliderItem>,
    onItemClick: (SliderItem) -> Unit,
    onShareClick: (SliderItem) -> Unit,
    onAddToListClick: (SliderItem) -> Unit,
    onWatchClick: (SliderItem) -> Unit,
    onPageChanged: (Int) -> Unit,
    onPosterBottom: (Float) -> Unit = {},
    bookmarkedSlugs: Set<String> = emptySet(),
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { items.size })
    val scope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        onPageChanged(pagerState.currentPage)
    }

    LaunchedEffect(items.size) {
        if (items.size <= 1) return@LaunchedEffect
        while (true) {
            delay(6_000)
            val next = (pagerState.currentPage + 1) % items.size
            pagerState.animateScrollToPage(next, animationSpec = tween(700, easing = FastOutSlowInEasing))
        }
    }

    Column(modifier = modifier.fillMaxWidth().padding(top = 6.dp, bottom = 12.dp)) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 64.dp),
            pageSpacing = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val item = items[page]
            SliderCard(
                item = item,
                onClick = { onItemClick(item) },
                onShareClick = { onShareClick(item) },
                onAddToListClick = { onAddToListClick(item) },
                onWatchClick = { onWatchClick(item) },
                onPosterBottom = onPosterBottom,
                isBookmarked = bookmarkedSlugs.contains(SliderLink.parse(item.deepLink).slug)
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(items.size) { index ->
                val selected = pagerState.currentPage == index
                val w by animateDpAsState(
                    targetValue = if (selected) 18.dp else 6.dp,
                    animationSpec = tween(220),
                    label = "slider_dot_$index"
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(width = w, height = 6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (selected) Color.White else Color.White.copy(alpha = 0.35f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            scope.launch {
                                pagerState.animateScrollToPage(index, animationSpec = tween(400))
                            }
                        }
                )
            }
        }
    }
}

@Composable
private fun SliderCard(
    item: SliderItem,
    onClick: () -> Unit,
    onShareClick: () -> Unit,
    onAddToListClick: () -> Unit,
    onWatchClick: () -> Unit,
    onPosterBottom: (Float) -> Unit,
    isBookmarked: Boolean
) {
    val target = remember(item.deepLink) { SliderLink.parse(item.deepLink) }
    val isContent = target.isContent
    val isSports = target.kind == SliderKind.SPORTS

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.667f)
                .onGloballyPositioned { onPosterBottom(it.positionInRoot().y + it.size.height) }
                .clip(RoundedCornerShape(22.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
        ) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.title.ifBlank { null },
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Bottom scrim keeps the overlaid title and the action buttons legible.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.45f to Color.Transparent,
                            1.0f to Color.Black.copy(alpha = 0.72f)
                        )
                    )
            )

            if (item.title.isNotBlank()) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 18.dp, end = 92.dp, bottom = 18.dp)
                )
            }

            // Right-side action stack.
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 18.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isContent) {
                    GlassActionButton(Icons.Default.Share, "Share", onShareClick)
                    GlassActionButton(
                        if (isBookmarked) Icons.Default.Check else Icons.Default.Add,
                        if (isBookmarked) "In My List" else "Add to My List",
                        onAddToListClick
                    )
                }
                when {
                    isContent -> PlayActionButton(onWatchClick)
                    isSports -> WatchLivePill(onWatchClick)
                }
            }
        }

        // Text block under the card — fixed height so pages don't jump.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(top = 10.dp)
        ) {
            if (item.middleText.isNotBlank()) {
                Text(
                    text = item.middleText,
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (item.smallText.isNotBlank()) {
                Text(
                    text = item.smallText,
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun GlassActionButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(Brush.verticalGradient(listOf(Color.White.copy(0.32f), Color.White.copy(0.12f))))
            .border(1.dp, Color.White.copy(0.55f), RoundedCornerShape(15.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = Color.White, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun PlayActionButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(21.dp))
            .background(Color.White)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.PlayArrow,
            contentDescription = "Watch",
            tint = Color.Black,
            modifier = Modifier.size(40.dp)
        )
    }
}

@Composable
private fun WatchLivePill(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(Color(0xFFE50914))
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Watch Live",
            color = Color.Black,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
