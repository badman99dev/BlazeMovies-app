package com.movie.app.best.ui.screens.player

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.movie.app.best.data.model.ImdbCredit
import com.movie.app.best.data.model.ImdbTitleDetails
import com.movie.app.best.ui.theme.WasmerBlack
import com.movie.app.best.util.FullscreenPlayerState
import com.movie.app.best.util.ImmersiveMode

@Composable
fun YoutubeTrailerScreen(
    youtubeId: String,
    title: String,
    imdbId: String = "",
    onBackClick: () -> Unit,
    trailerViewModel: TrailerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    var isFullscreen by remember { mutableStateOf(false) }

    LaunchedEffect(isFullscreen) {
        FullscreenPlayerState.isActive = isFullscreen
        activity?.let {
            if (isFullscreen) ImmersiveMode.enter(it) else ImmersiveMode.exit(it)
        }
    }

    LaunchedEffect(imdbId) {
        if (imdbId.isNotBlank()) trailerViewModel.loadData(imdbId)
    }

    DisposableEffect(Unit) {
        onDispose { FullscreenPlayerState.isActive = false }
    }

    val exitFullscreen = {
        isFullscreen = false
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        activity?.let { ImmersiveMode.exit(it) }
    }

    val enterFullscreen = {
        isFullscreen = true
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        activity?.let { ImmersiveMode.enter(it) }
    }

    BackHandler {
        if (isFullscreen) {
            exitFullscreen()
        } else {
            activity?.let { ImmersiveMode.exit(it) }
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            onBackClick()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            activity?.let { ImmersiveMode.exit(it) }
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    val trailerState by trailerViewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WasmerBlack)
    ) {
        if (!isFullscreen) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        activity?.let { ImmersiveMode.exit(it) }
                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        onBackClick()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Box(
            modifier = if (isFullscreen)
                Modifier.fillMaxSize()
            else
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
        ) {
            YoutubeWebView(
                youtubeId = youtubeId,
                modifier = Modifier.fillMaxSize()
            )
            IconButton(
                onClick = { if (isFullscreen) exitFullscreen() else enterFullscreen() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                    contentDescription = if (isFullscreen) "Exit Fullscreen" else "Fullscreen",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (!isFullscreen) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(WasmerBlack),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp)
            ) {
                when (val s = trailerState) {
                    is TrailerUiState.Loading -> {
                        item { TrailerLoadingShimmer() }
                    }
                    is TrailerUiState.Success -> {
                        item { TrailerInfoHeader(s.title, s.certificate?.rating ?: "") }
                        item { Spacer(modifier = Modifier.height(12.dp)) }
                        item { RatingRow(s.title) }
                        item { Spacer(modifier = Modifier.height(12.dp)) }
                        if (s.title.genres.isNotEmpty()) {
                            item { GenreChips(s.title.genres) }
                            item { Spacer(modifier = Modifier.height(12.dp)) }
                        }
                        if (s.title.plot.isNotBlank()) {
                            item { SynopsisSection(s.title.plot) }
                            item { Spacer(modifier = Modifier.height(12.dp)) }
                        }
                        if (s.cast.isNotEmpty()) {
                            item { CastRow(s.cast) }
                            item { Spacer(modifier = Modifier.height(12.dp)) }
                        }
                        item { CrewSection(s.title) }
                        item { Spacer(modifier = Modifier.height(12.dp)) }
                        item { MoreDetailsSection(s.title) }
                    }
                    is TrailerUiState.Error -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Details unavailable",
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                    is TrailerUiState.Idle -> {}
                }
            }
        }
    }
}

@Composable
private fun TrailerInfoHeader(title: ImdbTitleDetails, certificate: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        Text(
            text = title.primaryTitle,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 28.sp
        )
        if (title.originalTitle != null && title.originalTitle != title.primaryTitle) {
            Text(
                text = title.originalTitle,
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (title.startYear > 0) {
                InfoBadge(text = title.startYear.toString())
            }
            if (title.runtimeMinutes > 0) {
                val hrs = title.runtimeMinutes / 60
                val mins = title.runtimeMinutes % 60
                val runtime = if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"
                InfoBadge(text = runtime)
            }
            if (certificate.isNotBlank()) {
                InfoBadge(text = certificate, color = Color(0xFFE50914))
            }
            if (title.type.isNotBlank() && title.type != "movie") {
                InfoBadge(text = title.type.replaceFirstChar { it.uppercase() })
            }
        }
    }
}

