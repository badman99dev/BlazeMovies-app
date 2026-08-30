package com.movie.app.best.ui.screens.celebs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.movie.app.best.data.model.CelebIntro
import com.movie.app.best.data.settings.ModerationSettings
import com.movie.app.best.ui.components.CompactPageHeader
import com.movie.app.best.ui.screens.home.components.movieGridItems

@Composable
fun CelebsScreen(
    nameId: String,
    name: String = "",
    onBackClick: () -> Unit,
    onContentClick: (String, Boolean, String) -> Unit,
    onBioClick: (String, String) -> Unit = { _, _ -> },
    onSearchClick: () -> Unit = {},
    viewModel: CelebsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    LaunchedEffect(nameId) {
        viewModel.configure(nameId)
    }

    val filteredMovies = remember(uiState.movies, context) { ModerationSettings.filterMovies(context, uiState.movies) }

    val shouldLoadMore by remember(uiState) {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            lastVisible >= totalItems - 6 && uiState.hasMore && !uiState.isLoadingMore
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMore()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        CompactPageHeader(
            title = uiState.intro?.displayName?.takeIf { it.isNotBlank() } ?: name.ifBlank { "Celebrity" },
            onBackClick = onBackClick,
            onSearchClick = onSearchClick
        )

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when {
                uiState.isLoading && uiState.movies.isEmpty() -> {
                    CircularProgressIndicator(
                        color = Color.Red,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.error != null && uiState.movies.isEmpty() -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uiState.error ?: "Unknown error",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = viewModel::load,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Text("Retry", color = Color.White)
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (uiState.intro != null) {
                            item {
                                CelebIntroCard(
                                    intro = uiState.intro!!,
                                    nameId = nameId,
                                    onBioClick = onBioClick
                                )
                            }
                        }

                        movieGridItems(
                            movies = filteredMovies,
                            onMovieClick = onContentClick
                        )

                        item { Spacer(modifier = Modifier.height(80.dp)) }

                        if (uiState.isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = Color.Red,
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

@Composable
private fun CelebIntroCard(
    intro: CelebIntro,
    nameId: String,
    onBioClick: (String, String) -> Unit
) {
    val aspect = if (intro.photoWidth > 0 && intro.photoHeight > 0)
        intro.photoWidth.toFloat() / intro.photoHeight.toFloat() else 2f / 3f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF1D1D1D))
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .width(140.dp)
                .aspectRatio(aspect)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF2A2A2A))
        ) {
            if (intro.photoUrl.isNotBlank()) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(intro.photoUrl)
                        .crossfade(600)
                        .build(),
                    contentDescription = intro.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            if (intro.biography.isNotBlank()) {
                Text(
                    text = intro.biography,
                    color = Color(0xFFE8E8E8),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "more",
                    color = Color(0xFFC9A227),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        val n = intro.displayName.ifBlank { nameId }
                        onBioClick(nameId, n)
                    }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (intro.birthDate.isNotBlank()) {
                Row {
                    Text(
                        text = "Born",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formatBirthDate(intro.birthDate),
                        color = Color(0xFFE8E8E8),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

private fun formatBirthDate(dateStr: String): String {
    val parts = dateStr.split("-")
    if (parts.size != 3) return dateStr
    val months = listOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val y = parts[0].toIntOrNull() ?: 0
    val m = parts[1].toIntOrNull() ?: 0
    val d = parts[2].toIntOrNull() ?: 0
    if (y == 0 || m !in 1..12 || d == 0) return dateStr
    return "${months[m]} $d, $y"
}