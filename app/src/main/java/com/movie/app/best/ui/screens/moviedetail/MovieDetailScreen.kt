package com.movie.app.best.ui.screens.moviedetail

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.movie.app.best.data.model.WasmerMovieDetails
import com.movie.app.best.data.settings.ModerationSettings
import com.movie.app.best.ui.components.CelebrationOverlay
import com.movie.app.best.ui.components.ErrorView
import com.movie.app.best.ui.components.SkeletonDetailPage
import com.movie.app.best.ui.components.StorylineWarningBadge
import com.movie.app.best.ui.screens.moviedetail.components.*
import com.movie.app.best.data.model.DownloadPhase

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailScreen(
    slug: String,
    onBackClick: () -> Unit,
    onPlayClick: (playerUrl: String, streamUrl: String, title: String, youtubeId: String, movieId: String, slug: String) -> Unit,
    onTrailerClick: (youtubeId: String, title: String, imdbId: String) -> Unit = { _, _, _ -> },
    onWatchClick: (imdbId: String, title: String, movieId: String, slug: String, hasStream: Boolean, playerUrl: String, posterUrl: String, cast: String, director: String, description: String) -> Unit = { _, _, _, _, _, _, _, _, _, _ -> },
    onSeriesClick: (slug: String) -> Unit,
    onMovieClick: (slug: String) -> Unit = {},
    onDownloadClick: (linkUrl: String) -> Unit = { },
    onGoToDownloads: () -> Unit = {},
    onOpenExtractedSeries: (String, String, String) -> Unit = { _, _, _ -> },
    viewModel: MovieDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var hasStoragePermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q)
                context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED
            else true
        )
    }
    var hasNotifPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= 33)
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED
            else true
        )
    }
    var showDownloadSheet by remember { mutableStateOf(false) }
    var pendingDownload by remember { mutableStateOf<Pair<String, Int?>?>(null) }

    LaunchedEffect(uiState.downloadPhase) {
        if (uiState.downloadPhase != DownloadPhase.NONE && !showDownloadSheet && uiState.movie != null) {
            showDownloadSheet = true
        }
    }

    val storageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasStoragePermission = granted
        if (granted) pendingDownload?.let { viewModel.resolveDownloadLink(it.first, it.second); pendingDownload = null }
    }
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted
        pendingDownload?.let { viewModel.resolveDownloadLink(it.first, it.second); pendingDownload = null }
    }

    fun requestDownload(linkUrl: String, linkId: Int? = null) {
        when {
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q && !hasStoragePermission -> {
                pendingDownload = Pair(linkUrl, linkId)
                storageLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            Build.VERSION.SDK_INT >= 33 && !hasNotifPermission -> {
                pendingDownload = Pair(linkUrl, linkId)
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            else -> viewModel.resolveDownloadLink(linkUrl, linkId)
        }
    }

    // ── Render ─────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        when {
            uiState.isLoading && uiState.movie == null -> SkeletonDetailPage()
            uiState.error != null && uiState.movie == null -> {
                ErrorView(
                    error   = uiState.error ?: "Failed to load details",
                    onRetry = viewModel::loadMovieDetails
                )
            }
            uiState.movie != null -> {
                MovieDetailContent(
                    movie              = uiState.movie!!,
                    uiState            = uiState,
                    onBackClick        = onBackClick,
                    onPlayClick        = onPlayClick,
                    onTrailerClick     = onTrailerClick,
                    onWatchClick       = onWatchClick,
                    onDownloadClick    = { showDownloadSheet = true },
                    onPostComment      = viewModel::postComment,
                    onRequestStream    = viewModel::requestStream,
                    onResetCommentState = viewModel::resetCommentState,
                    onSeriesClick      = onSeriesClick,
                    onToggleBookmark   = viewModel::toggleBookmark,
                    onToggleLike       = viewModel::toggleLike,
                    onReportClick      = viewModel::openReportDrawer,
                    onContentClick     = { slug, isSeries ->
                        if (isSeries) onSeriesClick(slug)
                        else onMovieClick(slug)
                    },
                    onGoToDownloads    = onGoToDownloads,
                    onOpenExtractedSeries = onOpenExtractedSeries
                )
            }
        }

        CelebrationOverlay(
            play = uiState.showCelebration,
            onFinished = viewModel::dismissCelebration
        )

        if (showDownloadSheet && uiState.movie != null) {
            androidx.compose.material3.ModalBottomSheet(
                onDismissRequest = { showDownloadSheet = false },
                containerColor = Color(0xFF1A1A1A),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                DownloadBottomSheetContent(
                    downloadLinks = uiState.downloadLinks,
                    downloadLoadingLinkId = uiState.downloadLoadingLinkId,
                    downloadPhase = uiState.downloadPhase,
                    downloadProgress = uiState.downloadProgress,
                    downloadError = uiState.downloadError,
                    downloadFailureReason = uiState.downloadFailureReason,
                    resolvedMirrors = uiState.resolvedMirrors,
                    expandedLinkId = uiState.expandedLinkId,
                    onStartDownload = { linkUrl, linkId -> requestDownload(linkUrl, linkId) },
                    onPickMirror = { mirror ->
                        viewModel.startDirectDownload(mirror)
                    },
                    onToggleExpand = { linkId -> viewModel.toggleExpandLink(linkId) },
                    onDismiss = { showDownloadSheet = false },
                    onGoToDownloads = {
                        showDownloadSheet = false
                        onGoToDownloads()
                    },
                    isZip = uiState.downloadIsZip,
                    extractionProgress = uiState.downloadExtractionProgress
                )
            }
        }

        if (uiState.showReportDrawer) {
            androidx.compose.material3.ModalBottomSheet(
                onDismissRequest = viewModel::closeReportDrawer,
                containerColor = Color(0xFF1A1A1A)
            ) {
                ReportDrawer(
                    movieId = uiState.movie?.id ?: 0,
                    currentModeration = uiState.movie?.contentModeration,
                    isModerator = uiState.isModerator,
                    onSubmit = { movieId, reportType, reason ->
                        viewModel.submitContentModeration(movieId, reportType, reason)
                    },
                    onModeratorVerdict = { movieId, poster, screenshots, storyline, reasoning ->
                        viewModel.submitModeratorVerdict(movieId, poster, screenshots, storyline, reasoning)
                    },
                    onDismiss = viewModel::closeReportDrawer
                )
            }
        }

        if (uiState.showReportWaiting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                ReportWaitingPopup(isObjection = uiState.isObjectionReport)
            }
        }

        if (uiState.reportModerationResult != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                ReportResultModal(
                    moderation = uiState.reportModerationResult!!,
                    previousModeration = uiState.previousModerationResult,
                    isObjection = uiState.isObjectionReport,
                    onDismiss = viewModel::dismissReportResult
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
//  Content (main layout)
// ─────────────────────────────────────────────────────────
@Composable
private fun MovieDetailContent(
    movie: WasmerMovieDetails,
    uiState: MovieDetailUiState,
    onBackClick: () -> Unit,
    onPlayClick: (String, String, String, String, String, String) -> Unit,
    onTrailerClick: (String, String, String) -> Unit = { _, _, _ -> },
    onWatchClick: (String, String, String, String, Boolean, String, String, String, String, String) -> Unit,
    onDownloadClick: () -> Unit,
    onPostComment: (String, String) -> Unit,
    onRequestStream: () -> Unit,
    onResetCommentState: () -> Unit,
    onSeriesClick: (String) -> Unit,
    onToggleBookmark: () -> Unit,
    onToggleLike: () -> Unit,
    onReportClick: () -> Unit = {},
    onContentClick: (String, Boolean) -> Unit = { _, _ -> },
    onGoToDownloads: () -> Unit = {},
    onOpenExtractedSeries: (String, String, String) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    val isContentHidden = ModerationSettings.shouldHideDetail(context, movie.contentModeration)

    if (isContentHidden) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            DetailHeroSection(
                movie       = movie,
                onBackClick = onBackClick,
                onReportClick = onReportClick
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(32.dp))
                Text(
                    text = "⚠️",
                    fontSize = 40.sp,
                    color = Color.White
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Content is hidden due to the app's moderation filter",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(40.dp))
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // 1. Full-bleed hero
        DetailHeroSection(
            movie       = movie,
            onBackClick = onBackClick,
            onReportClick = onReportClick
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
        ) {
            // 2. Play + Download + My List buttons
            DetailActionButtons(
                hasStream       = movie.hasStream || movie.playerUrl.isNotEmpty(),
                streamRequested = uiState.streamRequested,
                isInMyList      = uiState.isBookmarked,
                isLiked         = uiState.isLiked,
                hasDownloadLinks = uiState.downloadLinks.isNotEmpty(),
                alwaysShowWatch = true,
                onPlayClick     = {
                    onWatchClick(movie.imdbId, movie.title, movie.id.toString(), movie.slug, movie.hasStream, movie.playerUrl, movie.posterUrl, movie.cast, movie.director, movie.description)
                },
                onDownloadClick = onDownloadClick,
                onMyListClick   = onToggleBookmark,
                onLikeClick     = onToggleLike,
                onRequestStream = onRequestStream
            )

            // 3. Meta info chips
            MetaChipsRow(movie = movie)

            Divider(color = Color.White.copy(alpha = 0.07f), modifier = Modifier.padding(horizontal = 18.dp))

            // 4. Description
            if (movie.description.isNotEmpty()) {
                ExpandableDescription(text = movie.description)
                if (ModerationSettings.shouldBlurStoryline(context, movie.contentModeration)) {
                    StorylineWarningBadge(isSexual = true, modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp))
                }
            }

            Divider(color = Color.White.copy(alpha = 0.07f), modifier = Modifier.padding(horizontal = 18.dp))

            // 5. Cast & Crew (text-only, no cast images)
            CastSection(director = movie.director, cast = movie.cast)

            Divider(color = Color.White.copy(alpha = 0.07f), modifier = Modifier.padding(horizontal = 18.dp))

            // 6. Episodes row (series only)
            EpisodesRow(
                isSeries       = movie.isSeries,
                onViewEpisodes = { onSeriesClick(movie.slug) }
            )

            // 7. Trailers row
            TrailersRow(
                youtubeId      = movie.youtubeId,
                onTrailerClick = {
                    onTrailerClick(movie.youtubeId, movie.title, movie.imdbId)
                }
            )

            Divider(color = Color.White.copy(alpha = 0.07f), modifier = Modifier.padding(horizontal = 18.dp))

            // 8. Screenshots
            if (uiState.screenshots.isNotEmpty()) {
                Divider(color = Color.White.copy(alpha = 0.07f), modifier = Modifier.padding(horizontal = 18.dp))
                ScreenshotsSection(
                    screenshots = uiState.screenshots,
                    shouldBlur = ModerationSettings.shouldBlurScreenshots(context, movie.contentModeration)
                )
            }

            // 9. More Like This (after screenshots)
            if (movie.imdbId.startsWith("tt")) {
                Divider(color = Color.White.copy(alpha = 0.07f), modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp))
                if (uiState.isSimilarLoading) {
                    FindingSimilarSection()
                } else if (uiState.similarMovies.isNotEmpty()) {
                    MoreLikeThisSection(
                        movies       = uiState.similarMovies,
                        onMovieClick = onContentClick
                    )
                }
            }

            // 10. Comments
            Divider(color = Color.White.copy(alpha = 0.07f), modifier = Modifier.padding(horizontal = 18.dp))
            CommentsSection(
                comments          = uiState.comments,
                isPosting         = uiState.isCommentPosting,
                posted            = uiState.commentPosted,
                error             = uiState.commentError,
                onPost            = onPostComment,
                onReset           = onResetCommentState
            )

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun Divider(color: Color, modifier: Modifier = Modifier) {
    HorizontalDivider(color = color, modifier = modifier)
}
