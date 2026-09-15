package com.movie.app.best.ui.screens.moviewatch

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movie.app.best.BuildConfig
import com.movie.app.best.data.model.BookmarkItem
import com.movie.app.best.data.model.GemmaEpisodeInfo
import com.movie.app.best.data.model.GemmaExtractionResult
import com.movie.app.best.data.model.ImdbCertificatesResponse
import com.movie.app.best.data.model.ImdbTitleDetails
import com.movie.app.best.data.model.Resource
import com.movie.app.best.data.model.Movie
import com.movie.app.best.data.model.PlaybackKind
import com.movie.app.best.data.model.PlaybackOption
import com.movie.app.best.data.model.ServerScanRow
import com.movie.app.best.data.model.SourceHubRequest
import com.movie.app.best.data.model.SourceHubSource
import com.movie.app.best.data.model.buildMasterPlaylist
import com.movie.app.best.data.model.groupByServer
import com.movie.app.best.data.remote.GemmaExtractorService
import com.movie.app.best.data.remote.ImdbApiService
import com.movie.app.best.data.remote.SourceHubClient
import com.movie.app.best.data.remote.StreamRequestApiResponse
import com.movie.app.best.data.repository.FirebaseRepository
import com.movie.app.best.data.repository.MovieRepository
import com.movie.app.best.data.repository.MyListRefreshState
import com.movie.app.best.data.repository.SourceCacheStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class MovieWatchViewModel @Inject constructor(
    private val gemmaExtractor: GemmaExtractorService,
    private val imdbApi: ImdbApiService,
    private val sourceHubClient: SourceHubClient,
    private val sourceCache: SourceCacheStore,
    private val repository: MovieRepository,
    private val firebaseRepository: FirebaseRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val imdbId: String = savedStateHandle["imdbId"] ?: ""
    private val movieTitle: String = savedStateHandle["title"] ?: ""
    private val movieId: String = savedStateHandle["movieId"] ?: ""
    private val slug: String = savedStateHandle["slug"] ?: ""
    private val hasStream: Boolean = savedStateHandle["hasStream"] ?: false
    private val playerUrl: String = savedStateHandle["playerUrl"] ?: ""
    val posterUrl: String = savedStateHandle["posterUrl"] ?: ""
    val backendCast: String = savedStateHandle["cast"] ?: ""
    val backendDirector: String = savedStateHandle["director"] ?: ""
    val backendDescription: String = savedStateHandle["description"] ?: ""
    val backendGenres: String = savedStateHandle["genres"] ?: ""
    val title: String get() = movieTitle
    val contentSlug: String get() = slug

    private val _state = MutableStateFlow(MovieWatchState())
    val state: StateFlow<MovieWatchState> = _state.asStateFlow()

    private var gemmaResult: GemmaExtractionResult? = null
    private var sourceHubSources: List<SourceHubSource> = emptyList()
    private val options = mutableListOf<PlaybackOption>()
    private val altCursor = mutableMapOf<String, Int>()
    private var userPinned = false
    private var gemmaScanStatus = "resolving"
    private var scanStarted = false
    private var lastHubRows: List<ServerScanRow> = emptyList()
    private var hubSettled = false
    private var defaultPlaylistReady = false
    private var playbackCommitted = false
    private val cacheKey: String? = if (imdbId.startsWith("tt")) SourceCacheStore.movieKey(imdbId) else null

    init {
        MyListRefreshState.markStale()
        loadAll()
    }

    private fun nativeOption(): PlaybackOption? {
        if (hasStream || playerUrl.isNotEmpty()) {
            val url = if (playerUrl.isNotEmpty()) playerUrl else BuildConfig.SPARKLE_BASE_URL + "?id=$movieId"
            return PlaybackOption(id = "native:main", label = "Native", kind = PlaybackKind.NATIVE, url = url)
        }
        return null
    }

    private fun rebuildOptions() {
        val list = mutableListOf<PlaybackOption>()
        nativeOption()?.let { list.add(it) }
        sourceHubSources.groupByServer().forEach { g ->
            val master = g.buildMasterPlaylist()
            val direct = g.sources.map { it.url }
            if (master != null) {
                list.add(
                    PlaybackOption(
                        id = "sourcehub:" + g.key,
                        label = g.label,
                        kind = PlaybackKind.SOURCE_HUB,
                        url = sourceCache.writeHlsMaster(g.fileBaseName(), master),
                        headers = g.headers,
                        alternates = direct,
                        playbackType = g.playbackType
                    )
                )
            } else {
                list.add(
                    PlaybackOption(
                        id = "sourcehub:" + g.key,
                        label = g.label,
                        kind = PlaybackKind.SOURCE_HUB,
                        url = g.sources.first().url,
                        headers = g.headers,
                        alternates = g.sources.drop(1).map { it.url },
                        playbackType = g.playbackType
                    )
                )
            }
        }
        val g = gemmaResult
        if (g != null && g.seasons.isNotEmpty()) {
            val episode = getMovieEpisode(g)
            if (episode != null) {
                val ordered = episode.languages.keys.sortedWith(languageComparator())
                ordered.forEach { lang ->
                    val file = episode.languages[lang] ?: return@forEach
                    val resolved = cacheKey?.let { sourceCache.get(it) }?.resolved?.get("m1:$lang")
                    list.add(
                        PlaybackOption(
                            id = "gemma:$lang",
                            label = "Gemma",
                            kind = PlaybackKind.GEMMA,
                            url = resolved ?: "",
                            language = lang
                        )
                    )
                }
            }
        }
        options.clear()
        options.addAll(list)
        _state.update { it.copy(options = list) }
    }

    private fun languageComparator(): Comparator<String> = compareBy<String> { lang ->
        when {
            lang.contains("Hindi", ignoreCase = true) -> 0
            lang.contains("English", ignoreCase = true) -> 1
            else -> 2
        }
    }.thenBy { it }

    private fun loadAll() {
        val needsImdb = imdbId.startsWith("tt")
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    showBuffering = needsImdb,
                    error = null,
                    backendCast = backendCast,
                    backendDirector = backendDirector,
                    backendDescription = backendDescription
                )
            }

            val titleDetailsDeferred = viewModelScope.async {
                try { imdbApi.getTitleDetails(imdbId) } catch (_: Exception) { null }
            }
            val certificatesDeferred = viewModelScope.async {
                try { imdbApi.getCertificates(imdbId) } catch (_: Exception) { null }
            }

            // Kick stream resolution immediately
            viewModelScope.launch { resolveAndPlay() }

            loadSimilarMovies(imdbId)
            checkBookmarkStatus()

            if (needsImdb) {
                val titleDetails = withTimeoutOrNull(4000) { titleDetailsDeferred.await() }
                val certificates = withTimeoutOrNull(4000) { certificatesDeferred.await() }
                _state.update {
                    it.copy(
                        showBuffering = false,
                        titleDetails = titleDetails,
                        ageRating = certificates?.let { c -> extractAgeRating(c) } ?: ""
                    )
                }
            } else {
                _state.update { it.copy(showBuffering = false) }
            }
        }
    }

    private suspend fun resolveAndPlay() {
        val cached = cacheKey?.let { sourceCache.get(it) }

        if (cached != null) {
            val usable = cached.sources.isNotEmpty() || cached.gemma?.seasons?.isNotEmpty() == true
            if (usable) {
                sourceHubSources = cached.sources
                gemmaResult = cached.gemma
                cached.gemma?.let { if (it.seasons.isNotEmpty()) collectAvailableLanguages(it) }
                rebuildOptions()
                playFirstAvailable()
                if (cached.gemma == null && imdbId.startsWith("tt")) {
                    viewModelScope.launch {
                        val g = try { gemmaExtractor.extract(imdbId) } catch (_: Exception) { null }
                        if (g != null && g.seasons.isNotEmpty()) {
                            gemmaResult = g
                            collectAvailableLanguages(g)
                            rebuildOptions()
                            cacheKey?.let { k -> sourceCache.update(k) { cur -> (cur ?: com.movie.app.best.data.repository.SourceCacheEntry()).copy(gemma = g) } }
                            maybeSwitchToPreferred()
                        }
                    }
                }
                if (cached.sources.isEmpty() && imdbId.startsWith("tt")) {
                    viewModelScope.launch {
                        try {
                            val sh = sourceHubClient.resolve(
                                SourceHubRequest(id = imdbId, type = "movie"),
                                onService = { partial ->
                                    if (partial.sources.isNotEmpty()) {
                                        sourceHubSources = sourceHubSources + partial.sources
                                        rebuildOptions()
                                    }
                                },
                                onScan = { rows -> onHubScan(rows) }
                            )
                            if (sh.sources.isNotEmpty()) {
                                sourceHubSources = sh.sources.distinctBy { it.id }
                                rebuildOptions()
                                cacheKey?.let { k -> sourceCache.update(k) { cur -> (cur ?: com.movie.app.best.data.repository.SourceCacheEntry()).copy(sources = sourceHubSources) } }
                                maybeSwitchToPreferred()
                            }
                        } catch (_: Exception) { }
                    }
                }
                return
            }
            // poisoned/empty entry → drop it and re-resolve below
            cacheKey?.let { sourceCache.invalidate(it) }
        }

        // Cache miss → resolve SourceHub (streamed) + Gemma in parallel
        scanStarted = false
        lastHubRows = emptyList()
        hubSettled = false
        defaultPlaylistReady = false
        playbackCommitted = false
        gemmaScanStatus = "resolving"

        val hasNative = nativeOption() != null
        val nativePlayed = hasNative
        if (hasNative) {
            // Native is already the playing source → commit is satisfied by it; scan overlay never gates native.
            playbackCommitted = true
            _state.update {
                it.copy(
                    isLoading = false,
                    currentM3u8 = nativeOption()?.url,
                    currentHeaders = emptyMap(),
                    activeSource = "native",
                    selectedOptionId = "native:main",
                    error = null
                )
            }
        }

        val gemmaDeferred = viewModelScope.async {
            if (imdbId.startsWith("tt")) {
                try { gemmaExtractor.extract(imdbId) } catch (_: Exception) { null }
            } else null
        }

        val sourceHubDeferred = viewModelScope.async {
            if (imdbId.startsWith("tt")) {
                try {
                    sourceHubClient.resolve(
                        SourceHubRequest(id = imdbId, type = "movie"),
                        onService = { partial ->
                            if (partial.sources.isNotEmpty()) {
                                sourceHubSources = sourceHubSources + partial.sources
                                rebuildOptions()
                            }
                        },
                        onScan = { rows -> onHubScan(rows) }
                    )
                } catch (_: Exception) { null }
            } else null
        }

        // Gemma completion → start its playlist fetch immediately (does NOT wait for SourceHub)
        viewModelScope.launch {
            val g = gemmaDeferred.await()
            gemmaResult = g
            setGemmaScan(if (g != null && g.seasons.isNotEmpty()) "found" else "fail")
            if (g != null && g.seasons.isNotEmpty()) collectAvailableLanguages(g)
            rebuildOptions()
            if (!nativePlayed) {
                val pref = preferredOption()
                if (pref != null && pref.kind == PlaybackKind.GEMMA) {
                    resolveGemmaUrl(pref.language) // warm the playlist; playOption reuses cache
                    defaultPlaylistReady = true
                } else if (pref != null) {
                    defaultPlaylistReady = true
                }
                tryCommitPlayback()
            } else {
                resolveGemmaDefaults()
                maybeSwitchToPreferred()
            }
            persistCache(cached)
        }

        // SourceHub completion → all rows settle here; commit needs the default playlist too
        viewModelScope.launch {
            val sh = sourceHubDeferred.await()
            if (sh != null && sh.sources.isNotEmpty()) {
                sourceHubSources = sh.sources.distinctBy { it.id }
                rebuildOptions()
            }
            hubSettled = true
            if (!nativePlayed) {
                if (preferredOption()?.kind != PlaybackKind.GEMMA) defaultPlaylistReady = true
                tryCommitPlayback()
            }
            persistCache(cached)
        }
    }

    private fun persistCache(cached: com.movie.app.best.data.repository.SourceCacheEntry?) {
        if (cacheKey != null && (sourceHubSources.isNotEmpty() || gemmaResult != null)) {
            val resolvedMap = mutableMapOf<String, String>()
            cached?.resolved?.forEach { (k, v) -> resolvedMap[k] = v }
            sourceCache.put(
                cacheKey,
                com.movie.app.best.data.repository.SourceCacheEntry(
                    sources = sourceHubSources,
                    gemma = gemmaResult,
                    resolved = resolvedMap
                )
            )
        }
    }

    /** Hub fanout began (WS connected + 'start' received) → this is what turns the scan overlay on. */
    private fun onHubScan(rows: List<ServerScanRow>) {
        lastHubRows = rows
        scanStarted = true
        publishScan()
    }

    /** Play only once BOTH the hub has responded AND the default playlist is fetched. */
    private fun tryCommitPlayback() {
        if (playbackCommitted) return
        if (!hubSettled || !defaultPlaylistReady) return
        playbackCommitted = true
        val pref = preferredOption()
        if (pref == null) {
            cacheKey?.let { sourceCache.invalidate(it) }
            _state.update { it.copy(isLoading = false, currentM3u8 = null, error = "Source not found") }
            return
        }
        playOption(pref)
    }

    private fun publishScan() {
        if (!scanStarted) return
        _state.update { it.copy(serverScan = lastHubRows + ServerScanRow(name = "Gemma", status = gemmaScanStatus)) }
    }

    private fun setGemmaScan(status: String) {
        gemmaScanStatus = status
        publishScan()
    }

    /** Gemma rows win by default (selected language first); otherwise list order (native → SourceHub). */
    private fun preferredOption(): PlaybackOption? {
        val gemmaOpts = options.filter { it.kind == PlaybackKind.GEMMA }
        if (gemmaOpts.isNotEmpty()) {
            val sel = _state.value.selectedLanguage
            return gemmaOpts.firstOrNull { it.language == sel } ?: gemmaOpts.firstOrNull()
        }
        return options.firstOrNull()
    }

    /** Late-arrival upgrade: if native auto-started and Gemma just became available, switch to it. */
    private fun maybeSwitchToPreferred() {
        if (userPinned) return
        val cur = _state.value
        if (cur.currentM3u8 == null) { playFirstAvailable(); return }
        if (cur.activeSource == "native") {
            val p = preferredOption()
            if (p != null && p.kind == PlaybackKind.GEMMA && p.id != cur.selectedOptionId) playOption(p)
        }
    }

    private fun playFirstAvailable() {
        val first = preferredOption() ?: run {
            cacheKey?.let { sourceCache.invalidate(it) }
            _state.update { it.copy(isLoading = false, currentM3u8 = null, error = "Source not found") }
            return
        }
        playOption(first)
    }

    private fun playOption(opt: PlaybackOption) {
        altCursor[opt.id] = 0
        _state.update {
            it.copy(
                isLoading = true,
                currentM3u8 = null,
                selectedOptionId = opt.id,
                activeSource = opt.kind,
                error = null
            )
        }
        viewModelScope.launch {
            val url = if (opt.kind == PlaybackKind.GEMMA && opt.url.isEmpty()) {
                resolveGemmaUrl(opt.language)
            } else opt.url
            if (url.isNullOrEmpty()) {
                advanceFrom(opt.id)
                return@launch
            }
            _state.update {
                it.copy(
                    isLoading = false,
                    currentM3u8 = url,
                    currentHeaders = opt.headers,
                    currentPlaybackType = opt.playbackType,
                    selectedOptionId = opt.id,
                    activeSource = opt.kind,
                    error = null
                )
            }
        }
    }

    private suspend fun resolveGemmaUrl(lang: String?): String? {
        val result = gemmaResult ?: return null
        val episode = getMovieEpisode(result) ?: return null
        val chosen = lang?.let { episode.languages[it] } ?: episode.languages.values.firstOrNull()
        if (chosen == null) return null
        val ck = lang?.let { "m1:$it" }
        if (ck != null) {
            cacheKey?.let { sourceCache.get(it)?.resolved?.get(ck) }?.let { return it }
        }
        val m3u8 = gemmaExtractor.resolveFile(chosen, result.csrfKey) ?: return null
        if (cacheKey != null && ck != null) {
            sourceCache.update(cacheKey) { cur ->
                val base = cur ?: com.movie.app.best.data.repository.SourceCacheEntry(gemma = gemmaResult)
                base.copy(resolved = base.resolved.toMutableMap().apply { put(ck, m3u8) })
            }
        }
        return m3u8
    }

    private fun resolveGemmaDefaults() {
        viewModelScope.launch {
            val g = gemmaResult ?: return@launch
            val episode = getMovieEpisode(g) ?: return@launch
            val lang = _state.value.selectedLanguage
            val chosen = if (episode.languages.containsKey(lang)) lang else episode.languages.keys.firstOrNull()
            if (chosen != null) resolveGemmaUrl(chosen)
        }
    }

    private fun advanceFrom(failedId: String) {
        val idx = options.indexOfFirst { it.id == failedId }
        val next = options.getOrNull(idx + 1)
        if (next == null) {
            cacheKey?.let { sourceCache.invalidate(it) }
            _state.update { it.copy(isLoading = false, currentM3u8 = null, error = "Source not found") }
            return
        }
        playOption(next)
    }

    fun selectOption(id: String) {
        val opt = options.firstOrNull { it.id == id } ?: return
        userPinned = true
        playOption(opt)
    }

    fun onPlaybackError() {
        val failed = _state.value.selectedOptionId
        val opt = failed?.let { id -> options.firstOrNull { it.id == id } }
        val idx = failed?.let { altCursor[it] ?: 0 } ?: 0
        if (opt != null && opt.alternates.isNotEmpty() && idx < opt.alternates.size) {
            altCursor[failed!!] = idx + 1
            _state.update {
                it.copy(
                    showBuffering = false,
                    isLoading = false,
                    currentM3u8 = opt.alternates[idx],
                    currentHeaders = opt.headers,
                    currentPlaybackType = opt.playbackType,
                    selectedOptionId = opt.id,
                    activeSource = opt.kind,
                    error = null
                )
            }
            return
        }
        _state.update { it.copy(showBuffering = false, currentM3u8 = null) }
        viewModelScope.launch {
            if (failed != null) advanceFrom(failed) else {
                _state.update { it.copy(isLoading = true) }
                playFirstAvailable()
            }
        }
    }

    private fun getMovieEpisode(result: GemmaExtractionResult): GemmaEpisodeInfo? {
        val season = result.seasons.values.firstOrNull() ?: return null
        return season.episodes.values.firstOrNull()
    }

    private fun collectAvailableLanguages(result: GemmaExtractionResult) {
        val langs = mutableSetOf<String>()
        for (season in result.seasons.values) {
            for (ep in season.episodes.values) {
                langs.addAll(ep.languages.keys)
            }
        }
        val sorted = langs.sortedWith(languageComparator())
        val default = sorted.firstOrNull { it.contains("Hindi", ignoreCase = true) } ?: sorted.firstOrNull() ?: "Hindi"
        _state.update { it.copy(availableLanguages = sorted, selectedLanguage = default) }
    }

    fun selectLanguage(lang: String) {
        userPinned = true
        _state.update { it.copy(selectedLanguage = lang) }
        val gemmaOpt = options.firstOrNull { it.kind == PlaybackKind.GEMMA && it.language == lang }
        if (gemmaOpt != null) {
            playOption(gemmaOpt)
        }
    }

    fun toggleBookmark() {
        viewModelScope.launch {
            val currentlyBookmarked = _state.value.isBookmarked
            if (currentlyBookmarked) {
                firebaseRepository.removeBookmark(slug)
            } else {
                firebaseRepository.addBookmark(
                    BookmarkItem(
                        slug = slug,
                        title = movieTitle,
                        posterUrl = posterUrl,
                        isSeries = false
                    )
                )
            }
            _state.update { it.copy(isBookmarked = !currentlyBookmarked) }
            MyListRefreshState.markStale()
        }
    }

    private fun checkBookmarkStatus() {
        viewModelScope.launch {
            val bookmarked = firebaseRepository.isBookmarked(slug)
            _state.update { it.copy(isBookmarked = bookmarked) }
        }
    }

    private fun loadSimilarMovies(imdbId: String) {
        if (!imdbId.startsWith("tt")) return
        viewModelScope.launch {
            _state.update { it.copy(isSimilarLoading = true) }
            repository.getSimilar(imdbId).collect { result ->
                when (result) {
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        _state.update {
                            it.copy(
                                similarMovies = result.data?.items ?: emptyList(),
                                isSimilarLoading = false,
                                similarError = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        _state.update {
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

    fun requestStream() {
        viewModelScope.launch {
            _state.update { it.copy(isStreamRequesting = true) }
            try {
                val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val tokenResult = user?.getIdToken(false)?.await()
                val authHeader = "Bearer ${tokenResult?.token ?: ""}"
                val response = repository.submitStreamRequest(authHeader, slug)
                _state.update {
                    it.copy(
                        isStreamRequesting = false,
                        streamRequested = !response.already_requested && !response.has_stream,
                        streamRequestResult = response,
                        showStreamRequestResult = true
                    )
                }
                if (response.already_requested) {
                    _state.update { it.copy(streamRequested = true) }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isStreamRequesting = false,
                        streamRequestError = e.message
                    )
                }
            }
        }
    }

    fun dismissStreamRequestResult() {
        _state.update { it.copy(showStreamRequestResult = false) }
    }

    private fun extractAgeRating(response: ImdbCertificatesResponse): String {
        return response.certificates.find { it.country?.code == "IN" }?.rating
            ?: response.certificates.find { it.country?.code == "US" }?.rating
            ?: ""
    }
}

data class MovieWatchState(
    val isLoading: Boolean = false,
    val showBuffering: Boolean = false,
    val currentM3u8: String? = null,
    val currentHeaders: Map<String, String> = emptyMap(),
    val currentPlaybackType: String = "hls",
    val serverScan: List<ServerScanRow> = emptyList(),
    val activeSource: String = "",
    val selectedOptionId: String? = null,
    val options: List<PlaybackOption> = emptyList(),
    val titleDetails: ImdbTitleDetails? = null,
    val ageRating: String = "",
    val availableLanguages: List<String> = emptyList(),
    val selectedLanguage: String = "Hindi",
    val similarMovies: List<Movie> = emptyList(),
    val isSimilarLoading: Boolean = false,
    val similarError: String? = null,
    val error: String? = null,
    val streamRequested: Boolean = false,
    val isStreamRequesting: Boolean = false,
    val streamRequestResult: StreamRequestApiResponse? = null,
    val showStreamRequestResult: Boolean = false,
    val streamRequestError: String? = null,
    val isBookmarked: Boolean = false,
    val backendCast: String = "",
    val backendDirector: String = "",
    val backendDescription: String = "",
    val backendGenres: String = ""
)
