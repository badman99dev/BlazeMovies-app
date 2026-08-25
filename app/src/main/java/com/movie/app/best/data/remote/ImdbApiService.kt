package com.movie.app.best.data.remote

import com.movie.app.best.data.model.ImdbCertificatesResponse
import com.movie.app.best.data.model.ImdbCreditsResponse
import com.movie.app.best.data.model.ImdbEpisodeResponse
import com.movie.app.best.data.model.ImdbNameDetails
import com.movie.app.best.data.model.ImdbRelationshipsResponse
import com.movie.app.best.data.model.ImdbTitleDetails
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ImdbApiService {

    @GET("names/{nameId}")
    suspend fun getNameDetails(
        @Path("nameId") nameId: String
    ): ImdbNameDetails

    @GET("names/{nameId}/relationships")
    suspend fun getRelationships(
        @Path("nameId") nameId: String
    ): ImdbRelationshipsResponse

    @GET("titles/{titleId}/episodes")
    suspend fun getEpisodes(
        @Path("titleId") titleId: String,
        @Query("season") season: Int? = null
    ): ImdbEpisodeResponse

    @GET("titles/{titleId}")
    suspend fun getTitleDetails(
        @Path("titleId") titleId: String
    ): ImdbTitleDetails

    @GET("titles/{titleId}/certificates")
    suspend fun getCertificates(
        @Path("titleId") titleId: String
    ): ImdbCertificatesResponse

    @GET("titles/{titleId}/credits")
    suspend fun getCredits(
        @Path("titleId") titleId: String,
        @Query("pageSize") pageSize: Int = 20
    ): ImdbCreditsResponse
}
