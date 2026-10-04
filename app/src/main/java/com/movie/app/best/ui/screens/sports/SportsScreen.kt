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
import com.google.accompanist.swiperefresh.SwipeRefresh
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

            // Inline search (event/team/league filter)
            AnimatedVisibility(
                visible = uiState.isSearchActive,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
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
            SwipeRefresh(
                state = rememberSwipeRefreshState(isRefreshing),
                onRefresh = { viewModel.loadData(forceRefresh = true) },
                modifier = Modifier.weight(1f)
            ) {
                if (uiState.isLoading && uiState.events.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = AppRed,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                } else if (uiState.filteredEvents.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
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
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.filteredEvents, key = { it.id + it.slug }) { event ->
                            SportMatchCard(
                                event = event,
                                timeTick = uiState.timeTick,
                                onClick = {
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

@Composable
private fun SportsCategoryRow(
    categories: List<SportCategory>,
    selectedCategory: String,
    counts: Map<String, Int>,
    onSelect: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(categories, key = { it.id.toString() + it.title }) { cat ->
            val isSelected = selectedCategory.equals(cat.title, ignoreCase = true)
            val count = counts[cat.title] ?: 0

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelect(cat.title) }
            ) {
                Box(modifier = Modifier.size(54.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(if (isSelected) CardDark else Color(0xFF111111))
                            .border(
                                width = if (isSelected) 1.8.dp else 1.dp,
                                color = if (isSelected) AppRed else Color.White.copy(alpha = 0.12f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (cat.image.isNotBlank()) {
                            AsyncImage(
                                model = cat.image,
                                contentDescription = cat.title,
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            val defaultIcon = getSportIcon(cat.title)
                            Icon(
                                imageVector = defaultIcon,
                                contentDescription = cat.title,
                                tint = if (isSelected) AppRed else Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    if (count > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 2.dp, y = (-2).dp)
                                .background(AppRed, RoundedCornerShape(50))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = if (count > 99) "99+" else count.toString(),
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = cat.title,
                    color = if (isSelected) AppRed else Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1
                )
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
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardDark)
            .border(
                width = if (isLive) 1.2.dp else 0.8.dp,
                color = if (isLive) AppRed.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 12.dp, start = 14.dp, end = 14.dp)
        ) {
            val category = (info?.eventCat ?: event.cat ?: "").uppercase()
            val league = (info?.eventName ?: event.title).uppercase()
            val headerText = if (category.isNotBlank() && league.isNotBlank()) "$category | $league" else league

            Text(
                text = headerText,
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    TeamFlagBadge(flagUrl = info?.teamAFlag)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = info?.teamA ?: event.title,
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Column(
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .widthIn(min = 90.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when {
                        isLive -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .background(AppRed.copy(alpha = liveAlpha), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "((•)) Live",
                                    color = AppRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            val elapsed = event.elapsedLiveText
                            if (elapsed.isNotBlank()) {
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = elapsed,
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        event.isUpcoming -> {
                            Text(
                                text = event.formattedStartTime,
                                color = InfoBlue,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = event.countdownText,
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                        else -> {
                            Text(
                                text = "Ended",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = info?.teamB ?: "",
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TeamFlagBadge(flagUrl = info?.teamBFlag)
                }
            }

            if (isHot) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .background(SuccessGreen, RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HOT 🔥",
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}