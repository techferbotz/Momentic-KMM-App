package com.ferbotz.momentic

import com.ferbotz.momentic.data.local.preferences.AppPreferences
import com.ferbotz.momentic.data.repository.DraftSessionRepository
import com.ferbotz.momentic.data.repository.PrincipalGuard
import com.ferbotz.momentic.data.result.AppError
import com.ferbotz.momentic.data.result.AppResult
import com.ferbotz.momentic.data.result.ErrorCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Creation calls need a principal: the device's draft token while signed out. A token the server no
 * longer knows (an unclaimed session purged after 7 days) is replaced once, never looped on.
 */
class PrincipalGuardTest {

    private class FakeDraftSessions(private val prefs: AppPreferences, var failMinting: Boolean = false) : DraftSessionRepository {
        var minted = 0
        var discarded = 0
        override suspend fun ensureDraftToken(): AppResult<String> {
            prefs.draftToken?.let { return AppResult.Success(it) }
            if (failMinting) return AppResult.Failure(AppError.Network("offline"))
            minted++
            return AppResult.Success("draft-$minted").also { prefs.saveDraftToken("draft-$minted") }
        }
        override fun discardDraftToken() {
            discarded++
            prefs.clearDraftToken()
        }
    }

    private val refused = AppResult.Failure(AppError.Api(401, ErrorCode.INVALID_TOKEN, "Unknown draft token"))

    @Test
    fun aDraftSessionExistsBeforeTheFirstCall() = runTest {
        val prefs = AppPreferences(InMemorySettings())
        val sessions = FakeDraftSessions(prefs)
        var tokenSeen: String? = null

        val result = PrincipalGuard(prefs, sessions).run { tokenSeen = prefs.draftToken; AppResult.Success(Unit) }

        assertIs<AppResult.Success<Unit>>(result)
        assertEquals("draft-1", tokenSeen)
        assertEquals(1, sessions.minted)
    }

    @Test
    fun aRefusedDraftTokenIsReplacedAndTheCallRetriedOnce() = runTest {
        val prefs = AppPreferences(InMemorySettings()).apply { saveDraftToken("stale") }
        val sessions = FakeDraftSessions(prefs)
        val tokens = mutableListOf<String?>()

        val result = PrincipalGuard(prefs, sessions).run<String> {
            tokens += prefs.draftToken
            if (tokens.size == 1) refused else AppResult.Success("created")
        }

        assertEquals(AppResult.Success("created"), result)
        assertEquals(listOf<String?>("stale", "draft-1"), tokens)
        assertEquals(1, sessions.discarded)
    }

    @Test
    fun aSecondRefusalIsReturned_notLoopedOn() = runTest {
        val prefs = AppPreferences(InMemorySettings()).apply { saveDraftToken("stale") }
        var calls = 0
        val result = PrincipalGuard(prefs, FakeDraftSessions(prefs)).run<Unit> { calls++; refused }
        assertEquals(refused, result)
        assertEquals(2, calls)
    }

    @Test
    fun signedInCallsNeitherMintNorRetry() = runTest {
        val prefs = AppPreferences(InMemorySettings()).apply { saveAuthToken("jwt") }
        val sessions = FakeDraftSessions(prefs)
        var calls = 0
        PrincipalGuard(prefs, sessions).run<Unit> { calls++; refused }
        assertEquals(1, calls)
        assertEquals(0, sessions.minted)
    }

    @Test
    fun noDraftSessionMeansTheCallIsNeverMade() = runTest {
        val prefs = AppPreferences(InMemorySettings())
        var calls = 0
        val result = PrincipalGuard(prefs, FakeDraftSessions(prefs, failMinting = true)).run { calls++; AppResult.Success(Unit) }
        assertIs<AppResult.Failure>(result)
        assertEquals(0, calls)
    }
}
