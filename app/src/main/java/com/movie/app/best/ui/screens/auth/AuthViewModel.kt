package com.movie.app.best.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.movie.app.best.data.model.AppUser
import com.movie.app.best.data.repository.AuthRepository
import com.movie.app.best.data.repository.FirebaseRepository
import com.movie.app.best.data.repository.MyListRefreshState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val user: AppUser? = null,
    val error: String? = null,
    val successMessage: String? = null,
    val needsVerification: Boolean = false,
    val isRegisterMode: Boolean = false,
    val verificationEmailSent: Boolean = false,
    val isLoggingOut: Boolean = false,
    val isForgotPasswordMode: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val firebaseRepository: FirebaseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        refreshAuthState()
        viewModelScope.launch {
            authRepository.authEvent.collect { event ->
                if (!event.initial) {
                    refreshAuthState()
                }
                // FCM token har cold start / login / signup par Firestore mein
                // (authEvent tab fire hota hai jab session restore ready ho — race-free)
                if (event.isLoggedIn) {
                    firebaseRepository.registerFcmToken()
                }
            }
        }
    }

    fun refreshAuthState() {
        val token = authRepository.getToken()
        val user = authRepository.getUser()
        _uiState.value = _uiState.value.copy(
            isLoggedIn = token != null,
            user = user,
            needsVerification = user != null && user.isVerified == 0,
            error = null,
            successMessage = null
        )
    }

    fun toggleMode() {
        _uiState.value = _uiState.value.copy(
            isRegisterMode = !_uiState.value.isRegisterMode,
            error = null,
            successMessage = null
        )
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, successMessage = null)
            val result = authRepository.firebaseSignIn(email, password)
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Login failed"
                )
                return@launch
            }
            val authResult = result.getOrNull()!!
            MyListRefreshState.markStale()
            viewModelScope.launch { firebaseRepository.mergeLocalToFirestore() }
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isLoggedIn = true,
                user = authResult.user,
                needsVerification = authResult.needsVerification,
                successMessage = if (authResult.needsVerification) null else "Welcome back!"
            )
        }
    }

    fun register(email: String, password: String, firstName: String, lastName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, successMessage = null)
            val result = authRepository.firebaseSignUp(email, password, firstName, lastName)
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Registration failed"
                )
                return@launch
            }
            val authResult = result.getOrNull()!!
            MyListRefreshState.markStale()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isLoggedIn = true,
                user = authResult.user,
                needsVerification = true,
                verificationEmailSent = true,
                successMessage = "Account created! Check your email to verify."
            )
        }
    }

    fun loginWithGoogle(googleIdToken: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, successMessage = null)
            val result = authRepository.firebaseSignInWithGoogle(googleIdToken)
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Google sign-in failed"
                )
                return@launch
            }
            val authResult = result.getOrNull()!!
            MyListRefreshState.markStale()
            viewModelScope.launch { firebaseRepository.mergeLocalToFirestore() }
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isLoggedIn = true,
                user = authResult.user,
                needsVerification = authResult.needsVerification,
                successMessage = if (authResult.needsVerification) null else "Welcome!"
            )
        }
    }

    fun resendVerificationEmail() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, successMessage = null)
            val result = authRepository.sendVerificationEmail()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                verificationEmailSent = result.isSuccess,
                successMessage = if (result.isSuccess) "Verification email sent!" else null,
                error = if (result.isFailure) result.exceptionOrNull()?.message else null
            )
        }
    }

    fun autoCheckVerification() {
        viewModelScope.launch {
            val result = authRepository.checkAndSyncVerification()
            if (result.isSuccess) {
                val updatedUser = result.getOrNull()
                if (updatedUser != null) {
                    _uiState.value = _uiState.value.copy(
                        user = updatedUser,
                        needsVerification = updatedUser.isVerified == 0
                    )
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoggingOut = true)
            kotlinx.coroutines.delay(800)
            MyListRefreshState.markStale()
            authRepository.logout()
            _uiState.value = AuthUiState()
        }
    }

    fun getGoogleSignInOptions(): GoogleSignInOptions {
        return authRepository.getGoogleSignInOptions()
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, successMessage = null)
    }

    fun enterForgotPassword() {
        _uiState.value = _uiState.value.copy(
            isForgotPasswordMode = true,
            isRegisterMode = false,
            error = null,
            successMessage = null
        )
    }

    fun exitForgotPassword() {
        _uiState.value = _uiState.value.copy(
            isForgotPasswordMode = false,
            error = null,
            successMessage = null
        )
    }

    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, successMessage = null)
            val result = authRepository.sendPasswordResetEmail(email)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                successMessage = if (result.isSuccess) "Password reset link sent to your email!" else null,
                error = if (result.isFailure) result.exceptionOrNull()?.message else null
            )
        }
    }
}
