package com.ferbotz.momentic.data.network

import com.ferbotz.momentic.data.local.preferences.AppPreferences
import com.ferbotz.momentic.data.platform.AppBuildInfo
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.statement.request
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json

/**
 * Provides the platform HTTP engine: **OkHttp** on Android, **Darwin** on iOS. The only
 * platform-specific networking code is this choice — every plugin and default below is shared.
 */
expect fun httpClientEngine(): HttpClientEngine

/**
 * Builds the single shared [HttpClient].
 *
 * Cross-cutting request setup lives in `defaultRequest`, which runs per request, so the tokens are
 * always read fresh from [prefs] — a sign-in takes effect on the very next call. Every request
 * carries the install id and the three build headers from [buildInfo] (HOA protocol 11 §1), plus
 * exactly one principal: the access token once signed in, otherwise the anonymous draft token.
 *
 * `expectSuccess = false` keeps Ktor from throwing on non-2xx; the envelope helpers decide success
 * so the backend's `code` and `message` can be read.
 *
 * A `401` on a request that **carried an access token** is the app's single "token rejected"
 * signal (HOA protocol 04: tokens never expire, so this is a rejection, never a timeout) and calls
 * [onTokenRejected]. It is watched here because every signed-in request passes through this point.
 *
 * @param engine the transport; defaults to the platform engine. Tests pass a `MockEngine`.
 */
fun createHttpClient(
    prefs: AppPreferences,
    buildInfo: AppBuildInfo,
    engine: HttpClientEngine = httpClientEngine(),
    onTokenRejected: suspend () -> Unit = {},
): HttpClient =
    HttpClient(engine) {
        expectSuccess = false

        install(ContentNegotiation) {
            json(appJson)
        }

        // Validators run for every response, whatever `expectSuccess` says. Only a request that
        // carried an access token can have it rejected; a 401 on an anonymous call means something
        // else (publishing needs a user, or the draft token is unknown) and is handled by the caller.
        HttpResponseValidator {
            validateResponse { response ->
                val sentToken = response.request.headers[HttpHeaders.Authorization] != null
                if (response.status == HttpStatusCode.Unauthorized && sentToken) onTokenRejected()
            }
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 30_000
        }

        // Logs requests and responses through Kermit. Both credentials are redacted, and photo
        // uploads are skipped — their bodies are megabytes of JPEG.
        install(Logging) {
            logger = KermitKtorLogger
            level = LogLevel.ALL
            sanitizeHeader { header ->
                header.equals(HttpHeaders.Authorization, ignoreCase = true) ||
                    header.equals(ApiConfig.HEADER_DRAFT_TOKEN, ignoreCase = true)
            }
            filter { request -> request.url.pathSegments.lastOrNull() != ApiConfig.Paths.IMAGES }
        }

        defaultRequest {
            url(ApiConfig.BASE_URL)
            // Content-Type is set per body (JSON via ContentNegotiation, multipart for photo
            // uploads) — forcing it here would break the uploads.
            header(ApiConfig.HEADER_DEVICE_ID, prefs.deviceId)
            header(ApiConfig.HEADER_APP_PLATFORM, buildInfo.platform.wire)
            header(ApiConfig.HEADER_APP_VERSION, buildInfo.versionName)
            header(ApiConfig.HEADER_APP_BUILD, buildInfo.buildNumber.toString())
            // One principal per request. An access token wins over a draft token anyway, so sending
            // only the token keeps a signed-in request from naming the device at all.
            val accessToken = prefs.authToken
            if (accessToken != null) {
                bearerAuth(accessToken)
            } else {
                prefs.draftToken?.let { header(ApiConfig.HEADER_DRAFT_TOKEN, it) }
            }
        }
    }
