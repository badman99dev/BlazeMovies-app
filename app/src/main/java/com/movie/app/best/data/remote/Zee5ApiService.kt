package com.movie.app.best.data.remote

import com.movie.app.best.data.model.Zee5CollectionResponse
import com.movie.app.best.data.model.Zee5DetailResponse
import com.movie.app.best.data.model.Zee5PlaybackResponse
import com.movie.app.best.data.model.Zee5SearchResponse
import com.movie.app.best.data.model.Zee5TokensResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface Zee5ApiService {
    
    companion object {
        const val BASE_URL = "https://zee5-no-ads.onrender.com/"
        const val DEFAULT_LANGS = "hi,hr"
    }
    
    // ─── FREE5 Paginated Buckets ───
    @GET("free5")
    suspend fun getFree5(
        @Query("page") page: Int = 0,
        @Query("lang") lang: String = DEFAULT_LANGS
    ): Zee5CollectionResponse
    
    // ─── Collection (TV Shows, Movies, etc.) ───
    @GET("collection/{id}")
    suspend fun getCollection(
        @Path("id") id: String,
        @Query("page") page: Int = 0,
        @Query("limit") limit: Int = 20,
        @Query("languages") languages: String = DEFAULT_LANGS
    ): Zee5CollectionResponse
    
    // ─── Search ───
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("limit") limit: Int = 20,
        @Query("lang") lang: String = DEFAULT_LANGS
    ): Zee5SearchResponse
    
    // ─── Content Details ───
    @GET("details/{id}")
    suspend fun getDetails(
        @Path("id") id: String
    ): Zee5DetailResponse
    
    // ─── Playback URL (with DRM bypass) ───
    @GET("m3u8/{id}")
    suspend fun getPlayback(
        @Path("id") id: String
    ): Zee5PlaybackResponse
    
    // ─── ZEE5 Tokens (for direct GraphQL calls) ───
    @GET("tokens")
    suspend fun getTokens(): Zee5TokensResponse

    // ─── Seasons for a TV Show ───
    @GET("seasons/{showId}")
    suspend fun getSeasons(
        @Path("showId") showId: String
    ): Zee5DetailResponse
    
    // ─── Episodes for a Season ───
    // Note: page is 1-indexed. page=0 and page=1 return the same first page.
    @GET("episodes/{seasonId}")
    suspend fun getEpisodes(
        @Path("seasonId") seasonId: String,
        @Query("limit") limit: Int = 25,
        @Query("page") page: Int = 1
    ): Zee5CollectionResponse
}
