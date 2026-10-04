package com.movie.app.best.ui.screens.sports

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsBaseball
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.SportsMotorsports
import androidx.compose.material.icons.filled.SportsMma
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.movie.app.best.data.model.SportCategory
import com.movie.app.best.data.model.SportEvent
import com.movie.app.best.ui.components.AppHeader
import com.movie.app.best.ui.components.TeamFlagBadge
import com.movie.app.best.ui.navigation.Screen
import com.movie.app.best.ui.navigation.navigateToBottomTab
import com.movie.app.best.ui.theme.AppRed
import com.movie.app.best.ui.theme.CardDark
import com.movie.app.best.ui.theme.AppSurface
import com.movie.app.best.ui.theme.InfoBlue
import com.movie.app.best.ui.theme.SuccessGreen
import com.movie.app.best.ui.util.LocalCollapsibleBarsState

@Composable
fun SportsScreen(
    navController: NavController,
    onMenuClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    viewModel: SportsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing = uiState.isRefreshing
    val listState = rememberLazyListState()

    val barsState = LocalCollapsibleBarsState.current
    val isBarsVisible = barsState?.isBarsVisible?.value ?: true

    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        if (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) {
            barsState?.show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = isBarsVisible,
                enter = slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = spring(
                        stiffness = Spring.StiffnessMediumLow,
                        dampingRatio = Spring.DampingRatioNoBouncy
                    )
                ) + expandVertically(
                    animationSpec = spring(
                        stiffness = Spring.StiffnessMediumLow,
                        dampingRatio = Spring.DampingRatioNoBouncy
                    )
                ),
                exit = slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = spring(
                        stiffness = Spring.StiffnessMediumLow,
                        dampingRatio = Spring.DampingRatioNoBouncy
                    )
                ) + shrinkVertically(
                    animationSpec = spring(
                        stiffness = Spring.StiffnessMediumLow,
                        dampingRatio = Spring.DampingRatioNoBouncy
                    )
                )
            ) {
                AppHeader(
                    onMenuClick = onMenuClick,
                    onSearchClick = { viewModel.setSearchActive(!uiState.isSearchActive) },
                    onNotificationClick = onNotificationClick,
                    onDownloadClick = { navController.navigateToBottomTab(Screen.Downloads.route) },
                    hasNotification = false
                )
            }

            // ── Pull-To-Refresh wrapping the whole screen content ──
            SwipeRefresh(
                state = rememberSwipeRefreshState(isRefreshing),
                onRefresh = { viewModel.loadData(forceRefresh = true) },
                indicator = { state, trigger ->
                    SwipeRefreshIndicator(
                        state = state,
                        refreshTriggerDistance = trigger,
                        scale = true,
                        backgroundColor = Color.White,
                        contentColor = Color.Black
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Inline search (event/team/league filter)
                    AnimatedVisibility(
                        visible = uiState.isSearchActive,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .background(AppSurface, RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                BasicTextField(
                                    value = uiState.searchQuery,
                                    onValueChange = { viewModel.onSearchQueryChange(it) },
                                    singleLine = true,
                                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                                    cursorBrush = SolidColor(AppRed),
                                    modifier = Modifier.weight(1f),
                                    decorationBox = { innerTextField ->
                                        if (uiState.searchQuery.isEmpty()) {
                                            Text(
                                                "Search matches, teams, leagues...",
                                                color = Color.White.copy(alpha = 0.4f),
                                                fontSize = 14.sp
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                                if (uiState.searchQuery.isNotEmpty()) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clickable { viewModel.onSearchQueryChange("") }
                                    )
                                }
                            }
                        }
                    }

                    // ── Category Icons Row ───────────────────────────────
                    if (uiState.categories.isNotEmpty()) {
                        SportsCategoryRow(
                            categories = uiState.categories,
                            selectedCategory = uiState.selectedCategory,
                            counts = uiState.categoryCounts,
                            onSelect = { viewModel.selectCategory(it) }
                        )
                    }

                    // ── Status Filter Tabs (All, Live, Recent, Upcoming) ──
                    StatusFilterTabs(
                        selected = uiState.selectedStatus,
                        allCount = uiState.allCount,
                        liveCount = uiState.liveCount,
                        recentCount = uiState.recentCount,
                        upcomingCount = uiState.upcomingCount,
                        onSelect = { viewModel.selectStatus(it) }
                    )

                    // ── Events List ──────────────────────────────────────
                    if (uiState.isLoading && uiState.events.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = AppRed,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    } else if (uiState.filteredEvents.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "No matches found",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Try selecting a different sport or filter tab",
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 96.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.filteredEvents, key = { it.id + it.slug }) { event ->
                                SportMatchCard(
                                    event = event,
                                    timeTick = uiState.timeTick,
                                    onClick = {
                                        viewModel.onMatchCardClick(event.slug)
                                        navController.navigate(Screen.SportsWatch.createRoute(event.slug))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SportsCategoryRow(
    categories: List<SportCategory>,
    selectedCategory: String,
    counts: Map<String, Int>,
    onSelect: (String) -> Unit
) {
    val visibleCategories = remember(categories, counts) {
        categories.filter { cat ->
            cat.title.equals("All", ignoreCase = true) || (counts[cat.title] ?: 0) > 0
        }
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(visibleCategories, key = { it.id.toString() + it.title }) { cat ->
            val isSelected = selectedCategory.equals(cat.title, ignoreCase = true)
            val count = counts[cat.title] ?: 0

            Box(
                modifier = Modifier.size(56.dp)
            ) {
                // Circle Button
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(if (isSelected) CardDark else Color(0xFF111111))
                        .border(
                            width = if (isSelected) 1.8.dp else 1.dp,
                            color = if (isSelected) AppRed else Color.White.copy(alpha = 0.12f),
                            shape = CircleShape
                        )
                        .clickable { onSelect(cat.title) },
                    contentAlignment = Alignment.Center
                ) {
                    if (cat.image.isNotBlank()) {
                        AsyncImage(
                            model = cat.image,
                            contentDescription = cat.title,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        val defaultIcon = getSportIcon(cat.title)
                        Icon(
                            imageVector = defaultIcon,
                            contentDescription = cat.title,
                            tint = if (isSelected) AppRed else Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Unclipped Badge on Top-End
                if (count > 0) {
                    val countText = if (count > 99) "99+" else count.toString()
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                            .clip(CircleShape)
                            .background(AppRed)
                            .padding(horizontal = if (countText.length > 1) 4.5.dp else 0.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = countText,
                            color = Color.White,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

private fun getSportIcon(title: String): ImageVector {
    val t = title.lowercase()
    return when {
        t.contains("cricket") -> Icons.Default.SportsCricket
        t.contains("football") || t.contains("soccer") -> Icons.Default.SportsSoccer
        t.contains("boxing") || t.contains("mma") || t.contains("wwe") || t.contains("ufc") -> Icons.Default.SportsMma
        t.contains("basket") -> Icons.Default.SportsBasketball
        t.contains("baseball") -> Icons.Default.SportsBaseball
        t.contains("motor") || t.contains("formula") || t.contains("motogp") -> Icons.Default.SportsMotorsports
        else -> Icons.Default.SportsSoccer
    }
}

@Composable
private fun StatusFilterTabs(
    selected: SportStatusFilter,
    allCount: Int,
    liveCount: Int,
    recentCount: Int,
    upcomingCount: Int,
    onSelect: (SportStatusFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatusFilterPill(
            label = "All ($allCount)",
            isSelected = selected == SportStatusFilter.ALL,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(SportStatusFilter.ALL) }
        )
        StatusFilterPill(
            label = "Live ($liveCount)",
            isSelected = selected == SportStatusFilter.LIVE,
            isLiveTab = true,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(SportStatusFilter.LIVE) }
        )
        StatusFilterPill(
            label = "Recent ($recentCount)",
            isSelected = selected == SportStatusFilter.RECENT,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(SportStatusFilter.RECENT) }
        )
        StatusFilterPill(
            label = "Upcoming ($upcomingCount)",
            isSelected = selected == SportStatusFilter.UPCOMING,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(SportStatusFilter.UPCOMING) }
        )
    }
}

@Composable
private fun StatusFilterPill(
    label: String,
    isSelected: Boolean,
    isLiveTab: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        targetValue = if (isSelected) CardDark else Color(0xFF111111),
        animationSpec = tween(150),
        label = "pillBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) AppRed else Color.White.copy(alpha = 0.12f),
        animationSpec = tween(150),
        label = "pillBorder"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) AppRed else Color.White.copy(alpha = 0.8f),
            fontSize = 11.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun SportMatchCard(
    event: SportEvent,
    timeTick: Long,
    onClick: () -> Unit
) {
    val info = event.eventInfo
    val isLive = event.isLive
    val isHot = event.isHot

    val pulseTransition = rememberInfiniteTransition(label = "livePulse")
    val liveAlpha by pulseTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0F1523))
            .border(
                width = 1.dp,
                color = if (isLive) AppRed.copy(alpha = 0.55f) else Color(0xFF1E283C),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Top Bar: Category Icon & League Name ────────────────
            Spacer(modifier = Modifier.height(7.dp))
            val category = (info?.eventCat ?: event.cat ?: "").uppercase()
            val league = (info?.eventName ?: event.title).uppercase()
            val headerText = if (category.isNotBlank() && league.isNotBlank()) "$category | $league" else league

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                val sportIcon = getSportIcon(category)
                Icon(
                    imageVector = sportIcon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = headerText,
                    color = Color.White,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ── Center Row: Team A | Center Status | Team B ─────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Team A (Left)
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    TeamFlagBadge(flagUrl = info?.teamAFlag, size = 35.dp)
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = info?.teamA ?: event.title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier
                            .weight(1f)
                            .basicMarquee()
                    )
                }

                // Center Column: Live Dot / Time & Date / Ended
                Column(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .widthIn(min = 80.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when {
                        isLive -> {
                            val liveComposition by rememberLottieComposition(
                                LottieCompositionSpec.RawRes(com.movie.app.best.R.raw.live_animation)
                            )
                            Box(
                                modifier = Modifier.size(22.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (liveComposition != null) {
                                    LottieAnimation(
                                        composition = liveComposition,
                                        iterations = LottieConstants.IterateForever,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .background(AppRed.copy(alpha = liveAlpha), CircleShape)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Live",
                                color = AppRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        else -> {
                            Text(
                                text = event.formattedTimeOnly.ifBlank { "00:00" },
                                color = Color(0xFF00B4D8),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = event.formattedDateOnly.ifBlank { "" },
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Team B (Right)
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = info?.teamB ?: "",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        modifier = Modifier
                            .weight(1f)
                            .basicMarquee()
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    TeamFlagBadge(flagUrl = info?.teamBFlag, size = 35.dp)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ── Bottom Notch Pill (Flush against bottom border) ────
            val notchText = event.getNotchText(timeTick)
            if (notchText.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 0.dp, bottomEnd = 0.dp))
                        .background(Color(0xFF080D1A))
                        .border(
                            width = 0.8.dp,
                            color = Color.White.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
                        )
                        .padding(horizontal = 18.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = notchText,
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.2.sp
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(7.dp))
            }
        }

        // HOT Badge at Top Right if active
        if (isHot) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 10.dp)
                    .background(SuccessGreen, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HOT 🔥",
                    color = Color.Black,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}