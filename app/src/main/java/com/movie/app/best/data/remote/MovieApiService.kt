package com.movie.app.best.data.remote

import com.movie.app.best.data.model.WasmerApiResponse
import com.movie.app.best.data.model.WasmerSearchResult
import com.movie.app.best.data.model.WasmerSliderResult
import com.movie.app.best.data.model.WasmerOffsetResult
import com.movie.app.best.data.model.WasmerCategoryOffsetResult
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
    suspend fun getNotification(): WasmerApiResponse<com.movie.app.best.data.model.WasmerNotification?>

    @GET("slider")
    suspend fun getSlider(): WasmerApiResponse<WasmerSliderResult>

    @GET("content/{slug}")
    suspend fun getContentDetails(@Path("slug") slug: String): WasmerApiResponse<com.movie.app.best.data.model.WasmerContentDetailResponse>

    @GET("search")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("page") page: Int = 1
    ): WasmerApiResponse<WasmerSearchResult>

    @GET("category/{slug}")
    suspend fun getCategoryMovies(
        @Path("slug") slug: String,
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 45
    ): WasmerApiResponse<WasmerCategoryOffsetResult>

    @GET("categories")
    suspend fun getCategories(): WasmerApiResponse<List<com.movie.app.best.data.model.WasmerCategory>>

    @POST("comment")
    @FormUrlEncoded
    suspend fun postComment(
        @Header("Authorization") authHeader: String,
        @Field("movie_id") movieId: Int,
        @Field("msg") msg: String
    ): WasmerApiResponse<Map<String, String>>

    @POST("stream-request")
    suspend fun postStreamRequest(
        @Header("Authorization") authHeader: String,
        @Body request: Map<String, @JvmSuppressWildcards Any>
    ): WasmerApiResponse<StreamRequestApiResponse>

    @POST("content-moderation")
    suspend fun postContentModeration(
        @Header("Authorization") authHeader: String,
        @Body request: Map<String, @JvmSuppressWildcards Any>
    ): WasmerApiResponse<ContentModerationApiResponse>

    @GET("latest-uploads")
    suspend fun getLatestUploads(
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 45
    ): WasmerApiResponse<WasmerOffsetResult>

    @GET("watchable-content")
    suspend fun getWatchableContent(
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 45
    ): WasmerApiResponse<WasmerOffsetResult>

    @GET("trending")
    suspend fun getTrending(
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 45
    ): WasmerApiResponse<WasmerOffsetResult>

    @GET("my-feed")
    suspend fun getMyFeed(
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 45
    ): WasmerApiResponse<WasmerOffsetResult>

    @GET("my-feed")
    suspend fun recreateMyFeed(
        @Query("action") action: String = "recreate"
    ): WasmerApiResponse<Any?>

    @GET("similar")
    suspend fun getSimilar(
        @Query("imdb_id") imdbId: String
    ): WasmerApiResponse<WasmerOffsetResult>

    @GET("broadcast")
    suspend fun getBroadcasts(): WasmerApiResponse<BroadcastResponse>

    // ── Live TV streams (served by Wasmer Hub v1 API; auto-refreshed every 6h) ──
    @GET("tv-stream")
    suspend fun getTvStreams(
        @Query("category") category: String? = null,
        @Query("limit") limit: Int = 70,
        @Query("offset") offset: Int = 0
    ): WasmerApiResponse<TvStreamResponse>

    @GET("tv-stream/categories")
    suspend fun getTvStreamCategories(): WasmerApiResponse<TvStreamCategoriesResponse>

    @GET("tv-stream")
    suspend fun searchTvStreams(
        @Query("q") query: String,
        @Query("limit") limit: Int = 50
    ): WasmerApiResponse<TvStreamResponse>

    @GET("app-update")
    suspend fun checkForUpdate(@Query("currentCode") currentCode: Int): WasmerApiResponse<UpdateResponse>
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
