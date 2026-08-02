package com.movie.app.best.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.movie.app.best.data.model.Movie

@Composable
fun TVShowCard(
    movie: Movie,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    MovieCard(
        movie = movie,
        onClick = onClick,
        modifier = modifier
    )
}
