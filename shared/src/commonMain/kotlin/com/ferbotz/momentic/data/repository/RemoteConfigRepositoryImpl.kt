package com.ferbotz.momentic.data.repository

import com.ferbotz.momentic.data.local.preferences.AppPreferences
import com.ferbotz.momentic.data.mapper.toDomain
import com.ferbotz.momentic.data.model.RemoteConfigSnapshot
import com.ferbotz.momentic.data.network.apiLogger
import com.ferbotz.momentic.data.network.appJson
import com.ferbotz.momentic.data.network.datasource.RemoteConfigRemoteDataSource
import com.ferbotz.momentic.data.network.dto.RemoteConfigResponseDto
import com.ferbotz.momentic.data.result.AppResult
import com.ferbotz.momentic.data.result.runApiCatching
import com.ferbotz.momentic.data.util.nowEpochMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

/**
 * Cache-then-network remote config. The cached document is restored **synchronously** in the
 * constructor (a small preferences read), so the very first [snapshot] value is already the last
 * session's config rather than the defaults whenever one exists.
 *
 * @param now injectable clock (epoch millis) so staleness can be tested.
 */
class RemoteConfigRepositoryImpl(
    private val remote: RemoteConfigRemoteDataSource,
    private val prefs: AppPreferences,
    private val json: Json = appJson,
    private val now: () -> Long = ::nowEpochMillis,
) : RemoteConfigRepository {

    private val _snapshot = MutableStateFlow(loadCached() ?: RemoteConfigSnapshot.Defaults)
    override val snapshot: StateFlow<RemoteConfigSnapshot> = _snapshot.asStateFlow()

    /** Serialises refreshes so two triggers (cold start + foreground) don't double-fetch. */
    private val refreshMutex = Mutex()

    override suspend fun refresh(): AppResult<RemoteConfigSnapshot> = refreshMutex.withLock {
        runApiCatching {
            val dto = remote.getConfig()
            val fetchedAt = now()
            val fresh = dto.toDomain(fetchedAt, RemoteConfigSnapshot.Source.NETWORK)
            // Cache what we understood (re-encoded), so an unreadable server payload can never
            // poison the next start — it would have failed decoding above instead.
            prefs.cacheRemoteConfig(json.encodeToString(dto), fetchedAt)
            _snapshot.value = fresh
            if (fresh.schemaVersion > RemoteConfigSnapshot.SUPPORTED_SCHEMA_VERSION) {
                apiLogger.w { "Remote config schema ${fresh.schemaVersion} is newer than supported ${RemoteConfigSnapshot.SUPPORTED_SCHEMA_VERSION}; unknown keys ignored" }
            }
            apiLogger.i { "Remote config applied: variant=${fresh.variant} revision=${fresh.revision} ttl=${fresh.ttlSeconds}s" }
            fresh
        }.also { result ->
            if (result is AppResult.Failure) {
                apiLogger.w { "Remote config refresh failed (${result.error}); keeping ${_snapshot.value.source} copy" }
            }
        }
    }

    override suspend fun refreshIfStale(): AppResult<RemoteConfigSnapshot> {
        // A refresh already in flight will land shortly — don't queue a second one behind it.
        if (refreshMutex.isLocked) return AppResult.Success(_snapshot.value)
        if (!_snapshot.value.isStale(now())) return AppResult.Success(_snapshot.value)
        return refresh()
    }

    private fun loadCached(): RemoteConfigSnapshot? {
        val raw = prefs.remoteConfigJson ?: return null
        return runCatching {
            json.decodeFromString<RemoteConfigResponseDto>(raw)
                .toDomain(prefs.remoteConfigFetchedAt, RemoteConfigSnapshot.Source.CACHE)
        }.onFailure { e ->
            apiLogger.w(e) { "Ignoring unreadable cached remote config" }
        }.getOrNull()
    }
}
