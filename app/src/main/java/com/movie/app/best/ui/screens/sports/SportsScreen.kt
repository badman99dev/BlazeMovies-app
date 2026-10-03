package com.movie.app.best.ui.screens.sports

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsBaseball
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.SportsFootball
import androidx.compose.material.icons.filled.SportsMotorsports
import androidx.compose.material.icons.filled.SportsMma
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.movie.app.best.data.model.SportCategory
import com.movie.app.best.data.model.SportEvent
import com.movie.app.best.ui.navigation.Screen
import com.movie.app.best.ui.theme.AppRed

@Composable
fun SportsScreen(
    navController: NavController,
    onMenuClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    viewModel: SportsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing = uiState.isRefreshing

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0D14))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // ── Top Bar ──────────────────────────────────────────
            SportsTopHeader(
                onMenuClick = onMenuClick,
                onRefreshClick = { viewModel.loadData(forceRefresh = true) },
                onNotificationClick = onNotificationClick,
                isSearchActive = uiState.isSearchActive,
                searchQuery = uiState.searchQuery,
                onSearchToggle = { viewModel.setSearchActive(!uiState.isSearchActive) },
                onSearchQueryChange = { viewModel.onSearchQueryChange(it) }
            )

            // ── Announcement Ticker Banner ───────────────────────
            AnnouncementBanner(
                text = "⚡ Live sports streams updated in real-time · Tap any match to watch",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )

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
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.filteredEvents, key = { it.id + it.slug }) { event ->
                            SportMatchCard(
                                event = event,
                                timeTick = uiState.timeTick,
                                onClick = {
                                    val route = Screen.SportsWatch.createRoute(
                                        slug = event.slug,
                                        title = event.title,
                                        eventCat = event.eventInfo?.eventCat ?: "",
                                        eventName = event.eventInfo?.eventName ?: "",
                                        teamA = event.eventInfo?.teamA ?: "",
                                        teamB = event.eventInfo?.teamB ?: "",
                                        teamAFlag = event.eventInfo?.teamAFlag ?: "",
                                        teamBFlag = event.eventInfo?.teamBFlag ?: "",
                                        startTime = event.eventInfo?.startTime ?: "",
                                        isLive = event.isLive
                                    )
                                    navController.navigate(route)
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
private fun SportsTopHeader(
    onMenuClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onNotificationClick: () -> Unit,
    isSearchActive: Boolean,
    searchQuery: String,
    onSearchToggle: () -> Unit,
    onSearchQueryChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Menu Icon
            HeaderIcon(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Brand Title (CRICFy TV / BLAZE SPORTS style)
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Black)) {
                        append("CRIC")
                    }
                    withStyle(SpanStyle(color = AppRed, fontWeight = FontWeight.Black)) {
                        append("Fy ")
                    }
                    withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) {
                        append("TV")
                    }
                },
                fontSize = 20.sp,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.weight(1f)
            )

            // Notifications
            HeaderIcon(onClick = onNotificationClick) {
                Box {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = Color.White,
                        modifier = Modifier.size(23.dp)
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(7.dp)
                            .background(AppRed, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Star / Favorites
            HeaderIcon(onClick = {}) {
                Icon(
                    imageVector = Icons.Default.StarBorder,
                    contentDescription = "Favorites",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(23.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Refresh
            HeaderIcon(onClick = onRefreshClick) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(23.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Search
            HeaderIcon(onClick = onSearchToggle) {
                Icon(
                    imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = "Search",
                    tint = if (isSearchActive) AppRed else Color.White,
                    modifier = Modifier.size(23.dp)
                )
            }
        }

        // Inline Search Bar
        AnimatedVisibility(
            visible = isSearchActive,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 4.dp)
                    .background(Color(0xFF191C28), RoundedCornerShape(10.dp))
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
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                        cursorBrush = SolidColor(AppRed),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    "Search matches, teams, leagues...",
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 14.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderIcon(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun AnnouncementBanner(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF281816), Color(0xFF1E1E28))
                ),
                shape = RoundedCornerShape(10.dp)
            )
            .border(0.8.dp, Color(0xFFFA5035).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Campaign,
            contentDescription = null,
            tint = Color(0xFFFF643D),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
                    // Circular icon container
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(if (isSelected) Color(0xFF242938) else Color(0xFF151824))
                            .border(
                                width = if (isSelected) 1.8.dp else 1.dp,
                                color = if (isSelected) Color(0xFFFF643D) else Color.White.copy(alpha = 0.12f),
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
                                tint = if (isSelected) Color(0xFFFF643D) else Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    // Red counter badge at top right
                    if (count > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 2.dp, y = (-2).dp)
                                .background(Color(0xFFE50914), RoundedCornerShape(50))
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
                    color = if (isSelected) Color(0xFFFF643D) else Color.White.copy(alpha = 0.7f),
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
        targetValue = if (isSelected) Color(0xFF281C1B) else Color(0xFF141722),
        animationSpec = tween(150),
        label = "pillBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFFFF5238) else Color.White.copy(alpha = 0.12f),
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
            color = if (isSelected) Color(0xFFFF5238) else Color.White.copy(alpha = 0.8f),
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
            .background(Color(0xFF141722))
            .border(
                width = if (isLive) 1.2.dp else 0.8.dp,
                color = if (isLive) Color(0xFFE50914).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 12.dp, start = 14.dp, end = 14.dp)
        ) {
            // ── Top Header: SPORT | LEAGUE ────────────────────────
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

            // ── Main Row: Team A ── VS / Center Status ── Team B ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Team A (Left)
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    TeamFlag(flagUrl = info?.teamAFlag)
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

                // Center Column: Status / Timer / Countdown
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
                                        .background(Color(0xFFE50914).copy(alpha = liveAlpha), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "((•)) Live",
                                    color = Color(0xFFE50914),
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
                                color = Color(0xFF38BDF8),
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

                // Team B (Right)
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
                    TeamFlag(flagUrl = info?.teamBFlag)
                }
            }

            // Bottom HOT badge if applicable
            if (isHot) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFF10B981), Color(0xFF059669))
                            ),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HOT 🔥",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
private fun TeamFlag(flagUrl: String?) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color(0xFF222636))
            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (!flagUrl.isNullOrBlank()) {
            AsyncImage(
                model = flagUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.SportsSoccer,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
