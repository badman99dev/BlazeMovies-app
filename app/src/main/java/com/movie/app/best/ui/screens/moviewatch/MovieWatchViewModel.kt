package com.movie.app.best.ui.screens.moviewatch

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movie.app.best.data.model.BookmarkItem
import com.movie.app.best.data.model.CrewPerson
import com.movie.app.best.data.model.ImdbCertificatesResponse
import com.movie.app.best.data.model.ImdbTitleDetails
import com.movie.app.best.data.model.Resource
import com.movie.app.best.data.model.Movie
import com.movie.app.best.data.model.PlaybackKind
import com.movie.app.best.data.model.PlaybackOption
import com.movie.app.best.data.model.ServerScanRow
import com.movie.app.best.data.model.SourceHubGroup
import com.movie.app.best.data.model.SourceHubRequest
import com.movie.app.best.data.remote.ImdbApiService
import com.movie.app.best.data.remote.SourceHubClient
import com.movie.app.best.data.remote.StreamRequestApiResponse
import com.movie.app.best.data.repository.FirebaseRepository
import com.movie.app.best.data.repository.MovieRepository
import com.movie.app.best.data.repository.MyListRefreshState
import com.movie.app.best.data.repository.SourceCacheEntry
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

    private var sourceHubGroups: List<SourceHubGroup> = emptyList()
    private val options = mutableListOf<PlaybackOption>()
    private val altCursor = mutableMapOf<String, Int>()
    private var scanStarted = false
    private var lastHubRows: List<ServerScanRow> = emptyList()
    private var hubSettled = false
    private var playbackCommitted = false
    private val cacheKey: String? = if (imdbId.startsWith("tt")) SourceCacheStore.movieKey(imdbId) else null

    init {
        MyListRefreshState.markStale()
        loadAll()
    }

    private fun nativeOption(): PlaybackOption? {
        if (hasStream || playerUrl.isNotEmpty()) {
            val url = if (playerUrl.isNotEmpty()) playerUrl else com.movie.app.best.data.config.RemoteConfig.playerAlt() + "?id=$movieId"
            return PlaybackOption(id = "native:main", label = "Native", kind = PlaybackKind.NATIVE, url = url)
        }
        return null
    }

    private fun isHindiOption(opt: PlaybackOption): Boolean {
        val l = opt.language ?: opt.languages.firstOrNull() ?: return false
        return l.contains("Hindi", ignoreCase = true)
    }

    /**
     * Pure order-based priority: Native first, then Hindi options (Gemma at the top of its tier),
     * then every other language (Gemma again at the top). What is on top plays first.
     */
    private fun orderOptions(opts: List<PlaybackOption>): List<PlaybackOption> {
        val native = opts.filter { it.kind == PlaybackKind.NATIVE }
        val rest = opts.filterNot { it.kind == PlaybackKind.NATIVE }
        val hindi = rest.filter { isHindiOption(it) }
        val others = rest.filterNot { isHindiOption(it) }
        fun byGemmaFirst(l: List<PlaybackOption>) = l.sortedWith(compareBy { if (it.service == "gemma") 0 else 1 })
        return native + byGemmaFirst(hindi) + byGemmaFirst(others)
    }

    private fun rebuildOptions() {
        val list = mutableListOf<PlaybackOption>()
        nativeOption()?.let { list.add(it) }
        sourceHubGroups.forEach { g ->
            val langs = g.sortedLanguages
            val master = g.master
            if (!master.isNullOrEmpty()) {
                list.add(
                    PlaybackOption(
                        id = "sourcehub:" + g.key,
                        label = g.label,
                        kind = PlaybackKind.SOURCE_HUB,
                        service = g.service,
                        url = sourceCache.writeHlsMaster(g.fileBaseName(), master),
                        headers = g.headers,
                        alternates = g.variants,
                        playbackType = g.playbackType,
                        language = g.language,
                        languages = langs
                    )
                )
            } else {
                val url = g.variants.firstOrNull() ?: return@forEach
                list.add(
                    PlaybackOption(
                        id = "sourcehub:" + g.key,
                        label = g.label,
                        kind = PlaybackKind.SOURCE_HUB,
                        service = g.service,
                        url = url,
                        headers = g.headers,
                        alternates = g.variants.drop(1),
                        playbackType = g.playbackType,
                        language = g.language,
                        languages = langs
                    )
                )
            }
        }
        val orderedOptions = orderOptions(list)
        options.clear()
        options.addAll(orderedOptions)
        _state.update { it.copy(options = orderedOptions) }
        updateLanguages()
    }

    private fun updateLanguages() {
        val langs = options.filter { it.service == "gemma" }
            .mapNotNull { it.language }
            .distinctBy { it.lowercase() }
            .sortedWith(languageComparator())
        val playing = _state.value.selectedOptionId?.let { id -> options.firstOrNull { it.id == id }?.language }
        val default = playing?.takeIf { l -> langs.any { it.equals(l, true) } }
            ?: langs.firstOrNull { it.contains("Hindi", ignoreCase = true) }
            ?: langs.firstOrNull()
            ?: _state.value.selectedLanguage
        _state.update { it.copy(availableLanguages = langs, selectedLanguage = default) }
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
                    imdbReady = !needsImdb,
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
            val creditsDeferred = viewModelScope.async {
                try { imdbApi.getCredits(imdbId, pageSize = 30) } catch (_: Exception) { null }
            }

            // Kick stream resolution immediately
            viewModelScope.launch { resolveAndPlay() }

            loadSimilarMovies(imdbId)
            checkBookmarkStatus()

            if (needsImdb) {
                val titleDetails = withTimeoutOrNull(4000) { titleDetailsDeferred.await() }
                val certificates = withTimeoutOrNull(4000) { certificatesDeferred.await() }
                val credits = withTimeoutOrNull(4000) { creditsDeferred.await() }
                _state.update {
                    it.copy(
                        imdbReady = true,
                        titleDetails = titleDetails,
                        crewCredits = CrewPerson.sortCrew(
                            credits?.credits?.mapNotNull { c -> CrewPerson.fromCredit(c) } ?: emptyList()
                        ),
                        ageRating = certificates?.let { c -> extractAgeRating(c) } ?: ""
                    )
                }
            } else {
                _state.update { it.copy(imdbReady = true) }
            }
        }
    }

    private suspend fun resolveAndPlay() {
        val cached = cacheKey?.let { sourceCache.get(it) }

        if (cached != null && cached.groups.isNotEmpty()) {
            sourceHubGroups = cached.groups
            rebuildOptions()
            playFirstAvailable()
            return
        }
        if (cached != null) cacheKey?.let { sourceCache.invalidate(it) }

        scanStarted = false
        lastHubRows = emptyList()
        hubSettled = false
        playbackCommitted = false

        val nativePlayed = nativeOption() != null
        if (nativePlayed) {
            // Native is already the playing source → commit is satisfied by it; scan overlay never gates native.
            playbackCommitted = true
            _state.update {
                it.copy(
                    isLoading = false,
                    currentM3u8 = nativeOption()?.url,
                    currentHeaders = emptyMap(),
                    activeSource = "native",
                    activeService = "",
                    selectedOptionId = "native:main",
                    error = null
                )
            }
        }

        val sourceHubDeferred = viewModelScope.async {
            if (imdbId.startsWith("tt")) {
                try {
                    sourceHubClient.resolve(
                        SourceHubRequest(id = imdbId, type = "movie"),
                        onService = { partial ->
                            if (partial.groups.isNotEmpty()) {
                                mergeGroups(partial.groups)
                                rebuildOptions()
                            }
                        },
                        onScan = { rows -> onHubScan(rows) }
                    )
                } catch (_: Exception) { null }
            } else null
        }

        // SourceHub completion → all rows settle here; the options list is complete.
        viewModelScope.launch {
            val sh = sourceHubDeferred.await()
            if (sh != null && sh.groups.isNotEmpty()) {
                sourceHubGroups = sh.groups.distinctBy { it.key }
                rebuildOptions()
            }
            hubSettled = true
            if (!nativePlayed) tryCommitPlayback()
            persistCache()
        }
    }

    private fun mergeGroups(partial: List<SourceHubGroup>) {
        val merged = LinkedHashMap<String, SourceHubGroup>()
        sourceHubGroups.forEach { merged[it.key] = it }
        partial.forEach { merged[it.key] = it }
        sourceHubGroups = merged.values.toList()
    }

    private fun persistCache() {
        if (cacheKey != null && sourceHubGroups.isNotEmpty()) {
            sourceCache.put(cacheKey, SourceCacheEntry(groups = sourceHubGroups))
        }
    }

    /** Hub fanout began (WS connected + 'start' received) → this is what turns the scan overlay on. */
    private fun onHubScan(rows: List<ServerScanRow>) {
        lastHubRows = rows
        scanStarted = true
        publishScan()
    }

    /** Play only once the hub has responded and this movie's options exist. */
    private fun tryCommitPlayback() {
        if (playbackCommitted) return
        if (!hubSettled) return
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
        _state.update { it.copy(serverScan = lastHubRows) }
    }

    /** Pure list order — whatever sits on top of the option list plays first. */
    private fun preferredOption(): PlaybackOption? = options.firstOrNull()

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
                activeService = opt.service,
                selectedLanguage = opt.language ?: it.selectedLanguage,
                error = null,
                playToken = it.playToken + 1
            )
        }
        viewModelScope.launch {
            val url = opt.url
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
                    activeService = opt.service,
                    error = null
                )
            }
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
                    isLoading = false,
                    currentM3u8 = opt.alternates[idx],
                    currentHeaders = opt.headers,
                    currentPlaybackType = opt.playbackType,
                    selectedOptionId = opt.id,
                    activeSource = opt.kind,
                    activeService = opt.service,
                    error = null
                )
            }
            return
        }
        _state.update { it.copy(currentM3u8 = null) }
        viewModelScope.launch {
            if (failed != null) advanceFrom(failed) else {
                _state.update { it.copy(isLoading = true) }
                playFirstAvailable()
            }
        }
    }

    fun selectLanguage(lang: String) {
        _state.update { it.copy(selectedLanguage = lang) }
        val gemmaOpt = options.firstOrNull { it.service == "gemma" && it.language == lang }
        if (gemmaOpt != null) playOption(gemmaOpt)
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
    val playToken: Long = 0L,
    val imdbReady: Boolean = false,
    val currentM3u8: String? = null,
    val crewCredits: List<CrewPerson> = emptyList(),
    val currentHeaders: Map<String, String> = emptyMap(),
    val currentPlaybackType: String = "hls",
    val serverScan: List<ServerScanRow> = emptyList(),
    val activeSource: String = "",
    val activeService: String = "",
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
