package com.movie.app.best.ui.screens.trending

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.movie.app.best.data.settings.ModerationSettings
import com.movie.app.best.ui.components.CompactPageHeader
import com.movie.app.best.ui.screens.home.components.movieGridItems

@Composable
fun TrendingScreen(
    navController: androidx.navigation.NavController,
    onContentClick: (String, Boolean, String) -> Unit,
    onSearchClick: () -> Unit = {},
    viewModel: TrendingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val filteredMovies = remember(uiState.popularMovies, context) { ModerationSettings.filterMovies(context, uiState.popularMovies) }

    val shouldLoadMore by remember(uiState) {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            lastVisible >= totalItems - 6 && uiState.hasMore && !uiState.isPopularLoadingMore
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMorePopular()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        CompactPageHeader(
            title = "Trending",
            onBackClick = { navController.popBackStack() },
            onSearchClick = onSearchClick
        )

        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when {
                uiState.isPopularLoading && uiState.popularMovies.isEmpty() -> {
                    CircularProgressIndicator(
                        color = Color.Red,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.popularError != null && uiState.popularMovies.isEmpty() -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.popularError ?: "Unknown error",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = viewModel::loadPopular,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Text("Retry", color = Color.White)
                        }
                    }
                }
                uiState.popularMovies.isEmpty() -> {
                    Text(
                        text = "No trending content",
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        movieGridItems(
                            movies = filteredMovies,
                            onMovieClick = onContentClick
                        )

                        item { Spacer(modifier = Modifier.height(80.dp)) }

                        if (uiState.isPopularLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = Color.Red,
                                        modifier = Modifier.height(32.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
