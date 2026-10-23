package com.ferbotz.momentic.presentation.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ferbotz.momentic.data.model.SignInResult
import com.ferbotz.momentic.data.repository.AuthRepository
import com.ferbotz.momentic.data.repository.RemoteConfigRepository
import com.ferbotz.momentic.data.result.AppError
import com.ferbotz.momentic.data.result.AppResult
import com.ferbotz.momentic.data.result.ErrorCode
import com.ferbotz.momentic.presentation.util.EventChannel
import com.ferbotz.momentic.presentation.util.toUserMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * What the sign-in screen renders. Sign-in is asked for only at publish — the moment the user has
 * seen their own page — and carries the consent line with both legal links (HOA protocol 12 §3).
 *
 * @param googleSignInEnabled remote config `features.googleLogin`; hide the button when off.
 * @param termsUrl / [privacyUrl] from remote config `links.*`, never hard-coded.
 */
data class SignInUiState(
    val isSigningIn: Boolean = false,
    val googleSignInEnabled: Boolean = true,
    val termsUrl: String = "",
    val privacyUrl: String = "",
    val error: String? = null,
)

sealed interface SignInEvent {
    /** Signed in; the device's drafts moved onto the account in the same call. */
    data class SignedIn(val result: SignInResult) : SignInEvent

    data class OpenUrl(val url: String) : SignInEvent
}

/**
 * Backs sign-in. The screen runs the platform's Google sign-in (Credential Manager on Android,
 * GoogleSignIn on iOS) for the backend's web client id and hands the id token to [onGoogleIdToken];
 * this exchanges it for the app's never-expiring token and adopts the device's drafts.
 */
class SignInViewModel(
    private val auth: AuthRepository,
    private val remoteConfig: RemoteConfigRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        remoteConfig.config.let { config ->
            SignInUiState(
                googleSignInEnabled = config.features.googleLogin,
                termsUrl = config.links.terms,
                privacyUrl = config.links.privacyPolicy,
            )
        },
    )
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    private val _events = EventChannel<SignInEvent>()
    val events: Flow<SignInEvent> = _events.events

    init {
        viewModelScope.launch {
            remoteConfig.snapshot.collect { snapshot ->
                val config = snapshot.config
                _uiState.update {
                    it.copy(
                        googleSignInEnabled = config.features.googleLogin,
                        termsUrl = config.links.terms,
                        privacyUrl = config.links.privacyPolicy,
                    )
                }
            }
        }
    }

    /** Call when the platform Google sign-in starts, so the button shows progress. */
    fun onGoogleSignInStarted() {
        _uiState.update { it.copy(isSigningIn = true, error = null) }
    }

    /** The id token from Google: exchange it with the backend. */
    fun onGoogleIdToken(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSigningIn = true, error = null) }
            when (val result = auth.signInWithGoogle(idToken)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isSigningIn = false) }
                    _events.send(SignInEvent.SignedIn(result.data))
                }
                is AppResult.Failure -> _uiState.update {
                    it.copy(isSigningIn = false, error = result.error.signInMessage())
                }
            }
        }
    }

    /** The platform sign-in was dismissed — not an error. */
    fun onGoogleSignInCancelled() {
        _uiState.update { it.copy(isSigningIn = false) }
    }

    /** The platform sign-in failed before there was a token. */
    fun onGoogleSignInFailed(message: String? = null) {
        _uiState.update {
            it.copy(isSigningIn = false, error = message ?: "Google sign-in didn't work. Please try again.")
        }
    }

    fun openTerms() = _events.send(SignInEvent.OpenUrl(_uiState.value.termsUrl))

    fun openPrivacyPolicy() = _events.send(SignInEvent.OpenUrl(_uiState.value.privacyUrl))

    fun errorShown() {
        _uiState.update { it.copy(error = null) }
    }

    private fun AppError.signInMessage(): String =
        if (hasCode(ErrorCode.INVALID_GOOGLE_TOKEN)) "Google couldn't confirm it's you. Please try again." else toUserMessage()
}
