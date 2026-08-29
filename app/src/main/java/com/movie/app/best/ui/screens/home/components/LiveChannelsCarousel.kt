package com.movie.app.best.ui.screens.home.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.movie.app.best.data.model.LiveChannel
import com.movie.app.best.data.model.UnifiedChannel

@Composable
fun LiveChannelsCarousel(
    channels: List<LiveChannel>,
    onChannelClick: (LiveChannel) -> Unit,
    onMoreClick: (() -> Unit)? = null,
) {
    val listState = rememberLazyListState()
    val headerClearance = 40.dp   // = 36dp AppHeader row + 4dp gap (status bar handled by MainActivity)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = headerClearance, bottom = 12.dp)  // hugs AppHeader bottom
    ) {
        LiveTvSectionHeader(onMoreClick = onMoreClick)

        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(channels, key = { "lc_${it.id}" }) { channel ->
                ChannelCircle(
                    logoUrl = channel.logoUrl,
                    name = channel.name,
                    onClick = { onChannelClick(channel) }
                )
            }
        }
    }
}

// Overload for UnifiedChannel — used by SearchScreen (and any future viewer of merged channels)
@Composable
fun LiveChannelsCarousel(
    channels: List<UnifiedChannel>,
    onChannelClick: (UnifiedChannel) -> Unit,
    onMoreClick: (() -> Unit)? = null,
    applyTopPadding: Boolean = false,
    isLoading: Boolean = false,
) {
    val listState = rememberLazyListState()
    val headerClearance = 40.dp
    val topPadding = if (applyTopPadding) headerClearance else 8.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding, bottom = 12.dp)
    ) {
        LiveTvSectionHeader(onMoreClick = onMoreClick)

        if (isLoading) {
            val shimmer = rememberInfiniteTransition(label = "shimmer")
            val alpha by shimmer.animateFloat(
                initialValue = 0.3f, targetValue = 0.7f,
                animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
                label = "alpha"
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(6) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(80.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = alpha * 0.5f))
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .width(56.dp)
                                .height(10.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = alpha * 0.4f))
                        )
                    }
                }
            }
        } else {
            LazyRow(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(channels, key = { it.uniqueKey }) { channel ->
                    ChannelCircle(
                        logoUrl = channel.logoUrl,
                        name = channel.name,
                        onClick = { onChannelClick(channel) }
                    )
                }
            }
        }
    }
}
        }
    }
}

@Composable
private fun LiveTvSectionHeader(onMoreClick: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color(0xFFFF0000), CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "LIVE TV",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
        if (onMoreClick != null) {
            Text(
                text = "More >",
                color = Color(0xFFFF4081),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onMoreClick
                )
            )
        }
    }
}

@Composable
private fun ChannelCircle(
    logoUrl: String,
    name: String,
    onClick: () -> Unit
) {
    val redGradient = Brush.linearGradient(
        colors = listOf(Color(0xFFFF0000), Color(0xFFFF4081)),
        start = Offset(0f, 0f),
        end = Offset(1f, 1f)
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        // ── Circle with thin red border + logo ──
        Box(
            modifier = Modifier
                .size(72.dp)
                .drawWithCache {
                    onDrawWithContent {
                        drawCircle(
                            brush = redGradient,
                            radius = size.minDimension / 2,
                            center = center,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f * density)
                        )
                        drawContent()
                    }
                }
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = logoUrl,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // ── LIVE badge BELOW circle ──
        Box(
            modifier = Modifier
                .offset(y = (-6).dp)
                .background(
                    color = Color(0xFFFF0000),
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
            Text(
                text = "LIVE",
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.5f),
                        offset = Offset(0f, 1f),
                        blurRadius = 2f
                    )
                )
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // ── Channel name below — center aligned ──────────────────
        Text(
            text = name,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