@Composable
private fun InfoBadge(text: String, color: Color = Color.White.copy(alpha = 0.7f)) {
    Text(
        text = text,
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
private fun RatingRow(title: ImdbTitleDetails) {
    if (title.ratingValue <= 0 && title.metacriticScore <= 0) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (title.ratingValue > 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1A1A1A))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = Color(0xFFF5C518),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = String.format("%.1f", title.ratingValue),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "(${formatVoteCount(title.voteCount)})",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }
        }
        if (title.metacriticScore > 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1A1A1A))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when {
                                title.metacriticScore >= 75 -> Color(0xFF4CAF50)
                                title.metacriticScore >= 50 -> Color(0xFFFFA726)
                                else -> Color(0xFFEF4444)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title.metacriticScore.toString(),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Metacritic",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun formatVoteCount(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}

@Composable
private fun GenreChips(genres: List<String>) {
    LazyRow(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(genres) { genre ->
            Text(
                text = genre,
                color = Color(0xFFE50914),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFE50914).copy(alpha = 0.12f))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun SynopsisSection(plot: String) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
    ) {
        Text(
            text = "Synopsis",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = plot,
            color = Color.White.copy(alpha = 0.65f),
            fontSize = 14.sp,
            lineHeight = 21.sp,
            maxLines = if (expanded) Int.MAX_VALUE else 4,
            overflow = if (expanded) TextOverflow.Visible else TextOverflow.Ellipsis
        )
        if (plot.length > 200) {
            Text(
                text = if (expanded) "Show less" else "Read more",
                color = Color(0xFFE50914),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clickable { expanded = !expanded }
            )
        }
    }
}

@Composable
private fun CastRow(cast: List<ImdbCredit>) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Cast",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 18.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(cast) { credit ->
                CastCard(credit)
            }
        }
    }
}

@Composable
private fun CastCard(credit: ImdbCredit) {
    Column(
        modifier = Modifier.width(80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0xFF1A1A1A)),
            contentAlignment = Alignment.Center
        ) {
            if (credit.photoUrl.isNotBlank()) {
                AsyncImage(
                    model = credit.photoUrl,
                    contentDescription = credit.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = credit.displayName,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        if (credit.characterText.isNotBlank()) {
            Text(
                text = credit.characterText,
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun CrewSection(title: ImdbTitleDetails) {
    if (title.directors.isEmpty() && title.writers.isEmpty()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
    ) {
        if (title.directors.isNotEmpty()) {
            CrewItem(label = "Director", value = title.directorsText)
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (title.writers.isNotEmpty()) {
            CrewItem(label = "Writer", value = title.writersText)
        }
    }
}

@Composable
private fun CrewItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(70.dp)
        )
        Text(
            text = value,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
private fun MoreDetailsSection(title: ImdbTitleDetails) {
    val details = mutableListOf<Pair<String, String>>()
    if (title.countriesText.isNotBlank()) details.add("Country" to title.countriesText)
    if (title.languagesText.isNotBlank()) details.add("Language" to title.languagesText)
    if (title.runtimeMinutes > 0) {
        val hrs = title.runtimeMinutes / 60
        val mins = title.runtimeMinutes % 60
        details.add("Runtime" to (if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"))
    }
    if (title.type.isNotBlank()) details.add("Type" to title.type.replaceFirstChar { it.uppercase() })
    if (details.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
    ) {
        Text(
            text = "Details",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))
        details.forEach { (label, value) ->
            CrewItem(label = label, value = value)
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun TrailerLoadingShimmer() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White.copy(alpha = 0.08f))
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .height(24.dp)
                        .width(60.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.05f))
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        Spacer(modifier = Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            repeat(4) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.06f))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .width(50.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                    )
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun YoutubeWebView(
    youtubeId: String,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.domStorageEnabled = true
                settings.userAgentString = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
                val html = """<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width,initial-scale=1">
<meta name="referrer" content="strict-origin-when-cross-origin">
<style>
*{margin:0;padding:0;box-sizing:border-box}
html,body{width:100%;height:100%;background:#000;overflow:hidden}
iframe{width:100%;height:100%;border:0}
</style>
</head>
<body>
<iframe src="https://www.youtube.com/embed/$youtubeId?autoplay=1&playsinline=1&rel=0"
frameborder="0" allow="autoplay; encrypted-media; fullscreen" allowfullscreen
referrerpolicy="strict-origin-when-cross-origin"></iframe>
</body>
</html>""".trimIndent()
                loadDataWithBaseURL("https://wasmer-hub.vercel.app/", html, "text/html", "UTF-8", null)
            }
        }
    )
}
