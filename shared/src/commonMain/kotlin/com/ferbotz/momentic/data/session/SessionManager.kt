package com.ferbotz.momentic.data.session

import com.ferbotz.momentic.data.local.preferences.AppPreferences
import com.ferbotz.momentic.data.mapper.toDomain
import com.ferbotz.momentic.data.mapper.toDto
import com.ferbotz.momentic.data.model.SessionState
import com.ferbotz.momentic.data.model.User
import com.ferbotz.momentic.data.network.apiLogger
import com.ferbotz.momentic.data.network.appJson
import com.ferbotz.momentic.data.network.dto.UserDto
import com.ferbotz.momentic.data.repository.CreationChange
import com.ferbotz.momentic.data.repository.CreationEvents
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/** Why the local session ended. Only [TOKEN_REJECTED] is a surprise to the user. */
enum class SignOutReason {
    /** The user asked to sign out. */
    USER_REQUESTED,

    /** The account was deleted from the app (`DELETE /me`). */
    ACCOUNT_DELETED,

    /**
     * The server rejected the token (a `401` on a request that carried one). Tokens never expire
     * (HOA protocol 04), so this is never a timeout: the signing secret was rotated, or the token
     * came from another environment.
     */
    TOKEN_REJECTED,
}

/**
 * The one owner of the local session: it starts it, holds it as [session], and tears it down.
 *
 * Sign-in returns one access token with no expiry and there is no refresh endpoint, so the app stores
 * it once and sends it forever: **nothing here discards a token on a timer.** The only way back in is
 * the sign-in screen, which makes the single `401` path — discard the token, then send the user
 * through Google sign-in again — the thing that must keep working.
 *
 * Creating stays available throughout: the anonymous draft token is a device identity, untouched by
 * sign-out.
 */
class SessionManager(
    private val prefs: AppPreferences,
    private val creationEvents: CreationEvents,
) {

    private val _session = MutableStateFlow(restore())

    /** Signed in or out, observed by everything that changes with the account. */
    val session: StateFlow<SessionState> = _session.asStateFlow()

    private val _signedOut = MutableSharedFlow<SignOutReason>(extraBufferCapacity = 4)

    /** Announces each teardown, so the UI can return to its signed-out state and say why. */
    val signedOut: SharedFlow<SignOutReason> = _signedOut.asSharedFlow()

    val isSignedIn: Boolean get() = prefs.isSignedIn

    /** Stores the token and profile from a successful sign-in. The owner of every creation just changed. */
    fun start(accessToken: String, user: User) {
        prefs.saveAuthToken(accessToken)
        cache(user)
        _session.value = SessionState.SignedIn(user)
        creationEvents.publish(CreationChange.OwnerChanged)
    }

    /** A fresher profile for the signed-in account (`GET /me`). Ignored when signed out. */
    fun updateUser(user: User) {
        if (!prefs.isSignedIn) return
        cache(user)
        _session.value = SessionState.SignedIn(user)
    }

    /**
     * Forgets the token and the cached profile, then announces [reason]. A no-op when there is no
     * session, so two parallel `401`s produce one sign-out.
     */
    suspend fun signOut(reason: SignOutReason) {
        if (!prefs.isSignedIn) return
        if (reason == SignOutReason.TOKEN_REJECTED) {
            // Rare by design: a spike means a rotated JWT_SECRET or a token from the wrong environment.
            apiLogger.w { "Access token rejected (401); discarding the session" }
        }
        prefs.clearSession()
        _session.value = SessionState.SignedOut
        _signedOut.emit(reason)
        creationEvents.publish(CreationChange.OwnerChanged)
    }

    private fun restore(): SessionState {
        if (!prefs.isSignedIn) return SessionState.SignedOut
        val user = prefs.cachedUserJson?.let { json ->
            runCatching { appJson.decodeFromString<UserDto>(json).toDomain() }.getOrNull()
        }
        return SessionState.SignedIn(user)
    }

    private fun cache(user: User) {
        prefs.cacheUserJson(appJson.encodeToString(user.toDto()))
    }
}
