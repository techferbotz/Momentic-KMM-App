package com.ferbotz.momentic.presentation.config

import com.ferbotz.momentic.data.model.RemoteConfig
import com.ferbotz.momentic.data.model.UpdateRequirement

/**
 * What the app-root gate renders from the remote config.
 *
 * @param config the live document, provided to every screen through [LocalRemoteConfig].
 * @param isUnderMaintenance `maintenance.enabled` — show [maintenanceMessage] instead of the app.
 * @param updateRequirement how `update.*Build` applies to this build (hard block vs nudge).
 * @param updateMessage optional server copy for the update prompt.
 * @param storeUrl this platform's store listing, or null until the listings exist.
 * @param updateNudgeDismissed the user tapped "Not now" on the recommended-update nudge.
 */
data class RemoteConfigUiState(
    val config: RemoteConfig = RemoteConfig.Default,
    val isUnderMaintenance: Boolean = false,
    val maintenanceMessage: String = DEFAULT_MAINTENANCE_MESSAGE,
    val updateRequirement: UpdateRequirement = UpdateRequirement.NONE,
    val updateMessage: String? = null,
    val storeUrl: String? = null,
    val updateNudgeDismissed: Boolean = false,
) {
    val isUpdateRequired: Boolean
        get() = updateRequirement == UpdateRequirement.REQUIRED

    val showUpdateNudge: Boolean
        get() = updateRequirement == UpdateRequirement.RECOMMENDED && !updateNudgeDismissed

    companion object {
        const val DEFAULT_MAINTENANCE_MESSAGE: String =
            "Momentic is getting a little care right now. Please check back in a few minutes."
    }
}
