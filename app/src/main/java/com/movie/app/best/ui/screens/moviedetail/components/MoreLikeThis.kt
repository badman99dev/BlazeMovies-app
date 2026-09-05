package com.movie.app.best.ui.screens.moviedetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.movie.app.best.data.model.Movie
import com.movie.app.best.data.settings.ModerationSettings
import com.movie.app.best.ui.components.BlurredContent

@Composable
fun FindingSimilarSection(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
                color = Color.Red,
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Finding similar content...",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun MoreLikeThisSection(
    movies: List<Movie>,
    onMovieClick: (String, Boolean, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val visibleMovies = remember(movies, context, ModerationSettings.changeVersion) { ModerationSettings.filterMovies(context, movies) }
    if (visibleMovies.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        DetailSectionTitle(title = "More Like This")
        LazyRow(
            contentPadding        = PaddingValues(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(visibleMovies, key = { it.slug }) { movie ->
                SimilarMovieCard(
                    movie   = movie,
                    onClick = { onMovieClick(movie.slug, movie.isSeries, movie.imdbId) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SimilarMovieCard(
    movie: Movie,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val context = LocalContext.current
    val shouldBlur = ModerationSettings.shouldBlur(context, movie)

    Column(
        modifier = Modifier
            .width(110.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick    = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .width(110.dp)
                .height(160.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1A1A1A))
        ) {
            if (shouldBlur) {
                BlurredContent(
                    shouldBlur = true,
                    modifier = Modifier.fillMaxSize()
                ) {
                    AsyncImage(
                        model            = movie.posterUrl,
                        contentDescription = movie.title,
                        contentScale     = ContentScale.Crop,
                        modifier         = Modifier.fillMaxSize()
                    )
                }
            } else {
                AsyncImage(
                    model            = movie.posterUrl,
                    contentDescription = movie.title,
                    contentScale     = ContentScale.Crop,
                    modifier         = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text       = movie.title,
            color      = Color.White.copy(alpha = 0.80f),
            fontSize   = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines   = 2,
            overflow   = TextOverflow.Ellipsis,
            lineHeight = 14.sp
        )
    }
}
