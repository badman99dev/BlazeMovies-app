package com.movie.app.best.ui.screens.sports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movie.app.best.data.model.SportCategory
import com.movie.app.best.data.model.SportEvent
import com.movie.app.best.data.model.SportStream
import com.movie.app.best.data.repository.SportsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SportStatusFilter(val label: String) {
    ALL("All"),
    LIVE("Live"),
    RECENT("Recent"),
    UPCOMING("Upcoming")
}

data class SportsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val categories: List<SportCategory> = emptyList(),
    val events: List<SportEvent> = emptyList(),
    val filteredEvents: List<SportEvent> = emptyList(),
    val selectedCategory: String = "All",
    val selectedStatus: SportStatusFilter = SportStatusFilter.ALL,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val categoryCounts: Map<String, Int> = emptyMap(),
    val allCount: Int = 0,
    val liveCount: Int = 0,
    val recentCount: Int = 0,
    val upcomingCount: Int = 0,
    val timeTick: Long = 0L
)

@HiltViewModel
class SportsViewModel @Inject constructor(
    private val repository: SportsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SportsUiState())
    val uiState: StateFlow<SportsUiState> = _uiState.asStateFlow()

    private var tickerJob: Job? = null

    init {
        loadData()
        startTicker()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _uiState.update { it.copy(timeTick = System.currentTimeMillis()) }
                // Recompute filtered events if match status flips
                applyFilters()
            }
        }
    }

    fun loadData(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (forceRefresh) {
                _uiState.update { it.copy(isRefreshing = true, error = null) }
            } else {
                _uiState.update { it.copy(isLoading = true, error = null) }
            }

            val catResult = repository.getCategories(forceRefresh)
            val eventsResult = repository.getEvents(forceRefresh)

            val allCategories = catResult.getOrDefault(emptyList()).toMutableList()
            if (allCategories.none { it.title.equals("All", ignoreCase = true) }) {
                allCategories.add(0, SportCategory(id = 0, title = "All", image = ""))
            }

            val events = eventsResult.getOrDefault(emptyList())

            val counts = mutableMapOf<String, Int>()
            allCategories.forEach { cat ->
                counts[cat.title] = repository.getCategoryCount(cat.title, events)
            }

            // Automatically hide categories with 0 matches (keep 'All')
            val visibleCategories = allCategories.filter { cat ->
                cat.title.equals("All", ignoreCase = true) || (counts[cat.title] ?: 0) > 0
            }

            val currentSelected = _uiState.value.selectedCategory
            val validSelected = if (visibleCategories.any { it.title.equals(currentSelected, ignoreCase = true) }) {
                currentSelected
            } else {
                "All"
            }

            val allCount = events.size
            val liveCount = events.count { it.isLive }
            val recentCount = events.count { it.isRecent }
            val upcomingCount = events.count { it.isUpcoming }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    categories = visibleCategories,
                    selectedCategory = validSelected,
                    events = events,
                    categoryCounts = counts,
                    allCount = allCount,
                    liveCount = liveCount,
                    recentCount = recentCount,
                    upcomingCount = upcomingCount,
                    error = if (events.isEmpty() && eventsResult.isFailure) "Unable to load sports events. Please retry." else null
                )
            }
            applyFilters()
        }
    }

    fun selectCategory(categoryTitle: String) {
        _uiState.update { it.copy(selectedCategory = categoryTitle) }
        applyFilters()
    }

    fun selectStatus(status: SportStatusFilter) {
        _uiState.update { it.copy(selectedStatus = status) }
        applyFilters()
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    fun setSearchActive(active: Boolean) {
        _uiState.update {
            it.copy(
                isSearchActive = active,
                searchQuery = if (!active) "" else it.searchQuery
            )
        }
        applyFilters()
    }

    private fun applyFilters() {
        val s = _uiState.value

        // 1. Filter by category
        val categoryEvents = if (s.selectedCategory.equals("All", ignoreCase = true)) {
            s.events
        } else {
            s.events.filter { event ->
                val cat = event.eventInfo?.eventCat ?: ""
                val eventName = event.eventInfo?.eventName ?: ""
                val mainCat = event.cat ?: ""
                cat.contains(s.selectedCategory, ignoreCase = true) ||
                    eventName.contains(s.selectedCategory, ignoreCase = true) ||
                    mainCat.contains(s.selectedCategory, ignoreCase = true)
            }
        }

        var list = categoryEvents

        // 2. Filter by status
        list = when (s.selectedStatus) {
            SportStatusFilter.ALL -> list
            SportStatusFilter.LIVE -> list.filter { it.isLive }
            SportStatusFilter.RECENT -> list.filter { it.isRecent }
            SportStatusFilter.UPCOMING -> list.filter { it.isUpcoming }
        }

        // 3. Filter by search query
        if (s.searchQuery.isNotBlank()) {
            val q = s.searchQuery.trim().lowercase()
            list = list.filter { event ->
                event.title.lowercase().contains(q) ||
                    (event.eventInfo?.teamA?.lowercase()?.contains(q) == true) ||
                    (event.eventInfo?.teamB?.lowercase()?.contains(q) == true) ||
                    (event.eventInfo?.eventName?.lowercase()?.contains(q) == true) ||
                    (event.eventInfo?.eventCat?.lowercase()?.contains(q) == true)
            }
        }

        // 4. Sort according to priority rules:
        // - LIVE at the top (with Cricket events prioritized first among live)
        // - UPCOMING in the middle (sorted by nearest start time first)
        // - RECENT / ENDED at the very bottom (sorted by most recently ended first)
        val sortedList = sortEvents(list)

        // Dynamic status counts for the selected category (e.g. All (15), Live (0), etc.)
        val allCount = categoryEvents.size
        val liveCount = categoryEvents.count { it.isLive }
        val recentCount = categoryEvents.count { it.isRecent }
        val upcomingCount = categoryEvents.count { it.isUpcoming }

        _uiState.update {
            it.copy(
                filteredEvents = sortedList,
                allCount = allCount,
                liveCount = liveCount,
                recentCount = recentCount,
                upcomingCount = upcomingCount
            )
        }
    }

    private fun sortEvents(events: List<SportEvent>): List<SportEvent> {
        val live = events.filter { it.isLive }.sortedWith(
            compareByDescending<SportEvent> { it.isCricket }
                .thenBy { it.startDate?.time ?: 0L }
        )

        val upcoming = events.filter { it.isUpcoming }.sortedBy {
            it.startDate?.time ?: Long.MAX_VALUE
        }

        val recent = events.filter { !it.isLive && !it.isUpcoming }.sortedByDescending {
            it.endDate?.time ?: it.startDate?.time ?: 0L
        }

        return live + upcoming + recent
    }

    suspend fun getStreamsForEvent(slug: String): List<SportStream> {
        return repository.getEventStreams(slug).getOrDefault(emptyList())
    }

    override fun onCleared() {
        super.onCleared()
        tickerJob?.cancel()
    }
}
