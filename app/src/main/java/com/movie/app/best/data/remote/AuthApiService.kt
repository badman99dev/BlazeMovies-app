package com.movie.app.best.data.remote

import com.movie.app.best.data.model.AuthResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface AuthApiService {

    @POST("auth/register")
    suspend fun register(
        @Body request: Map<String, @JvmSuppressWildcards String>
    ): AuthResponse

    @POST("auth/login")
    suspend fun login(
        @Body request: Map<String, @JvmSuppressWildcards String>
    ): AuthResponse

    @POST("auth/verify")
    suspend fun verifyEmail(
        @Body request: Map<String, @JvmSuppressWildcards String>
    ): AuthResponse

    @POST("auth/logout")
    suspend fun logout(
        @Header("X-Auth-Token") authHeader: String
    ): AuthResponse

    @POST("auth/firebase-sync")
    suspend fun firebaseSync(
        @Body request: Map<String, @JvmSuppressWildcards String>
    ): AuthResponse

    @GET("auth/profile")
    suspend fun getProfile(
        @Header("X-Auth-Token") authHeader: String
    ): AuthResponse

    // Device registration handshake — binds FCM token + FID to current user
    // (or marks device "anon" when logged out). Never cached server-side.
    @GET("register")
    suspend fun registerDevice(
        @Query("uid") uid: String?,
        @Query("fid") fid: String,
        @Query("device") device: String,
        @Query("fcm_token") fcmToken: String,
        @Query("anon") anon: Int? = null,
        @Header("X-Auth-Token") authHeader: String? = null
    ): AuthResponse
}
