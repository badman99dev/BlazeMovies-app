package com.movie.app.best.data.remote

import com.movie.app.best.data.model.ApiResponse
import com.movie.app.best.data.model.SearchResult
import com.movie.app.best.data.model.SliderResult
import com.movie.app.best.data.model.OffsetResult
import com.movie.app.best.data.model.CategoryOffsetResult
import com.movie.app.best.data.model.ContentModerationResponse
import com.movie.app.best.data.model.BroadcastResponse
import com.movie.app.best.data.model.TvStreamResponse
import com.movie.app.best.data.model.TvStreamCategoriesResponse
import com.movie.app.best.data.model.TvStreamCategory
import com.movie.app.best.data.model.UpdateResponse
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface MovieApiService {

    @GET("notification")
    suspend fun getNotification(): ApiResponse<com.movie.app.best.data.model.AppNotification?>

    @GET("slider")
    suspend fun getSlider(): ApiResponse<SliderResult>

    @GET("content/{slug}")
    suspend fun getContentDetails(@Path("slug") slug: String): ApiResponse<com.movie.app.best.data.model.ContentDetailResponse>

    @GET("search")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("page") page: Int = 1
    ): ApiResponse<SearchResult>

    @GET("category/{slug}")
    suspend fun getCategoryMovies(
        @Path("slug") slug: String,
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 45
    ): ApiResponse<CategoryOffsetResult>

    @GET("categories")
    suspend fun getCategories(): ApiResponse<List<com.movie.app.best.data.model.Category>>

    @POST("comment")
    @FormUrlEncoded
    suspend fun postComment(
        @Header("Authorization") authHeader: String,
        @Field("movie_id") movieId: Int,
        @Field("msg") msg: String
    ): ApiResponse<Map<String, String>>

    @POST("stream-request")
    suspend fun postStreamRequest(
        @Header("Authorization") authHeader: String,
        @Body request: Map<String, @JvmSuppressWildcards Any>
    ): ApiResponse<StreamRequestApiResponse>

    @POST("content-moderation")
    suspend fun postContentModeration(
        @Header("Authorization") authHeader: String,
        @Body request: Map<String, @JvmSuppressWildcards Any>
    ): ApiResponse<ContentModerationApiResponse>

    @GET("latest-uploads")
    suspend fun getLatestUploads(
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 45
    ): ApiResponse<OffsetResult>

    @GET("watchable-content")
    suspend fun getWatchableContent(
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 45
    ): ApiResponse<OffsetResult>

    @GET("trending")
    suspend fun getTrending(
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 45
    ): ApiResponse<OffsetResult>

    @GET("my-feed")
    suspend fun getMyFeed(
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 45
    ): ApiResponse<OffsetResult>

    @GET("my-feed")
    suspend fun recreateMyFeed(
        @Query("action") action: String = "recreate"
    ): ApiResponse<Any?>

    @GET("similar")
    suspend fun getSimilar(
        @Query("imdb_id") imdbId: String
    ): ApiResponse<OffsetResult>

    @GET("broadcast")
    suspend fun getBroadcasts(): ApiResponse<BroadcastResponse>

    // ── Live TV streams (served by v1 API; auto-refreshed every 6h) ──
    @GET("tv-stream")
    suspend fun getTvStreams(
        @Query("category") category: String? = null,
        @Query("limit") limit: Int = 70,
        @Query("offset") offset: Int = 0
    ): ApiResponse<TvStreamResponse>

    @GET("tv-stream/categories")
    suspend fun getTvStreamCategories(): ApiResponse<TvStreamCategoriesResponse>

    @GET("tv-stream")
    suspend fun searchTvStreams(
        @Query("q") query: String,
        @Query("limit") limit: Int = 50
    ): ApiResponse<TvStreamResponse>

    @GET("app-update")
    suspend fun checkForUpdate(@Query("currentCode") currentCode: Int): ApiResponse<UpdateResponse>
}

data class ContentModerationApiResponse(
    val verdict: String? = null,
    val moderation: ContentModerationResponse? = null,
    val previous_moderation: ContentModerationResponse? = null,
    val images_analyzed: Int = 0,
    val debug: List<String> = emptyList()
)

data class StreamRequestApiResponse(
    val already_requested: Boolean = false,
    val movie_id: Int = 0,
    val slug: String = "",
    val request_count: Int = 0,
    val has_stream: Boolean = false,
    val tier: String = "normal_user",
    val skynet: Map<String, @JvmSuppressWildcards Any>? = null,
    val skynet_debug: List<String> = emptyList(),
    val fallback: Boolean = false
)
