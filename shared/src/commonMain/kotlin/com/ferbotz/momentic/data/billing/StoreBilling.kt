package com.ferbotz.momentic.data.billing

import com.ferbotz.momentic.data.result.AppError
import com.ferbotz.momentic.data.result.AppResult
import com.ferbotz.momentic.data.result.ErrorCode

/** A store product as the user sees it — the price comes from the store, never computed in the app. */
data class StoreProduct(
    val productId: String,
    val title: String,
    /** Localised by the store, e.g. "₹100.00". */
    val formattedPrice: String,
)

/** How a store purchase ended. */
sealed interface PurchaseOutcome {

    /** Paid. [storeTransactionId] is what `POST /creations/:id/publish` verifies against RevenueCat. */
    data class Purchased(val storeTransactionId: String) : PurchaseOutcome

    /** The user backed out of the store sheet — not an error. */
    data object Cancelled : PurchaseOutcome

    data class Failed(val error: AppError) : PurchaseOutcome
}

/**
 * The store side of publishing — RevenueCat in production (`DEPLOY.md` § RevenueCat): products named
 * `publish_tier{A|B|C}_{1|3|7|30|90}d`, imported into RevenueCat as Offerings.
 *
 * The backend verifies a purchase **scoped to the RevenueCat customer**, so [identify] must set the
 * RevenueCat app user id to the backend's `user.id` before buying; a mismatch means a genuine purchase
 * won't verify.
 */
interface StoreBilling {

    /** False until the store SDK is configured in this build; publishing is unavailable meanwhile. */
    val isAvailable: Boolean

    /** Sets the store's customer to the backend user ([appUserId] = `user.id`). */
    suspend fun identify(appUserId: String): AppResult<Unit>

    /** Back to an anonymous store customer, after sign-out. */
    suspend fun reset(): AppResult<Unit>

    /** The store's listing (title, localised price) for each of [productIds] it knows. */
    suspend fun products(productIds: List<String>): AppResult<List<StoreProduct>>

    /** Runs the store's purchase sheet for [productId]. */
    suspend fun purchase(productId: String): PurchaseOutcome
}

/**
 * The [StoreBilling] for a build without store keys: everything answers `BILLING_UNAVAILABLE`, which is
 * also what the backend answers today while RevenueCat is unset (contract BE-002). Replaced by the
 * RevenueCat implementation once the public SDK keys are in the build.
 */
class UnavailableStoreBilling : StoreBilling {

    override val isAvailable: Boolean = false

    override suspend fun identify(appUserId: String): AppResult<Unit> = AppResult.Failure(unavailable)

    override suspend fun reset(): AppResult<Unit> = AppResult.Success(Unit)

    override suspend fun products(productIds: List<String>): AppResult<List<StoreProduct>> = AppResult.Failure(unavailable)

    override suspend fun purchase(productId: String): PurchaseOutcome = PurchaseOutcome.Failed(unavailable)

    private val unavailable = AppError.Api(
        status = 503,
        code = ErrorCode.BILLING_UNAVAILABLE,
        message = "In-app purchases aren't set up in this build yet.",
    )
}
