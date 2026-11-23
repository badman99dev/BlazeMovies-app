package com.movie.app.best.ui.screens.serieswatch

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movie.app.best.data.model.CrewPerson
import com.movie.app.best.data.model.ExtractionState
import com.movie.app.best.data.model.FirebaseHistoryItem
import com.movie.app.best.data.model.ImdbEpisode
import com.movie.app.best.data.model.PlaybackKind
import com.movie.app.best.data.model.PlaybackOption
import com.movie.app.best.data.model.ServerScanRow
import com.movie.app.best.data.model.SourceHubGroup
import com.movie.app.best.data.model.SourceHubRequest
import com.movie.app.best.data.model.SourceHubTree
import com.movie.app.best.data.model.WatchEpisode
import com.movie.app.best.data.remote.ImdbApiService
import com.movie.app.best.data.remote.SourceHubClient
import com.movie.app.best.data.repository.SourceCacheEntry
import com.movie.app.best.data.repository.SourceCacheStore
import com.movie.app.best.data.repository.FirebaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.movie.app.best.data.telemetry.IncidentReporter
import com.movie.app.best.data.telemetry.PlayerFailure
import com.movie.app.best.data.telemetry.PlayerIncidents
import com.movie.app.best.util.isRealImdbId
import javax.inject.Inject

