package com.movie.app.best.ui.screens.drawer

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.movie.app.best.data.debug.NetworkMonitor
import com.movie.app.best.data.model.DrawerMenuItem
import com.movie.app.best.data.repository.DrawerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DrawerMenuViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: DrawerRepository
) : ViewModel() {

    val items = repository.items

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        prefetchIcons(repository.items.value)
        refresh()
        viewModelScope.launch {
            NetworkMonitor.refreshCounter.collect { if (it > 0) refresh() }
        }
    }

    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            val ok = repository.refresh()
            if (ok) prefetchIcons(repository.items.value)
            _isRefreshing.value = false
        }
    }

    /**
     * Drawer band ho tab bhi sab icons download ho kar cache me baithe rahe.
     * Coil memory + disk cache dono URL se keyed hain, aur UI bhi wahi
     * memoryCacheKey/diskCacheKey use karta hai — to drawer kholte hi instant.
     */
    private fun prefetchIcons(items: List<DrawerMenuItem>) {
        val urls = items
            .map { it.iconUrl.trim() }
            .filter { it.startsWith("http://", true) || it.startsWith("https://", true) }
            .distinct()

        if (urls.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            val loader = context.imageLoader
            urls.forEach { url ->
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .memoryCacheKey(url)
                    .diskCacheKey(url)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .networkCachePolicy(CachePolicy.ENABLED)
                    .size(ICON_PREFETCH_PX)
                    .build()
                loader.enqueue(request)
            }
        }
    }

    private companion object {
        const val ICON_PREFETCH_PX = 96
    }
}