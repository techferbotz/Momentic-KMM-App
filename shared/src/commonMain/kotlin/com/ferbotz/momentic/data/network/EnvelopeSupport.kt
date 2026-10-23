package com.ferbotz.momentic.data.network

import com.ferbotz.momentic.data.network.dto.ApiEnvelope
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.serialization.ContentConvertException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonElement

/**
 * Reads the [ApiEnvelope] of type [T] from this response and returns its `data`, throwing
 * [ApiException] when the status is not 2xx or the envelope reports `success: false`.
 *
 * The client runs with `expectSuccess = false`, so this is the single place that turns a
 * transport-level response into either a value or a typed failure.
 */
suspend inline fun <reified T> HttpResponse.unwrap(): T {
    val envelope = try {
        body<ApiEnvelope<T>>()
    } catch (e: ContentConvertException) {
        throw unreadable(e)
    } catch (e: SerializationException) {
        throw unreadable(e)
    } catch (e: NoTransformationFoundException) {
        throw unreadable(e)
    }
    if (status.isSuccess() && envelope.success) {
        return envelope.data ?: throw UnreadableResponseException(status.value, null)
    }
    throw failure(envelope.code, envelope.message)
}

/**
 * Like [unwrap] for endpoints whose `data` carries nothing the app needs (`DELETE …`): only the
 * success flag matters.
 */
suspend fun HttpResponse.ensureSuccess() {
    unwrap<JsonElement>()
}

/**
 * A body that is not an envelope. On a failure status that is a proxy's page (an nginx 502, a
 * gateway timeout), so the status is what matters; on a 2xx it is a shape this build can't read.
 */
@PublishedApi
internal fun HttpResponse.unreadable(cause: Exception): Exception =
    if (status.isSuccess()) UnreadableResponseException(status.value, cause) else failure(null, null)

@PublishedApi
internal fun HttpResponse.failure(code: String?, message: String?): ApiException =
    ApiException(
        status = status.value,
        code = code,
        message = message,
        // Seconds, per the contract's rate-limit section. Only `429 RATE_LIMITED` sends it.
        retryAfterSeconds = headers[HttpHeaders.RetryAfter]?.trim()?.toLongOrNull(),
    )
