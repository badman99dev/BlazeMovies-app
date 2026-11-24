package com.movie.app.best.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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

private const val TOKEN_CHANNEL_ICONS = "channel_icons"
private val TOKEN_REGEX = Regex("\\{([^}]*)\\}")

private sealed interface ChipPart {
    data class Text(val value: String) : ChipPart
    data object ChannelIcons : ChipPart
}

/**
 * Splits a chip label into text + supported token parts.
 * - `{channel_icons}` → overlapping live TV channel logos.
 * - Any other `{token}` is unsupported and silently dropped (hidden).
 * Text keeps its original order, so "Live tv {channel_icons}" renders text first, then icons.
 */
private fun parseChipLabel(label: String): List<ChipPart> {
    val parts = mutableListOf<ChipPart>()
    var last = 0
    for (match in TOKEN_REGEX.findAll(label)) {
        val before = label.substring(last, match.range.first).trim()
        if (before.isNotEmpty()) parts += ChipPart.Text(before)
        if (match.groupValues[1].trim().lowercase() == TOKEN_CHANNEL_ICONS) {
            parts += ChipPart.ChannelIcons
        }
        last = match.range.last + 1
    }
    val tail = label.substring(last).trim()
    if (tail.isNotEmpty()) parts += ChipPart.Text(tail)
    return parts
}

private fun ChipItem.hasRenderableContent(): Boolean =
    iconUrl.isNotBlank() || parseChipLabel(label).isNotEmpty()

@Composable
fun HomeChipsRow(
    chips: List<ChipItem>,
    liveChannels: List<UnifiedChannel>,
    onChipClick: (ChipItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleChips = remember(chips) { chips.filter { it.hasRenderableContent() } }

    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(visibleChips, key = { "chip_${it.id}_${it.label}" }) { chip ->
            ChipPill(chip = chip, liveChannels = liveChannels, onClick = { onChipClick(chip) })
        }
    }
}

@Composable
private fun ChipPill(
    chip: ChipItem,
    liveChannels: List<UnifiedChannel>,
    onClick: () -> Unit
) {
    val parts = remember(chip.label) { parseChipLabel(chip.label) }

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
            .padding(horizontal = if (chip.iconUrl.isNotBlank()) 12.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        if (chip.iconUrl.isNotBlank()) {
            AsyncImage(
                model = chip.iconUrl,
                contentDescription = chip.label,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(18.dp)
            )
        }
        parts.forEach { part ->
            when (part) {
                is ChipPart.Text -> Text(
                    text = part.value,
                    color = Color.White,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
                ChipPart.ChannelIcons -> if (liveChannels.isNotEmpty()) {
                    OverlappingChannelLogos(channels = liveChannels.take(4))
                }
            }
        }
    }
}

@Composable
private fun OverlappingChannelLogos(channels: List<UnifiedChannel>) {
    val size = 20.dp
    val step = 9.dp
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