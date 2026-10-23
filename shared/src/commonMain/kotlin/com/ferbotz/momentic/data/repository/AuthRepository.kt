package com.ferbotz.momentic.data.repository

import com.ferbotz.momentic.data.local.preferences.AppPreferences
import com.ferbotz.momentic.data.mapper.toDomain
import com.ferbotz.momentic.data.model.SessionState
import com.ferbotz.momentic.data.model.SignInResult
import com.ferbotz.momentic.data.model.User
import com.ferbotz.momentic.data.network.UnreadableResponseException
import com.ferbotz.momentic.data.network.datasource.AuthRemoteDataSource
import com.ferbotz.momentic.data.network.dto.GoogleSignInRequest
import com.ferbotz.momentic.data.result.AppResult
import com.ferbotz.momentic.data.result.runApiCatching
import com.ferbotz.momentic.data.session.SessionManager
import com.ferbotz.momentic.data.session.SignOutReason
import kotlinx.coroutines.flow.StateFlow

/**
 * Accounts. Sign-in is needed only to publish — creating is anonymous-first — and signing in adopts
 * the device's drafts in the same call.
 *
 * The Google id token itself comes from the platform's Google sign-in (Credential Manager on Android,
 * GoogleSignIn on iOS), requested for the backend's web client id: the server checks that `aud`
 * equals its `GOOGLE_CLIENT_ID`.
 */
interface AuthRepository {

    val session: StateFlow<SessionState>

    /** `POST /auth/google` with the device's draft token, so its drafts move onto the account. */
    suspend fun signInWithGoogle(idToken: String): AppResult<SignInResult>

    /** `GET /me` — refreshes the cached profile. */
    suspend fun refreshProfile(): AppResult<User>

    /** Signing out is the app discarding its token; there is no logout endpoint. */
    suspend fun signOut()

    /** `DELETE /me`, then the local session ends. */
    suspend fun deleteAccount(): AppResult<Unit>
}

class AuthRepositoryImpl(
    private val remote: AuthRemoteDataSource,
    private val prefs: AppPreferences,
    private val sessionManager: SessionManager,
) : AuthRepository {

    override val session: StateFlow<SessionState> = sessionManager.session

    override suspend fun signInWithGoogle(idToken: String): AppResult<SignInResult> = runApiCatching {
        val response = remote.signInWithGoogle(GoogleSignInRequest(idToken = idToken, draftToken = prefs.draftToken))
        val user = response.user?.toDomain()
            ?: throw UnreadableResponseException(200, IllegalStateException("Sign-in response without a user"))
        sessionManager.start(response.accessToken, user)
        SignInResult(
            user = user,
            claimedCreations = response.claimed?.creations ?: 0,
            claimedImages = response.claimed?.images ?: 0,
        )
    }

    override suspend fun refreshProfile(): AppResult<User> = runApiCatching {
        val user = remote.me().toDomain()
            ?: throw UnreadableResponseException(200, IllegalStateException("Profile without an id"))
        sessionManager.updateUser(user)
        user
    }

    override suspend fun signOut() {
        sessionManager.signOut(SignOutReason.USER_REQUESTED)
    }

    override suspend fun deleteAccount(): AppResult<Unit> = runApiCatching {
        remote.deleteMe()
        sessionManager.signOut(SignOutReason.ACCOUNT_DELETED)
    }
}
