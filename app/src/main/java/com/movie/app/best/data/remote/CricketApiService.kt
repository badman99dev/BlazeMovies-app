package com.movie.app.best.data.remote

import com.movie.app.best.data.model.CrexMatchDetailResponse
import com.movie.app.best.data.model.FindMatchResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface CricketApiService {

    @GET("find-match")
    suspend fun findMatch(
        @Query("team1") team1: String,
        @Query("team2") team2: String,
        @Query("startTime") startTime: String? = null
    ): FindMatchResponse

    @GET("find-match")
    suspend fun findMatchByTeams(
        @Query("team1") team1: String,
        @Query("team2") team2: String
    ): FindMatchResponse

    @GET("matches/{id}")
    suspend fun getMatchDetail(
        @Path("id") id: String,
        @Query("json") json: Boolean = true
    ): CrexMatchDetailResponse
}
