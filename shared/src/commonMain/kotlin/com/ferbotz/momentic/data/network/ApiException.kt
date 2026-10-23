package com.ferbotz.momentic.data.network

import com.ferbotz.momentic.data.result.AppError
import com.ferbotz.momentic.data.result.ErrorCode

/**
 * Thrown by the remote data sources when the backend answers with a failure — a non-2xx status or
 * `success: false`. It carries the HTTP [status] and the machine-readable [code] so
 * [toAppError] can produce a precise [AppError]. Repositories don't catch this themselves —
 * `runApiCatching` does.
 *
 * @param retryAfterSeconds the `Retry-After` header of a `429 RATE_LIMITED`, when sent.
 */
class ApiException(
    val status: Int,
    val code: String?,
    override val message: String?,
    val retryAfterSeconds: Long? = null,
) : Exception(message) {

    fun toAppError(): AppError = AppError.Api(
        status = status,
        code = ErrorCode.fromWire(code),
        message = message,
        retryAfterSeconds = retryAfterSeconds,
    )
}

/**
 * A 2xx response whose body could not be read as the expected envelope — the backend sent a shape
 * this build doesn't understand. Distinct from a network failure so it is never reported to the
 * user as "you're offline".
 */
class UnreadableResponseException(val status: Int, cause: Throwable?) :
    Exception("Unreadable response body (HTTP $status)", cause)
