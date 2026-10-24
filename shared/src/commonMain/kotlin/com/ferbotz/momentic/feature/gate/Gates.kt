package com.ferbotz.momentic.feature.gate

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ferbotz.momentic.data.local.preferences.AppPreferences
import com.ferbotz.momentic.feature.shell.LocalUpdateNudge
import com.ferbotz.momentic.feature.shell.UpdateNudge
import com.ferbotz.momentic.presentation.config.LocalRemoteConfig
import com.ferbotz.momentic.presentation.config.RemoteConfigUiState
import com.ferbotz.momentic.presentation.config.RemoteConfigViewModel
import com.ferbotz.momentic.presentation.di.koinGet
import com.ferbotz.momentic.presentation.di.koinViewModel
import com.ferbotz.momentic.presentation.feed.FeedViewModel
import com.ferbotz.momentic.presentation.links.rememberLinkOpener

/**
 * The remote-config gate around the whole UI (HOA protocol 11 §3). The config is fetched on every cold
 * start and re-fetched on foreground once its TTL lapses; until it lands the cached copy (or the
 * compiled-in defaults) applies, so launch never waits on it. Maintenance and a required update
 * *replace* the app; a recommended update becomes a banner on Explore ([LocalUpdateNudge]). Everything
 * inside reads the live document through [LocalRemoteConfig].
 */
@Composable
fun RemoteConfigGate(content: @Composable () -> Unit) {
    val viewModel: RemoteConfigViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val links = rememberLinkOpener()

    // Each return to the foreground re-checks the TTL (the cold-start fetch runs in the VM's init).
    LifecycleStartEffect(viewModel) {
        viewModel.onAppForegrounded()
        onStopOrDispose { }
    }

    CompositionLocalProvider(LocalRemoteConfig provides state.config) {
        val storeUrl = state.storeUrl
        // The store app takes the listing, not an in-app browser; no listing yet means no button.
        val openStore: (() -> Unit)? = if (storeUrl == null) null else ({ links.openOutside(storeUrl) })
        when {
            // The server's own words when it sent some; otherwise the screen's.
            state.isUnderMaintenance -> MaintenanceScreen(
                message = state.maintenanceMessage.takeUnless { it == RemoteConfigUiState.DEFAULT_MAINTENANCE_MESSAGE },
                onRetry = viewModel::retry,
            )
            state.isUpdateRequired -> UpdateRequiredScreen(message = state.updateMessage, onUpdate = openStore)
            else -> {
                val nudge = if (state.showUpdateNudge) {
                    UpdateNudge(message = state.updateMessage, onUpdate = openStore, onDismiss = viewModel::dismissUpdateNudge)
                } else {
                    null
                }
                CompositionLocalProvider(LocalUpdateNudge provides nudge, content = content)
            }
        }
    }
}

/**
 * The first launch's welcome, once per device: "Start creating" lets the app through and is remembered
 * (`AppPreferences.hasSeenWelcome`), so it never shows again — not even after signing out.
 */
@Composable
fun WelcomeGate(content: @Composable () -> Unit) {
    val prefs: AppPreferences = koinGet()
    var seen by remember { mutableStateOf(prefs.hasSeenWelcome) }
    if (seen) {
        content()
        return
    }
    // The feed lends the welcome its art — one experience from each of the first occasions, for variety.
    val feed: FeedViewModel = koinViewModel()
    val state by feed.uiState.collectAsStateWithLifecycle()
    WelcomeScreen(
        templates = state.sections.mapNotNull { it.templates.firstOrNull() },
        onStart = {
            prefs.markWelcomeSeen()
            seen = true
        },
    )
}
