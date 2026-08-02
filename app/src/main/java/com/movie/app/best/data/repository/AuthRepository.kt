package com.movie.app.best.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.gson.Gson
import com.movie.app.best.data.debug.NetworkLogger
import com.movie.app.best.data.model.AppUser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_auth", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val googleWebClientId = "191496010275-1el0keetbtndhei2sn3ots6le8bta6ln.apps.googleusercontent.com"
    private val firebaseAuth = FirebaseAuth.getInstance()

    private val _authEvent = MutableStateFlow(AuthEvent(initial = true))
    val authEvent: StateFlow<AuthEvent> = _authEvent.asStateFlow()

    data class AuthEvent(
        val isLoggedIn: Boolean = false,
        val user: AppUser? = null,
        val timestamp: Long = System.currentTimeMillis(),
        val initial: Boolean = false
    )

    init {
        firebaseAuth.addAuthStateListener { auth ->
            val user = auth.currentUser
            if (user != null) {
                NetworkLogger.logAction("AUTH_STATE", "uid=${user.uid} email=${user.email} verified=${user.isEmailVerified} provider=${user.providerData.map { it.providerId }}")
                _authEvent.value = AuthEvent(isLoggedIn = true, user = getUser())
            } else {
                NetworkLogger.logAction("AUTH_STATE", "signed_out")
                _authEvent.value = AuthEvent(isLoggedIn = false, user = null)
            }
        }
    }

    fun getToken(): String? = prefs.getString("token", null)

    fun getUser(): AppUser? {
        val json = prefs.getString("user", null) ?: return null
        return try { gson.fromJson(json, AppUser::class.java) } catch (_: Exception) { null }
    }

    fun isLoggedIn(): Boolean = getToken() != null

    fun isVerified(): Boolean = getUser()?.isVerified == 1

    private fun saveAuth(token: String, user: AppUser) {
        prefs.edit()
            .putString("token", token)
            .putString("user", gson.toJson(user))
            .apply()
        _authEvent.value = AuthEvent(isLoggedIn = true, user = user)
    }

    fun clearAuth() {
        prefs.edit().clear().apply()
        _authEvent.value = AuthEvent(isLoggedIn = false, user = null)
    }

    private fun saveFirebaseIdToken(token: String?) {
        if (token != null) prefs.edit().putString("firebase_id_token", token).apply()
        else prefs.edit().remove("firebase_id_token").apply()
    }

    fun getGoogleSignInOptions(): GoogleSignInOptions {
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(googleWebClientId)
            .requestEmail()
            .build()
    }

    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        return GoogleSignIn.getClient(context, getGoogleSignInOptions())
    }

    data class AuthResult(
        val token: String,
        val user: AppUser,
        val needsVerification: Boolean
    )

    suspend fun firebaseSignUp(email: String, password: String, firstName: String, lastName: String): Result<AuthResult> = withContext(Dispatchers.IO) {
        NetworkLogger.logAction("AUTH", "signUp attempt: email=$email")
        try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: return@withContext Result.failure(Exception("Sign up failed"))
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName("$firstName $lastName")
                .build()
            user.updateProfile(profileUpdates).await()
            user.sendEmailVerification().await()
            val idToken = user.getIdToken(false).await().token
                ?: return@withContext Result.failure(Exception("Failed to get token"))
            saveFirebaseIdToken(idToken)
            val appUser = AppUser(
                id = 0, username = email.substringBefore("@"), email = email,
                role = "user", avatarUrl = "", isVerified = 0, createdAt = "",
                firstName = firstName, lastName = lastName
            )
            saveAuth(idToken, appUser)
            NetworkLogger.logAction("AUTH", "signUp success: uid=${user.uid}")
            Result.success(AuthResult(token = idToken, user = appUser, needsVerification = true))
        } catch (e: Exception) {
            NetworkLogger.logAction("AUTH", "signUp failed: ${e.message}")
            val msg = when {
                e.message?.contains("EMAIL_EXISTS") == true -> "This email is already registered"
                e.message?.contains("WEAK_PASSWORD") == true -> "Password is too weak (min 6 characters)"
                e.message?.contains("INVALID_EMAIL") == true -> "Invalid email address"
                else -> e.message ?: "Sign up failed"
            }
            Result.failure(Exception(msg))
        }
    }

    suspend fun firebaseSignIn(email: String, password: String): Result<AuthResult> = withContext(Dispatchers.IO) {
        NetworkLogger.logAction("AUTH", "signIn attempt: email=$email")
        try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: return@withContext Result.failure(Exception("Login failed"))
            val idToken = user.getIdToken(false).await().token
                ?: return@withContext Result.failure(Exception("Failed to get token"))
            saveFirebaseIdToken(idToken)
            val displayName = user.displayName ?: ""
            val nameParts = displayName.split(" ", limit = 2)
            val appUser = AppUser(
                id = 0, username = displayName.ifBlank { email.substringBefore("@") }, email = email,
                role = "user", avatarUrl = "",
                isVerified = if (user.isEmailVerified) 1 else 0, createdAt = "",
                firstName = nameParts.getOrNull(0), lastName = nameParts.getOrNull(1)
            )
            saveAuth(idToken, appUser)
            NetworkLogger.logAction("AUTH", "signIn success: uid=${user.uid} verified=${user.isEmailVerified}")
            Result.success(AuthResult(token = idToken, user = appUser, needsVerification = !user.isEmailVerified))
        } catch (e: Exception) {
            NetworkLogger.logAction("AUTH", "signIn failed: ${e.message}")
            val msg = when {
                e.message?.contains("EMAIL_NOT_FOUND") == true -> "No account found with this email"
                e.message?.contains("INVALID_PASSWORD") == true -> "Wrong password"
                e.message?.contains("USER_DISABLED") == true -> "Account disabled"
                else -> e.message ?: "Login failed"
            }
            Result.failure(Exception(msg))
        }
    }

    suspend fun firebaseSignInWithGoogle(googleIdToken: String): Result<AuthResult> = withContext(Dispatchers.IO) {
        NetworkLogger.logAction("AUTH", "googleSignIn attempt: idToken=${googleIdToken.take(20)}...")
        try {
            val credential = GoogleAuthProvider.getCredential(googleIdToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            val user = result.user ?: return@withContext Result.failure(Exception("Google sign-in failed"))
            val idToken = user.getIdToken(false).await().token
                ?: return@withContext Result.failure(Exception("Failed to get token"))
            saveFirebaseIdToken(idToken)
            val displayName = user.displayName ?: ""
            val nameParts = displayName.split(" ", limit = 2)
            val appUser = AppUser(
                id = 0, username = displayName.ifBlank { user.email?.substringBefore("@") ?: "" },
                email = user.email ?: "",
                role = "user", avatarUrl = user.photoUrl?.toString(),
                isVerified = if (user.isEmailVerified) 1 else 0, createdAt = "",
                firstName = nameParts.getOrNull(0),
                lastName = nameParts.getOrNull(1)
            )
            saveAuth(idToken, appUser)
            NetworkLogger.logAction("AUTH", "googleSignIn success: uid=${user.uid} email=${user.email} name=${user.displayName}")
            Result.success(AuthResult(token = idToken, user = appUser, needsVerification = false))
        } catch (e: Exception) {
            NetworkLogger.logAction("AUTH", "googleSignIn failed: ${e.message}")
            Result.failure(Exception(e.message ?: "Google sign-in failed"))
        }
    }

    suspend fun sendVerificationEmail(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = firebaseAuth.currentUser ?: return@withContext Result.failure(Exception("Not logged in"))
            user.sendEmailVerification().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Failed to send verification email"))
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        NetworkLogger.logAction("AUTH", "passwordReset attempt: email=$email")
        try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            NetworkLogger.logAction("AUTH", "passwordReset email sent")
            Result.success(Unit)
        } catch (e: Exception) {
            NetworkLogger.logAction("AUTH", "passwordReset failed: ${e.message}")
            val msg = when {
                e.message?.contains("USER_NOT_FOUND") == true -> "No account found with this email"
                e.message?.contains("INVALID_EMAIL") == true -> "Invalid email address"
                else -> e.message ?: "Failed to send reset email"
            }
            Result.failure(Exception(msg))
        }
    }

    suspend fun checkAndSyncVerification(): Result<AppUser?> = withContext(Dispatchers.IO) {
        try {
            val user = firebaseAuth.currentUser ?: return@withContext Result.success(getUser())
            user.reload().await()
            val current = getUser()
            if (user.isEmailVerified && current != null && current.isVerified == 0) {
                val updated = current.copy(isVerified = 1)
                prefs.edit().putString("user", gson.toJson(updated)).apply()
                return@withContext Result.success(updated)
            }
            Result.success(getUser())
        } catch (e: Exception) {
            Result.success(getUser())
        }
    }

    suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firebaseAuth.signOut()
            clearAuth()
            Result.success(Unit)
        } catch (e: Exception) {
            clearAuth()
            Result.failure(e)
        }
    }
}