@HiltViewModel
class SeriesWatchViewModel @Inject constructor(
    private val imdbApi: ImdbApiService,
    private val sourceHubClient: SourceHubClient,
    private val sourceCache: SourceCacheStore,
    private val firebaseRepository: FirebaseRepository,
    private val incidentReporter: IncidentReporter,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val rawImdbId: String = savedStateHandle["imdbId"] ?: ""
    val seriesTitle: String = savedStateHandle["title"] ?: ""
    private val movieId: String = savedStateHandle["movieId"] ?: ""
    // Watch deep links carry only an IMDb id; treat an IMDb-shaped slug as the imdbId
    // and keep (slug ?: imdbId) as the identity for telemetry/bookmarks.
    private val slugRaw: String = savedStateHandle["slug"] ?: ""
    private val imdbId: String = rawImdbId.ifBlank { slugRaw.takeIf { isRealImdbId(it) }.orEmpty() }
    private val slug: String = slugRaw.ifBlank { imdbId }
    private val targetSeason: Int = savedStateHandle["targetSeason"] ?: -1
    private val targetEpisode: Int = savedStateHandle["episode"] ?: -1
    val backendCast: String = savedStateHandle["cast"] ?: ""
    val backendDirector: String = savedStateHandle["director"] ?: ""
    val backendDescription: String = savedStateHandle["description"] ?: ""
    val backendGenres: String = savedStateHandle["genres"] ?: ""

    private var historyRecorded = false

    private val _state = MutableStateFlow(ExtractionState())
    val state: StateFlow<ExtractionState> = _state.asStateFlow()

    private val imdbCache = mutableMapOf<Int, List<ImdbEpisode>>()
    private var sourceHubGroups: List<SourceHubGroup> = emptyList()
    private val options = mutableListOf<PlaybackOption>()
    private val altCursor = mutableMapOf<String, Int>()
    private var scanStarted = false
    private var lastHubRows: List<ServerScanRow> = emptyList()
    private var hubSettled = false
    private var playbackCommitted = false
    private var scanEpoch = 0
    private val tvTreeKey: String? = if (imdbId.startsWith("tt")) SourceCacheStore.tvTreeKey(imdbId) else null
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
            val creditsDeferred = viewModelScope.async {
                try { imdbApi.getCredits(imdbId, pageSize = 30) } catch (_: Exception) { null }
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

            // Previously-resolved tree (from the hub) paints the season list instantly.
            tvTreeKey?.let { key -> sourceCache.get(key)?.tree }?.let { setHubTree(it, persist = false) }

            val selectedSeason = initialSeason()
            _state.update { it.copy(selectedSeason = selectedSeason) }

            val episodesDeferred = viewModelScope.async {
                try { imdbApi.getEpisodes(imdbId, selectedSeason) } catch (_: Exception) { null }
            }
            viewModelScope.launch {
                try {
                    val resp = episodesDeferred.await()
                    if (resp != null) {
                        imdbCache[selectedSeason] = resp.episodes
                        _state.update { it.copy(imdbEpisodes = it.imdbEpisodes + (selectedSeason to resp.episodes)) }
                    }
                } catch (_: Exception) {}
            }

            // Wait up to 2 seconds for the IMDb requests (not cancelling them)
            val titleDetails = withTimeoutOrNull(2000) { titleDetailsDeferred.await() }
            val certificates = withTimeoutOrNull(2000) { certificatesDeferred.await() }
            val credits = withTimeoutOrNull(2000) { creditsDeferred.await() }
            val episodesResponse = withTimeoutOrNull(2000) { episodesDeferred.await() }
            episodesResponse?.let { resp -> imdbCache[selectedSeason] = resp.episodes }

            _state.update {
                it.copy(
                    isLoading = false,
                    selectedSeason = selectedSeason,
                    error = null,
                    titleDetails = titleDetails,
                    crewCredits = CrewPerson.sortCrew(
                        credits?.credits?.mapNotNull { CrewPerson.fromCredit(it) } ?: emptyList()
                    ),
                    ageRating = certificates?.let { extractAgeRating(it) } ?: ""
                )
            }

            // Auto-select the first episode of the selected season (or a deep-linked episode).
            // If IMDb has none yet, still resolve season/episode 1 so the hub can hand back
            // the tree and sources.
            val episodes = _state.value.mergedEpisodes
            val first = (if (targetEpisode > 0) episodes.firstOrNull { it.episodeNo == targetEpisode } else null)
                ?: episodes.firstOrNull()
                ?: placeholderEpisode(selectedSeason)
            _state.update { it.copy(currentEpisode = first) }
            onEpisodeClick(first)
        }
    }

    private fun initialSeason(): Int {
        if (targetSeason != -1) return targetSeason
        val tree = _state.value.hubTree ?: return 1
        return tree.seasons.maxByOrNull { it.season }?.season ?: 1
    }

    private fun placeholderEpisode(season: Int) = WatchEpisode(
        episodeNo = 1,
        seasonNo = season,
        title = "Episode 1",
        stillImageUrl = "",
        runtimeMinutes = 0,
        rating = 0.0,
        releaseDate = "",
        plot = "",
        languages = emptyList()
    )

    fun loadImdbEpisodes(seasonNo: Int, onLoaded: ((List<ImdbEpisode>) -> Unit)? = null) {
        if (imdbCache.containsKey(seasonNo)) {
            val cached = imdbCache[seasonNo]!!
            _state.update { it.copy(imdbEpisodes = it.imdbEpisodes + (seasonNo to cached)) }
            onLoaded?.invoke(cached)
            return
        }
        viewModelScope.launch {
            try {
                val resp = imdbApi.getEpisodes(imdbId, seasonNo)
                val eps = resp.episodes
                imdbCache[seasonNo] = eps
                _state.update { it.copy(imdbEpisodes = it.imdbEpisodes + (seasonNo to eps)) }
                onLoaded?.invoke(eps)
            } catch (_: Exception) { onLoaded?.invoke(emptyList()) }
        }
    }

    fun selectSeason(seasonNo: Int) {
        _state.update { it.copy(selectedSeason = seasonNo) }
        loadImdbEpisodes(seasonNo) { episodes ->
            if (episodes.isNotEmpty()) {
                val merged = _state.value.mergedEpisodes
                if (merged.isNotEmpty()) onEpisodeClick(merged.first())
            }
        }
    }

    fun selectLanguage(lang: String) {
        // Audio tracks belong to the playing server: only switch a track this row actually exposes,
        // never jump to a different server that happens to carry the language.
        val cur = options.firstOrNull { it.id == _state.value.selectedOptionId }
        if (cur == null || !cur.audio || cur.languages.none { it.equals(lang, true) }) return
        _state.update { it.copy(selectedLanguage = lang) }
        playOption(cur, lang)
    }

    fun onEpisodeClick(episode: WatchEpisode) {
        scanEpoch++
        scanStarted = false
        lastHubRows = emptyList()
        playbackCommitted = false
        // Stop the old episode instantly and show this episode's scan animation from the click itself.
        _state.update {
            it.copy(
                currentEpisode = episode,
                episodeNoSource = false,
                isLoading = true,
                currentM3u8 = null,
                serverScan = emptyList(),
                error = null
            )
        }
        if (!imdbId.startsWith("tt")) {
            _state.update { it.copy(isLoading = false, currentM3u8 = null, episodeNoSource = true) }
            return
        }
        resolveEpisode(episode)
    }

    private fun isHindiOption(opt: PlaybackOption): Boolean {
        val l = opt.language ?: opt.languages.firstOrNull() ?: return false
        return l.contains("Hindi", ignoreCase = true)
    }

    /** Pure order-based priority: Hindi tier first, then other languages. Server `pinned` decides within a tier. */
    private fun orderOptions(opts: List<PlaybackOption>): List<PlaybackOption> {
        fun byPinned(l: List<PlaybackOption>) = l.sortedWith(compareByDescending { it.pinned })
        val hindi = opts.filter { isHindiOption(it) }
        val others = opts.filterNot { isHindiOption(it) }
        return byPinned(hindi) + byPinned(others)
    }

    private fun buildOptions() {
        val list = mutableListOf<PlaybackOption>()
        sourceHubGroups.forEach { g ->
            val langs = g.sortedLanguages
            val master = g.master
            val trackUrl = g.tracks.firstOrNull()?.url
            val url = when {
                !master.isNullOrEmpty() -> sourceCache.writeHlsMaster(g.fileBaseName(), master)
                trackUrl != null -> trackUrl
                else -> g.variants.firstOrNull() ?: return@forEach
            }
            list.add(
                PlaybackOption(
                    id = "sourcehub:" + g.key,
                    label = g.label,
                    kind = PlaybackKind.SOURCE_HUB,
                    service = g.service,
                    url = url,
                    headers = g.headers,
                    alternates = g.variants.filter { it != url },
                    playbackType = g.playbackType,
                    language = langs.firstOrNull(),
                    languages = langs,
                    tracks = g.tracks,
                    audio = g.audio,
                    pinned = g.pinned
                )
            )
        }
        val ordered = orderOptions(list)
        options.clear()
        options.addAll(ordered)
        _state.update { it.copy(options = ordered) }
        updateLanguages()
    }

    /** Audio tracks are per-server: the menu reflects ONLY the currently playing row's languages. */
    private fun updateLanguages() {
        val current = options.firstOrNull { it.id == _state.value.selectedOptionId }
        val langs = (if (current?.audio == true) current.languages else emptyList())
            .distinctBy { it.lowercase() }
            .sortedWith(compareBy<String> { lang ->
                when {
                    lang.contains("Hindi", ignoreCase = true) -> 0
                    lang.contains("English", ignoreCase = true) -> 1
                    else -> 2
                }
            }.thenBy { it })
        val cur = _state.value.selectedLanguage
        val default = cur.takeIf { l -> langs.any { it.equals(l, true) } }
            ?: langs.firstOrNull { it.contains("Hindi", ignoreCase = true) }
            ?: langs.firstOrNull()
            ?: cur
        _state.update { it.copy(availableLanguages = langs, selectedLanguage = default) }
    }

    private fun mergeGroups(partial: List<SourceHubGroup>) {
        val merged = LinkedHashMap<String, SourceHubGroup>()
        sourceHubGroups.forEach { merged[it.key] = it }
        partial.forEach { merged[it.key] = it }
        sourceHubGroups = merged.values.toList()
    }

    private fun setHubTree(tree: SourceHubTree, persist: Boolean = true) {
        if (tree.seasons.isEmpty()) return
        if (persist) {
            tvTreeKey?.let { key ->
                val cur = sourceCache.get(key)
                sourceCache.put(key, (cur ?: SourceCacheEntry()).copy(tree = tree))
            }
        }
        _state.update { it.copy(hubTree = tree) }
    }

    private fun resolveEpisode(episode: WatchEpisode) {
        viewModelScope.launch {
            val epoch = scanEpoch
            val key = episodeCacheKey(episode.seasonNo, episode.episodeNo)
            val cached = key?.let { sourceCache.get(it) }

            scanStarted = false
            lastHubRows = emptyList()
            hubSettled = false
            playbackCommitted = false

            if (cached != null && cached.groups.isNotEmpty()) {
                sourceHubGroups = cached.groups
                buildOptions()
                playFirstOption()
                return@launch
            }
            key?.let { sourceCache.invalidate(it) }

            _state.update { it.copy(isLoading = true) }
            sourceHubGroups = emptyList()
            buildOptions()

            val hubDeferred = viewModelScope.async {
                try {
                    sourceHubClient.resolve(
                        SourceHubRequest(
                            id = imdbId,
                            type = "tv",
                            season = episode.seasonNo,
                            episode = episode.episodeNo
                        ),
                        onService = { partial ->
                            if (epoch == scanEpoch) {
                                partial.tree?.let { setHubTree(it) }
                                if (partial.groups.isNotEmpty()) {
                                    mergeGroups(partial.groups)
                                    buildOptions()
                                }
                            }
                        },
                        onScan = { rows -> if (epoch == scanEpoch) onHubScan(rows) }
                    )
                } catch (_: Exception) { null }
            }

            val sh = hubDeferred.await()
            if (epoch != scanEpoch) return@launch
            sh?.tree?.let { setHubTree(it) }
            if (sh != null && sh.groups.isNotEmpty()) {
                sourceHubGroups = sh.groups.distinctBy { it.key }
                buildOptions()
                key?.let { k -> sourceCache.put(k, SourceCacheEntry(groups = sourceHubGroups)) }
            }
            hubSettled = true
            tryCommitPlayback()
        }
    }

    /** Hub fanout began (WS 'start') → turns the scan overlay on. */
    private fun onHubScan(rows: List<ServerScanRow>) {
        lastHubRows = rows
        scanStarted = true
        publishScan()
    }

    private fun tryCommitPlayback() {
        if (playbackCommitted) return
        playbackCommitted = true
        val pref = preferredOption()
        if (pref == null) {
            _state.value.currentEpisode?.let { ep ->
                episodeCacheKey(ep.seasonNo, ep.episodeNo)?.let { sourceCache.invalidate(it) }
            }
            _state.update { it.copy(isLoading = false, currentM3u8 = null, episodeNoSource = true, error = null) }
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

    private fun playOption(opt: PlaybackOption, trackLang: String? = null) {
        val playUrl = resolveUrl(opt, trackLang)
        altCursor[opt.id] = 0
        _state.update {
            it.copy(
                isLoading = true,
                currentM3u8 = null,
                selectedOptionId = opt.id,
                activeSource = opt.kind,
                activeService = opt.service,
                selectedLanguage = trackLang ?: opt.language ?: it.selectedLanguage,
                error = null,
                playToken = it.playToken + 1
            )
        }
        updateLanguages()
        viewModelScope.launch {
            if (playUrl.isNullOrEmpty()) {
                advanceFrom(opt.id)
                return@launch
            }
            _state.update {
                it.copy(
                    isLoading = false,
                    currentM3u8 = playUrl,
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

    /** A per-language group's stream for [trackLang]; falls back to the row's default url. */
    private fun resolveUrl(opt: PlaybackOption, trackLang: String?): String {
        if (trackLang != null) {
            val t = opt.tracks.firstOrNull { it.language.equals(trackLang, true) }
            if (t != null && t.url.isNotBlank()) return t.url
        }
        return opt.url
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
                    activeService = opt.service,
                    error = null
                )
            }
            return
        }
        _state.update { it.copy(currentM3u8 = null) }
        if (failed != null) advanceFrom(failed)
    }

    /** Ship a playback-source incident: a silent failover, or "no server left". */
    fun reportPlayerFailure(error: PlaybackException?, player: Player?) {
        val failed = _state.value.selectedOptionId
        val opt = options.firstOrNull { it.id == failed }
        val idx = options.indexOfFirst { it.id == failed }
        val hasNext = options.getOrNull(idx + 1) != null
        val hasAlt = failed != null && opt != null && (altCursor[failed] ?: 0) < opt.alternates.size
        incidentReporter.report(
            PlayerIncidents.build(
                PlayerFailure(
                    imdbId = imdbId,
                    slug = slug,
                    contentTitle = _state.value.currentEpisode?.displayTitle ?: seriesTitle,
                    serverName = opt?.label ?: "",
                    serverKind = opt?.kind ?: "",
                    serverService = opt?.service ?: "",
                    url = _state.value.currentM3u8 ?: opt?.url ?: "",
                    serverIndex = idx,
                    totalServers = options.size,
                    hasMoreServers = hasNext || hasAlt,
                    error = error,
                    player = player
                )
            )
        )
    }

    /**
     * Mirror the detail screen: record this series in history the first time it plays.
     * Keyed by [slug] (the IMDb id for a deep-linked watch), so an IMDb-keyed entry
     * round-trips through History straight back to this screen. Existing progress is
     * preserved so re-opening a series never resets resume position.
     */
    fun recordHistory() {
        if (historyRecorded || slug.isBlank()) return
        val t = _state.value.titleDetails?.primaryTitle?.takeIf { it.isNotBlank() }
            ?: seriesTitle.takeIf { it.isNotBlank() }
            ?: return
        historyRecorded = true
        viewModelScope.launch {
            val prev = firebaseRepository.getLocalProgress(slug)
            firebaseRepository.addToHistory(
                FirebaseHistoryItem(
                    slug = slug,
                    title = t,
                    posterUrl = _state.value.currentEpisode?.stillImageUrl?.takeIf { it.isNotEmpty() }
                        ?: _state.value.titleDetails?.posterUrl?.takeIf { it.isNotEmpty() }
                        ?: "",
                    isSeries = true,
                    imdbId = imdbId,
                    progressMs = prev?.progressMs ?: 0,
                    durationMs = prev?.durationMs ?: 0,
                    progressPercent = prev?.progressPercent ?: 0f
                )
            )
        }
    }

    private fun extractAgeRating(response: com.movie.app.best.data.model.ImdbCertificatesResponse): String {
        return response.certificates.find { it.country?.code == "IN" }?.rating
            ?: response.certificates.find { it.country?.code == "US" }?.rating
            ?: ""
    }
}
