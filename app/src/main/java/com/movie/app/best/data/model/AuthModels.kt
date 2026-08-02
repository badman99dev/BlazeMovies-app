package com.movie.app.best.data.model

import com.google.gson.annotations.SerializedName

data class AppUser(
    val id: Int,
    val username: String,
    val email: String,
    val role: String,
    @SerializedName("avatar_url") val avatarUrl: String?,
    @SerializedName("is_verified") val isVerified: Int,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("first_name") val firstName: String? = null,
    @SerializedName("last_name") val lastName: String? = null,
    val tier: String = "normal_user"
)

data class AuthResponse(
    val status: String,
    val data: AuthData?,
    val message: String?
)

data class AuthData(
    val token: String,
    val user: AppUser,
    @SerializedName("verification_required") val verificationRequired: Boolean? = null,
    @SerializedName("verification_hint") val verificationHint: String? = null
)

data class FirebaseSignUpRequest(
    val email: String,
    val password: String,
    @SerializedName("returnSecureToken") val returnSecureToken: Boolean = true
)

data class FirebaseSignInRequest(
    val email: String,
    val password: String,
    @SerializedName("returnSecureToken") val returnSecureToken: Boolean = true
)

data class FirebaseSendOobRequest(
    @SerializedName("requestType") val requestType: String = "VERIFY_EMAIL",
    @SerializedName("idToken") val idToken: String
)

data class FirebaseAuthResponse(
    @SerializedName("idToken") val idToken: String,
    val email: String,
    val refreshToken: String,
    @SerializedName("expiresIn") val expiresIn: String,
    @SerializedName("localId") val localId: String,
    @SerializedName("emailVerified") val emailVerified: Boolean? = null
)

data class FirebaseOobResponse(
    val email: String
)

data class FirebaseErrorResponse(
    val error: FirebaseError?
)

data class FirebaseError(
    val message: String,
    val code: Int?
)
