package com.movie.app.best.ui.screens.moviedetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movie.app.best.ui.theme.AppRed

/**
 * Series episodes tappable row — navigates to series detail screen.
 */
@Composable
fun EpisodesRow(
    isSeries: Boolean,
    onViewEpisodes: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isSeries) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick    = onViewEpisodes
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.PlayCircle, null,
                tint     = AppRed,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text       = "Episodes",
                color      = Color.White,
                fontSize   = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Icon(
            Icons.Default.ChevronRight, null,
            tint     = Color.White.copy(0.5f),
            modifier = Modifier.size(22.dp)
        )
    }
}

/**
 * Trailers & More tappable row.
 */
@Composable
fun TrailersRow(
    youtubeId: String,
    onTrailerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (youtubeId.isEmpty()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick    = onTrailerClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.PlayCircle, null,
                tint     = Color.White.copy(0.7f),
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text       = "Trailers & More",
                color      = Color.White,
                fontSize   = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Icon(
            Icons.Default.ChevronRight, null,
            tint     = Color.White.copy(0.5f),
            modifier = Modifier.size(22.dp)
        )
    }
}
