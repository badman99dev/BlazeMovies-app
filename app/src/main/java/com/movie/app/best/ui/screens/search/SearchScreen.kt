package com.movie.app.best.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.movie.app.best.data.model.MeiliHit
import com.movie.app.best.data.model.UnifiedChannel
import com.movie.app.best.data.model.Zee5Bucket
import com.movie.app.best.data.model.Zee5Item
import com.movie.app.best.data.settings.ModerationSettings
import com.movie.app.best.ui.components.BlurredContent
import com.movie.app.best.ui.components.SkeletonPosterCard
import com.movie.app.best.ui.screens.home.components.LiveChannelsCarousel
import com.movie.app.best.ui.screens.home.components.QualityBadge
import com.movie.app.best.ui.screens.home.components.StreamBadge
import com.movie.app.best.ui.screens.home.components.SeriesBadge

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun SearchScreen(
    onContentClick: (String, Boolean, String) -> Unit,
    onZee5Click: (String) -> Unit,
    onTvChannelClick: (UnifiedChannel) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val gridState = rememberLazyGridState()

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val shouldPaginate by remember {
        derivedStateOf {
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= uiState.ownResults.size - 6
        }
    }

    if (shouldPaginate && !uiState.isLoadingMore && uiState.canLoadMore && !uiState.isShowingSuggestions) {
        viewModel.loadNextPage()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        placeholder = { Text("Search movies & series...", color = Color.Gray) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                keyboardController?.hide()
                                viewModel.onSearchSubmit()
                            }
                        ),
                        leadingIcon = {
                            Icon(Icons.Default.Search, null, tint = Color.Gray)
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, null, tint = Color.Gray)
                                }
                            }
                        },
                        colors = TextFieldDefaults.textFieldColors(
                            containerColor = Color.DarkGray.copy(alpha = 0.6f),
                            cursorColor = Color.Red,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        containerColor = Color.Black
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
        ) {
            if (uiState.searchQuery.isEmpty()) {
                EmptySearchState()
            } else if (uiState.isShowingSuggestions) {
                SuggestionsView(
                    state = uiState,
                    onMeiliClick = { hit -> onContentClick(hit.slug, hit.isSeriesBool, hit.imdbId) },
                    onZee5Click = { text -> viewModel.searchUniversal(text) }
                )
            } else {
                SearchResultsView(
                    state = uiState,
                    onContentClick = onContentClick,
                    onZee5Click = onZee5Click,
                    onTvChannelClick = onTvChannelClick,
                    gridState = gridState
                )
            }
        }
    }
}

@Composable
fun EmptySearchState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Find your next favorite", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Search for movies & series by title", style = MaterialTheme.typography.bodyLarge, color = Color.Gray, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))
        Icon(Icons.Default.Search, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
    }
}

@Composable
fun SuggestionsView(
    state: SearchUiState,
    onMeiliClick: (MeiliHit) -> Unit,
    onZee5Click: (String) -> Unit
) {
    val hasAnySuggestions = state.meiliSuggestions.isNotEmpty() ||
            state.zee5Suggestions.isNotEmpty()

    if (!hasAnySuggestions && !state.isSuggestionLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Type to search...", color = Color.Gray, fontSize = 14.sp)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        if (state.isSuggestionLoading && !hasAnySuggestions) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Red, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Getting suggestions...", color = Color.Gray, fontSize = 14.sp)
                }
            }
            return@LazyColumn
        }

        if (state.meiliSuggestions.isNotEmpty()) {
            item { SuggestionHeader("Movies & Series") }
            items(state.meiliSuggestions, key = { "meili_${it.id}" }) { hit ->
                MeiliSuggestionRow(hit = hit, onClick = { onMeiliClick(hit) })
            }
        }

        if (state.zee5Suggestions.isNotEmpty()) {
            item { SuggestionHeader("Suggestions from Zee5") }
            items(state.zee5Suggestions.take(5), key = { "zee5_$it" }) { text ->
                Zee5SuggestionRow(text = text, onClick = { onZee5Click(text) })
            }
        }
    }
}

