package com.movie.app.best.data.remote

import com.movie.app.best.data.model.SportCategoriesResponse
import com.movie.app.best.data.model.SportEventsResponse
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query

interface SportsApiService {

    @GET("api/event-categories")
    suspend fun getEventCategories(): SportCategoriesResponse

    @GET("api/events")
    suspend fun getEvents(): SportEventsResponse

    @GET("api/slug-details")
    suspend fun getSlugDetails(
        @Query("slug") slug: String
    ): retrofit2.Response<SportEvent>

    @GET("api/event")
    suspend fun getEventStreams(
        @Query("slug") slug: String
    ): ResponseBody
}
