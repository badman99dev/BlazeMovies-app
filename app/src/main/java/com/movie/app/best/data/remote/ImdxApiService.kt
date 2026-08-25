package com.movie.app.best.data.remote

import com.movie.app.best.data.model.ImdbNameDetails
import com.movie.app.best.data.model.ImdbRelationshipsResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface ImdxApiService {

    @GET("names/{nameId}")
    suspend fun getNameDetails(
        @Path("nameId") nameId: String
    ): ImdbNameDetails

    @GET("names/{nameId}/relationships")
    suspend fun getRelationships(
        @Path("nameId") nameId: String
    ): ImdbRelationshipsResponse
}
