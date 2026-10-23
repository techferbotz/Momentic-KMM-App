package com.ferbotz.momentic.data.repository

import com.ferbotz.momentic.data.model.RemoteConfig
import com.ferbotz.momentic.data.model.RemoteConfigSnapshot
import com.ferbotz.momentic.data.result.AppResult
import kotlinx.coroutines.flow.StateFlow

/**
 * The app's remote config (`GET /api/v1/config`, HOA protocols 10 and 11).
 *
 * [snapshot] **always** has a value, resolved in this order: the compiled-in defaults → the last
 * good document cached on this device → whatever the backend returned during this process.
 * Consumers read it synchronously via [config] or observe [snapshot]; nothing waits on the network,
 * so config can never be the reason the app doesn't open.
 */
interface RemoteConfigRepository {

    /** The current config plus where it came from. Never null; starts from cache or defaults. */
    val snapshot: StateFlow<RemoteConfigSnapshot>

    /** Shorthand for the current document. */
    val config: RemoteConfig get() = snapshot.value.config

    /**
     * `GET /config`. On success the document is applied to [snapshot] and cached for the next
     * session; on failure the current snapshot is kept and the error is returned. Call it on every
     * cold start. Never throws.
     */
    suspend fun refresh(): AppResult<RemoteConfigSnapshot>

    /**
     * [refresh] only when the current snapshot is older than its `ttlSeconds` (or was never
     * fetched); otherwise returns it as is. Call it when the app returns to the foreground.
     */
    suspend fun refreshIfStale(): AppResult<RemoteConfigSnapshot>
}
