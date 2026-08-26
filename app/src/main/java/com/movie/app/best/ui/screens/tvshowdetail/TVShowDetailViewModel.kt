package com.movie.app.best.ui.screens.tvshowdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movie.app.best.data.model.BookmarkItem
import com.movie.app.best.data.model.FirebaseHistoryItem
import com.movie.app.best.data.model.LikeItem
import com.movie.app.best.data.model.Resource
import com.movie.app.best.data.model.Comment
import com.movie.app.best.data.model.DownloadLink
import com.movie.app.best.data.model.Episode
import com.movie.app.best.data.model.MovieDetails
import com.movie.app.best.data.model.ImdbTitleDetails
import com.movie.app.best.data.model.CrewPerson
import com.movie.app.best.data.remote.ImdbApiService
import com.movie.app.best.data.model.Season
import com.movie.app.best.data.debug.NetworkMonitor
import com.movie.app.best.data.repository.DownloadRepository
import com.movie.app.best.data.repository.FirebaseRepository
import com.movie.app.best.data.repository.MovieRepository
import com.movie.app.best.data.repository.MyListRefreshState
import com.movie.app.best.data.repository.ResolvedMirror
import com.movie.app.best.data.model.DownloadPhase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class TVShowDetailViewModel @Inject constructor(
    private val repository: MovieRepository,
    private val downloadRepository: DownloadRepository,
    private val firebaseRepository: FirebaseRepository,
    private val imdbApi: ImdbApiService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val slug: String = checkNotNull(savedStateHandle["slug"])
    private val passedImdbId: String = savedStateHandle["imdbId"] ?: ""

    private val _uiState = MutableStateFlow(TVShowDetailUiState())
    val uiState: StateFlow<TVShowDetailUiState> = _uiState.asStateFlow()

    init {
        MyListRefreshState.markStale()
        if (passedImdbId.startsWith("tt")) {
            loadSimilarMovies(passedImdbId)
            loadCrewAndTitle(passedImdbId)
        }
        loadSeriesDetails()
        viewModelScope.launch {
            try {
                val profile = firebaseRepository.getOrCreateUserProfile()
                val isMod = profile?.tier == "moderator"
                _uiState.update { it.copy(isModerator = isMod) }
            } catch (_: Exception) {}
        }
        viewModelScope.launch {
            NetworkMonitor.refreshCounter.collect { if (it > 0) loadSeriesDetails() }
        }
    }

    fun loadSeriesDetails() {
        viewModelScope.launch {
            repository.getContentDetails(slug).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is Resource.Success -> {
                        val data = result.data
                        if (data != null) {
                            val seriesImdbId = data.movie.imdbId ?: ""
                            val noImdbNeeded = !passedImdbId.startsWith("tt") && !seriesImdbId.startsWith("tt")
                            _uiState.update {
                                it.copy(
                                    series = data.movie,
                                    episodesBySeason = data.episodesBySeason.mapKeys { it.key.toIntOrNull() ?: 0 },
                                    linksByEpisode = data.linksByEpisode.mapKeys { it.key.toIntOrNull() ?: 0 },
                                    comments = data.comments,
                                    screenshots = data.screenshots,
                                    moreSeasons = data.moreSeasons,
                                    downloadLinks = data.linksNoEpisode,
                                    fallbackGenres = data.genres,
                                    contentReady = true,
                                    imdbReady = if (noImdbNeeded) true else it.imdbReady,
                                    isLoading = false,
                                    error = null
                                )
                            }
                            addToFirebaseHistory()
                            checkBookmarkAndLikeStatus()
                            if (!passedImdbId.startsWith("tt")) {
                                loadSimilarMovies(data.movie.imdbId)
                                loadCrewAndTitle(data.movie.imdbId)
                            }
                        } else {
                            _uiState.update { it.copy(contentReady = true, isLoading = false, error = "No data") }
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                contentReady = true,
                                isLoading = false,
                                error = result.error
                            )
                        }
                    }
                }
            }
        }
    }

    fun postComment(name: String, msg: String) {
        val seriesId = _uiState.value.series?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isCommentPosting = true) }
            val authHeader = try {
                val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val tokenResult = user?.getIdToken(false)?.await()
                "Bearer ${tokenResult?.token ?: ""}"
            } catch (_: Exception) { "" }
            repository.postComment(authHeader, seriesId, msg).collect { result ->
                when (result) {
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(isCommentPosting = false, commentPosted = true, commentError = null)
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(isCommentPosting = false, commentPosted = false, commentError = result.error)
                        }
                    }
                }
            }
        }
    }

    fun requestStream() {
        val slug = _uiState.value.series?.slug ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isStreamRequesting = true) }
            try {
                val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val tokenResult = user?.getIdToken(false)?.await()
                val authHeader = "Bearer ${tokenResult?.token ?: ""}"
                val response = repository.submitStreamRequest(authHeader, slug)
                _uiState.update {
                    it.copy(
                        isStreamRequesting = false,
                        streamRequested = !response.already_requested && !response.has_stream,
                        streamRequestResult = response,
                        showStreamRequestResult = true
                    )
                }
                if (response.already_requested) {
                    _uiState.update { it.copy(streamRequested = true) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isStreamRequesting = false,
                        streamRequestError = e.message
                    )
                }
            }
        }
    }

    fun dismissStreamRequestResult() {
        _uiState.update { it.copy(showStreamRequestResult = false) }
    }

    fun resolveDownloadLink(linkUrl: String, linkId: Int?, episodeId: Int? = null, episodeLabel: String? = null) {
        val series = _uiState.value.series
        val title = series?.title ?: "Series"
        val slug = series?.slug ?: ""
        val posterUrl = series?.posterUrl ?: ""
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    downloadLoadingLinkId = linkId,
                    downloadError = null,
                    downloadPhase = DownloadPhase.NONE,
                    downloadTitle = title,
                    downloadLogs = it.downloadLogs + (linkId to emptyList()),
                    expandedLogsLinkId = linkId
                )
            }
            downloadRepository.resolveDownloadUrls(linkUrl, onLog = { line ->
                _uiState.update { state ->
                    val prev = state.downloadLogs[linkId] ?: emptyList()
                    state.copy(downloadLogs = state.downloadLogs + (linkId to (prev + line).takeLast(200)))
                }
            }).collect { result ->
                when (result) {
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        val mirrors = result.data ?: emptyList()
                        if (mirrors.size == 1) {
                            val m = mirrors.first()
                            _uiState.update { it.copy(downloadPhase = DownloadPhase.INITIALIZING, downloadLoadingLinkId = null, downloadIsZip = m.isZip) }
                            val ketchId = downloadRepository.startDownloadWithMetadata(
                                m, slug, posterUrl, title, "series", episodeId, episodeLabel
                            )
                            val meta = downloadRepository.getMetadata(slug + (episodeLabel ?: ""))
                            _uiState.update {
                                it.copy(
                                    downloadKetchId = ketchId,
                                    downloadPhase = DownloadPhase.DOWNLOADING,
                                    downloadStarted = true,
                                    downloadError = null,
                                    downloadIsZip = m.isZip,
                                    downloadFilePath = meta?.filePath,
                                    downloadTitle = title,
                                    downloadStartedLinkIds = it.downloadStartedLinkIds + (linkId ?: 0)
                                )
                            }
                            observeDownloadStatus(ketchId, slug + (episodeLabel ?: ""))
                        } else {
                            _uiState.update {
                                it.copy(
                                    downloadLoadingLinkId = null,
                                    resolvedMirrors = it.resolvedMirrors + (linkId to mirrors),
                                    expandedLinkId = linkId,
                                    downloadError = null
                                )
                            }
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                downloadLoadingLinkId = null,
                                downloadError = result.error
                            )
                        }
                    }
                }
            }
        }
    }

    fun startDirectDownload(mirror: ResolvedMirror, episodeId: Int? = null, episodeLabel: String? = null) {
        val series = _uiState.value.series
        val title = series?.title ?: "Series"
        val slug = series?.slug ?: ""
        val posterUrl = series?.posterUrl ?: ""
        viewModelScope.launch {
            _uiState.update { it.copy(downloadPhase = DownloadPhase.INITIALIZING, expandedLinkId = null, downloadTitle = title, downloadIsZip = mirror.isZip) }
            val ketchId = downloadRepository.startDownloadWithMetadata(
                mirror, slug, posterUrl, title, "series", episodeId, episodeLabel
            )
            val meta = downloadRepository.getMetadata(slug + (episodeLabel ?: ""))
            _uiState.update {
                it.copy(
                    downloadKetchId = ketchId,
                    downloadPhase = DownloadPhase.DOWNLOADING,
                    downloadStarted = true,
                    downloadError = null,
                    downloadIsZip = mirror.isZip,
                    downloadFilePath = meta?.filePath,
                    downloadTitle = title
                )
            }
            observeDownloadStatus(ketchId, slug + (episodeLabel ?: ""))
        }
    }

    private fun observeDownloadStatus(ketchId: Int, metaKey: String) {
        viewModelScope.launch {
            downloadRepository.observeDownloadStatus(ketchId).collect { statusInfo ->
                if (_uiState.value.downloadPhase == DownloadPhase.COMPLETE) return@collect
                when (statusInfo.phase) {
                    DownloadPhase.COMPLETE -> {
                        val meta = downloadRepository.getMetadata(metaKey)
                        if (meta?.isZip == true) {
                            _uiState.update {
                                it.copy(
                                    downloadPhase = DownloadPhase.EXTRACTING,
                                    downloadProgress = 100,
                                    downloadStarted = true,
                                    downloadIsZip = true,
                                    downloadExtractionProgress = 0
                                )
                            }
                            try {
                                downloadRepository.postProcessDownload(ketchId, metaKey).collect { extractionProgress ->
                                    _uiState.update {
                                        it.copy(
                                            downloadPhase = DownloadPhase.EXTRACTING,
                                            downloadExtractionProgress = extractionProgress.percent
                                        )
                                    }
                                }
                            } catch (e: Exception) {
                                com.movie.app.best.data.debug.NetworkLogger.logAction("EXTRACT_ERR_TV", e.message ?: "unknown")
                            }
                            val updatedMeta = downloadRepository.getMetadata(metaKey)
                            _uiState.update {
                                it.copy(
                                    downloadPhase = DownloadPhase.COMPLETE,
                                    downloadExtractPath = updatedMeta?.extractPath,
                                    downloadIsZip = true,
                                    downloadFilePath = updatedMeta?.filePath,
                                    downloadExtractionProgress = 100
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    downloadPhase = DownloadPhase.COMPLETE,
                                    downloadProgress = 100,
                                    downloadStarted = true,
                                    downloadIsZip = false,
                                    downloadExtractPath = null,
                                    downloadFilePath = meta?.filePath
                                )
                            }
                        }
                    }
                    DownloadPhase.CANCELLED -> {
                        _uiState.update {
                            it.copy(downloadPhase = DownloadPhase.CANCELLED, downloadKetchId = null, downloadStarted = false)
                        }
                    }
                    DownloadPhase.FAILED -> {
                        val reason = statusInfo.failureReason
                        val meta = downloadRepository.getMetadata(metaKey)
                        val url = meta?.url
                        val alreadyBypassed = (meta?.bypassAttempts ?: 0) >= 1
                        if (downloadRepository.isCloudflareFailure(reason) && !alreadyBypassed && !url.isNullOrBlank()) {
                            meta?.let { downloadRepository.saveMetadataDirect(metaKey, it.copy(bypassAttempts = 1)) }
                            _uiState.update {
                                it.copy(
                                    downloadPhase = DownloadPhase.BYPASSING,
                                    downloadBypassUrl = url,
                                    downloadBypassLogs = emptyList(),
                                    downloadBypassMetaKey = metaKey,
                                    downloadKetchId = ketchId,
                                    downloadStarted = true,
                                    downloadFailureReason = reason
                                )
                            }
                            com.movie.app.best.data.debug.NetworkLogger.logAction("CF_BYPASS_TRIGGER_TV", "ketchId=$ketchId host=$url")
                        } else {
                            _uiState.update {
                                it.copy(downloadPhase = DownloadPhase.FAILED, downloadFailureReason = reason, downloadStarted = false)
                            }
                        }
                    }
                    DownloadPhase.DOWNLOADING -> {
                        val meta = downloadRepository.getMetadata(metaKey)
                        _uiState.update {
                            it.copy(
                                downloadPhase = DownloadPhase.DOWNLOADING,
                                downloadProgress = statusInfo.progress,
                                downloadStarted = true,
                                downloadIsZip = meta?.isZip == true
                            )
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    fun appendBypassLog(line: String) {
        _uiState.update { it.copy(downloadBypassLogs = (it.downloadBypassLogs + line).takeLast(200)) }
    }

    fun onBypassSolved(result: com.movie.app.best.util.cf.ModuleResult) {
        val ketchId = _uiState.value.downloadKetchId ?: return
        val bypassMetaKey = _uiState.value.downloadBypassMetaKey
        val bypassUrl = _uiState.value.downloadBypassUrl ?: ""
        viewModelScope.launch {
            if (bypassUrl.isNotBlank() && result.cookies != null) {
                downloadRepository.cacheCookieForUrl(bypassUrl, result.cookies)
            }
            val newId = downloadRepository.retryDownloadWithUrl(
                ketchId,
                result.directUrl,
                result.cookies,
                originUrl = bypassUrl.ifBlank { null },
                fileNameOverride = result.fileName
            )
            _uiState.update {
                it.copy(
                    downloadPhase = if (newId != null) DownloadPhase.DOWNLOADING else DownloadPhase.FAILED,
                    downloadBypassUrl = null,
                    downloadBypassLogs = emptyList(),
                    downloadBypassMetaKey = "",
                    downloadKetchId = newId,
                    downloadStarted = newId != null,
                    downloadError = if (newId == null) "Bypass retry failed" else null
                )
            }
            if (newId != null && bypassMetaKey.isNotBlank()) observeDownloadStatus(newId, bypassMetaKey)
        }
    }

    fun onBypassFailed() {
        _uiState.update {
            it.copy(
                downloadPhase = DownloadPhase.FAILED,
                downloadBypassUrl = null,
                downloadBypassLogs = emptyList(),
                downloadBypassMetaKey = "",
                downloadStarted = false,
                downloadFailureReason = "Cloudflare bypass failed — 403"
            )
        }
    }

    fun toggleExpandLink(linkId: Int?) {
        _uiState.update {
            it.copy(expandedLinkId = if (it.expandedLinkId == linkId) null else linkId)
        }
    }

    fun toggleLogs(linkId: Int?) {
        _uiState.update {
            it.copy(expandedLogsLinkId = if (it.expandedLogsLinkId == linkId) null else linkId)
        }
    }

    fun dismissResolvedMirrors(linkId: Int?) {
        _uiState.update {
            val updated = it.resolvedMirrors.toMutableMap()
            updated.remove(linkId)
            it.copy(
                resolvedMirrors = updated,
                expandedLinkId = if (it.expandedLinkId == linkId) null else it.expandedLinkId
            )
        }
    }

    fun resetCommentState() {
        _uiState.update { it.copy(commentPosted = false, commentError = null) }
    }

    fun checkBookmarkAndLikeStatus() {
        viewModelScope.launch {
            val bookmarked = firebaseRepository.isBookmarked(slug)
            val liked = firebaseRepository.isLiked(slug)
            _uiState.update { it.copy(isBookmarked = bookmarked, isLiked = liked) }
        }
    }

    fun toggleBookmark() {
        val series = _uiState.value.series ?: return
        viewModelScope.launch {
            val currentlyBookmarked = _uiState.value.isBookmarked
            if (currentlyBookmarked) {
                firebaseRepository.removeBookmark(slug)
            } else {
                firebaseRepository.addBookmark(
                    BookmarkItem(
                        slug = series.slug,
                        title = series.title,
                        posterUrl = series.posterUrl,
                        isSeries = true,
                        contentModeration = series.contentModeration?.toModerationMap()
                    )
                )
            }
            _uiState.update { it.copy(isBookmarked = !currentlyBookmarked) }
            MyListRefreshState.markStale()
        }
    }

    fun toggleLike() {
        val series = _uiState.value.series ?: return
        viewModelScope.launch {
            val currentlyLiked = _uiState.value.isLiked
            if (currentlyLiked) {
                firebaseRepository.removeLike(slug)
            } else {
                firebaseRepository.addLike(
                    LikeItem(
                        slug = series.slug,
                        title = series.title,
                        posterUrl = series.posterUrl,
                        isSeries = true,
                        contentModeration = series.contentModeration?.toModerationMap()
                    )
                )
            }
            _uiState.update { it.copy(isLiked = !currentlyLiked) }
            MyListRefreshState.markStale()
        }
    }

    fun addToFirebaseHistory() {
        val series = _uiState.value.series ?: return
        viewModelScope.launch {
            firebaseRepository.addToHistory(
                FirebaseHistoryItem(
                    slug = series.slug,
                    title = series.title,
                    posterUrl = series.posterUrl,
                    isSeries = true,
                    imdbId = series.imdbId,
                    contentModeration = series.contentModeration?.toModerationMap()
                )
            )
        }
    }

    fun openReportDrawer() {
        _uiState.update { it.copy(showReportDrawer = true) }
    }

    fun closeReportDrawer() {
        _uiState.update { it.copy(showReportDrawer = false, isSubmittingReport = false) }
    }

    fun submitContentModeration(movieId: Int, reportType: String, reason: String) {
        val isObjection = reportType == "objection"
        viewModelScope.launch {
            _uiState.update { it.copy(showReportDrawer = false, showReportWaiting = true, isObjectionReport = isObjection) }
            try {
                val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val tokenResult = user?.getIdToken(false)?.await()
                val authHeader = "Bearer ${tokenResult?.token ?: ""}"
                val response = repository.submitContentModeration(authHeader, movieId, reportType, reason)
                handleModerationResponse(response, isObjection)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        showReportWaiting = false,
                        moderationError = e.message
                    )
                }
            }
        }
    }

    // Instant save for normal users (and moderator's regular submit): report goes
    // straight to the moderation queue with uid/name/email/slug; thanks message turant.
    fun submitUserReport(movieId: Int, reportType: String, reason: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingReport = true) }
            try {
                val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val tokenResult = user?.getIdToken(false)?.await()
                val authHeader = "Bearer ${tokenResult?.token ?: ""}"
                val response = repository.submitContentModeration(authHeader, movieId, reportType, reason)
                _uiState.update {
                    it.copy(
                        isSubmittingReport = false,
                        showReportDrawer = false,
                        reportSuccessMessage = response.message
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSubmittingReport = false, moderationError = e.message) }
            }
        }
    }

    fun dismissReportSuccess() {
        _uiState.update { it.copy(reportSuccessMessage = null) }
    }

    fun submitModeratorVerdict(movieId: Int, poster: String, screenshots: String, storyline: String, reasoning: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(showReportDrawer = false) }
            try {
                val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val tokenResult = user?.getIdToken(false)?.await()
                val authHeader = "Bearer ${tokenResult?.token ?: ""}"
                val verdict = mapOf<String, @JvmSuppressWildcards Any>(
                    "poster" to poster,
                    "screenshots" to screenshots,
                    "storyline" to storyline,
                    "confidence" to "high",
                    "reasoning" to reasoning
                )
                val response = repository.submitModeratorVerdict(authHeader, movieId, "manual", reasoning, verdict)
                handleModerationResponse(response, false)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(moderationError = e.message)
                }
            }
        }
    }

    private suspend fun handleModerationResponse(response: com.movie.app.best.data.remote.ContentModerationApiResponse, isObjection: Boolean) {
        val modResult = response.moderation
        val prevResult = response.previous_moderation

        val anyChange = if (prevResult != null && modResult != null) {
            modResult.poster != prevResult.poster || modResult.screenshots != prevResult.screenshots || modResult.storyline != prevResult.storyline
        } else {
            modResult?.poster == "sexual" || modResult?.screenshots == "sexual" || modResult?.storyline == "sexual"
        }

        val showCeleb = if (isObjection) anyChange else (modResult?.poster == "sexual" || modResult?.screenshots == "sexual" || modResult?.storyline == "sexual")

        _uiState.update { state ->
            state.copy(
                showReportWaiting = false,
                reportModerationResult = modResult,
                previousModerationResult = prevResult,
                showCelebration = showCeleb
            )
        }
        if (modResult != null) {
            val newCm = com.movie.app.best.data.model.ContentModeration(
                poster = modResult.poster,
                screenshots = modResult.screenshots,
                storyline = modResult.storyline
            )
            _uiState.update { it.copy(series = it.series?.copy(contentModeration = newCm)) }
        }
    }

    fun dismissReportResult() {
        _uiState.update { it.copy(reportModerationResult = null) }
    }

    fun dismissCelebration() {
        _uiState.update { it.copy(showCelebration = false) }
    }

    private fun loadSimilarMovies(imdbId: String) {
        if (!imdbId.startsWith("tt")) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSimilarLoading = true) }
            repository.getSimilar(imdbId).collect { result ->
                when (result) {
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                similarMovies = result.data?.items ?: emptyList(),
                                isSimilarLoading = false,
                                similarError = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isSimilarLoading = false,
                                similarError = result.error
                            )
                        }
                    }
                }
            }
        }
    }

    private fun loadCrewAndTitle(imdbId: String) {
        if (!imdbId.startsWith("tt")) {
            _uiState.update { it.copy(imdbReady = true) }
            return
        }
        viewModelScope.launch {
            try {
                coroutineScope {
                    val creditsDeferred = async {
                        try { imdbApi.getCredits(imdbId, pageSize = 30) } catch (_: Exception) { null }
                    }
                    val titleDeferred = async {
                        try { imdbApi.getTitleDetails(imdbId) } catch (_: Exception) { null }
                    }
                    val credits = withTimeoutOrNull(4000) { creditsDeferred.await() }
                    val titleDetails = withTimeoutOrNull(4000) { titleDeferred.await() }
                    val crew = CrewPerson.sortCrew(
                        credits?.credits?.mapNotNull { CrewPerson.fromCredit(it) } ?: emptyList()
                    )
                    _uiState.update {
                        it.copy(
                            crewCredits = crew,
                            titleDetails = titleDetails
                        )
                    }
                }
            } catch (_: Exception) {
            } finally {
                _uiState.update { it.copy(imdbReady = true) }
            }
        }
    }
}