@Composable
fun SuggestionHeader(text: String) {
    Text(
        text = text,
        color = Color(0xFF888888),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun MeiliSuggestionRow(hit: MeiliHit, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hit.posterUrl.isNotEmpty()) {
            AsyncImage(
                model = hit.posterUrl,
                contentDescription = hit.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(36.dp)
                    .height(54.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(54.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF1A1A1A))
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = highlightedTitle(hit.highlightedTitle),
                color = Color.White,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (hit.releaseYear > 0) {
                    Text(text = hit.releaseYearStr, color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                }
                if (hit.qualityLabel.isNotBlank()) {
                    QualityBadge(label = hit.qualityLabel)
                    Spacer(modifier = Modifier.width(6.dp))
                }
                if (hit.isSeriesBool) {
                    SeriesBadge()
                }
                if (hit.hasStream) {
                    Spacer(modifier = Modifier.width(4.dp))
                    StreamBadge()
                }
            }
        }
    }
}

@Composable
fun Zee5SuggestionRow(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            color = Color.White,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun SearchResultsView(
    state: SearchUiState,
    onContentClick: (String, Boolean, String) -> Unit,
    onZee5Click: (String) -> Unit,
    onTvChannelClick: (UnifiedChannel) -> Unit,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState
) {
    val ownLoading = state.isOwnLoading && state.ownResults.isEmpty()

    if (ownLoading && state.zee5Results.isEmpty() && !state.isZee5Loading && state.tvChannels.isEmpty()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(9) { SkeletonPosterCard(modifier = Modifier.fillMaxWidth()) }
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        state = gridState,
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // ── LIVE TV channels row (searched from /v1/tv-stream endpoint) ──
        // Shown ONLY if at least one channel matched; silently hidden if 0 results.
        if (state.tvChannels.isNotEmpty()) {
            item(span = { GridItemSpan(3) }) {
                LiveChannelsCarousel(
                    channels = state.tvChannels,
                    onChannelClick = onTvChannelClick,
                    applyTopPadding = false
                )
            }
        }

        if (state.zee5Results.isNotEmpty()) {
            item(span = { GridItemSpan(3) }) {
                Column {
                    Text(
                        text = "Results from Zee5",
                        color = Color(0xFF888888),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                    )
                    state.zee5Results.forEach { bucket ->
                        Zee5RailSection(
                            title = bucket.title ?: "",
                            items = bucket.items ?: emptyList(),
                            onZee5Click = onZee5Click
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Text(
                        text = "Movies & Series",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        if (state.isZee5Loading && state.zee5Results.isEmpty()) {
            item(span = { GridItemSpan(3) }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Red, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Searching Zee5...", color = Color.Gray, fontSize = 12.sp)
                }
            }
        }

        if (ownLoading) {
            items(6) { SkeletonPosterCard(modifier = Modifier.fillMaxWidth()) }
        } else if (state.ownResults.isEmpty() && !state.isOwnLoading) {
            item(span = { GridItemSpan(3) }) {
                if (state.zee5Results.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No results found", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Try different keywords or check for typos", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                }
            }
        } else {
            items(state.ownResults, key = { "own_${it.id}" }) { hit ->
                MeiliResultCard(hit = hit, onClick = { onContentClick(hit.slug, hit.isSeriesBool, hit.imdbId) })
            }
        }

        if (state.isLoadingMore) {
            item(span = { GridItemSpan(3) }) {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Red, strokeWidth = 2.dp)
                }
            }
        }
    }
}

@Composable
fun MeiliResultCard(hit: MeiliHit, onClick: () -> Unit) {
    val movie = remember(hit) { hit.toMovie() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(4.dp))
    ) {
        BlurredContent(
            shouldBlur = ModerationSettings.effectiveShouldBlur(movie),
            modifier = Modifier.fillMaxSize()
        ) {
            AsyncImage(
                model = hit.posterUrl,
                contentDescription = hit.title,
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
                if (hit.qualityLabel.isNotBlank()) {
                    QualityBadge(label = hit.qualityLabel)
                }
                if (hit.hasStream) {
                    StreamBadge()
                }
            }
            if (hit.isSeriesBool) {
                SeriesBadge()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
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
                text = hit.title,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = hit.releaseYearStr, color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp)
                if (hit.rating > 0) {
                    Text(text = " • ", color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp)
                    Text(text = "⭐ ${hit.ratingStr}", color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
fun Zee5RailSection(
    title: String,
    items: List<Zee5Item>,
    onZee5Click: (String) -> Unit
) {
    if (items.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth()) {
        if (title.isNotBlank()) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items, key = { "zee5_${it.id}" }) { item ->
                Zee5ResultCard(item = item, onClick = {
                    item.id?.let { onZee5Click(it) }
                })
            }
        }
    }
}

@Composable
fun Zee5ResultCard(item: Zee5Item, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .width(120.dp)
                .aspectRatio(2 / 3f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1A1A1A))
        ) {
            AsyncImage(
                model = item.effectiveImageUrl ?: item.effectiveLandscapeUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                            startY = 80f
                        )
                    )
            )
            Text(
                text = item.title ?: "",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
            )
        }
    }
}

fun highlightedTitle(formatted: String): AnnotatedString {
    if (!formatted.contains("\u0001")) return AnnotatedString(formatted)
    return buildAnnotatedString {
        val parts = formatted.split("\u0001", "\u0002")
        var inHighlight = false
        for (part in parts) {
            if (inHighlight) {
                withStyle(SpanStyle(color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)) {
                    append(part)
                }
            } else {
                append(part)
            }
            inHighlight = !inHighlight
        }
    }
}
