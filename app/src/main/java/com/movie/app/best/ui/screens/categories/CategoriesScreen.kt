package com.movie.app.best.ui.screens.categories

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.movie.app.best.data.model.Movie
import com.movie.app.best.ui.components.AppHeader
import com.movie.app.best.ui.components.CompactPageHeader
import com.movie.app.best.ui.components.ErrorView
import com.movie.app.best.ui.components.SkeletonCategoryTile
import com.movie.app.best.ui.screens.categories.components.CategoryTile
import com.movie.app.best.ui.screens.home.components.movieGridItems
import com.movie.app.best.ui.theme.AppFonts
import com.movie.app.best.ui.theme.AppRed
import com.movie.app.best.ui.theme.SecondaryText
import com.movie.app.best.ui.util.LocalCollapsibleBarsState
import kotlinx.coroutines.delay

@Composable
fun CategoriesScreen(
    onCategoryClick: (categorySlug: String, categoryName: String) -> Unit,
    onContentClick: (slug: String, isSeries: Boolean, imdbId: String) -> Unit = { _, _, _ -> },
    onSearchClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    viewModel: CategoriesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val barsState = LocalCollapsibleBarsState.current
    val isBarsVisible = barsState?.isBarsVisible?.value ?: true

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        TopGlow()

        Column(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = isBarsVisible,
                enter = slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = spring(
                        stiffness = Spring.StiffnessMedium,
                        dampingRatio = Spring.DampingRatioNoBouncy
                    )
                ),
                exit = slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = spring(
                        stiffness = Spring.StiffnessMedium,
                        dampingRatio = Spring.DampingRatioNoBouncy
                    )
                )
            ) {
                AppHeader(
                    onMenuClick = onMenuClick,
                    onSearchClick = onSearchClick,
                    onNotificationClick = onNotificationClick,
                    hasNotification = false
                )
            }

            val context = LocalContext.current
            val visibleCategories = remember(uiState.categories, context) {
                com.movie.app.best.data.settings.ModerationSettings
                    .filterCategories(context, uiState.categories)
            }

            TitleRow(count = visibleCategories.size)

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    uiState.isLoading && visibleCategories.isEmpty() -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 40.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(8) {
                                SkeletonCategoryTile(modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                    uiState.error != null && visibleCategories.isEmpty() -> {
                        ErrorView(
                            error = uiState.error ?: "Unknown error",
                            onRetry = viewModel::loadCategories
                        )
                    }
                    visibleCategories.isEmpty() -> {
                        Text(
                            text = "No categories available",
                            color = SecondaryText,
                            fontFamily = AppFonts.ui,
                            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
                        )
                    }
                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 40.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(visibleCategories, key = { _, c -> c.slug }) { index, category ->
                                StaggerIn(index = index) {
                                    CategoryTile(
                                        category = category,
                                        onClick = { onCategoryClick(category.slug, category.categoryName) },
                                        modifier = Modifier.fillMaxWidth()
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

@Composable
private fun TitleRow(count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = "Categories",
            color = Color.White,
            fontFamily = AppFonts.display,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 26.sp,
            letterSpacing = (-0.7).sp
        )
        Spacer(modifier = Modifier.width(10.dp))
        if (count > 0) {
            Text(
                text = "$count collections",
                color = SecondaryText,
                fontFamily = AppFonts.ui,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 3.dp)
            )
        }
    }
}

/** Soft red bloom behind the top area so the pure-black AMOLED page has some depth. */
@Composable
private fun TopGlow() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(Color.Black)
            .drawWithCache {
                val brush = Brush.radialGradient(
                    colors = listOf(AppRed.copy(alpha = 0.16f), Color.Transparent),
                    center = Offset(size.width * 0.25f, 0f),
                    radius = size.width * 0.9f
                )
                onDrawWithContent {
                    drawContent()
                    drawRect(brush)
                }
            }
    )
}

@Composable
private fun StaggerIn(index: Int, content: @Composable () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay((index % 12).toLong() * 16L)
        shown = true
    }
    val progress by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(durationMillis = 320),
        label = "stagger"
    )
    Box(
        modifier = Modifier
            .graphicsLayerCompat(
                alpha = progress,
                translationY = (1f - progress) * 28f
            )
    ) {
        content()
    }
}

private fun Modifier.graphicsLayerCompat(alpha: Float, translationY: Float): Modifier =
    this.graphicsLayer {
        this.alpha = alpha
        this.translationY = translationY
    }

@Composable
fun CategoryPageScreen(
    categorySlug: String,
    categoryName: String,
    onContentClick: (slug: String, isSeries: Boolean, imdbId: String) -> Unit,
    onBackClick: () -> Unit,
    onSearchClick: () -> Unit = {},
    viewModel: CategoryPageViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(categorySlug) {
        viewModel.loadMovies(categorySlug)
    }

    val listState = rememberLazyListState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            lastVisible >= totalItems - 6 && uiState.canLoadMore && !uiState.isLoading
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadNextPage()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        CompactPageHeader(
            title = categoryName,
            onBackClick = onBackClick,
            onSearchClick = onSearchClick
        )

        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when {
                uiState.isLoading && uiState.movies.isEmpty() -> {
                    CircularProgressIndicator(color = AppRed)
                }
                uiState.error != null && uiState.movies.isEmpty() -> {
                    ErrorView(
                        error = uiState.error ?: "Unknown error",
                        onRetry = { viewModel.loadMovies(categorySlug) }
                    )
                }
                uiState.movies.isEmpty() -> {
                    Text(
                        text = "No content found",
                        color = SecondaryText,
                        fontFamily = AppFonts.ui,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
                    )
                }
                else -> {
                    val context = LocalContext.current
                    val visibleMovies: List<Movie> = remember(
                        uiState.movies,
                        context,
                        com.movie.app.best.data.settings.ModerationSettings.changeVersion
                    ) {
                        com.movie.app.best.data.settings.ModerationSettings
                            .filterMovies(context, uiState.movies)
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        movieGridItems(
                            movies = visibleMovies,
                            onMovieClick = onContentClick
                        )

                        item { Spacer(modifier = Modifier.height(80.dp)) }

                        if (uiState.isLoading) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = AppRed,
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
