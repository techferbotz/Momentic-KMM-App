package com.ferbotz.momentic.presentation.config

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import com.ferbotz.momentic.data.model.RemoteConfig

/**
 * The live [RemoteConfig] for everything under the app root's gate. Screens and components read
 * `LocalRemoteConfig.current.features.*` to hide an entry point the backend has switched off, so a
 * kill switch needs no plumbing through every screen's parameters. Outside the gate (previews,
 * tests) it is the compiled-in [RemoteConfig.Default].
 */
val LocalRemoteConfig: ProvidableCompositionLocal<RemoteConfig> =
    compositionLocalOf { RemoteConfig.Default }
