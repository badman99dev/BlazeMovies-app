package com.movie.app.best.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.movie.app.best.data.model.ChipItem
import com.movie.app.best.data.model.UnifiedChannel

private val PillShape = RoundedCornerShape(50)
private val GlassBrush = Brush.verticalGradient(
    listOf(Color.White.copy(alpha = 0.26f), Color.White.copy(alpha = 0.10f))
)
private val GlassBorder = Color.White.copy(alpha = 0.34f)

@Composable
fun HomeChipsRow(
    chips: List<ChipItem>,
    liveChannels: List<UnifiedChannel>,
    onLiveTvClick: () -> Unit,
    onChipClick: (ChipItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "live_tv_default") {
            LiveTvChip(channels = liveChannels, onClick = onLiveTvClick)
        }
        items(chips, key = { "chip_${it.id}_${it.label}" }) { chip ->
            NormalChip(chip = chip, onClick = { onChipClick(chip) })
        }
    }
}

@Composable
private fun LiveTvChip(channels: List<UnifiedChannel>, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(40.dp)
            .clip(PillShape)
            .background(GlassBrush)
            .border(1.dp, GlassBorder, PillShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (channels.isNotEmpty()) {
            OverlappingChannelLogos(channels = channels.take(4))
            Spacer(Modifier.width(9.dp))
        }
        Text(
            text = "Live TV",
            color = Color.White,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun OverlappingChannelLogos(channels: List<UnifiedChannel>) {
    val size = 22.dp
    val step = 13.dp
    val overlapWidth = size + step * (channels.size - 1)

    Box(
        modifier = Modifier
            .width(overlapWidth)
            .height(size),
        contentAlignment = Alignment.CenterStart
    ) {
        channels.forEachIndexed { index, channel ->
            val ring = if (channel.logoUrl.isNotBlank()) Color.White.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.5f)
            Box(
                modifier = Modifier
                    .offset(x = step * index)
                    .zIndex((channels.size - index).toFloat())
                    .size(size)
                    .clip(CircleShape)
                    .background(Color(0xFF1E1E1E))
                    .border(1.5.dp, ring, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (channel.logoUrl.isNotBlank()) {
                    AsyncImage(
                        model = channel.logoUrl,
                        contentDescription = channel.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(size)
                            .clip(CircleShape)
                    )
                } else {
                    Text(
                        text = channel.name.take(1).uppercase(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun NormalChip(chip: ChipItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(40.dp)
            .clip(PillShape)
            .background(GlassBrush)
            .border(1.dp, GlassBorder, PillShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = if (chip.iconUrl.isNotBlank()) 12.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (chip.iconUrl.isNotBlank()) {
            AsyncImage(
                model = chip.iconUrl,
                contentDescription = chip.label,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(7.dp))
        }
        Text(
            text = chip.label,
            color = Color.White,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
