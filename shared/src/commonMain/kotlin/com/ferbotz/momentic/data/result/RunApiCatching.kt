package com.ferbotz.momentic.data.result

import com.ferbotz.momentic.data.network.ApiException
import com.ferbotz.momentic.data.network.UnreadableResponseException
import com.ferbotz.momentic.data.network.apiLogger
import kotlinx.coroutines.CancellationException

/**
 * Runs a suspending data-layer [block] and folds its outcome into an [AppResult]:
 *
 * - the value becomes [AppResult.Success];
 * - an [ApiException] (a failure envelope) maps to [AppError.Api] via [ApiException.toAppError];
 * - an [UnreadableResponseException] (a 2xx we couldn't read) maps to [AppError.Unknown];
 * - anything else (dropped connection, timeout, DNS) maps to [AppError.Network];
 * - [CancellationException] is always rethrown, so coroutine cancellation is never swallowed.
 *
 * The catch-all branch is what shows as "you're offline", so it logs the real throwable — that is
 * how a request that never left the device gets diagnosed.
 */
suspend inline fun <T> runApiCatching(block: () -> T): AppResult<T> =
    try {
        AppResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: ApiException) {
        apiLogger.w { "API error: HTTP ${e.status} code=${e.code} message=${e.message}" }
        AppResult.Failure(e.toAppError())
    } catch (e: UnreadableResponseException) {
        apiLogger.e(e) { "Unreadable response: HTTP ${e.status}" }
        AppResult.Failure(AppError.Unknown(e.message))
    } catch (e: Throwable) {
        apiLogger.e(e) { "Request failed without a response: ${e::class.simpleName}: ${e.message}" }
        AppResult.Failure(AppError.Network(e.message))
    }
