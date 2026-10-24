package com.ferbotz.momentic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.ferbotz.momentic.data.local.preferences.AppPreferences
import com.ferbotz.momentic.data.repository.DraftSessionRepository
import com.ferbotz.momentic.designsystem.ProvideMomenticImageLoader
import com.ferbotz.momentic.feature.gate.RemoteConfigGate
import com.ferbotz.momentic.feature.gate.WelcomeGate
import com.ferbotz.momentic.feature.shell.MomenticShell
import com.ferbotz.momentic.presentation.di.koinGet
import com.ferbotz.momentic.theme.MomenticTheme

/**
 * App root: the theme and image loader, then the gates — remote config (maintenance, a required
 * update, feature switches) and the first-launch welcome — and below them the tabs and every screen.
 */
@Composable
fun App() {
    MomenticTheme {
        ProvideMomenticImageLoader()
        DraftSessionBootstrap()
        RemoteConfigGate {
            WelcomeGate {
                MomenticShell()
            }
        }
    }
}

/**
 * Mints the anonymous draft session on first launch (contract: "call once on first launch and store
 * the token"), off the critical path. If this attempt fails — offline on first launch — the editor
 * mints one before its first save instead.
 */
@Composable
private fun DraftSessionBootstrap() {
    val prefs: AppPreferences = koinGet()
    val draftSessions: DraftSessionRepository = koinGet()
    LaunchedEffect(Unit) {
        if (!prefs.isSignedIn) draftSessions.ensureDraftToken()
    }
}
