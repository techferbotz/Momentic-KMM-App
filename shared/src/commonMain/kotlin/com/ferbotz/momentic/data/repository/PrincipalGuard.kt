package com.ferbotz.momentic.data.repository

import com.ferbotz.momentic.data.local.preferences.AppPreferences
import com.ferbotz.momentic.data.network.apiLogger
import com.ferbotz.momentic.data.result.AppError
import com.ferbotz.momentic.data.result.AppResult
import com.ferbotz.momentic.data.result.ErrorCode

/**
 * Runs calls that need a principal — the creation and photo endpoints accept either the signed-in user
 * or the device's draft token.
 *
 * While signed out it makes sure a draft token exists before the first call (the launch attempt may
 * have been offline). If the server then refuses the stored token — an unclaimed draft session is
 * purged after 7 days — it forgets it, mints a fresh one and retries **once**. A refused call created
 * nothing, so the retry can't duplicate anything.
 */
class PrincipalGuard(
    private val prefs: AppPreferences,
    private val draftSessions: DraftSessionRepository,
) {

    suspend fun <T> run(block: suspend () -> AppResult<T>): AppResult<T> {
        if (!prefs.isSignedIn) {
            val ensured = draftSessions.ensureDraftToken()
            if (ensured is AppResult.Failure) return ensured
        }
        val first = block()
        if (first is AppResult.Failure && !prefs.isSignedIn && first.error.isDraftTokenRefused()) {
            apiLogger.w { "Draft token refused; minting a new draft session and retrying once" }
            draftSessions.discardDraftToken()
            val ensured = draftSessions.ensureDraftToken()
            if (ensured is AppResult.Failure) return ensured
            return block()
        }
        return first
    }

    /** A `401` on a device-scoped call means the server no longer knows the draft token. */
    private fun AppError.isDraftTokenRefused(): Boolean =
        this is AppError.Api && status == 401 && (code == ErrorCode.INVALID_TOKEN || code == ErrorCode.AUTH_REQUIRED)
}
