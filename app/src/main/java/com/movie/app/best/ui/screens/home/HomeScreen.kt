package com.movie.app.best.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import com.movie.app.best.ui.util.LocalCollapsibleBarsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.movie.app.best.ui.components.AppHeader
import com.movie.app.best.ui.components.DrawerLinkRouter
import com.movie.app.best.util.ShareLinks
import com.movie.app.best.util.SliderLink
import com.movie.app.best.data.settings.ModerationSettings
import com.movie.app.best.ui.screens.home.components.*
import com.movie.app.best.ui.screens.home.components.LiveChannelsCarousel
import com.movie.app.best.data.model.UnifiedChannel

@Composable
fun HomeScreen(
    onContentClick: (String, Boolean, String) -> Unit,
    navController: NavController,
    onSearchClick: () -> Unit = {},
    onDownloadClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val pullRefreshState = rememberPullToRefreshState()
    val density = LocalDensity.current

    val allMovies = remember(uiState.allTabMovies, context, ModerationSettings.changeVersion) { ModerationSettings.filterMovies(context, uiState.allTabMovies) }
    val trending    = remember(uiState.trendingMovies, context, ModerationSettings.changeVersion) { ModerationSettings.filterMovies(context, uiState.trendingMovies) }
    val myFeed      = remember(uiState.myFeedMovies, context, ModerationSettings.changeVersion) { ModerationSettings.filterMovies(context, uiState.myFeedMovies) }
    val newReleases = remember(allMovies) { allMovies.sortedByDescending { it.id }.take(12) }
    val series      = remember(uiState.seriesMovies, context, ModerationSettings.changeVersion) { ModerationSettings.filterMovies(context, uiState.seriesMovies) }
    val newIndia    = remember(uiState.newIndiaReleases, context, ModerationSettings.changeVersion) { ModerationSettings.filterMovies(context, uiState.newIndiaReleases) }
    val newUs       = remember(uiState.newUsReleases, context, ModerationSettings.changeVersion) { ModerationSettings.filterMovies(context, uiState.newUsReleases) }

    val barsState = LocalCollapsibleBarsState.current
    val isBarsVisible = barsState?.isBarsVisible?.value ?: true

    // Hero slider: track the current page (for the blurred backdrop) and the slider's
    // on-screen top so the backdrop covers exactly the region above it.
    var currentSliderPage by remember { mutableIntStateOf(0) }
    val sliderBottomPx = remember { mutableFloatStateOf(0f) }
    val sliderItems = uiState.sliderItems

    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        if (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) {
            barsState?.show()
        }
    }

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
        // Blurred poster backdrop covering the header + Live TV row down to the bottom of
        // the hero poster (so the area around the slider is blurred too). It fades into
        // the AMOLED black background at its lower edge.
        SliderBackdrop(
            imageUrl = sliderItems.getOrNull(currentSliderPage)?.imageUrl,
            heightProvider = { sliderBottomPx.floatValue },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize(),
            state = pullRefreshState,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullRefreshState,
                    isRefreshing = uiState.isRefreshing,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = with(density) { (barsState?.headerHeightPx ?: 0f).toDp() }),
                    containerColor = Color(0xFF1A1A1A),
                    color = Color(0xFFE50914)
                )
            }
        ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = 88.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // LIVE TV — heading hamesha, content skeleton
            if (uiState.liveChannelsError == null || uiState.liveChannels.isNotEmpty()) {
                item {
                    LiveChannelsCarousel(
                        channels = uiState.liveChannels,
                        isLoading = uiState.isLiveChannelsLoading && uiState.liveChannels.isEmpty(),
                        onChannelClick = { channel ->
                            navController.navigate(
                                com.movie.app.best.ui.navigation.Screen.LiveTvWatch.createRoute(
                                    slug = channel.slug,
                                    name = channel.name,
                                    logo = channel.logoUrl
                                )
                            )
                        },
                        onMoreClick = { navController.navigate(com.movie.app.best.ui.navigation.Screen.LiveTv.route) },
                        applyTopPadding = true
                    )
                }
            }

            // Hero slider — sits between the Live TV row and Trending.
            if (uiState.sliderItems.isNotEmpty()) {
                item {
                    HomeSliderSection(
                        items = uiState.sliderItems,
                        onItemClick = { item -> DrawerLinkRouter.open(context, navController, item.deepLink) },
                        onShareClick = { item ->
                            val target = SliderLink.parse(item.deepLink)
                            if (target.isContent && target.slug.isNotBlank()) {
                                val url = if (target.isSeries) ShareLinks.series(target.slug) else ShareLinks.movie(target.slug)
                                ShareLinks.share(context, url, item.title)
                            }
                        },
                        onAddToListClick = { item ->
                            if (viewModel.isLoggedIn()) viewModel.addSliderToMyList(item)
                            else navController.navigate(com.movie.app.best.ui.navigation.Screen.Login.route)
                        },
                        onWatchClick = { item -> DrawerLinkRouter.open(context, navController, item.deepLink) },
                        onPageChanged = { currentSliderPage = it },
                        onPosterBottom = { sliderBottomPx.floatValue = it }
                    )
                }
            }

            // Trending Now
            if (uiState.trendingError == null || trending.isNotEmpty()) {
                item {
                    MovieRowSection(
                        title        = "Trending Now 🔥",
                        movies       = trending,
                        isLoading    = uiState.isTrendingLoading && trending.isEmpty(),
                        cardSize     = CardSize.NORMAL,
                        onMovieClick = onContentClick,
                        onSeeAllClick = { navController.navigate(com.movie.app.best.ui.navigation.Screen.Trending.route) }
                    )
                }
            }

            // Newly Indian Releases
            if (uiState.newIndiaError == null || newIndia.isNotEmpty()) {
                item {
                    MovieRowSection(
                        title        = "Newly Indian Releases 🎬",
                        movies       = newIndia,
                        isLoading    = uiState.isNewIndiaLoading && newIndia.isEmpty(),
                        cardSize     = CardSize.LARGE,
                        onMovieClick = onContentClick,
                        onSeeAllClick = { navController.navigate(com.movie.app.best.ui.navigation.Screen.NewRelease.createRoute("in")) }
                    )
                }
            }

            // Hollywood Crazy Drops
            if (uiState.newUsError == null || newUs.isNotEmpty()) {
                item {
                    MovieRowSection(
                        title        = "Hollywood Crazy Drops 🗽",
                        movies       = newUs,
                        isLoading    = uiState.isNewUsLoading && newUs.isEmpty(),
                        cardSize     = CardSize.NORMAL,
                        onMovieClick = onContentClick,
                        onSeeAllClick = { navController.navigate(com.movie.app.best.ui.navigation.Screen.NewRelease.createRoute("us")) }
                    )
                }
            }

            // Latest Uploads
            if (uiState.allTabError == null || allMovies.isNotEmpty()) {
                item {
                    MovieRowSection(
                        title        = "Latest Uploads",
                        movies       = newReleases,
                        isLoading    = uiState.isAllTabLoading && allMovies.isEmpty(),
                        cardSize     = CardSize.LARGE,
                        onMovieClick = onContentClick,
                        onSeeAllClick = { navController.navigate(com.movie.app.best.ui.navigation.Screen.LatestUploads.route) }
                    )
                }
            }

            // Binge-Worthy Series
            if (uiState.seriesError == null || series.isNotEmpty()) {
                item {
                    MovieRowSection(
                        title        = "Binge-Worthy Series",
                        movies       = series,
                        isLoading    = uiState.isSeriesLoading && series.isEmpty(),
                        cardSize     = CardSize.NORMAL,
                        onMovieClick = onContentClick,
                        onSeeAllClick = { navController.navigate(com.movie.app.best.ui.navigation.Screen.SeriesList.route) }
                    )
                }
            }

            // Because You Watched Similar
            if (uiState.myFeedError == null || myFeed.isNotEmpty()) {
                item {
                    MovieRowSection(
                        title        = "Because You Watched Similar",
                        movies       = myFeed,
                        isLoading    = uiState.isMyFeedLoading && myFeed.isEmpty(),
                        cardSize     = CardSize.NORMAL,
                        onMovieClick = onContentClick,
                        onSeeAllClick = { navController.navigate(com.movie.app.best.ui.navigation.Screen.MyFeed.route) }
                    )
                }
            }

            // More to Explore
            if (uiState.allTabError == null || allMovies.isNotEmpty()) {
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
        }

        AppHeader(
            onMenuClick         = onMenuClick,
            onSearchClick       = onSearchClick,
            onDownloadClick     = onDownloadClick,
            onNotificationClick = { navController.navigate(com.movie.app.best.ui.navigation.Screen.Notifications.route) },
            hasNotification     = uiState.notification?.isActive == true,
            modifier            = Modifier
                .align(Alignment.TopCenter)
                .offset { IntOffset(0, (barsState?.headerOffset ?: 0f).roundToInt()) }
        )
    }
}

/**
 * Blurred copy of the current hero-slide poster, sized to the region above the slider.
 * Reads its height through [heightProvider] so only this layer recomposes while the
 * list scrolls. Fades into the black background at its bottom edge.
 */
@Composable
private fun SliderBackdrop(
    imageUrl: String?,
    heightProvider: () -> Float,
    modifier: Modifier = Modifier
) {
    if (imageUrl.isNullOrBlank()) return
    val heightPx = heightProvider()
    if (heightPx < 8f) return
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(with(density) { heightPx.toDp() })
            .clipToBounds()
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(38.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Black.copy(alpha = 0.30f),
                        0.55f to Color.Black.copy(alpha = 0.45f),
                        1.0f to Color.Black
                    )
                )
        )
    }
}