package com.ferbotz.momentic.platform

import androidx.compose.runtime.Composable

/**
 * The GoogleSignIn SDK (a Swift package in the Xcode project, called through a small Swift bridge) goes
 * here once the backend's web client id exists; until then, unavailable.
 */
@Composable
actual fun rememberGoogleSignIn(): GoogleSignIn = UnavailableGoogleSignIn
