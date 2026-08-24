package com.movie.app.best.ui.screens.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.movie.app.best.data.model.Movie

fun LazyListScope.movieGridItems(
    movies: List<Movie>,
    onMovieClick: (String, Boolean, String) -> Unit
) {
    val columns = 3
    val rows = (movies.size + columns - 1) / columns
    for (rowIdx in 0 until rows) {
        item(key = "grid_row_$rowIdx") {
            GridRow(
                movies = movies,
                rowIdx = rowIdx,
                columns = columns,
                onMovieClick = onMovieClick
            )
        }
    }
}

@Composable
private fun GridRow(
    movies: List<Movie>,
    rowIdx: Int,
    columns: Int,
    onMovieClick: (String, Boolean, String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (colIdx in 0 until columns) {
            val index = rowIdx * columns + colIdx
            if (index < movies.size) {
                val movie = movies[index]
                androidx.compose.foundation.layout.Box(modifier = Modifier.weight(1f)) {
                    MoviePosterCard(
                        movie = movie,
                        size = CardSize.NORMAL,
                        onClick = { onMovieClick(movie.slug, movie.isSeries, movie.imdbId) }
                    )
                }
            } else {
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}
