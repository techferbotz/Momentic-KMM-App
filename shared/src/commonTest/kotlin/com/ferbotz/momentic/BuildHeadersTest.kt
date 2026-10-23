package com.ferbotz.momentic

import com.ferbotz.momentic.data.local.preferences.AppPreferences
import com.ferbotz.momentic.data.network.ApiConfig
import com.ferbotz.momentic.data.network.createHttpClient
import com.ferbotz.momentic.data.platform.AppBuildInfo
import com.ferbotz.momentic.data.platform.AppPlatform
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * HOA protocol 11 §1: every request carries the install id plus the three build headers, so the
 * backend can target this build/platform with remote config later — and exactly one principal.
 */
class BuildHeadersTest {

    /**
     * Runs on a real dispatcher: the production client installs `HttpTimeout`, whose timer would fire
     * at once under `runTest`'s virtual clock while the mock engine answers on a real thread.
     */
    private fun runClientTest(block: suspend CoroutineScope.() -> Unit): TestResult = runTest {
        withContext(Dispatchers.Default) { block() }
    }

    private class Recorder {
        var headers: Headers? = null
        var url: String = ""
        var status: HttpStatusCode = HttpStatusCode.OK
        val engine = MockEngine { request ->
            headers = request.headers
            url = request.url.toString()
            respond("""{ "success": true, "data": {} }""", status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
    }

    @Test
    fun everyRequestCarriesDeviceAndBuildHeaders() = runClientTest {
        val recorder = Recorder()
        val prefs = AppPreferences(InMemorySettings())
        val client = createHttpClient(
            prefs = prefs,
            buildInfo = AppBuildInfo(platform = AppPlatform.ANDROID, versionName = "1.2.3", buildNumber = 42L),
            engine = recorder.engine,
        )

        client.get(ApiConfig.Paths.CONFIG)
        client.close()

        val headers = assertNotNull(recorder.headers)
        assertEquals("https://momentic.ferbotz.com/api/v1/config", recorder.url)
        assertEquals(prefs.deviceId, headers[ApiConfig.HEADER_DEVICE_ID])
        assertEquals("android", headers[ApiConfig.HEADER_APP_PLATFORM])
        assertEquals("1.2.3", headers[ApiConfig.HEADER_APP_VERSION])
        assertEquals("42", headers[ApiConfig.HEADER_APP_BUILD])
        assertNull(headers[HttpHeaders.Authorization], "anonymous until signed in")
        assertNull(headers[ApiConfig.HEADER_DRAFT_TOKEN], "no draft session yet")
    }

    @Test
    fun anonymousRequestsCarryTheDraftToken() = runClientTest {
        val recorder = Recorder()
        val prefs = AppPreferences(InMemorySettings()).apply { saveDraftToken("draft-1") }
        val client = createHttpClient(prefs, AppBuildInfo(AppPlatform.IOS, "2.0", 7L), engine = recorder.engine)

        client.get(ApiConfig.Paths.CREATIONS)
        client.close()

        val headers = assertNotNull(recorder.headers)
        assertEquals("draft-1", headers[ApiConfig.HEADER_DRAFT_TOKEN])
        assertNull(headers[HttpHeaders.Authorization])
        assertEquals("ios", headers[ApiConfig.HEADER_APP_PLATFORM])
        assertEquals("7", headers[ApiConfig.HEADER_APP_BUILD])
    }

    @Test
    fun signedInRequestsCarryOnlyTheBearerToken() = runClientTest {
        val recorder = Recorder()
        val prefs = AppPreferences(InMemorySettings()).apply {
            saveDraftToken("draft-1")
            saveAuthToken("jwt-1")
        }
        val client = createHttpClient(prefs, AppBuildInfo(AppPlatform.ANDROID, "1.0", 1L), engine = recorder.engine)

        client.get(ApiConfig.Paths.ME)
        client.close()

        val headers = assertNotNull(recorder.headers)
        assertEquals("Bearer jwt-1", headers[HttpHeaders.Authorization])
        assertNull(headers[ApiConfig.HEADER_DRAFT_TOKEN], "one principal per request")
    }

    @Test
    fun aRejectedAccessTokenIsReported_anAnonymous401IsNot() = runClientTest {
        var rejections = 0
        val recorder = Recorder().apply { status = HttpStatusCode.Unauthorized }
        val prefs = AppPreferences(InMemorySettings()).apply { saveDraftToken("draft-1") }
        val client = createHttpClient(
            prefs,
            AppBuildInfo(AppPlatform.ANDROID, "1.0", 1L),
            engine = recorder.engine,
            onTokenRejected = { rejections++ },
        )

        // Publishing with only a draft token answers 401 AUTH_REQUIRED: that asks for sign-in, it
        // is not a rejected session.
        client.get(ApiConfig.Paths.CREATIONS)
        assertEquals(0, rejections)

        prefs.saveAuthToken("jwt-1")
        client.get(ApiConfig.Paths.ME)
        client.close()
        assertEquals(1, rejections)
        assertTrue(prefs.isSignedIn, "the client reports; the session manager decides")
    }
}
