package com.movie.app.best.ui.screens.serieswatch

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movie.app.best.data.model.ExtractionState
import com.movie.app.best.data.model.GemmaExtractionResult
import com.movie.app.best.data.model.ImdbEpisode
import com.movie.app.best.data.model.PlaybackKind
import com.movie.app.best.data.model.PlaybackOption
import com.movie.app.best.data.model.ServerScanRow
import com.movie.app.best.data.model.SourceHubRequest
import com.movie.app.best.data.model.SourceHubSource
import com.movie.app.best.data.model.buildMasterPlaylist
import com.movie.app.best.data.model.groupByServer
import com.movie.app.best.data.model.WatchEpisode
import com.movie.app.best.data.remote.GemmaExtractorService
import com.movie.app.best.data.remote.ImdbApiService
import com.movie.app.best.data.remote.SourceHubClient
import com.movie.app.best.data.repository.SourceCacheStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class SeriesWatchViewModel @Inject constructor(
    private val gemmaExtractor: GemmaExtractorService,
    private val imdbApi: ImdbApiService,
    private val sourceHubClient: SourceHubClient,
    private val sourceCache: SourceCacheStore,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val imdbId: String = savedStateHandle["imdbId"] ?: ""
    val seriesTitle: String = savedStateHandle["title"] ?: ""
    private val movieId: String = savedStateHandle["movieId"] ?: ""
    private val slug: String = savedStateHandle["slug"] ?: ""
    private val targetSeason: Int = savedStateHandle["targetSeason"] ?: -1
    val backendCast: String = savedStateHandle["cast"] ?: ""
    val backendDirector: String = savedStateHandle["director"] ?: ""
    val backendDescription: String = savedStateHandle["description"] ?: ""
    val backendGenres: String = savedStateHandle["genres"] ?: ""

    private val _state = MutableStateFlow(ExtractionState())
    val state: StateFlow<ExtractionState> = _state.asStateFlow()

    private val imdbCache = mutableMapOf<Int, List<ImdbEpisode>>()
    private var sourceHubSources: List<SourceHubSource> = emptyList()
    private val options = mutableListOf<PlaybackOption>()
    private val altCursor = mutableMapOf<String, Int>()
    private var userPinned = false
    private var gemmaScanStatus = "resolving"
    private val gemmaTreeKey: String? = if (imdbId.startsWith("tt")) SourceCacheStore.gemmaTreeKey(imdbId) else null
    private fun episodeCacheKey(season: Int, episode: Int): String? =
        if (imdbId.startsWith("tt")) SourceCacheStore.episodeKey(imdbId, season, episode) else null

    init {
        loadAll()
    }

    private fun loadAll() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, backendCast = backendCast, backendDirector = backendDirector, backendDescription = backendDescription, backendGenres = backendGenres) }

            // Start IMDb title & certificate requests in viewModelScope so they survive
            // the 2-second wait and keep running in the background until they finish.
            val titleDetailsDeferred = viewModelScope.async {
                try { imdbApi.getTitleDetails(imdbId) } catch (_: Exception) { null }
            }
            val certificatesDeferred = viewModelScope.async {
                try { imdbApi.getCertificates(imdbId) } catch (_: Exception) { null }
            }

            // Later-arrival observers: update state when the responses finally come in
            viewModelScope.launch {
                try {
                    val details = titleDetailsDeferred.await()
                    if (details != null) _state.update { it.copy(titleDetails = details) }
                } catch (_: Exception) {}
            }
            viewModelScope.launch {
                try {
                    val certs = certificatesDeferred.await()
                    if (certs != null) _state.update { it.copy(ageRating = extractAgeRating(certs)) }
                } catch (_: Exception) {}
            }

            gemmaScanStatus = "resolving"
            val gemmaResult = gemmaTreeKey?.let { sourceCache.get(it)?.gemma } ?: run {
                val fresh = gemmaExtractor.extract(imdbId)
                gemmaTreeKey?.let { key ->
                    val cur = sourceCache.get(key)
                    sourceCache.put(key, (cur ?: com.movie.app.best.data.repository.SourceCacheEntry()).copy(gemma = fresh))
                }
                fresh
            }

            setGemmaScan(if (gemmaResult.seasons.isNotEmpty()) "found" else "fail")
            if (gemmaResult.seasons.isEmpty() && !imdbId.startsWith("tt")) {
                _state.update { it.copy(isLoading = false, error = "Source not found") }
                return@launch
            }

            val availableSeasons = gemmaResult.seasons.keys

            // If target season is not in the backend, try IMDb to confirm it exists
            var targetImdbEpisodes: List<ImdbEpisode>? = null
            if (targetSeason != -1 && targetSeason !in availableSeasons) {
                val targetDeferred = viewModelScope.async {
                    try { imdbApi.getEpisodes(imdbId, targetSeason) } catch (_: Exception) { null }
                }
                viewModelScope.launch {
                    try {
                        val resp = targetDeferred.await()
                        if (resp != null) {
                            imdbCache[targetSeason] = resp.episodes
                            _state.update { it.copy(imdbEpisodes = it.imdbEpisodes.toMutableMap().apply { put(targetSeason, resp.episodes) }) }
                        }
                    } catch (_: Exception) {}
                }
                val targetResponse = withTimeoutOrNull(2000) { targetDeferred.await() }
                targetImdbEpisodes = targetResponse?.episodes?.takeIf { it.isNotEmpty() }
                targetImdbEpisodes?.let { imdbCache[targetSeason] = it }
            }

            val selectedSeason = when {
                targetSeason != -1 && (targetSeason in availableSeasons || targetImdbEpisodes?.isNotEmpty() == true) -> targetSeason
                targetSeason != -1 && availableSeasons.isNotEmpty() -> availableSeasons.maxOrNull() ?: availableSeasons.firstOrNull() ?: 1
                availableSeasons.isNotEmpty() -> availableSeasons.maxOrNull() ?: availableSeasons.firstOrNull() ?: 1
                targetSeason != -1 -> targetSeason
                else -> 1
            }

            // Start IMDb episodes for the selected season; observer updates cache/state when it arrives
            val episodesDeferred = if (imdbCache[selectedSeason] == null) {
                viewModelScope.async {
                    try { imdbApi.getEpisodes(imdbId, selectedSeason) } catch (_: Exception) { null }
                }
            } else null

            episodesDeferred?.let { deferred ->
                viewModelScope.launch {
                    try {
                        val resp = deferred.await()
                        if (resp != null) {
                            imdbCache[selectedSeason] = resp.episodes
                            _state.update { it.copy(imdbEpisodes = it.imdbEpisodes.toMutableMap().apply { put(selectedSeason, resp.episodes) }) }
                        }
                    } catch (_: Exception) {}
                }
            }

            // Wait up to 2 seconds for the IMDb requests (not cancelling them)
            val titleDetails = withTimeoutOrNull(2000) { titleDetailsDeferred.await() }
            val certificates = withTimeoutOrNull(2000) { certificatesDeferred.await() }
            val episodesResponse = withTimeoutOrNull(2000) { episodesDeferred?.await() }

            episodesResponse?.let { resp ->
                imdbCache[selectedSeason] = resp.episodes
            }

            collectAvailableLanguages(gemmaResult)

            val imdbEpisodesMap = mutableMapOf<Int, List<ImdbEpisode>>()
            imdbCache[selectedSeason]?.let { imdbEpisodesMap[selectedSeason] = it }
            targetImdbEpisodes?.let { imdbEpisodesMap[targetSeason] = it }

            _state.update {
                it.copy(
                    isLoading = false,
                    result = gemmaResult,
                    selectedSeason = selectedSeason,
                    error = null,
                    imdbEpisodes = imdbEpisodesMap,
                    titleDetails = titleDetails,
                    ageRating = certificates?.let { extractAgeRating(it) } ?: ""
                )
            }

            // Auto-select the first episode of the selected season
            val episodes = _state.value.mergedEpisodes
            if (episodes.isNotEmpty()) {
                val first = episodes.first()
                _state.update { it.copy(currentEpisode = first) }
                onEpisodeClick(first)
            }
        }
    }

    private fun collectAvailableLanguages(result: GemmaExtractionResult) {
        val langs = mutableSetOf<String>()
        for (season in result.seasons.values) {
            for (ep in season.episodes.values) {
                langs.addAll(ep.languages.keys)
            }
        }
        val sorted = langs.sortedWith(compareBy<String> { lang ->
            when {
                lang.contains("Hindi", ignoreCase = true) -> 0
                lang.contains("English", ignoreCase = true) -> 1
                else -> 2
            }
        }.thenBy { it })
        val default = sorted.firstOrNull { it.contains("Hindi", ignoreCase = true) } ?: sorted.firstOrNull() ?: "Hindi"
        _state.update { it.copy(availableLanguages = sorted, selectedLanguage = default) }
    }

    fun loadImdbEpisodes(seasonNo: Int, onLoaded: ((List<ImdbEpisode>) -> Unit)? = null) {
        if (imdbCache.containsKey(seasonNo)) {
            val cached = imdbCache[seasonNo]!!
            _state.update { it.copy(imdbEpisodes = it.imdbEpisodes.toMutableMap().apply { put(seasonNo, cached) }) }
            onLoaded?.invoke(cached)
            return
        }
        viewModelScope.launch {
            try {
                val resp = imdbApi.getEpisodes(imdbId, seasonNo)
                val eps = resp.episodes
                imdbCache[seasonNo] = eps
                _state.update { it.copy(imdbEpisodes = it.imdbEpisodes.toMutableMap().apply { put(seasonNo, eps) }) }
                onLoaded?.invoke(eps)
            } catch (_: Exception) { onLoaded?.invoke(emptyList()) }
        }
    }

    fun selectSeason(seasonNo: Int) {
        _state.update { it.copy(selectedSeason = seasonNo) }
        loadImdbEpisodes(seasonNo) { episodes ->
            if (episodes.isNotEmpty()) {
                val merged = _state.value.mergedEpisodes
                if (merged.isNotEmpty()) {
                    val first = merged.first()
                    onEpisodeClick(first)
                }
            }
        }
    }

    fun selectLanguage(lang: String) {
        userPinned = true
        _state.update { it.copy(selectedLanguage = lang) }
        val gemmaOpt = options.firstOrNull { it.kind == PlaybackKind.GEMMA && it.language == lang }
        if (gemmaOpt != null) playOption(gemmaOpt)
    }

    fun onEpisodeClick(episode: WatchEpisode) {
        _state.update { it.copy(currentEpisode = episode, episodeNoSource = false) }
        userPinned = false
        if (!imdbId.startsWith("tt") && episode.languages.isEmpty()) {
            // No Gemma entry and no IMDb id to query the hub with → nothing we can try.
            _state.update { it.copy(isLoading = false, currentM3u8 = null, episodeNoSource = true) }
            return
        }
        resolveEpisode(episode)
    }

    private fun buildOptions(episode: WatchEpisode?) {
        val list = mutableListOf<PlaybackOption>()
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
        val result = _state.value.result
        if (result != null && episode != null) {
            val season = result.seasons[episode.seasonNo]
            val gemmaEp = season?.episodes?.get(episode.episodeNo)
            if (gemmaEp != null) {
                gemmaEp.languages.keys.sortedWith(languageComparator()).forEach { lang ->
                    val file = gemmaEp.languages[lang] ?: return@forEach
                    val ck = "e:${episode.seasonNo}:${episode.episodeNo}:$lang"
                    val resolved = episodeCacheKey(episode.seasonNo, episode.episodeNo)
                        ?.let { sourceCache.get(it)?.resolved?.get(ck) }
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

    private fun resolveEpisode(episode: WatchEpisode) {
        viewModelScope.launch {
            val key = episodeCacheKey(episode.seasonNo, episode.episodeNo)
            val cached = key?.let { sourceCache.get(it) }
            if (cached != null && cached.sources.isNotEmpty()) {
                sourceHubSources = cached.sources
                buildOptions(episode)
                playFirstOption()
                return@launch
            }
            key?.let { sourceCache.invalidate(it) }

            _state.update { it.copy(isLoading = true) }
            publishScan(emptyList())
            sourceHubSources = emptyList()
            buildOptions(episode)

            if (imdbId.startsWith("tt")) {
                val sh = try {
                    sourceHubClient.resolve(
                        SourceHubRequest(
                            id = imdbId,
                            type = "tv",
                            season = episode.seasonNo,
                            episode = episode.episodeNo
                        ),
                        onService = { partial ->
                            if (partial.sources.isNotEmpty()) {
                                sourceHubSources = sourceHubSources + partial.sources
                                buildOptions(episode)
                            }
                        },
                        onScan = { rows -> publishScan(rows) }
                    )
                } catch (_: Exception) { null }

                if (sh != null && sh.sources.isNotEmpty()) {
                    sourceHubSources = sh.sources.distinctBy { it.id }
                    buildOptions(episode)
                }
            }

            if (key != null && sourceHubSources.isNotEmpty()) {
                sourceCache.update(key) { cur ->
                    (cur ?: com.movie.app.best.data.repository.SourceCacheEntry())
                        .copy(sources = sourceHubSources)
                }
            }
            _state.update { it.copy(isLoading = false) }
            playFirstOption()
        }
    }

    /** Gemma rows win by default (selected language first); otherwise list order. */
    private fun publishScan(rows: List<ServerScanRow>) {
        val gemmaRow = ServerScanRow(name = "Gemma", status = gemmaScanStatus)
        _state.update { it.copy(serverScan = rows + gemmaRow) }
    }

    private fun setGemmaScan(status: String) {
        gemmaScanStatus = status
        publishScan(_state.value.serverScan.filterNot { it.name == "Gemma" })
    }

    private fun preferredOption(): PlaybackOption? {
        val gemmaOpts = options.filter { it.kind == PlaybackKind.GEMMA }
        if (gemmaOpts.isNotEmpty()) {
            val sel = _state.value.selectedLanguage
            return gemmaOpts.firstOrNull { it.language == sel } ?: gemmaOpts.firstOrNull()
        }
        return options.firstOrNull()
    }

    private fun playFirstOption() {
        val first = preferredOption()
        if (first == null) {
            _state.value.currentEpisode?.let { ep ->
                episodeCacheKey(ep.seasonNo, ep.episodeNo)?.let { sourceCache.invalidate(it) }
            }
            // Hub was checked and returned nothing for this episode → "Available soon"
            _state.update { it.copy(isLoading = false, currentM3u8 = null, episodeNoSource = true, error = null) }
            return
        }
        playOption(first)
    }

    private fun playOption(opt: PlaybackOption) {
        altCursor[opt.id] = 0
        _state.update {
            it.copy(isLoading = true, currentM3u8 = null, serverScan = emptyList(), selectedOptionId = opt.id, activeSource = opt.kind, error = null)
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
        val episode = _state.value.currentEpisode ?: return null
        val result = _state.value.result ?: return null
        val season = result.seasons[episode.seasonNo] ?: return null
        val gemmaEp = season.episodes[episode.episodeNo] ?: return null
        val chosenLang = lang?.takeIf { gemmaEp.languages.containsKey(it) } ?: gemmaEp.languages.keys.firstOrNull() ?: return null
        val file = gemmaEp.languages[chosenLang] ?: return null
        val ck = "e:${episode.seasonNo}:${episode.episodeNo}:$chosenLang"
        val key = episodeCacheKey(episode.seasonNo, episode.episodeNo)
        key?.let { sourceCache.get(it)?.resolved?.get(ck) }?.let { return it }
        val m3u8 = gemmaExtractor.resolveFile(file, result.csrfKey) ?: return null
        if (key != null) {
            sourceCache.update(key) { cur ->
                val base = cur ?: com.movie.app.best.data.repository.SourceCacheEntry()
                base.copy(resolved = base.resolved.toMutableMap().apply { put(ck, m3u8) })
            }
        }
        return m3u8
    }

    private fun advanceFrom(failedId: String) {
        val idx = options.indexOfFirst { it.id == failedId }
        val next = options.getOrNull(idx + 1)
        if (next == null) {
            _state.value.currentEpisode?.let { ep ->
                episodeCacheKey(ep.seasonNo, ep.episodeNo)?.let { sourceCache.invalidate(it) }
            }
            _state.update { it.copy(isLoading = false, currentM3u8 = null, episodeNoSource = true, error = null) }
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
        _state.update { it.copy(currentM3u8 = null) }
        if (failed != null) advanceFrom(failed)
    }

    private fun extractAgeRating(response: com.movie.app.best.data.model.ImdbCertificatesResponse): String {
        return response.certificates.find { it.country?.code == "IN" }?.rating
            ?: response.certificates.find { it.country?.code == "US" }?.rating
            ?: ""
    }
}
