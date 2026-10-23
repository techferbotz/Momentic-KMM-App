package com.ferbotz.momentic.presentation.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ferbotz.momentic.data.model.SessionState
import com.ferbotz.momentic.data.repository.PublishRepository
import com.ferbotz.momentic.data.session.SessionManager
import com.ferbotz.momentic.data.session.SignOutReason
import com.ferbotz.momentic.presentation.util.EventChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

/**
 * App-root session state, created once next to the remote-config gate:
 *
 * - [session] — signed in or out, for anything app-wide (a profile badge, the account tab);
 * - [notices] — a line to show when the session ended unexpectedly (a rejected token);
 * - on launch, finishes any paid purchase whose publish wasn't confirmed last time.
 */
class AppSessionViewModel(
    sessionManager: SessionManager,
    private val publishing: PublishRepository,
) : ViewModel() {

    val session: StateFlow<SessionState> = sessionManager.session

    private val _notices = EventChannel<String>()

    /** One-off messages for a snackbar. */
    val notices: Flow<String> = _notices.events

    init {
        viewModelScope.launch {
            sessionManager.signedOut
                .mapNotNull { reason ->
                    if (reason == SignOutReason.TOKEN_REJECTED) "You've been signed out. Sign in again when you publish." else null
                }
                .collect(_notices::send)
        }
        if (sessionManager.isSignedIn && publishing.pendingPurchases().isNotEmpty()) {
            viewModelScope.launch { publishing.completePending() }
        }
    }
}
