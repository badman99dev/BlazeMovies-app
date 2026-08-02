package com.movie.app.best.ui.screens.zee5

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.movie.app.best.data.model.Zee5DetailResponse
import com.movie.app.best.data.model.Zee5Item
import com.movie.app.best.data.model.Zee5Season
import com.movie.app.best.ui.components.GlassBadge
import com.movie.app.best.ui.theme.AppBlack
import com.movie.app.best.ui.theme.CardDark
import com.movie.app.best.ui.theme.AppRed
import com.movie.app.best.ui.theme.SecondaryText

@Composable
fun Zee5DetailScreen(
    contentId: String,
    navController: NavController,
    viewModel: Zee5ViewModel = hiltViewModel()
) {
    val detailState by viewModel.detailState.collectAsState()
    val episodesState by viewModel.episodesState.collectAsState()

    val detail = (detailState as? Zee5DetailState.Success)?.detail

    var selectedSeasonId by remember { mutableStateOf<String?>(null) }
    val seasonsList = remember { mutableStateOf<List<Zee5Season>>(emptyList()) }
    val listState = rememberLazyListState()
    var selectedTab by remember { mutableStateOf(DetailTab.EPISODES) }

    val shouldLoadMoreEpisodes = remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItem >= totalItems - 3
        }
    }

    LaunchedEffect(contentId) {
        viewModel.loadDetails(contentId)
    }

    LaunchedEffect(detail) {
        detail?.let {
            if (it.isTvShow) {
                val seasonId = it.firstSeasonId
                if (seasonId != null) {
                    selectedSeasonId = seasonId
                }
                viewModel.loadEpisodes(it.id ?: "")
                try {
                    val seasonData = viewModel.apiService.getSeasons(it.id ?: "")
                    seasonsList.value = seasonData.seasons ?: emptyList()
                    if (selectedSeasonId == null && seasonData.seasons?.isNotEmpty() == true) {
                        selectedSeasonId = seasonData.seasons.last()?.id
                    }
                } catch(_: Exception) {}
            }
        }
    }

    LaunchedEffect(shouldLoadMoreEpisodes.value) {
        if (shouldLoadMoreEpisodes.value) {
            val state = episodesState
            if (state is Zee5EpisodesState.Success && state.hasMore) {
                viewModel.loadMoreEpisodes()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBlack)
    ) {
        when (detailState) {
            is Zee5DetailState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppRed)
                }
            }
            is Zee5DetailState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = (detailState as Zee5DetailState.Error).message,
                            color = SecondaryText
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.loadDetails(contentId) },
                            colors = ButtonDefaults.buttonColors(containerColor = AppRed)
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
            is Zee5DetailState.Success -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Zee5DetailHero(
                            detail = detail!!,
                            onBackClick = { navController.popBackStack() }
                        )
                    }

                    item {
                        Zee5DetailActions(
                            detail = detail!!,
                            onPlayClick = {
                                navController.navigate("zee5_watch/${contentId}")
                            }
                        )
                    }

                    item {
                        Zee5DetailDescription(detail = detail!!)
                    }

                    if (detail?.isTvShow == true) {
                        item {
                            DetailTabRow(
                                selectedTab = selectedTab,
                                onTabSelected = { selectedTab = it }
                            )
                        }

                        if (selectedTab == DetailTab.EPISODES) {
                            if (seasonsList.value.isNotEmpty()) {
                                item {
                                    Zee5SeasonSelector(
                                        seasons = seasonsList.value,
                                        selectedSeasonId = selectedSeasonId,
                                        onSeasonSelect = { seasonId ->
                                            selectedSeasonId = seasonId
                                            viewModel.loadEpisodesForSeason(seasonId)
                                        }
                                    )
                                }
                            }

                            when (episodesState) {
                                is Zee5EpisodesState.Loading -> {
                                    item {
                                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator(color = AppRed, modifier = Modifier.size(24.dp))
                                        }
                                    }
                                }
                                is Zee5EpisodesState.Error -> {
                                    item {
                                        Text(text = (episodesState as Zee5EpisodesState.Error).message, color = SecondaryText, fontSize = 12.sp, modifier = Modifier.padding(16.dp))
                                    }
                                }
                                is Zee5EpisodesState.Success -> {
                                    val episodes = (episodesState as Zee5EpisodesState.Success).episodes
                                    val hasMore = (episodesState as Zee5EpisodesState.Success).hasMore
                                    items(episodes, key = { it.id ?: it.hashCode() }) { episode ->
                                        Zee5EpisodeRow(
                                            episode = episode,
                                            onClick = {
                                                episode.id?.let { epId ->
                                                    val epNum = episode.episodeNumber ?: -1
                                                    navController.navigate("zee5_watch/${contentId}?epId=${epId}&epNum=${epNum}")
                                                }
                                            }
                                        )
                                    }
                                    if (hasMore) {
                                        item {
                                            Box(
                                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(color = AppRed, modifier = Modifier.size(24.dp))
                                            }
                                        }
                                    }
                                    if (!hasMore && episodes.isNotEmpty()) {
                                        item {
                                            Box(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
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

                    if (detail?.related?.isNotEmpty() == true && (selectedTab == DetailTab.MORE_LIKE_THIS || detail?.isTvShow != true)) {
                        item {
                            Zee5RelatedSection(
                                related = detail.related!!,
                                onItemClick = { item ->
                                    item.id?.let { id ->
                                        navController.navigate("zee5_detail/${id}")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

enum class DetailTab { EPISODES, MORE_LIKE_THIS }

@Composable
private fun DetailTabRow(
    selectedTab: DetailTab,
    onTabSelected: (DetailTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        DetailTabItem(
            text = "Episodes",
            selected = selectedTab == DetailTab.EPISODES,
            onClick = { onTabSelected(DetailTab.EPISODES) }
        )
        DetailTabItem(
            text = "More Like This",
            selected = selectedTab == DetailTab.MORE_LIKE_THIS,
            onClick = { onTabSelected(DetailTab.MORE_LIKE_THIS) }
        )
    }
}

@Composable
private fun DetailTabItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column {
        Text(
            text = text,
            color = if (selected) AppRed else SecondaryText,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.clickable(onClick = onClick)
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .width(24.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(50))
                    .background(AppRed)
            )
        }
    }
}

@Composable
fun Zee5DetailHero(
    detail: Zee5DetailResponse,
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(detail.effectiveLandscapeUrl ?: detail.effectiveImageUrl)
                .crossfade(true)
                .build(),
            contentDescription = detail.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            AppBlack.copy(alpha = 0.4f),
                            AppBlack.copy(alpha = 0.65f),
                            AppBlack.copy(alpha = 0.95f)
                        )
                    )
                )
        )

        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .padding(16.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f))
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text(
                text = detail.title ?: "",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                detail.releaseDate?.let {
                    Text(
                        text = it.take(4),
                        color = SecondaryText,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                detail.ageRating?.let {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .border(0.5.dp, SecondaryText, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = it,
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                if (detail.assetSubtype?.contains("HD", ignoreCase = true) == true || detail.assetSubtype?.contains("HDR", ignoreCase = true) == true) {
                    GlassBadge(text = "HD", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun Zee5DetailActions(
    detail: Zee5DetailResponse,
    onPlayClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Button(
            onClick = onPlayClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = AppRed),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Watch Now",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

    }
}

@Composable
fun Zee5DetailDescription(detail: Zee5DetailResponse) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        detail.description?.let { desc ->
            Text(
                text = desc,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 19.sp
            )

            if (desc.length > 120) {
                TextButton(
                    onClick = { expanded = !expanded },
                    colors = ButtonDefaults.textButtonColors(contentColor = AppRed),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = if (expanded) "Show Less" else "Show More",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        detail.actors?.let { actors ->
            if (actors.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Cast: ${actors.joinToString(", ")}",
                    color = SecondaryText,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        detail.directors?.let { directors ->
            if (directors.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Director: ${directors.joinToString(", ")}",
                    color = SecondaryText,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun Zee5SeasonSelector(
    seasons: List<Zee5Season>,
    selectedSeasonId: String?,
    onSeasonSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedSeason = selectedSeasonId ?: seasons.lastOrNull()?.id ?: ""
    val selectedTitle = seasons.find { it.id == selectedSeason }?.title ?: "Season 1"

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(CardDark)
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = selectedTitle,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Select season",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(CardDark)
        ) {
            seasons.forEach { season ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = season.title ?: "Season ${season.seasonNumber}",
                            color = if (season.id == selectedSeason) AppRed else Color.White
                        )
                    },
                    onClick = {
                        season.id?.let { onSeasonSelect(it) }
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun Zee5EpisodeRow(
    episode: Zee5Item,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        label = "ep_press"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(140.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(10.dp))
                .background(CardDark)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(episode.effectiveLandscapeUrl ?: episode.effectiveImageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = episode.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AppRed.copy(alpha = 0.9f))
                        .clickable { onClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            episode.duration?.let { duration ->
                if (duration > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${duration / 60} min",
                            color = Color.White,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            episode.episodeNumber?.let { epNum ->
                Text(
                    text = "$epNum. ${episode.title ?: "Untitled"}",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            } ?: Text(
                text = episode.title ?: "Untitled",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            episode.description?.let { desc ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = desc,
                    color = SecondaryText,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp
                )
            }
        }

        Icon(
            imageVector = Icons.Filled.Download,
            contentDescription = "Download",
            tint = SecondaryText,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun Zee5RelatedSection(
    related: List<Zee5Item>,
    onItemClick: (Zee5Item) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = "More Like This",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(related) { item ->
                Zee5Card(item = item, onClick = { onItemClick(item) })
            }
        }
    }
}
