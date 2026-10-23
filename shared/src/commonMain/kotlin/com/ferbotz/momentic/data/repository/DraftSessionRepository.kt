package com.ferbotz.momentic.data.repository

import com.ferbotz.momentic.data.local.preferences.AppPreferences
import com.ferbotz.momentic.data.network.apiLogger
import com.ferbotz.momentic.data.network.datasource.DraftSessionRemoteDataSource
import com.ferbotz.momentic.data.result.AppResult
import com.ferbotz.momentic.data.result.runApiCatching
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The anonymous device principal. Creation is anonymous-first: a device gets a draft token on first
 * launch and owns everything it makes; sign-in is needed only to publish, and the drafts move onto
 * the account at that moment (contract BE-001).
 */
interface DraftSessionRepository {

    /**
     * The device's draft token, minting one (`POST /draft-sessions`) when there is none yet. Called
     * on launch, and again before any creation call in case the launch attempt was offline.
     */
    suspend fun ensureDraftToken(): AppResult<String>

    /**
     * Forgets a token the server no longer accepts (an unclaimed session is purged after 7 days), so
     * the next [ensureDraftToken] mints a fresh one.
     */
    fun discardDraftToken()
}

class DraftSessionRepositoryImpl(
    private val remote: DraftSessionRemoteDataSource,
    private val prefs: AppPreferences,
) : DraftSessionRepository {

    /** Launch and a first save can race; the endpoint allows only 10 calls an hour, so mint once. */
    private val mintLock = Mutex()

    override suspend fun ensureDraftToken(): AppResult<String> = mintLock.withLock {
        prefs.draftToken?.let { return@withLock AppResult.Success(it) }
        runApiCatching {
            remote.createDraftSession().draftToken.also { token ->
                prefs.saveDraftToken(token)
                apiLogger.i { "Draft session created" }
            }
        }
    }

    override fun discardDraftToken() {
        prefs.clearDraftToken()
    }
}
