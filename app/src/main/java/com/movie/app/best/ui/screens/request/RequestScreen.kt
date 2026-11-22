package com.movie.app.best.ui.screens.request

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.movie.app.best.data.model.ImdbEpisode
import com.movie.app.best.data.model.ImdbSearchTitle
import com.movie.app.best.ui.components.LoginRequiredDialog
import com.movie.app.best.ui.screens.auth.AuthViewModel
import com.movie.app.best.ui.theme.AppRed

private val Bg = Color(0xFF0B0B0B)
private val CardBg = Color(0xFF161616)
private val Stroke = Color(0xFF2A2A2A)
private val Muted = Color(0xFF9E9E9E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestScreen(
    onBackClick: () -> Unit = {},
    onLoginClick: () -> Unit = {},
    viewModel: RequestViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsState()
    val keyboard = LocalSoftwareKeyboardController.current
    var showLoginDialog by remember { mutableStateOf(false) }

    if (showLoginDialog) {
        LoginRequiredDialog(
            onLoginClick = {
                showLoginDialog = false
                onLoginClick()
            },
            onDismiss = { showLoginDialog = false }
        )
    }

    val onSubmit: () -> Unit = {
        keyboard?.hide()
        if (authState.isLoggedIn) {
            viewModel.submit()
        } else {
            showLoginDialog = true
        }
    }

    Scaffold(
        containerColor = Bg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Surface(color = Bg, modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(52.dp).padding(start = 6.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "New Request",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (state.submitted) {
                item { SuccessCard(onAnother = { viewModel.resetAfterSubmit() }) }
            } else {
                item { TypeSwitch(selected = state.type, onSelect = viewModel::setType) }

                item {
                    SearchCard(
                        query = state.query,
                        isSearching = state.isSearching,
                        onQueryChange = viewModel::setQuery,
                        onSearch = {
                            keyboard?.hide()
                            viewModel.search()
                        }
                    )
                }

                if (state.results.isNotEmpty() && !state.manualMode) {
                    item {
                        Text(
                            "Select the exact title",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    items(state.results, key = { "${it.id}_${it.type}" }) { item ->
                        ResultRow(item = item, onClick = { viewModel.selectTitle(item) })
                    }
                    item {
                        NoneOfAboveButton(onClick = viewModel::useManualMode)
                    }
                } else if (state.searchError != null && !state.manualMode) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(state.searchError ?: "", color = Color(0xFFEF5350), fontSize = 13.sp)
                            NoneOfAboveButton(onClick = viewModel::useManualMode)
                        }
                    }
                }

                if (state.manualMode) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1C1C1C))
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = AppRed, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Manual entry — fill the details yourself",
                                color = Color(0xFFCFCFCF),
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "Change",
                                color = AppRed,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { viewModel.backToSearch() }
                            )
                        }
                    }
                }

                if (state.selected != null && !state.manualMode) {
                    item {
                        state.selected?.let { picked ->
                            SelectedCard(item = picked, onClear = viewModel::backToSearch)
                        }
                    }
                }

                if (state.selected != null || state.manualMode) {
                    item {
                        FormCard(
                            state = state,
                            onTitle = viewModel::setTitle,
                            onYear = viewModel::setYear,
                            onImdbId = viewModel::setImdbId,
                            onSeason = viewModel::setSeason,
                            onEpisode = viewModel::setEpisode,
                            onNote = viewModel::setNote
                        )
                    }

                    if (state.type == RequestType.TV && state.seasons.isNotEmpty()) {
                        item {
                            EpisodePicker(
                                state = state,
                                onSeason = viewModel::selectSeason,
                                onEpisode = viewModel::selectEpisode
                            )
                        }
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onSubmit,
                                enabled = state.canSubmit,
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AppRed,
                                    contentColor = Color.White,
                                    disabledContainerColor = Color(0xFF3A1416),
                                    disabledContentColor = Color(0xFF8A6A6A)
                                )
                            ) {
                                if (state.isSubmitting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Submit Request", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }

                            if (!authState.isLoggedIn) {
                                Text(
                                    "Login required to submit a request",
                                    color = Muted,
                                    fontSize = 12.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            state.error?.let {
                                Text(it, color = Color(0xFFEF5350), fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TypeSwitch(selected: RequestType, onSelect: (RequestType) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "What are you looking for?",
            color = Muted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CardBg)
                .padding(4.dp)
        ) {
            listOf(RequestType.MOVIE to "Movie", RequestType.TV to "TV Show").forEach { (type, label) ->
                val active = selected == type
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (active) AppRed else Color.Transparent)
                        .clickable { onSelect(type) }
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (type == RequestType.MOVIE) Icons.Default.Movie else Icons.Default.Tv,
                        contentDescription = null,
                        tint = if (active) Color.White else Muted,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        label,
                        color = if (active) Color.White else Muted,
                        fontSize = 14.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchCard(
    query: String,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            placeholder = { Text("Movie / series name", color = Color(0xFF777777), fontSize = 14.sp) },
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CardBg,
                unfocusedContainerColor = CardBg,
                focusedBorderColor = AppRed,
                unfocusedBorderColor = Stroke,
                cursorColor = AppRed,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )
        Button(
            onClick = onSearch,
            enabled = !isSearching,
            modifier = Modifier.height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppRed, contentColor = Color.White)
        ) {
            if (isSearching) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun ResultRow(item: ImdbSearchTitle, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 62.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF222222))
        ) {
            if (item.posterUrl.isNotBlank()) {
                AsyncImage(
                    model = item.posterUrl,
                    contentDescription = item.displayTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.displayTitle,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(5.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (item.displayYear.isNotEmpty()) {
                    Tag(item.displayYear)
                }
                if (item.type.isNotEmpty()) {
                    Tag(item.type.uppercase())
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Text("Select", color = AppRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Tag(text: String) {
    Text(
        text,
        color = Muted,
        fontSize = 10.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF262626))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
private fun NoneOfAboveButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(46.dp),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Stroke),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
    ) {
        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text("None of the above — fill manually", fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SelectedCard(item: ImdbSearchTitle, onClear: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1A1214))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 46.dp, height = 64.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF222222))
        ) {
            if (item.posterUrl.isNotBlank()) {
                AsyncImage(
                    model = item.posterUrl,
                    contentDescription = item.displayTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.displayTitle,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                listOf(item.displayYear, item.id).filter { it.isNotBlank() }.joinToString(" · "),
                color = Muted,
                fontSize = 11.sp
            )
        }
        Text(
            "Change",
            color = AppRed,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClear)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormCard(
    state: RequestUiState,
    onTitle: (String) -> Unit,
    onYear: (String) -> Unit,
    onImdbId: (String) -> Unit,
    onSeason: (String) -> Unit,
    onEpisode: (String) -> Unit,
    onNote: (String) -> Unit
) {
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = CardBg,
        unfocusedContainerColor = CardBg,
        focusedBorderColor = AppRed,
        unfocusedBorderColor = Stroke,
        cursorColor = AppRed,
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedLabelColor = AppRed,
        unfocusedLabelColor = Muted
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            if (state.type == RequestType.TV) "TV Show details" else "Movie details",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = state.title,
            onValueChange = onTitle,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(if (state.type == RequestType.TV) "Series name" else "Movie name") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = state.year,
                onValueChange = onYear,
                modifier = Modifier.weight(0.4f),
                label = { Text("Release year") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors
            )
            OutlinedTextField(
                value = state.imdbId,
                onValueChange = onImdbId,
                modifier = Modifier.weight(0.6f),
                label = { Text("IMDb id (optional)") },
                placeholder = { Text("tt1234567", color = Color(0xFF666666), fontSize = 13.sp) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors
            )
        }

        if (state.type == RequestType.TV) {
            Text(
                "Leave episode empty for a full season, or both empty for the whole series.",
                color = Muted,
                fontSize = 11.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = state.season,
                    onValueChange = onSeason,
                    modifier = Modifier.weight(1f),
                    label = { Text("Season") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors
                )
                OutlinedTextField(
                    value = state.episode,
                    onValueChange = onEpisode,
                    modifier = Modifier.weight(1f),
                    label = { Text("Episode (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors
                )
            }
        }

        OutlinedTextField(
            value = state.note,
            onValueChange = onNote,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Note (optional)") },
            placeholder = {
                Text(
                    "e.g. 4K + Hindi dubbed please",
                    color = Color(0xFF666666),
                    fontSize = 13.sp
                )
            },
            minLines = 2,
            maxLines = 4,
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EpisodePicker(
    state: RequestUiState,
    onSeason: (Int) -> Unit,
    onEpisode: (ImdbEpisode) -> Unit
) {
    val selectedSeason = state.season.toIntOrNull()
    val visible = state.seasonEpisodes.filter {
        selectedSeason == null || it.season.toIntOrNull() == selectedSeason
    }.take(200)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "Pick season / episode",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        if (state.loadingEpisodes) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = AppRed,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(10.dp))
                Text("Loading episodes…", color = Muted, fontSize = 12.sp)
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.seasons) { season ->
                FilterChip(
                    selected = selectedSeason == season,
                    onClick = { onSeason(season) },
                    label = { Text("S$season", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AppRed,
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFF242424),
                        labelColor = Muted
                    )
                )
            }
        }

        visible.forEach { ep ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF202020))
                    .clickable { onEpisode(ep) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "S${ep.season} · E${ep.episodeNumber}",
                    color = AppRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    ep.title.ifBlank { "Episode ${ep.episodeNumber}" },
                    color = Color(0xFFDDDDDD),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SuccessCard(onAnother: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardBg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(52.dp)
        )
        Text(
            "Request submitted",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Thanks! Our team will check availability and add it as soon as possible.",
            color = Muted,
            fontSize = 13.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = onAnother,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppRed, contentColor = Color.White)
        ) {
            Text("Request another", fontWeight = FontWeight.Bold)
        }
    }
}