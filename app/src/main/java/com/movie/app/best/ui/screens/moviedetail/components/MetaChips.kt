package com.movie.app.best.ui.screens.moviedetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movie.app.best.data.model.MovieDetails
import com.movie.app.best.ui.theme.*

/**
 * Scrollable row of quality / audio / language / country chips.
 */
@Composable
fun MetaChipsRow(movie: MovieDetails, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (movie.qualityLabel.isNotEmpty()) MetaChip(movie.qualityLabel, AppRed)
        if (movie.audioLabel.isNotEmpty())   MetaChip(movie.audioLabel, AccentPurple)
        if (movie.language.isNotEmpty())     MetaChip(movie.language, Color(0xFF00BFA5))
        if (movie.country.isNotEmpty())      MetaChip(movie.country, AppOrange)
    }
}

@Composable
fun MetaChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text(
            text       = text,
            color      = color,
            fontSize   = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
