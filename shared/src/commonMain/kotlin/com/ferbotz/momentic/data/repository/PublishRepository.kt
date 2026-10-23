package com.ferbotz.momentic.data.repository

import com.ferbotz.momentic.data.local.preferences.AppPreferences
import com.ferbotz.momentic.data.mapper.toCreationDto
import com.ferbotz.momentic.data.mapper.toDomain
import com.ferbotz.momentic.data.model.Creation
import com.ferbotz.momentic.data.network.UnreadableResponseException
import com.ferbotz.momentic.data.network.apiLogger
import com.ferbotz.momentic.data.network.appJson
import com.ferbotz.momentic.data.network.datasource.PublishRemoteDataSource
import com.ferbotz.momentic.data.network.dto.PublishResponseDto
import com.ferbotz.momentic.data.result.AppError
import com.ferbotz.momentic.data.result.AppResult
import com.ferbotz.momentic.data.result.ErrorCode
import com.ferbotz.momentic.data.result.runApiCatching
import com.ferbotz.momentic.data.util.nowEpochMillis
import com.ferbotz.momentic.data.util.parseInstantOrNull
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/** Publishing a draft for the first time, or extending a published link. */
enum class PublishKind { PUBLISH, RENEW }

/**
 * A store purchase the server hasn't confirmed yet. Saved **before** the call, so a crash or a lost
 * response between paying and publishing can be finished later with the same transaction — the
 * endpoints are idempotent per transaction, so finishing it never charges twice.
 */
@Serializable
data class PendingPurchase(
    val kind: PublishKind,
    val creationId: String,
    val storeTransactionId: String,
    val productId: String,
    val createdAtEpochMillis: Long,
)

/** A confirmed publish or renewal. */
data class PublishResult(
    /** The updated creation, when the response carried it. */
    val creation: Creation?,
    val shareCode: String,
    /** `https://momentic.ferbotz.com/<shareCode>` — what the user shares. */
    val shareUrl: String,
    val expiresAt: Instant?,
    /** True when this repeated an earlier call for the same transaction. */
    val alreadyPublished: Boolean,
)

/**
 * Publish and renew (`API.md` § Publishing). Both require a signed-in user and a verified store
 * purchase.
 *
 * Failures that may clear up on their own are retried with the **same** transaction id:
 * `PURCHASE_NOT_VERIFIED` ("it may not have propagated"), a lost connection, a `5xx`. The purchase
 * stays pending until the server answers definitively, so [completePending] can finish it later.
 */
interface PublishRepository {

    suspend fun publish(
        kind: PublishKind,
        creationId: String,
        storeTransactionId: String,
        productId: String,
    ): AppResult<PublishResult>

    /** Paid purchases still waiting for the server's confirmation. */
    fun pendingPurchases(): List<PendingPurchase>

    /** Retries every pending purchase; call on launch and when "My moments" opens. */
    suspend fun completePending(): List<Pair<PendingPurchase, AppResult<PublishResult>>>
}

/**
 * @param retryDelaysMillis the waits between attempts for a retryable failure; injectable for tests.
 */
class PublishRepositoryImpl(
    private val remote: PublishRemoteDataSource,
    private val prefs: AppPreferences,
    private val events: CreationEvents,
    private val retryDelaysMillis: List<Long> = listOf(2_000, 4_000, 8_000),
    private val sleep: suspend (Long) -> Unit = { delay(it) },
    private val now: () -> Long = ::nowEpochMillis,
) : PublishRepository {

    private val pendingLock = Mutex()

    override suspend fun publish(
        kind: PublishKind,
        creationId: String,
        storeTransactionId: String,
        productId: String,
    ): AppResult<PublishResult> {
        val pending = PendingPurchase(kind, creationId, storeTransactionId, productId, now())
        updatePending { list -> list.filterNot { it.storeTransactionId == storeTransactionId } + pending }

        var result = attempt(pending)
        for (wait in retryDelaysMillis) {
            if (result !is AppResult.Failure || !result.error.worthRetryingNow()) break
            apiLogger.w { "${kind.name.lowercase()} not confirmed yet (${result.error}); retrying in ${wait}ms" }
            sleep(wait)
            result = attempt(pending)
        }

        // The purchase is paid for, so it stays pending until it either worked or was refused in a way
        // no retry with this transaction can change. Anything else (rate-limited, billing down, signed
        // out, an incomplete draft the user can still fix) is finished later with the same transaction.
        val settled = when (result) {
            is AppResult.Success -> true
            is AppResult.Failure -> result.error.settlesPurchase()
        }
        if (settled) updatePending { list -> list.filterNot { it.storeTransactionId == storeTransactionId } }
        if (result is AppResult.Success) result.data.creation?.let { events.publish(CreationChange.Saved(it)) }
        return result
    }

    override fun pendingPurchases(): List<PendingPurchase> = readPending()

    override suspend fun completePending(): List<Pair<PendingPurchase, AppResult<PublishResult>>> =
        readPending().map { pending ->
            pending to publish(pending.kind, pending.creationId, pending.storeTransactionId, pending.productId)
        }

    private suspend fun attempt(pending: PendingPurchase): AppResult<PublishResult> = runApiCatching {
        val response = when (pending.kind) {
            PublishKind.PUBLISH -> remote.publish(pending.creationId, pending.storeTransactionId)
            PublishKind.RENEW -> remote.renew(pending.creationId, pending.storeTransactionId)
        }
        response.toResult()
    }

    private fun PublishResponseDto.toResult(): PublishResult {
        val creation = creation?.toCreationDto()?.toDomain()
        val code = shareCode ?: creation?.shareCode
        val url = shareUrl ?: creation?.shareUrl
        if (code == null || url == null) {
            throw UnreadableResponseException(200, IllegalStateException("Publish response without a share link"))
        }
        return PublishResult(
            creation = creation,
            shareCode = code,
            shareUrl = url,
            expiresAt = parseInstantOrNull(expiresAt) ?: creation?.expiresAt,
            alreadyPublished = alreadyPublished ?: false,
        )
    }

    /**
     * Worth another try within seconds, with the same transaction: the purchase may not have
     * propagated to RevenueCat yet, or no answer arrived at all.
     */
    private fun AppError.worthRetryingNow(): Boolean = when (this) {
        is AppError.Network -> true
        is AppError.Api -> code == ErrorCode.PURCHASE_NOT_VERIFIED ||
            (status >= 500 && code != ErrorCode.BILLING_UNAVAILABLE)
        else -> false
    }

    /** A refusal that no retry with this transaction can turn into a publish. */
    private fun AppError.settlesPurchase(): Boolean = this is AppError.Api && code in FINAL_REFUSALS

    private companion object {
        val FINAL_REFUSALS = setOf(
            ErrorCode.PURCHASE_ALREADY_USED,
            ErrorCode.UNKNOWN_PRODUCT,
            ErrorCode.ALREADY_PUBLISHED,
            ErrorCode.NOT_PUBLISHED,
            ErrorCode.NOT_FOUND,
        )
    }

    private fun readPending(): List<PendingPurchase> =
        prefs.pendingPurchasesJson
            ?.let { runCatching { appJson.decodeFromString<List<PendingPurchase>>(it) }.getOrNull() }
            .orEmpty()

    private suspend fun updatePending(transform: (List<PendingPurchase>) -> List<PendingPurchase>) {
        pendingLock.withLock {
            val next = transform(readPending())
            prefs.savePendingPurchasesJson(if (next.isEmpty()) null else appJson.encodeToString(next))
        }
    }
}
