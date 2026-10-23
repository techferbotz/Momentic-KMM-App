package com.ferbotz.momentic.presentation.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ferbotz.momentic.data.model.RemoteConfigSnapshot
import com.ferbotz.momentic.data.platform.AppBuildInfo
import com.ferbotz.momentic.data.repository.RemoteConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Owns the remote-config lifecycle at the app root (HOA protocol 11):
 *
 * - **every cold start** fetches `GET /config` (in `init`) — the cached/default document is live
 *   meanwhile, so launch never waits on the network;
 * - **returning to the foreground** re-fetches only once the document is older than its
 *   `ttlSeconds` ([onAppForegrounded]);
 * - the resulting [uiState] tells the gate whether to show maintenance, a forced update or a
 *   dismissible nudge, and carries the config every screen reads through [LocalRemoteConfig].
 *
 * Created once at the root and retained across configuration changes, so a rotation is not a new
 * "start".
 */
class RemoteConfigViewModel(
    private val repository: RemoteConfigRepository,
    private val buildInfo: AppBuildInfo,
) : ViewModel() {

    private val nudgeDismissed = MutableStateFlow(false)

    val uiState: StateFlow<RemoteConfigUiState> =
        combine(repository.snapshot, nudgeDismissed) { snapshot, dismissed -> snapshot.toUiState(dismissed) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, repository.snapshot.value.toUiState(false))

    init {
        viewModelScope.launch { repository.refresh() }
    }

    /** Call when the app comes to the foreground: re-fetches only if the TTL has lapsed. */
    fun onAppForegrounded() {
        viewModelScope.launch { repository.refreshIfStale() }
    }

    /** "Try again" on the maintenance screen: an unconditional re-fetch. */
    fun retry() {
        viewModelScope.launch { repository.refresh() }
    }

    fun dismissUpdateNudge() {
        nudgeDismissed.value = true
    }

    private fun RemoteConfigSnapshot.toUiState(nudgeDismissed: Boolean): RemoteConfigUiState =
        RemoteConfigUiState(
            config = config,
            isUnderMaintenance = config.maintenance.enabled,
            maintenanceMessage = config.maintenance.message?.takeIf { it.isNotBlank() }
                ?: RemoteConfigUiState.DEFAULT_MAINTENANCE_MESSAGE,
            updateRequirement = config.update.requirementFor(buildInfo.buildNumber),
            updateMessage = config.update.message?.takeIf { it.isNotBlank() },
            storeUrl = config.update.storeUrlFor(buildInfo.platform)?.takeIf { it.isNotBlank() },
            updateNudgeDismissed = nudgeDismissed,
        )
}
