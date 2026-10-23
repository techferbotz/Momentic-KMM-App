package com.ferbotz.momentic

import com.ferbotz.momentic.data.local.preferences.AppPreferences
import com.ferbotz.momentic.data.network.appJson
import com.ferbotz.momentic.data.network.datasource.PublishRemoteDataSource
import com.ferbotz.momentic.data.repository.CreationEvents
import com.ferbotz.momentic.data.repository.PublishKind
import com.ferbotz.momentic.data.repository.PublishRepositoryImpl
import com.ferbotz.momentic.data.repository.PublishResult
import com.ferbotz.momentic.data.result.AppError
import com.ferbotz.momentic.data.result.AppResult
import com.ferbotz.momentic.data.result.ErrorCode
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * A paid purchase must end in a live link or a definitive refusal — never in a lost purchase and never
 * in a second charge. Retries reuse the same transaction (the endpoints are idempotent per transaction).
 */
class PublishRepositoryTest {

    private val published = """
        { "success": true, "data": { "creation": { "id": "c1", "templateId": "greeting_open_letter", "status": "PUBLISHED",
          "shareCode": "aK3x9Qm2", "shareUrl": "https://momentic.ferbotz.com/aK3x9Qm2" },
          "shareCode": "aK3x9Qm2", "shareUrl": "https://momentic.ferbotz.com/aK3x9Qm2",
          "expiresAt": "2026-10-30T10:00:00Z", "alreadyPublished": false } }
    """.trimIndent()

    private fun failure(code: String) = """{ "success": false, "message": "nope", "code": "$code" }"""

    /** Answers with each scripted response in turn (the last one repeats), recording request bodies. */
    private class Backend(vararg val script: Pair<HttpStatusCode, String?>) {
        val bodies = mutableListOf<String>()
        private var call = 0
        val client = HttpClient(
            MockEngine { request ->
                bodies += (request.body as TextContent).text
                val (status, body) = script[minOf(call++, script.lastIndex)]
                if (body == null) throw IOException("connection reset")
                respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
            },
        ) {
            install(ContentNegotiation) { json(appJson) }
            defaultRequest { url("https://test.local/") }
        }
    }

    private fun repository(backend: Backend, prefs: AppPreferences, waits: MutableList<Long> = mutableListOf()) =
        PublishRepositoryImpl(
            remote = PublishRemoteDataSource(backend.client),
            prefs = prefs,
            events = CreationEvents(),
            sleep = { waits += it },
            now = { 1_000L },
        )

    @Test
    fun aPurchaseNotYetVerifiedIsRetriedWithTheSameTransaction() = runTest {
        val backend = Backend(HttpStatusCode.BadRequest to failure("PURCHASE_NOT_VERIFIED"), HttpStatusCode.OK to published)
        val prefs = AppPreferences(InMemorySettings())
        val waits = mutableListOf<Long>()

        val result = repository(backend, prefs, waits).publish(PublishKind.PUBLISH, "c1", "GPA.123", "publish_tierA_7d")

        val success = assertIs<AppResult.Success<PublishResult>>(result)
        assertEquals("https://momentic.ferbotz.com/aK3x9Qm2", success.data.shareUrl)
        assertEquals(2, backend.bodies.size)
        assertTrue(backend.bodies.all { "GPA.123" in it }, "never a second purchase — the same transaction again")
        assertEquals(listOf(2_000L), waits)
        assertEquals(null, prefs.pendingPurchasesJson, "settled")
    }

    @Test
    fun aLostConnectionKeepsThePurchasePendingUntilItCompletes() = runTest {
        val prefs = AppPreferences(InMemorySettings())
        val offline = Backend(HttpStatusCode.OK to null)

        val first = repository(offline, prefs).publish(PublishKind.PUBLISH, "c1", "GPA.123", "publish_tierA_7d")
        assertIs<AppError.Network>(assertIs<AppResult.Failure>(first).error)
        assertEquals(4, offline.bodies.size, "the first try plus three retries")

        val back = repository(Backend(HttpStatusCode.OK to published), prefs)
        assertEquals(listOf("GPA.123"), back.pendingPurchases().map { it.storeTransactionId })
        val finished = back.completePending()
        assertIs<AppResult.Success<PublishResult>>(finished.single().second)
        assertTrue(back.pendingPurchases().isEmpty())
    }

    @Test
    fun aDefinitiveRefusalSettlesThePurchaseWithoutRetrying() = runTest {
        val backend = Backend(HttpStatusCode.Conflict to failure("PURCHASE_ALREADY_USED"))
        val prefs = AppPreferences(InMemorySettings())

        val result = repository(backend, prefs).publish(PublishKind.PUBLISH, "c1", "GPA.123", "publish_tierA_7d")

        assertTrue(assertIs<AppResult.Failure>(result).error.hasCode(ErrorCode.PURCHASE_ALREADY_USED))
        assertEquals(1, backend.bodies.size)
        assertTrue(repository(backend, prefs).pendingPurchases().isEmpty())
    }

    @Test
    fun rateLimitingIsNotHammered_butThePurchaseStaysPending() = runTest {
        val backend = Backend(HttpStatusCode.TooManyRequests to failure("RATE_LIMITED"))
        val prefs = AppPreferences(InMemorySettings())

        repository(backend, prefs).publish(PublishKind.RENEW, "c1", "GPA.456", "publish_tierA_3d")

        assertEquals(1, backend.bodies.size)
        val pending = repository(backend, prefs).pendingPurchases().single()
        assertEquals(PublishKind.RENEW, pending.kind)
    }
}
