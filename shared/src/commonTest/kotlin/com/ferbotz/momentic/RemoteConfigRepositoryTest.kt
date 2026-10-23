package com.ferbotz.momentic

import com.ferbotz.momentic.data.local.preferences.AppPreferences
import com.ferbotz.momentic.data.model.RemoteConfig
import com.ferbotz.momentic.data.model.RemoteConfigSnapshot
import com.ferbotz.momentic.data.network.appJson
import com.ferbotz.momentic.data.network.datasource.RemoteConfigRemoteDataSource
import com.ferbotz.momentic.data.repository.RemoteConfigRepositoryImpl
import com.ferbotz.momentic.data.result.AppError
import com.ferbotz.momentic.data.result.AppResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The cache-then-network behaviour HOA protocol 11 asks for: defaults until the first fetch, the
 * fetched document cached for the next session, failures never regress the current snapshot, and
 * the TTL decides whether a foreground re-check hits the network.
 */
class RemoteConfigRepositoryTest {

    /** A scriptable backend that counts `GET /config` calls. */
    private class Backend(var body: String, var status: HttpStatusCode = HttpStatusCode.OK) {
        var calls = 0
        val engine = MockEngine {
            calls++
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(appJson) }
            defaultRequest { url("https://test.local/") }
        }
    }

    private fun repository(backend: Backend, prefs: AppPreferences, now: () -> Long) =
        RemoteConfigRepositoryImpl(RemoteConfigRemoteDataSource(backend.client), prefs, now = now)

    private val maintenanceOn = """
        { "success": true, "data": { "schemaVersion": 1, "variant": "default", "revision": "r1", "ttlSeconds": 60,
          "config": { "maintenance": { "enabled": true, "message": "Back soon" }, "features": { "publishing": false } } } }
    """.trimIndent()

    @Test
    fun coldStartWithoutCache_startsOnDefaults_thenAppliesAndCachesTheFetch() = runTest {
        val backend = Backend(maintenanceOn)
        val prefs = AppPreferences(InMemorySettings())
        val repo = repository(backend, prefs, now = { 5_000L })

        assertEquals(RemoteConfigSnapshot.Defaults, repo.snapshot.value)
        assertEquals(RemoteConfig.Default, repo.config)

        val result = repo.refresh()
        assertIs<AppResult.Success<RemoteConfigSnapshot>>(result)
        assertEquals(1, backend.calls)

        val applied = repo.snapshot.value
        assertEquals(RemoteConfigSnapshot.Source.NETWORK, applied.source)
        assertTrue(applied.config.maintenance.enabled)
        assertEquals("Back soon", applied.config.maintenance.message)
        assertFalse(applied.config.features.publishing)
        assertTrue(applied.config.features.googleLogin, "unmentioned leaves keep their defaults")
        assertEquals("r1", applied.revision)
        assertEquals(5_000L, applied.fetchedAtEpochMillis)

        assertNotNull(prefs.remoteConfigJson, "the document is cached for the next session")
        assertEquals(5_000L, prefs.remoteConfigFetchedAt)
    }

    @Test
    fun nextSession_startsFromTheCachedDocument_notDefaults() = runTest {
        val prefs = AppPreferences(InMemorySettings())
        repository(Backend(maintenanceOn), prefs, now = { 5_000L }).refresh()

        // A fresh process: new repository, same preferences, backend unreachable.
        val offline = Backend("""{ "success": false, "message": "down" }""", HttpStatusCode.BadGateway)
        val next = repository(offline, prefs, now = { 6_000L })

        val restored = next.snapshot.value
        assertEquals(RemoteConfigSnapshot.Source.CACHE, restored.source)
        assertTrue(restored.config.maintenance.enabled)
        assertFalse(restored.config.features.publishing)
        assertEquals("r1", restored.revision)
        assertEquals(5_000L, restored.fetchedAtEpochMillis)
        assertEquals(0, offline.calls, "restoring the cache is synchronous and offline")
    }

    @Test
    fun failedRefresh_keepsTheCurrentSnapshot() = runTest {
        val backend = Backend("""{ "success": false, "message": "boom", "code": "INTERNAL" }""", HttpStatusCode.InternalServerError)
        val repo = repository(backend, AppPreferences(InMemorySettings()), now = { 1L })

        val result = repo.refresh()
        val failure = assertIs<AppResult.Failure>(result)
        val error = assertIs<AppError.Api>(failure.error)
        assertEquals(500, error.status)
        assertEquals(RemoteConfigSnapshot.Defaults, repo.snapshot.value, "config never regresses on failure")
    }

    @Test
    fun malformedResponse_keepsTheCurrentSnapshot() = runTest {
        val backend = Backend("""{ "success": true, "data": { "ttlSeconds": "not-a-number" } }""")
        val repo = repository(backend, AppPreferences(InMemorySettings()), now = { 1L })

        assertIs<AppResult.Failure>(repo.refresh())
        assertEquals(RemoteConfigSnapshot.Defaults, repo.snapshot.value)
    }

    @Test
    fun emptyDataDocument_appliesDefaultsButCountsAsFetched() = runTest {
        val backend = Backend("""{ "success": true, "data": {} }""")
        val repo = repository(backend, AppPreferences(InMemorySettings()), now = { 42L })

        assertIs<AppResult.Success<RemoteConfigSnapshot>>(repo.refresh())
        val applied = repo.snapshot.value
        assertEquals(RemoteConfig.Default, applied.config)
        assertEquals(RemoteConfigSnapshot.Source.NETWORK, applied.source)
        assertEquals(42L, applied.fetchedAtEpochMillis)
    }

    @Test
    fun refreshIfStale_honoursTheTtl() = runTest {
        var now = 0L
        val backend = Backend(maintenanceOn) // ttlSeconds = 60
        val repo = repository(backend, AppPreferences(InMemorySettings()), now = { now })

        repo.refreshIfStale()
        assertEquals(1, backend.calls, "never fetched ⇒ stale ⇒ fetch")

        now = 30_000L
        repo.refreshIfStale()
        assertEquals(1, backend.calls, "inside the TTL ⇒ no network")

        now = 60_000L
        repo.refreshIfStale()
        assertEquals(2, backend.calls, "TTL lapsed ⇒ fetch again")

        repo.refresh()
        assertEquals(3, backend.calls, "refresh() is unconditional (cold start)")
    }
}
