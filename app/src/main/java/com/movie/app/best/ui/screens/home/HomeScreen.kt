package com.movie.app.best.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.movie.app.best.ui.components.AppHeader
import com.movie.app.best.ui.components.SkeletonHomeContent
import com.movie.app.best.data.settings.ModerationSettings
import com.movie.app.best.ui.screens.home.components.*
import com.movie.app.best.ui.screens.home.components.LiveChannelsCarousel
import com.movie.app.best.data.model.UnifiedChannel

@Composable
fun HomeScreen(
    onContentClick: (String, Boolean, String) -> Unit,
    navController: NavController,
    isOnline: Boolean = true,
    onRetryConnection: () -> Unit = {},
    onGoToDownloads: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onDownloadClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    val allMovies = remember(uiState.allTabMovies, context) { ModerationSettings.filterMovies(context, uiState.allTabMovies) }
    val trending    = remember(uiState.trendingMovies, context) { ModerationSettings.filterMovies(context, uiState.trendingMovies) }
    val myFeed      = remember(uiState.myFeedMovies, context) { ModerationSettings.filterMovies(context, uiState.myFeedMovies) }
    val newReleases = remember(allMovies) { allMovies.sortedByDescending { it.id }.take(12) }
    val series      = remember(uiState.seriesMovies, context) { ModerationSettings.filterMovies(context, uiState.seriesMovies) }

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            lastVisible >= totalItems - 6 && uiState.canLoadMoreAllTab && !uiState.isAllTabLoadingMore
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMoreAllTab()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            if (uiState.isPageLoading) {
                item { SkeletonHomeContent() }
            } else if (!isOnline) {
                item {
                    NoInternetHomeContent(
                        onRetry = onRetryConnection,
                        onGoToDownloads = onGoToDownloads
                    )
                }
            } else {
                if (uiState.liveChannels.isNotEmpty()) {
                    item {
                        LiveChannelsCarousel(
                            channels = uiState.liveChannels,
                            onChannelClick = { channel ->
                                navController.navigate(
                                    "videoPlayer?playerUrl=${channel.streamUrl}&title=${channel.name}&isLive=true"
                                )
                            },
                            onMoreClick = { navController.navigate(com.movie.app.best.ui.navigation.Screen.LiveTv.route) },
                            applyTopPadding = true
                        )
                    }
                }

                if (trending.isNotEmpty()) {
                    item {
                        MovieRowSection(
                            title        = "Trending Now 🔥",
                            movies       = trending,
                            cardSize     = CardSize.NORMAL,
                            onMovieClick = onContentClick,
                            onSeeAllClick = { navController.navigate(com.movie.app.best.ui.navigation.Screen.Trending.route) }
                        )
                    }
                }

                if (uiState.newIndiaReleases.isNotEmpty()) {
                    item {
                        MovieRowSection(
                            title        = "Newly Indian Releases 🎬",
                            movies       = uiState.newIndiaReleases,
                            cardSize     = CardSize.LARGE,
                            onMovieClick = onContentClick,
                            onSeeAllClick = { navController.navigate(com.movie.app.best.ui.navigation.Screen.NewRelease.createRoute("in")) }
                        )
                    }
                }

                if (uiState.newUsReleases.isNotEmpty()) {
                    item {
                        MovieRowSection(
                            title        = "Hollywood Crazy Drops 🗽",
                            movies       = uiState.newUsReleases,
                            cardSize     = CardSize.NORMAL,
                            onMovieClick = onContentClick,
                            onSeeAllClick = { navController.navigate(com.movie.app.best.ui.navigation.Screen.NewRelease.createRoute("us")) }
                        )
                    }
                }

                if (newReleases.isNotEmpty()) {
                    item {
                        MovieRowSection(
                            title        = "Latest Uploads",
                            movies       = newReleases,
                            cardSize     = CardSize.LARGE,
                            onMovieClick = onContentClick,
                            onSeeAllClick = { navController.navigate(com.movie.app.best.ui.navigation.Screen.LatestUploads.route) }
                        )
                    }
                }

                if (series.isNotEmpty()) {
                    item {
                        MovieRowSection(
                            title        = "Binge-Worthy Series",
                            movies       = series,
                            cardSize     = CardSize.NORMAL,
                            onMovieClick = onContentClick,
                            onSeeAllClick = { navController.navigate(com.movie.app.best.ui.navigation.Screen.SeriesList.route) }
                        )
                    }
                }

                if (myFeed.isNotEmpty()) {
                    item {
                        MovieRowSection(
                            title        = "Because You Watched Similar",
                            movies       = myFeed,
                            cardSize     = CardSize.NORMAL,
                            onMovieClick = onContentClick,
                            onSeeAllClick = { navController.navigate(com.movie.app.best.ui.navigation.Screen.MyFeed.route) }
                        )
                    }
                }

                if (allMovies.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title      = "More to Explore",
                            showSeeAll = false
                        )
                    }
                    movieGridItems(
                        movies = allMovies,
                        onMovieClick = onContentClick
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }

            if (uiState.isAllTabLoadingMore) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                            modifier = Modifier.height(32.dp)
                        )
                    }
                }
            }
        }

        AppHeader(
            onMenuClick         = onMenuClick,
            onSearchClick       = onSearchClick,
            onDownloadClick     = onDownloadClick,
            onNotificationClick = { navController.navigate(com.movie.app.best.ui.navigation.Screen.Notifications.route) },
            hasNotification     = uiState.notification?.isActive == true,
            modifier            = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun NoInternetHomeContent(
    onRetry: () -> Unit,
    onGoToDownloads: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(top = 48.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(Color(0xFF1A1A1A), androidx.compose.foundation.shape.RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.WifiOff,
                contentDescription = "No internet",
                tint = Color(0xFFE50914),
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "No Internet Connection",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Check your connection and try again",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp)
                .height(52.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Refresh,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Retry",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedButton(
            onClick = onGoToDownloads,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp)
                .height(52.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Download,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Go to Downloads",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