data class TVShowDetailUiState(
    val series: MovieDetails? = null,
    val isLoading: Boolean = false,
    val error: String? = null,

    val episodesBySeason: Map<Int, List<Episode>> = emptyMap(),
    val linksByEpisode: Map<Int, List<DownloadLink>> = emptyMap(),
    val downloadLinks: List<DownloadLink> = emptyList(),
    val comments: List<Comment> = emptyList(),
    val screenshots: List<String> = emptyList(),
    val moreSeasons: List<Season> = emptyList(),

    val similarMovies: List<com.movie.app.best.data.model.Movie> = emptyList(),
    val isSimilarLoading: Boolean = false,
    val similarError: String? = null,

    val crewCredits: List<CrewPerson> = emptyList(),
    val titleDetails: ImdbTitleDetails? = null,
    val fallbackGenres: List<String> = emptyList(),
    val contentReady: Boolean = false,
    val imdbReady: Boolean = false,

    val isCommentPosting: Boolean = false,
    val commentPosted: Boolean = false,
    val commentError: String? = null,

    val streamRequested: Boolean = false,
    val isStreamRequesting: Boolean = false,
    val streamRequestResult: com.movie.app.best.data.remote.StreamRequestApiResponse? = null,
    val showStreamRequestResult: Boolean = false,
    val streamRequestError: String? = null,

    val downloadUrl: String = "",
    val isDownloadLoading: Boolean = false,
    val downloadStarted: Boolean = false,
    val downloadError: String? = null,
    val downloadLogs: Map<Int?, List<String>> = emptyMap(),
    val expandedLogsLinkId: Int? = null,
    val downloadLoadingLinkId: Int? = null,
    val resolvedMirrors: Map<Int?, List<ResolvedMirror>> = emptyMap(),
    val expandedLinkId: Int? = null,
    val downloadPhase: DownloadPhase = DownloadPhase.NONE,
    val downloadProgress: Int = 0,
    val downloadKetchId: Int? = null,
    val downloadFailureReason: String? = null,
    val downloadIsZip: Boolean = false,
    val downloadExtractPath: String? = null,
    val downloadFilePath: String? = null,
    val downloadTitle: String = "",
    val downloadStartedLinkIds: Set<Int> = emptySet(),
    val downloadExtractionProgress: Int = 0,
    val downloadBypassUrl: String? = null,
    val downloadBypassLogs: List<String> = emptyList(),
    val downloadBypassMetaKey: String = "",

    val isBookmarked: Boolean = false,
    val isLiked: Boolean = false,

    val showReportDrawer: Boolean = false,
    val showReportWaiting: Boolean = false,
    val isObjectionReport: Boolean = false,
    val isModerator: Boolean = false,
    val moderationError: String? = null,
    val reportModerationResult: com.movie.app.best.data.model.ContentModerationResponse? = null,
    val previousModerationResult: com.movie.app.best.data.model.ContentModerationResponse? = null,
    val showCelebration: Boolean = false,
    val reportSuccessMessage: String? = null,
    val isSubmittingReport: Boolean = false
) {
    val isReady: Boolean get() = contentReady && imdbReady
}
