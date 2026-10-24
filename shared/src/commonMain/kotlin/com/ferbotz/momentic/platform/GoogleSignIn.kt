package com.ferbotz.momentic.platform

import androidx.compose.runtime.Composable

/**
 * The platform's Google sign-in, for the backend's **web** client id: it hands back the ID token the
 * backend verifies (`POST /auth/google`). Android: Credential Manager with Sign in with Google; iOS: the
 * GoogleSignIn SDK.
 */
interface GoogleSignIn {

    /** False while this build has no Google sign-in — until the web client id (and, on iOS, the SDK) are in. */
    val isAvailable: Boolean

    fun signIn(onIdToken: (String) -> Unit, onCancelled: () -> Unit, onFailed: (message: String?) -> Unit)
}

/** Remembers the platform [GoogleSignIn]. */
@Composable
expect fun rememberGoogleSignIn(): GoogleSignIn

/** What both platforms answer until the client id arrives: not available, and a sign-in attempt says so. */
internal object UnavailableGoogleSignIn : GoogleSignIn {
    override val isAvailable: Boolean = false

    override fun signIn(onIdToken: (String) -> Unit, onCancelled: () -> Unit, onFailed: (message: String?) -> Unit) =
        onFailed("Google sign-in isn't set up in this build yet.")
}
