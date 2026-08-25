package com.movie.app.best.ui.screens.tvshowdetail

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.movie.app.best.data.model.Comment
import com.movie.app.best.data.model.DownloadLink
import com.movie.app.best.data.model.Episode
import com.movie.app.best.data.model.Season
import com.movie.app.best.data.model.MovieDetails
import com.movie.app.best.data.repository.ResolvedMirror
import com.movie.app.best.data.settings.ModerationSettings
import com.movie.app.best.ui.components.BlurredContent
import com.movie.app.best.ui.components.CelebrationOverlay
import com.movie.app.best.ui.components.ScreenshotViewer
import com.movie.app.best.ui.components.SkeletonDetailPage
import com.movie.app.best.ui.components.ErrorView
import com.movie.app.best.ui.components.CollapsibleCrewRow
import com.movie.app.best.ui.components.InterestsGenreSection
import com.movie.app.best.ui.components.StorylineWarningBadge
import com.movie.app.best.ui.screens.moviedetail.components.DetailActionButtons
import com.movie.app.best.ui.screens.moviedetail.components.DetailHeroSection
import com.movie.app.best.ui.screens.moviedetail.components.MetaChipsRow
import com.movie.app.best.ui.screens.moviedetail.components.ExpandableDescription
import com.movie.app.best.ui.screens.moviedetail.components.CastSection
import com.movie.app.best.ui.screens.moviedetail.components.TrailersRow
import com.movie.app.best.ui.screens.moviedetail.components.DownloadBottomSheetContent
import com.movie.app.best.ui.util.CfBypassHost
import com.movie.app.best.ui.screens.moviedetail.components.DownloadStatusChip
import com.movie.app.best.ui.screens.moviedetail.components.ReportDrawer
import com.movie.app.best.ui.screens.moviedetail.components.StreamRequestWaitingPopup
import com.movie.app.best.ui.screens.moviedetail.components.StreamRequestResultModal
import com.movie.app.best.ui.screens.moviedetail.components.ReportWaitingPopup
import com.movie.app.best.ui.screens.moviedetail.components.ReportSuccessModal
import com.movie.app.best.ui.screens.moviedetail.components.ReportResultModal
import com.movie.app.best.ui.theme.SuccessGreen
import com.movie.app.best.ui.theme.AccentPurple
import com.movie.app.best.ui.theme.AppRed
import kotlinx.coroutines.delay

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun TVShowDetailScreen(
    slug: String,
    onBackClick: () -> Unit,
    onPlayClick: (playerUrl: String, streamUrl: String, title: String, youtubeId: String, movieId: String, slug: String) -> Unit,
    onTrailerClick: (youtubeId: String, title: String, imdbId: String) -> Unit = { _, _, _ -> },
    onWatchNow: (imdbId: String, title: String, movieId: String, slug: String, targetSeason: Int, cast: String, director: String, description: String, genres: String) -> Unit = { _, _, _, _, _, _, _, _, _ -> },
    onMovieClick: (String, String) -> Unit = { _, _ -> },
    onSeriesClick: (String, String) -> Unit = { _, _ -> },
    onGoToDownloads: () -> Unit = {},
    onOpenExtractedSeries: (String, String, String) -> Unit = { _, _, _ -> },
    onCelebClick: (String, String) -> Unit = { _, _ -> },
    onInterestClick: (String, String) -> Unit = { _, _ -> },
    viewModel: TVShowDetailViewModel = hiltViewModel()
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
    var sheetDownloadLinks by remember { mutableStateOf<List<DownloadLink>>(emptyList()) }
    var sheetTitle by remember { mutableStateOf("") }
    var pendingDownload by remember { mutableStateOf<Pair<String, Int?>?>(null) }

    LaunchedEffect(uiState.downloadPhase) {
        if (uiState.downloadPhase != com.movie.app.best.data.model.DownloadPhase.NONE && !showDownloadSheet && uiState.series != null) {
            if (sheetDownloadLinks.isEmpty()) {
                sheetDownloadLinks = uiState.downloadLinks
                sheetTitle = uiState.series?.title ?: ""
            }
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

    fun openEpisodeDownloadSheet(links: List<DownloadLink>, title: String) {
        sheetDownloadLinks = links
        sheetTitle = title
        showDownloadSheet = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        when {
            uiState.isLoading && uiState.series == null -> {
                SkeletonDetailPage()
            }
            uiState.error != null && uiState.series == null -> {
                ErrorView(
                    error = uiState.error ?: "Failed to load series details",
                    onRetry = viewModel::loadSeriesDetails
                )
            }
            uiState.series != null -> {
                TVShowDetailContent(
                    series = uiState.series!!,
                    uiState = uiState,
                    onBackClick = onBackClick,
                    onPlayClick = onPlayClick,
                    onTrailerClick = onTrailerClick,
                    onWatchNow = onWatchNow,
                    onPostComment = viewModel::postComment,
                    onRequestStream = viewModel::requestStream,
                    onStartDownload = { linkUrl, linkId -> requestDownload(linkUrl, linkId) },
                    onOpenEpisodeDownloadSheet = { links, title -> openEpisodeDownloadSheet(links, title) },
                    onResetCommentState = viewModel::resetCommentState,
                    onToggleBookmark = viewModel::toggleBookmark,
                    onToggleLike = viewModel::toggleLike,
                    onReportClick = viewModel::openReportDrawer,
                    onContentClick = { slug, isSeries, imdbId ->
                        if (isSeries) onSeriesClick(slug, imdbId)
                        else onMovieClick(slug, imdbId)
                    },
                    onGoToDownloads = onGoToDownloads,
                    onOpenExtractedSeries = onOpenExtractedSeries,
                    onCelebClick = onCelebClick,
                    onInterestClick = onInterestClick
                )
            }
        }

        CelebrationOverlay(
            play = uiState.showCelebration,
            onFinished = viewModel::dismissCelebration
        )

        if (uiState.showReportDrawer) {
            androidx.compose.material3.ModalBottomSheet(
                onDismissRequest = viewModel::closeReportDrawer,
                containerColor = Color(0xFF1A1A1A)
            ) {
                ReportDrawer(
                    movieId = uiState.series?.id ?: 0,
                    currentModeration = uiState.series?.contentModeration,
                    isModerator = uiState.isModerator,
                    onSubmit = { movieId, reportType, reason ->
                        viewModel.submitUserReport(movieId, reportType, reason)
                    },
                    onInstantCheck = { movieId, reportType, reason ->
                        viewModel.submitContentModeration(movieId, reportType, reason)
                    },
                    onModeratorVerdict = { movieId, poster, screenshots, storyline, reasoning ->
                        viewModel.submitModeratorVerdict(movieId, poster, screenshots, storyline, reasoning)
                    },
                    onDismiss = viewModel::closeReportDrawer,
                    isSubmitting = uiState.isSubmittingReport
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

        if (uiState.reportSuccessMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                ReportSuccessModal(
                    message = uiState.reportSuccessMessage!!,
                    onDismiss = viewModel::dismissReportSuccess
                )
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

        if (uiState.isStreamRequesting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                StreamRequestWaitingPopup()
            }
        }

        if (uiState.showStreamRequestResult && uiState.streamRequestResult != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                StreamRequestResultModal(
                    result = uiState.streamRequestResult!!,
                    isModerator = uiState.isModerator,
                    onDismiss = viewModel::dismissStreamRequestResult
                )
            }
        }

        if (showDownloadSheet && sheetDownloadLinks.isNotEmpty()) {
            androidx.compose.material3.ModalBottomSheet(
                onDismissRequest = { showDownloadSheet = false },
                containerColor = Color(0xFF1A1A1A),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                DownloadBottomSheetContent(
                    downloadLinks = sheetDownloadLinks,
                    downloadLoadingLinkId = uiState.downloadLoadingLinkId,
                    downloadLogs = uiState.downloadLogs,
                    expandedLogsLinkId = uiState.expandedLogsLinkId,
                    onToggleLogs = { linkId -> viewModel.toggleLogs(linkId) },
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
                    extractionProgress = uiState.downloadExtractionProgress,
                    bypassLogs = uiState.downloadBypassLogs
                )
            }
        }

        CfBypassHost(
            bypassUrl = uiState.downloadBypassUrl,
            onLog = { line -> viewModel.appendBypassLog(line) },
            onSolved = { result -> viewModel.onBypassSolved(result) },
            onFailed = { viewModel.onBypassFailed() }
        )
    }
}

@Composable
private fun TVShowDetailContent(
    series: MovieDetails,
    uiState: TVShowDetailUiState,
    onBackClick: () -> Unit,
    onPlayClick: (playerUrl: String, streamUrl: String, title: String, youtubeId: String, movieId: String, slug: String) -> Unit,
    onTrailerClick: (youtubeId: String, title: String, imdbId: String) -> Unit = { _, _, _ -> },
    onWatchNow: (imdbId: String, title: String, movieId: String, slug: String, targetSeason: Int, cast: String, director: String, description: String, genres: String) -> Unit,
    onPostComment: (name: String, msg: String) -> Unit,
    onRequestStream: () -> Unit,
    onStartDownload: (linkUrl: String, linkId: Int?) -> Unit,
    onOpenEpisodeDownloadSheet: (List<DownloadLink>, String) -> Unit,
    onResetCommentState: () -> Unit,
    onToggleBookmark: () -> Unit,
    onToggleLike: () -> Unit,
    onReportClick: () -> Unit = {},
    onContentClick: (String, Boolean, String) -> Unit = { _, _, _ -> },
    onGoToDownloads: () -> Unit = {},
    onOpenExtractedSeries: (String, String, String) -> Unit = { _, _, _ -> },
    onCelebClick: (String, String) -> Unit = { _, _ -> },
    onInterestClick: (String, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val isContentHidden = ModerationSettings.shouldHideDetail(context, series.contentModeration)
    val shouldBlurPoster = ModerationSettings.shouldBlurDetail(context, series.contentModeration)
    val seasonKeys = uiState.episodesBySeason.keys.sorted()
    val defaultSeason = series.seasonLabel.filter { it.isDigit() }.toIntOrNull()
    var selectedSeason by remember { mutableStateOf(defaultSeason?.takeIf { it in seasonKeys } ?: seasonKeys.firstOrNull() ?: 1) }

    val titleDetails = uiState.titleDetails
    val displayRating = titleDetails?.let {
        if (it.ratingValue > 0) String.format("%.1f", it.ratingValue) else null
    } ?: series.rating
    val displayVotes = titleDetails?.let {
        if (it.voteCount > 0) "  (${formatVoteCount(it.voteCount)})" else null
    }
    val crew = uiState.crewCredits
    val interests = titleDetails?.interests ?: emptyList()

    if (isContentHidden) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
        ) {
            com.movie.app.best.ui.screens.moviedetail.components.DetailHeroSection(
                movie = series,
                onBackClick = onBackClick,
                onReportClick = onReportClick,
                forceBlur = true,
                minimal = true
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
        DetailHeroSection(
            movie = series,
            onBackClick = onBackClick,
            onReportClick = onReportClick,
            overrideRating = displayRating,
            overrideVotes = displayVotes
        )

        DetailActionButtons(
            hasStream = series.hasStream,
            streamRequested = uiState.streamRequested,
            isInMyList = uiState.isBookmarked,
            isLiked = uiState.isLiked,
            isSeries = true,
            onPlayClick = {
                onWatchNow(series.imdbId, series.title, series.id.toString(), series.slug, selectedSeason, series.cast, series.director, series.description, uiState.fallbackGenres.joinToString(","))
            },
            onDownloadClick = {
                val allLinks = uiState.linksByEpisode.values.flatten() + uiState.downloadLinks
                if (allLinks.isNotEmpty()) {
                    onOpenEpisodeDownloadSheet(allLinks, series.title)
                }
            },
            onMyListClick = onToggleBookmark,
            onLikeClick = onToggleLike,
            onRequestStream = onRequestStream
        )

        MetaChipsRow(movie = series)

        if (series.description.isNotEmpty()) {
            ExpandableDescription(text = series.description)
            if (ModerationSettings.shouldBlurStoryline(context, series.contentModeration)) {
                StorylineWarningBadge(isSexual = true, modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp))
            }
        }

        if (crew.isNotEmpty()) {
            CollapsibleCrewRow(
                crew = crew,
                onNameClick = onCelebClick
            )
        } else {
            CastSection(director = series.director, cast = series.cast)
        }

        if (interests.isNotEmpty()) {
            InterestsGenreSection(
                interests = interests,
                onInterestClick = onInterestClick
            )
        }

        if (seasonKeys.isNotEmpty()) {
            SectionTitle("Episodes")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                seasonKeys.forEach { seasonNo ->
                    val isSelected = selectedSeason == seasonNo
                    Card(
                        modifier = Modifier.clickable { selectedSeason = seasonNo },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary
                            else Color.DarkGray.copy(alpha = 0.4f)
                        )
                    ) {
                        Text(
                            text = "Season $seasonNo",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            val episodes = uiState.episodesBySeason[selectedSeason] ?: emptyList()
            episodes.forEach { episode ->
                EpisodeItem(
                    episode = episode,
                    downloadLinks = uiState.linksByEpisode[episode.id] ?: emptyList(),
                    downloadLoadingLinkId = uiState.downloadLoadingLinkId,
                    downloadStartedLinkIds = uiState.downloadStartedLinkIds,
                    onPlayClick = {
                        onWatchNow(series.imdbId, series.title, series.id.toString(), series.slug, selectedSeason, series.cast, series.director, series.description, uiState.fallbackGenres.joinToString(","))
                    },
                    onDownloadClick = { links ->
                        onOpenEpisodeDownloadSheet(links, "${series.title} - S${selectedSeason}E${episode.episodeNo}")
                    }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = Color.DarkGray.copy(alpha = 0.5f)
                )
            }

            if (episodes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No episodes found for this season",
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }

        TVDownloadSection(
            uiState = uiState,
            onStartDownload = onStartDownload
        )

        TrailersRow(
            youtubeId = series.youtubeId,
            onTrailerClick = {
                onTrailerClick(series.youtubeId, series.title, series.imdbId)
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.screenshots.isNotEmpty()) {
            val shouldBlurScreenshots = ModerationSettings.shouldBlurScreenshots(context, series.contentModeration)
            var screenshotViewerOpen by remember { mutableStateOf(false) }
            var screenshotViewerIndex by remember { mutableIntStateOf(0) }

            if (screenshotViewerOpen) {
                ScreenshotViewer(
                    screenshots = uiState.screenshots,
                    initialIndex = screenshotViewerIndex,
                    shouldBlur = shouldBlurScreenshots,
                    onDismiss = { screenshotViewerOpen = false }
                )
            }

            SectionTitle("Screenshots")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(uiState.screenshots) { index, url ->
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .width(240.dp)
                            .clickable {
                                screenshotViewerIndex = index
                                screenshotViewerOpen = true
                            }
                    ) {
                        Box {
                            BlurredContent(
                                shouldBlur = shouldBlurScreenshots,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f)
                            ) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = "Screenshot",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(16f / 9f)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (uiState.comments.isNotEmpty()) {
            SectionTitle("Comments")
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                uiState.comments.forEach { comment ->
                    CommentItem(comment)
                    HorizontalDivider(
                        color = Color.DarkGray.copy(alpha = 0.3f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }

        if (uiState.moreSeasons.isNotEmpty()) {
            SectionTitle("More Seasons")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.moreSeasons, key = { it.id }) { season ->
                    MoreSeasonCard(season = season, onClick = { onContentClick(season.slug, true, "") })
                }
            }
        }

        if (series.imdbId.startsWith("tt")) {
            if (uiState.isSimilarLoading) {
                com.movie.app.best.ui.screens.moviedetail.components.FindingSimilarSection()
            } else if (uiState.similarMovies.isNotEmpty()) {
                com.movie.app.best.ui.screens.moviedetail.components.MoreLikeThisSection(
                    movies = uiState.similarMovies,
                    onMovieClick = onContentClick
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        TVCommentFormSection(
            isPosting = uiState.isCommentPosting,
            posted = uiState.commentPosted,
            error = uiState.commentError,
            onPost = onPostComment,
            onReset = onResetCommentState
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun EpisodeItem(
    episode: Episode,
    downloadLinks: List<DownloadLink>,
    downloadLoadingLinkId: Int?,
    downloadStartedLinkIds: Set<Int>,
    onPlayClick: () -> Unit,
    onDownloadClick: (List<DownloadLink>) -> Unit
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .width(160.dp)
                .aspectRatio(16f / 9f)
        ) {
            Card(shape = RoundedCornerShape(8.dp)) {
                AsyncImage(
                    model = episode.stillPath,
                    contentDescription = episode.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable(onClick = onPlayClick),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onPlayClick,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${episode.episodeNo}. ${episode.title}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (episode.airDate.isNotEmpty()) {
                Text(
                    text = episode.airDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (episode.overview.isNotEmpty()) {
                Text(
                    text = episode.overview,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (episode.voteAverage > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = Color(0xFFFFC107)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = String.format("%.1f", episode.voteAverage),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            if (downloadLinks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))

                val isAnyLoading = downloadLinks.any { downloadLoadingLinkId == it.id }
                val isAnyStarted = downloadLinks.any { it.id in downloadStartedLinkIds }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(AppRed.copy(alpha = 0.1f))
                        .border(1.dp, AppRed.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = !isAnyLoading,
                            onClick = { onDownloadClick(downloadLinks) }
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isAnyLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = AppRed,
                                strokeWidth = 2.dp
                            )
                        } else if (isAnyStarted) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = "Download",
                                tint = AppRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when {
                                isAnyLoading -> "Resolving..."
                                isAnyStarted -> "Started"
                                else -> "Download (${downloadLinks.size})"
                            },
                            color = if (isAnyLoading) Color.White.copy(0.5f) else if (isAnyStarted) SuccessGreen else AppRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentItem(comment: Comment) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(
            text = comment.userName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = comment.comment,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.8f)
        )
        Text(
            text = comment.createdAt,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun MoreSeasonCard(
    season: Season,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(130.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
            AsyncImage(
                model = season.posterUrl,
                contentDescription = season.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
            )
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = season.seasonLabel,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (season.qualityLabel.isNotEmpty()) {
                    val sLabel = season.qualityLabel
                    val sBgColor = when {
                        sLabel.contains("4K", ignoreCase = true) -> Color(0xFFFFD700).copy(alpha = 0.25f)
                        sLabel.contains("UHD", ignoreCase = true) -> Color(0xFF00E5FF).copy(alpha = 0.2f)
                        sLabel.contains("HD", ignoreCase = true) -> Color(0xFF4FC3F7).copy(alpha = 0.15f)
                        sLabel.contains("CAM", ignoreCase = true) -> Color(0xFFFF5252).copy(alpha = 0.2f)
                        else -> Color.White.copy(alpha = 0.15f)
                    }
                    val sTextColor = when {
                        sLabel.contains("4K", ignoreCase = true) -> Color(0xFFFFD700)
                        sLabel.contains("UHD", ignoreCase = true) -> Color(0xFF00E5FF)
                        sLabel.contains("HD", ignoreCase = true) -> Color(0xFF4FC3F7)
                        sLabel.contains("CAM", ignoreCase = true) -> Color(0xFFFF5252)
                        else -> Color.White
                    }
                    val sBorderColor = sTextColor.copy(alpha = 0.3f)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Brush.linearGradient(colors = listOf(sBgColor, Color.White.copy(alpha = 0.08f), sBgColor)))
                            .border(0.5.dp, Brush.linearGradient(colors = listOf(sBorderColor, Color.White.copy(alpha = 0.1f), sBorderColor)), RoundedCornerShape(50))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = sLabel,
                            color = sTextColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
                Text(
                    text = "${season.totalEpisodes} episodes",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun TVDownloadSection(
    uiState: TVShowDetailUiState,
    onStartDownload: (linkUrl: String, linkId: Int?) -> Unit
) {
    val context = LocalContext.current
    val links = uiState.downloadLinks

    if (links.isEmpty()) return

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionTitle("Full Season Pack")

        links.forEach { link ->
            val isLoading = uiState.downloadLoadingLinkId == link.id
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = !isLoading,
                        onClick = { onStartDownload(link.linkUrl, link.id) }
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppRed.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = AppRed,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            tint = AppRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = link.label.ifEmpty { "Download" },
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (link.fileSize.isNotEmpty()) {
                        Text(
                            text = link.fileSize,
                            color = Color.White.copy(0.5f),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = AppRed.copy(0.6f),
                        strokeWidth = 2.dp
                    )
                }
            }
        }

        if (uiState.downloadError != null) {
            Text(
                text = uiState.downloadError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TVCommentFormSection(
    isPosting: Boolean,
    posted: Boolean,
    error: String?,
    onPost: (name: String, msg: String) -> Unit,
    onReset: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionTitle("Post a Comment")

        if (posted) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20))
            ) {
                Text(
                    text = "Comment posted successfully!",
                    color = Color.White,
                    modifier = Modifier.padding(16.dp)
                )
            }
            LaunchedEffect(posted) {
                if (posted) {
                    delay(2000)
                    onReset()
                }
            }
        }

        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Your Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color.DarkGray.copy(alpha = 0.3f),
                focusedContainerColor = Color.DarkGray.copy(alpha = 0.3f)
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            label = { Text("Comment") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5,
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color.DarkGray.copy(alpha = 0.3f),
                focusedContainerColor = Color.DarkGray.copy(alpha = 0.3f)
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (name.isNotBlank() && message.isNotBlank()) {
                    onPost(name, message)
                }
            },
            enabled = !isPosting && name.isNotBlank() && message.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isPosting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
            } else {
                Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Post Comment")
            }
        }
    }
}

private fun formatVoteCount(count: Int): String = when {
    count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
    count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
    else -> count.toString()
}
