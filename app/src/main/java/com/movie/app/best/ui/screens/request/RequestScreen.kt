package com.movie.app.best.ui.screens.request

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.movie.app.best.data.model.ImdbEpisode
import com.movie.app.best.data.model.ImdbSearchTitle
import com.movie.app.best.ui.components.LoginRequiredDialog
import com.movie.app.best.ui.screens.auth.AuthViewModel
import com.movie.app.best.ui.theme.AppDarkRed
import com.movie.app.best.ui.theme.AppRed
import kotlinx.coroutines.delay

private val Glass = Color(0x0FFFFFFF)
private val Glass2 = Color(0x14FFFFFF)
private val Hairline = Color(0x1AFFFFFF)
private val Muted = Color(0xFF8E8E93)
private val FieldBlack = Color(0xFF0A0A0A)

private val RedGradient get() = Brush.horizontalGradient(listOf(AppRed, AppDarkRed))

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

    val formVisible = state.selected != null || state.manualMode
    val bringIntoView = remember { BringIntoViewRequester() }

    LaunchedEffect(state.selected?.id, state.manualMode) {
        if (state.selected != null || state.manualMode) {
            delay(220)
            runCatching { bringIntoView.bringIntoView() }
        }
    }

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

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        RequestBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
        ) {
            RequestHeader(onBackClick = onBackClick)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (state.submitted) {
                    item { SuccessCard(onAnother = { viewModel.resetAfterSubmit() }) }
                } else {
                    item { TypeSwitch(selected = state.type, onSelect = viewModel::setType) }

                    if (!formVisible) {
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

                        if (state.results.isNotEmpty()) {
                            item {
                                SectionLabel(
                                    dot = false,
                                    text = "SELECT THE EXACT TITLE"
                                )
                            }
                            items(state.results, key = { "${it.id}_${it.type}" }) { item ->
                                ResultRow(item = item, onClick = { viewModel.selectTitle(item) })
                            }
                            item {
                                NoneOfAboveButton(onClick = viewModel::useManualMode)
                            }
                        } else if (state.searchError != null) {
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(state.searchError ?: "", color = Color(0xFFFF5A5F), fontSize = 13.sp)
                                    NoneOfAboveButton(onClick = viewModel::useManualMode)
                                }
                            }
                        }
                    }

                    if (state.manualMode) {
                        item {
                            ManualBanner(onChange = viewModel::backToSearch)
                        }
                    }

                    if (state.selected != null) {
                        item {
                            state.selected?.let { picked ->
                                SelectedCard(item = picked, onClear = viewModel::backToSearch)
                            }
                        }
                    }

                    item {
                        AnimatedVisibility(
                            visible = formVisible,
                            enter = fadeIn(tween(260)) + slideInVertically(tween(320, easing = FastOutSlowInEasing)) { it / 5 } + expandVertically(tween(320)),
                            exit = fadeOut(tween(180)) + slideOutVertically(tween(200)) { it / 5 }
                        ) {
                                Column(
                                    modifier = Modifier.bringIntoViewRequester(bringIntoView),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    FormCard(
                                        state = state,
                                        onTitle = viewModel::setTitle,
                                        onYear = viewModel::setYear,
                                        onImdbId = viewModel::setImdbId,
                                        onSeason = viewModel::setSeason,
                                        onEpisode = viewModel::setEpisode,
                                        onNote = viewModel::setNote
                                    )

                                    if (state.type == RequestType.TV && state.seasons.isNotEmpty()) {
                                        EpisodePicker(
                                            state = state,
                                            onSeason = viewModel::selectSeason,
                                            onEpisode = viewModel::selectEpisode
                                        )
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        SubmitButton(
                                            isSubmitting = state.isSubmitting,
                                            enabled = state.canSubmit,
                                            onClick = onSubmit
                                        )

                                        if (!authState.isLoggedIn) {
                                            Text(
                                                "Login required to submit a request",
                                                color = Muted,
                                                fontSize = 12.sp,
                                                modifier = Modifier.fillMaxWidth(),
                                                textAlign = TextAlign.Center
                                            )
                                        }

                                        state.error?.let {
                                            Text(it, color = Color(0xFFFF5A5F), fontSize = 13.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
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

/** Pure-black AMOLED page with a soft red bloom so it has depth. */
@Composable
private fun RequestBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawWithCache {
                // Bloom lives in the top ~360dp, but the canvas is full-screen so
                // the radial gradient can decay to transparent instead of being
                // clipped into a hard horizontal line at the box edge.
                val bloomBottom = 360.dp.toPx()
                val top = Brush.radialGradient(
                    colors = listOf(AppRed.copy(alpha = 0.26f), Color.Transparent),
                    center = Offset(size.width * 0.5f, 0f),
                    radius = size.width * 1.15f
                )
                val bottom = Brush.radialGradient(
                    colors = listOf(AppRed.copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(size.width * 0.85f, bloomBottom),
                    radius = size.width * 1.0f
                )
                onDrawWithContent {
                    drawContent()
                    drawRect(top)
                    drawRect(bottom)
                }
            }
    )
}

@Composable
private fun RequestHeader(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Glass)
                .border(1.dp, Hairline, CircleShape)
                .clickable(onClick = onBackClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Brush.verticalGradient(listOf(AppRed, AppDarkRed)))
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                "NEW REQUEST",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            Text(
                "Can't find it? Ask for it.",
                color = Muted,
                fontSize = 11.sp,
                letterSpacing = 0.3.sp
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String, dot: Boolean = true) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
        if (dot) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(AppRed)
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text,
            color = Color(0xFFB5B5BA),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp
        )
    }
}

@Composable
private fun TypeSwitch(selected: RequestType, onSelect: (RequestType) -> Unit) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Glass)
            .border(1.dp, Hairline, RoundedCornerShape(18.dp))
            .padding(5.dp)
    ) {
        val segWidth = maxWidth / 2
        val offset by animateDpAsState(
            targetValue = if (selected == RequestType.MOVIE) 0.dp else segWidth,
            animationSpec = tween(280, easing = FastOutSlowInEasing),
            label = "segOffset"
        )

        Box(
            modifier = Modifier
                .offset(x = offset)
                .width(segWidth)
                .height(48.dp)
                .shadow(16.dp, RoundedCornerShape(14.dp), ambientColor = AppRed, spotColor = AppRed)
                .clip(RoundedCornerShape(14.dp))
                .background(RedGradient)
        )

        Row(modifier = Modifier.height(48.dp)) {
            listOf(RequestType.MOVIE to "Movie", RequestType.TV to "TV Show")
                .forEach { (type, label) ->
                    val active = selected == type
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelect(type) },
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
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .shadow(if (focused) 14.dp else 0.dp, RoundedCornerShape(16.dp), ambientColor = AppRed, spotColor = AppRed),
            singleLine = true,
            interactionSource = interaction,
            placeholder = { Text("Movie / series name", color = Color(0xFF6E6E73), fontSize = 14.sp) },
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Black,
                unfocusedContainerColor = FieldBlack,
                focusedBorderColor = AppRed,
                unfocusedBorderColor = Hairline,
                cursorColor = AppRed,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Box(
            modifier = Modifier
                .size(56.dp)
                .shadow(16.dp, RoundedCornerShape(16.dp), ambientColor = AppRed, spotColor = AppRed)
                .clip(RoundedCornerShape(16.dp))
                .background(RedGradient)
                .clickable(enabled = !isSearching && query.isNotBlank()) { onSearch() },
            contentAlignment = Alignment.Center
        ) {
            if (isSearching) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun ResultRow(item: ImdbSearchTitle, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Glass2)
            .border(1.dp, Hairline, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 46.dp, height = 64.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1C1C1E))
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
            Spacer(Modifier.height(6.dp))
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
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(RedGradient)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text("SELECT", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 0.6.sp)
        }
    }
}

