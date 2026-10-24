package com.ferbotz.momentic.platform

import androidx.compose.runtime.Composable

/**
 * Credential Manager (`androidx.credentials` + `googleid`'s `GetSignInWithGoogleOption`) goes here once the
 * backend's web client id exists; until then, unavailable.
 */
@Composable
actual fun rememberGoogleSignIn(): GoogleSignIn = UnavailableGoogleSignIn
