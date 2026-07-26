package com.movie.app.best.data.repository

import com.movie.app.best.data.model.Resource
import com.movie.app.best.data.model.WasmerApiResponse
import com.movie.app.best.data.model.WasmerContentDetailResponse
import com.movie.app.best.data.model.WasmerNotification
import com.movie.app.best.data.model.WasmerCategory
import com.movie.app.best.data.model.WasmerCategoryOffsetResult
import com.movie.app.best.data.model.WasmerOffsetResult
import com.movie.app.best.data.model.WasmerSearchResult
import com.movie.app.best.data.model.WasmerSliderResult
import com.movie.app.best.data.model.BroadcastResponse
import com.movie.app.best.data.model.LiveChannel
import com.movie.app.best.data.model.TvStreamResponse
import com.movie.app.best.data.model.TvStreamCategoriesResponse
import com.movie.app.best.data.model.UnifiedChannel
import com.movie.app.best.data.model.UpdateResponse
import com.movie.app.best.data.model.toUnified
import com.movie.app.best.data.remote.MovieApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MovieRepository @Inject constructor(
    private val apiService: MovieApiService
) {
    private suspend fun <T> safeApiCall(call: suspend () -> WasmerApiResponse<T>): Resource<T> {
        return try {
            val response = call()
            if (response.status == "success" && response.data != null) {
                Resource.Success(response.data)
            } else {
                Resource.Error(response.message ?: "Unknown error")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    fun getNotification(): Flow<Resource<WasmerNotification?>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.getNotification() })
    }

    fun getSlider(): Flow<Resource<WasmerSliderResult>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.getSlider() })
    }

    fun getContentDetails(slug: String): Flow<Resource<WasmerContentDetailResponse>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.getContentDetails(slug) })
    }

    fun searchMovies(query: String, page: Int = 1): Flow<Resource<WasmerSearchResult>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.searchMovies(query, page) })
    }

    fun getLatestUploads(offset: Int = 0, limit: Int = 45): Flow<Resource<WasmerOffsetResult>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.getLatestUploads(offset, limit) })
    }

    fun getWatchableContent(offset: Int = 0, limit: Int = 45): Flow<Resource<WasmerOffsetResult>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.getWatchableContent(offset, limit) })
    }

    fun getTrending(offset: Int = 0, limit: Int = 45): Flow<Resource<WasmerOffsetResult>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.getTrending(offset, limit) })
    }

    fun getMyFeed(offset: Int = 0, limit: Int = 45): Flow<Resource<WasmerOffsetResult>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.getMyFeed(offset, limit) })
    }

    suspend fun recreateMyFeed() {
        try { apiService.recreateMyFeed() } catch (_: Exception) {}
    }

    fun getSimilar(imdbId: String): Flow<Resource<WasmerOffsetResult>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.getSimilar(imdbId) })
    }

    fun getCategoryMovies(slug: String, offset: Int = 0, limit: Int = 45): Flow<Resource<WasmerCategoryOffsetResult>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.getCategoryMovies(slug, offset, limit) })
    }

    fun getCategories(): Flow<Resource<List<WasmerCategory>>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.getCategories() })
    }

    fun getBroadcasts(): Flow<Resource<List<LiveChannel>>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getBroadcasts()
            if (response.status == "success" && response.data != null) {
                Resource.Success(response.data.channels)
            } else {
                Resource.Error(response.message ?: "Unknown error")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }.also { emit(it) }
    }

    /** Broadcast channels normalized to UnifiedChannel (premium, third-party source) */
    fun getBroadcastsUnified(): Flow<Resource<List<UnifiedChannel>>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getBroadcasts()
            if (response.status == "success" && response.data != null) {
                Resource.Success(response.data.channels.mapIndexed { idx, ch -> ch.toUnified(idx) })
            } else {
                Resource.Error(response.message ?: "Unknown error")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }.also { emit(it) }
    }

    /** IPTV Indian channels (iptv-india repo) — paginated, category-filtered */
    fun getTvStreams(
        category: String? = null,
        offset: Int = 0,
        limit: Int = 70
    ): Flow<Resource<TvStreamResponse>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.getTvStreams(category, limit, offset) })
    }

    /** IPTV Indian channels normalized to UnifiedChannel (paginated) — keys are globally unique */
    fun getTvStreamsUnified(
        category: String? = null,
        offset: Int = 0,
        limit: Int = 70
    ): Flow<Resource<List<UnifiedChannel>>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getTvStreams(category, limit, offset)
            if (response.status == "success" && response.data != null) {
                Resource.Success(
                    response.data.channels.mapIndexed { idx, ch -> ch.toUnified(idx, offset) }
                )
            } else {
                Resource.Error(response.message ?: "Unknown error")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }.also { emit(it) }
    }

    /** Categories for IPTV channels (News/Entertainment/Movies etc.) */
    fun getTvStreamCategories(): Flow<Resource<TvStreamCategoriesResponse>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.getTvStreamCategories() })
    }

    /** Search IPTV channels by name/category — single-shot (no pagination) */
    fun searchTvStreams(query: String, limit: Int = 50): Flow<Resource<List<UnifiedChannel>>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.searchTvStreams(query, limit)
            if (response.status == "success" && response.data != null) {
                Resource.Success(
                    response.data.channels.mapIndexed { idx, ch -> ch.toUnified(idx) }
                )
            } else {
                Resource.Error(response.message ?: "Unknown error")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }.also { emit(it) }
    }

    suspend fun checkForUpdate(currentCode: Int): Resource<UpdateResponse> {
        return try {
            val response = apiService.checkForUpdate(currentCode)
            if (response.status == "success" && response.data != null) {
                Resource.Success(response.data)
            } else {
                Resource.Error(response.message ?: "Unknown error")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    fun postComment(authHeader: String, movieId: Int, msg: String): Flow<Resource<Map<String, String>>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.postComment(authHeader, movieId, msg) })
    }

    suspend fun submitStreamRequest(authHeader: String, slug: String): com.movie.app.best.data.remote.StreamRequestApiResponse {
        val response = apiService.postStreamRequest(authHeader, mapOf("slug" to slug))
        return response.data ?: com.movie.app.best.data.remote.StreamRequestApiResponse()
    }

    suspend fun submitContentModeration(authHeader: String, movieId: Int, reportType: String, reason: String): com.movie.app.best.data.remote.ContentModerationApiResponse {
        val response = apiService.postContentModeration(authHeader, mapOf(
            "movie_id" to movieId,
            "report_type" to reportType,
            "reason" to reason
        ))
        return response.data ?: com.movie.app.best.data.remote.ContentModerationApiResponse()
    }

    suspend fun submitModeratorVerdict(authHeader: String, movieId: Int, reportType: String, reason: String, verdict: Map<String, @JvmSuppressWildcards Any>): com.movie.app.best.data.remote.ContentModerationApiResponse {
        val response = apiService.postContentModeration(authHeader, mapOf(
            "movie_id" to movieId,
            "report_type" to reportType,
            "reason" to reason,
            "verdict" to verdict
        ))
        return response.data ?: com.movie.app.best.data.remote.ContentModerationApiResponse()
    }
}
