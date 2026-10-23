package com.ferbotz.momentic.presentation.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ferbotz.momentic.data.billing.StoreBilling
import com.ferbotz.momentic.data.model.RemoteConfig
import com.ferbotz.momentic.data.model.SessionState
import com.ferbotz.momentic.data.platform.AppBuildInfo
import com.ferbotz.momentic.data.repository.AuthRepository
import com.ferbotz.momentic.data.repository.RemoteConfigRepository
import com.ferbotz.momentic.data.result.AppResult
import com.ferbotz.momentic.data.util.isEmailAddress
import com.ferbotz.momentic.presentation.util.EventChannel
import com.ferbotz.momentic.presentation.util.toUserMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * What the account / settings screen renders.
 *
 * @param links Privacy Policy, Terms, Delete account and the support address — from remote config,
 *   never hard-coded (HOA protocol 12 §3), so the pages can move without a release.
 * @param appVersion "1.0 (1)" — for the footer and support emails.
 */
data class AccountUiState(
    val session: SessionState = SessionState.SignedOut,
    val links: RemoteConfig.Links = RemoteConfig.Links(),
    val appVersion: String = "",
    val isSigningOut: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val message: String? = null,
)

sealed interface AccountEvent {
    /** Open a page in the in-app browser. */
    data class OpenUrl(val url: String) : AccountEvent

    /** Start an email in the mail app. */
    data class ComposeEmail(val address: String, val subject: String, val body: String) : AccountEvent

    data object SignedOut : AccountEvent

    data object AccountDeleted : AccountEvent
}

/**
 * Backs the account screen: profile, the legal pages, support, sign-out and account deletion (Google
 * Play requires deletion in the app; the `links.deleteAccount` page covers users without it).
 */
class AccountViewModel(
    private val auth: AuthRepository,
    private val remoteConfig: RemoteConfigRepository,
    private val billing: StoreBilling,
    buildInfo: AppBuildInfo,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AccountUiState(
            session = auth.session.value,
            links = remoteConfig.config.links,
            appVersion = "${buildInfo.versionName} (${buildInfo.buildNumber})",
        ),
    )
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    private val _events = EventChannel<AccountEvent>()
    val events: Flow<AccountEvent> = _events.events

    private val platform = buildInfo.platform.wire

    init {
        viewModelScope.launch { auth.session.collect { session -> _uiState.update { it.copy(session = session) } } }
        viewModelScope.launch { remoteConfig.snapshot.collect { snapshot -> _uiState.update { it.copy(links = snapshot.config.links) } } }
        if (auth.session.value is SessionState.SignedIn) refreshProfile()
    }

    fun refreshProfile() {
        viewModelScope.launch { auth.refreshProfile() }
    }

    fun openPrivacyPolicy() = _events.send(AccountEvent.OpenUrl(_uiState.value.links.privacyPolicy))

    fun openTerms() = _events.send(AccountEvent.OpenUrl(_uiState.value.links.terms))

    /** The web page explaining deletion — for the store listing and users without the app. */
    fun openDeleteAccountPage() = _events.send(AccountEvent.OpenUrl(_uiState.value.links.deleteAccount))

    /** An email to `links.supportEmail`, with the build and config revision filled in for support. */
    fun contactSupport() {
        val address = _uiState.value.links.supportEmail.trim()
        if (!isEmailAddress(address)) return
        val snapshot = remoteConfig.snapshot.value
        _events.send(
            AccountEvent.ComposeEmail(
                address = address,
                subject = "Momentic support",
                body = "\r\n\r\n—\r\nMomentic ${_uiState.value.appVersion} · $platform · config ${snapshot.variant}/${snapshot.revision}",
            ),
        )
    }

    fun signOut() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSigningOut = true) }
            auth.signOut()
            billing.reset()
            _uiState.update { it.copy(isSigningOut = false) }
            _events.send(AccountEvent.SignedOut)
        }
    }

    /** `DELETE /me`: soft-deletes the account and ends the session. Confirm with the user first. */
    fun deleteAccount() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingAccount = true) }
            when (val result = auth.deleteAccount()) {
                is AppResult.Success -> {
                    billing.reset()
                    _uiState.update { it.copy(isDeletingAccount = false) }
                    _events.send(AccountEvent.AccountDeleted)
                }
                is AppResult.Failure -> _uiState.update {
                    it.copy(isDeletingAccount = false, message = result.error.toUserMessage())
                }
            }
        }
    }

    fun messageShown() {
        _uiState.update { it.copy(message = null) }
    }
}
