package com.movie.app.best.ui.screens.zee5

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.movie.app.best.R
import com.movie.app.best.data.model.Zee5Bucket
import com.movie.app.best.data.model.Zee5Item
import com.movie.app.best.ui.components.GlassBadge
import com.movie.app.best.ui.components.SkeletonBox
import com.movie.app.best.ui.theme.WasmerRed
import com.movie.app.best.ui.theme.WasmerBlack
import com.movie.app.best.ui.theme.WasmerCardDark
import com.movie.app.best.ui.theme.WasmerSubText
import com.movie.app.best.util.formatDurationSeconds
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@Composable
fun Zee5Screen(
    navController: NavController,
    viewModel: Zee5ViewModel = hiltViewModel(),
    onSearchClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val listState = rememberLazyListState()

    // Infinite scroll detection
    val shouldLoadMore = remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItem >= totalItems - 3
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            viewModel.loadMore()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WasmerBlack)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header with ZEE5 branding
            Zee5Header(
                onSearchClick = onSearchClick,
                onBackClick = { }
            )

            // Tab bar
            Zee5TabBar(
                currentTab = currentTab,
                onTabClick = { viewModel.switchTab(it) }
            )

            // Content
            when (val state = uiState) {
                is Zee5UiState.Loading -> {
                    Zee5ShimmerContent()
                }
                is Zee5UiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.message,
                                color = WasmerSubText,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.loadTab(currentTab) },
                                colors = ButtonDefaults.buttonColors(containerColor = WasmerRed)
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
                is Zee5UiState.Success -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        // Hero carousel for ALL tab (first bucket items)
                        if (currentTab == Zee5Tab.ALL && state.buckets.isNotEmpty()) {
                            val heroItems = state.buckets.firstOrNull()?.items?.take(5) ?: emptyList()
                            if (heroItems.isNotEmpty()) {
                                item {
                                    Zee5HeroCarousel(
                                        items = heroItems,
                                        onItemClick = { item ->
                                            item.id?.let { id ->
                                                navController.navigate("zee5_detail/${id}")
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        // Content rails
                        items(state.buckets) { bucket ->
                            Zee5Rail(
                                bucket = bucket,
                                onItemClick = { item ->
                                    item.id?.let { id ->
                                        navController.navigate("zee5_detail/${id}")
                                    }
                                },
                                onSeeAllClick = {
                                    val collectionId = bucket.id ?: bucket.collectionId
                                    if (!collectionId.isNullOrBlank()) {
                                        navController.navigate(
                                            "zee5_collection/${collectionId}?title=${bucket.title ?: ""}"
                                        )
                                    }
                                }
                            )
                        }

                        // Load more / end indicator at bottom
                        if (currentTab == Zee5Tab.ALL) {
                            item {
                                if (state.isLoadMore) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = WasmerRed,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                } else if (!state.hasMore && state.buckets.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        GlassBadge(text = "Reached to bottom")
                                    }
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
fun Zee5Header(
    onSearchClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.zee5_logo),
            contentDescription = "ZEE5",
            modifier = Modifier
                .height(28.dp)
                .width(34.dp)
        )

        IconButton(onClick = onSearchClick) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Search",
                tint = Color.White
            )
        }
    }
}

@Composable
fun Zee5TabBar(
    currentTab: Zee5Tab,
    onTabClick: (Zee5Tab) -> Unit
) {
    val tabs = listOf(
        Zee5Tab.ALL to "All",
        Zee5Tab.TV_SHOWS to "TV Shows",
        Zee5Tab.MOVIES to "Movies"
    )

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tabs) { (tab, label) ->
            val isSelected = currentTab == tab
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        if (isSelected) WasmerRed
                        else WasmerCardDark
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onTabClick(tab) }
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text(
                    text = label,
                    color = if (isSelected) Color.White else WasmerSubText,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun Zee5HeroCarousel(
    items: List<Zee5Item>,
    onItemClick: (Zee5Item) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { items.size })

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(5000)
            if (items.size > 1) {
                val nextPage = (pagerState.currentPage + 1) % items.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        ) { page ->
            val item = items[page]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onItemClick(item) }
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.effectiveLandscapeUrl ?: item.effectiveImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Gradient overlay for cinematic look
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.25f),
                                    Color.Transparent,
                                    Color.Transparent,
                                    WasmerBlack.copy(alpha = 0.85f),
                                    WasmerBlack.copy(alpha = 0.98f)
                                ),
                                startY = 0f,
                                endY = Float.POSITIVE_INFINITY
                            )
                        )
                )

                // Content info at bottom
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                ) {
                    val contentType = when {
                        item.isMovie -> "Movie"
                        item.isTvShow || item.isSeries -> "TV Show"
                        else -> ""
                    }
                    if (contentType.isNotBlank()) {
                        GlassBadge(text = contentType)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Text(
                        text = item.title ?: "",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val metaInfo = buildList {
                        item.duration?.let { if (it > 0) add(it.formatDurationSeconds()) }
                        item.ageRating?.let { if (it.isNotBlank()) add(it) }
                        item.genres?.firstOrNull()?.value?.let { add(it) }
                            ?: item.genre?.firstOrNull()?.value?.let { add(it) }
                    }.joinToString("  •  ")

                    if (metaInfo.isNotBlank()) {
                        Text(
                            text = metaInfo,
                            color = WasmerSubText,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Button(
                        onClick = { onItemClick(item) },
                        colors = ButtonDefaults.buttonColors(containerColor = WasmerRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Watch Now",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Page indicator dots
        if (items.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items.forEachIndexed { index, _ ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .width(if (isSelected) 18.dp else 6.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (isSelected) WasmerRed
                                else Color.White.copy(alpha = 0.4f)
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun Zee5Rail(
    bucket: Zee5Bucket,
    onItemClick: (Zee5Item) -> Unit,
    onSeeAllClick: () -> Unit = {}
) {
    val items = bucket.items ?: emptyList()
    val collectionId = bucket.id ?: bucket.collectionId
    val showSeeAll = !collectionId.isNullOrBlank() && items.isNotEmpty()

    Column(modifier = Modifier.padding(vertical = 10.dp)) {
        // Rail title with See All
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = bucket.title ?: "",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            if (showSeeAll) {
                Row(
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onSeeAllClick
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "See All",
                        color = WasmerRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = WasmerRed,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Horizontal rail
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items) { item ->
                Zee5Card(item = item, onClick = { onItemClick(item) })
            }
        }
    }
}

@Composable
fun Zee5Card(
    item: Zee5Item,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp? = 160.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        label = "card_press"
    )

    val contentTypeLabel = when {
        item.isMovie -> "Movie"
        item.isTvShow || item.isSeries -> "TV Show"
        item.isEpisode -> "Episode"
        else -> null
    }

    val sizeModifier = if (width != null) Modifier.width(width) else Modifier.fillMaxWidth()

    Column(
        modifier = modifier
            .then(sizeModifier)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .then(sizeModifier)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(WasmerCardDark)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.effectiveLandscapeUrl ?: item.effectiveImageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Subtle gradient overlay for badges and text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.35f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.45f)
                            )
                        )
                    )
            )

            // Top-left content type badge
            if (contentTypeLabel != null) {
                GlassBadge(
                    text = contentTypeLabel,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    tint = Color.White
                )
            }

            // Bottom-right duration badge
            val durationText = item.duration.formatDurationSeconds()
            if (durationText.isNotBlank()) {
                GlassBadge(
                    text = durationText,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                    tint = Color(0xFFFFD700)
                )
            }

            // Center play icon on press
            androidx.compose.animation.AnimatedVisibility(
                visible = isPressed,
                modifier = Modifier.fillMaxSize(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = item.title ?: "",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.then(sizeModifier)
        )

        val genreText = item.genres?.firstOrNull()?.value
            ?: item.genre?.firstOrNull()?.value
            ?: item.languages?.firstOrNull()
        if (genreText != null) {
            Text(
                text = genreText,
                color = WasmerSubText,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.then(sizeModifier)
            )
        }
    }
}

@Composable
private fun Zee5ShimmerContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WasmerBlack)
    ) {
        // Hero shimmer
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
            shape = RoundedCornerShape(0.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Rail shimmers
        repeat(3) {
            Column(modifier = Modifier.padding(vertical = 10.dp)) {
                SkeletonBox(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .width(180.dp)
                        .height(18.dp),
                    shape = RoundedCornerShape(4.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(4) {
                        Column {
                            SkeletonBox(
                                modifier = Modifier.width(160.dp).aspectRatio(16f / 9f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            SkeletonBox(
                                modifier = Modifier.width(140.dp).height(12.dp),
                                shape = RoundedCornerShape(4.dp)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            SkeletonBox(
                                modifier = Modifier.width(90.dp).height(10.dp),
                                shape = RoundedCornerShape(4.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
