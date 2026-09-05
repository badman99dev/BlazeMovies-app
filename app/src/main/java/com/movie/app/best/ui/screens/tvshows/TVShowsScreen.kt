package com.movie.app.best.ui.screens.tvshows

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.movie.app.best.data.model.Movie
import com.movie.app.best.data.settings.ModerationSettings
import com.movie.app.best.ui.components.BlurredContent
import com.movie.app.best.ui.components.SkeletonPosterCard
import com.movie.app.best.ui.components.AppHeader
import com.movie.app.best.ui.screens.home.components.QualityBadge
import com.movie.app.best.ui.screens.home.components.StreamBadge
import com.movie.app.best.ui.screens.home.components.SeriesBadge
import com.movie.app.best.ui.screens.home.components.AgeBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TVShowsScreen(
    navController: androidx.navigation.NavController,
    onContentClick: (String, Boolean, String) -> Unit,
    onSearchClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    viewModel: TVShowsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val gridState = rememberLazyGridState()

    val shouldPaginate by remember {
        derivedStateOf {
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= uiState.tvShows.size - 6 && uiState.canLoadMore && !uiState.isLoading
        }
    }

    if (shouldPaginate) {
        viewModel.loadNextPage()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            AppHeader(
                onMenuClick = onMenuClick,
                onSearchClick = onSearchClick,
                onNotificationClick = { navController.navigate(com.movie.app.best.ui.navigation.Screen.Notifications.route) },
                hasNotification = false
            )

            when {
                uiState.isLoading && uiState.tvShows.isEmpty() -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(12) {
                            SkeletonPosterCard(modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
                uiState.error != null && uiState.tvShows.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = uiState.error ?: "Unknown error",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.loadTVShows() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                            ) {
                                Text("Retry", color = Color.White)
                            }
                        }
                    }
                }
                uiState.tvShows.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No TV shows found",
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
                else -> {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val visibleShows = remember(uiState.tvShows, context, ModerationSettings.changeVersion) { ModerationSettings.filterMovies(context, uiState.tvShows) }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        state = gridState,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(visibleShows, key = { it.slug }) { tvShow ->
                            TVShowGridItem(
                                movie = tvShow,
                                onClick = { onContentClick(tvShow.slug, tvShow.isSeries, tvShow.imdbId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TVShowGridItem(
    movie: Movie,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
    ) {
        Box {
            BlurredContent(
                shouldBlur = ModerationSettings.effectiveShouldBlur(movie),
                modifier = Modifier.fillMaxSize()
            ) {
                AsyncImage(
                    model = movie.posterUrl,
                    contentDescription = movie.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2 / 3f)
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(5.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (movie.qualityLabel.isNotBlank()) {
                        QualityBadge(label = movie.qualityLabel)
                    }
                    if (movie.hasStream) {
                        StreamBadge()
                    }
                }
                if (movie.isSeries) {
                    SeriesBadge()
                }
                if (movie.contentModeration?.hasAnyFlag == true) {
                    AgeBadge()
                }
            }

            if (movie.rank <= 5) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = Color(0xFFFF6D00),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 4.dp, top = 16.dp)
                        .size(14.dp)
                )
            }



            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            ),
                            startY = 60f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
            ) {
                Text(
                    text = movie.title,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = movie.releaseYear,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 9.sp
                    )
                    if (movie.rating.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = " • ", color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp)
                            Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(10.dp))
                            Text(text = movie.rating, color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp)
                        }
                    }
                }
            }
        }
    }
}
