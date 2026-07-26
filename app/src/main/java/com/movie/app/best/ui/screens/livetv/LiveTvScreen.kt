package com.movie.app.best.ui.screens.livetv

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.movie.app.best.data.model.UnifiedChannel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTvScreen(
    onBackClick: () -> Unit,
    onChannelClick: (UnifiedChannel) -> Unit,
    onSearchClick: () -> Unit,
    viewModel: LiveTvViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val gridState = rememberLazyGridState()
    val channels = uiState.channels

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = gridState.layoutInfo.totalItemsCount
            lastVisible >= totalItems - 8 && uiState.canLoadMore && !uiState.isLoadingMore
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMore()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050505))
    ) {
        TopAppBar(
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFFFF0000), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "LIVE TV",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            },
            actions = {
                IconButton(onClick = onSearchClick) {
                    Icon(Icons.Default.Search, contentDescription = "Search channels", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF050505)
            )
        )

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when {
                uiState.isLoading && channels.isEmpty() -> {
                    CircularProgressIndicator(color = Color(0xFFFF0000))
                }
                uiState.error != null && channels.isEmpty() -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uiState.error ?: "Unknown error",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.loadInitial() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0000))
                        ) {
                            Text("Retry", color = Color.White)
                        }
                    }
                }
                channels.isEmpty() -> {
                    Text(
                        text = "No live channels available right now",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        state = gridState,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Filter chips row — spans all 4 columns
                        if (uiState.categories.isNotEmpty()) {
                            item(span = { GridItemSpan(4) }) {
                                LazyRow(
                                    contentPadding = PaddingValues(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    item {
                                        FilterChip(
                                            selected = uiState.selectedCategory == null,
                                            onClick = { viewModel.filterByCategory(null) },
                                            label = { Text("All", fontSize = 12.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFFFF0000),
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                    items(uiState.categories, key = { "cat_${it.name}" }) { cat ->
                                        FilterChip(
                                            selected = uiState.selectedCategory == cat.name,
                                            onClick = { viewModel.filterByCategory(cat.name) },
                                            label = { Text("${cat.name} (${cat.count})", fontSize = 12.sp, maxLines = 1) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFFFF0000),
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        items(channels, key = { it.uniqueKey }) { ch ->
                            CircleCell(channel = ch, onClick = { onChannelClick(ch) })
                        }

                        if (uiState.isLoadingMore || uiState.isFiltering) {
                            item(span = { GridItemSpan(4) }) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = Color(0xFFFF0000),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }

                        item(span = { GridItemSpan(4) }) {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CircleCell(channel: UnifiedChannel, onClick: () -> Unit) {
    val redGradient = Brush.linearGradient(
        colors = listOf(Color(0xFFFF0000), Color(0xFFFF4081)),
        start = Offset(0f, 0f),
        end = Offset(1f, 1f)
    )

    // ── Sizes match home carousel exactly (80dp column width, 72dp circle) ──
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .drawWithCache {
                    onDrawWithContent {
                        drawCircle(
                            brush = redGradient,
                            radius = size.minDimension / 2,
                            center = center,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f * density)
                        )
                        drawContent()
                    }
                }
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = channel.logoUrl,
                contentDescription = channel.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Box(
            modifier = Modifier
                .offset(y = (-6).dp)
                .background(
                    color = Color(0xFFFF0000),
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
            Text(
                text = "LIVE",
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.5f),
                        offset = Offset(0f, 1f),
                        blurRadius = 2f
                    )
                )
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = channel.name,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