@Composable
private fun Tag(text: String) {
    Text(
        text,
        color = Color(0xFFBFBFC4),
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Glass2)
            .padding(horizontal = 7.dp, vertical = 2.dp)
    )
}

@Composable
private fun NoneOfAboveButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Hairline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White, containerColor = Glass)
    ) {
        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = AppRed)
        Spacer(Modifier.width(8.dp))
        Text("None of the above — fill manually", fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ManualBanner(onChange: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(listOf(AppRed.copy(alpha = 0.16f), Color.Transparent)))
            .border(1.dp, AppRed.copy(alpha = 0.30f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Edit, contentDescription = null, tint = AppRed, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            "Manual entry — fill the details yourself",
            color = Color(0xFFD6D6D6),
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(9.dp))
                .background(Glass2)
                .clickable(onClick = onChange)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text("CHANGE", color = AppRed, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        }
    }
}

@Composable
private fun SelectedCard(item: ImdbSearchTitle, onClear: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(AppRed.copy(alpha = 0.20f), Color.Transparent)))
            .border(1.dp, AppRed.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(RedGradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "SELECTED",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(width = 54.dp, height = 76.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1C1C1E))
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
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.displayTitle,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    listOf(item.displayYear, item.id).filter { it.isNotBlank() }.joinToString("  ·  "),
                    color = Muted,
                    fontSize = 11.sp
                )
            }
        }

        OutlinedButton(
            onClick = onClear,
            modifier = Modifier.fillMaxWidth().height(42.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Hairline),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White, containerColor = Glass)
        ) {
            Text("CHANGE TITLE", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
        }
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
        focusedContainerColor = Color.Black,
        unfocusedContainerColor = FieldBlack,
        focusedBorderColor = AppRed,
        unfocusedBorderColor = Hairline,
        cursorColor = AppRed,
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedLabelColor = AppRed,
        unfocusedLabelColor = Muted
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Glass)
            .border(1.dp, Hairline, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionLabel(if (state.type == RequestType.TV) "TV SHOW DETAILS" else "MOVIE DETAILS")

        OutlinedTextField(
            value = state.title,
            onValueChange = onTitle,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(if (state.type == RequestType.TV) "Series name" else "Movie name") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = fieldColors
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = state.year,
                onValueChange = onYear,
                modifier = Modifier.weight(0.4f),
                label = { Text("Year") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(14.dp),
                colors = fieldColors
            )
            OutlinedTextField(
                value = state.imdbId,
                onValueChange = onImdbId,
                modifier = Modifier.weight(0.6f),
                label = { Text("IMDb id") },
                placeholder = { Text("tt1234567", color = Color(0xFF5A5A5F), fontSize = 13.sp) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
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
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors
                )
                OutlinedTextField(
                    value = state.episode,
                    onValueChange = onEpisode,
                    modifier = Modifier.weight(1f),
                    label = { Text("Episode") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
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
                    color = Color(0xFF5A5A5F),
                    fontSize = 13.sp
                )
            },
            minLines = 2,
            maxLines = 4,
            shape = RoundedCornerShape(14.dp),
            colors = fieldColors
        )
    }
}

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
            .clip(RoundedCornerShape(18.dp))
            .background(Glass)
            .border(1.dp, Hairline, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionLabel("PICK SEASON / EPISODE")

        if (state.loadingEpisodes) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = AppRed,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(10.dp))
                Text("Loading episodes...", color = Muted, fontSize = 12.sp)
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.seasons) { season ->
                val active = selectedSeason == season
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .then(
                            if (active) Modifier.background(RedGradient)
                            else Modifier.background(Glass2).border(1.dp, Hairline, RoundedCornerShape(10.dp))
                        )
                        .clickable { onSeason(season) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        "S$season",
                        color = if (active) Color.White else Color(0xFFBFBFC4),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        visible.forEach { ep ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Glass2)
                    .border(1.dp, Hairline, RoundedCornerShape(12.dp))
                    .clickable { onEpisode(ep) }
                    .padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "S${ep.season} · E${ep.episodeNumber}",
                    color = AppRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.width(12.dp))
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
private fun SubmitButton(
    isSubmitting: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(
                elevation = if (enabled) 20.dp else 0.dp,
                shape = shape,
                ambientColor = AppRed,
                spotColor = AppRed
            )
            .clip(shape)
            .background(
                if (enabled) RedGradient
                else Brush.horizontalGradient(listOf(Color(0xFF2A1416), Color(0xFF1E0E10)))
            )
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isSubmitting) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "SUBMIT REQUEST",
                    color = if (enabled) Color.White else Color(0xFF7A5A5A),
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    letterSpacing = 1.6.sp
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
            .padding(top = 24.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.verticalGradient(listOf(AppRed.copy(alpha = 0.18f), Color.Transparent)))
            .border(1.dp, AppRed.copy(alpha = 0.30f), RoundedCornerShape(22.dp))
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .drawWithCache {
                        val glow = Brush.radialGradient(
                            colors = listOf(AppRed.copy(alpha = 0.55f), Color.Transparent),
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = size.width / 2f
                        )
                        onDrawWithContent {
                            drawContent()
                            drawRect(glow)
                        }
                    }
            )
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .shadow(24.dp, CircleShape, ambientColor = AppRed, spotColor = AppRed)
                    .clip(CircleShape)
                    .background(RedGradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Text(
            "REQUEST SENT",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.5.sp
        )
        Text(
            "Thanks! Our team will check availability and add it as soon as possible.",
            color = Muted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 19.sp
        )

        Spacer(Modifier.height(4.dp))

        OutlinedButton(
            onClick = onAnother,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(15.dp),
            border = BorderStroke(1.dp, Hairline),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White, containerColor = Glass2)
        ) {
            Text("Request another", fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 0.5.sp)
        }
    }
}
