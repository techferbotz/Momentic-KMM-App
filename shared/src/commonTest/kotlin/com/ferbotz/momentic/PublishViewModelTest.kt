package com.ferbotz.momentic

import com.ferbotz.momentic.data.billing.PurchaseOutcome
import com.ferbotz.momentic.data.billing.StoreBilling
import com.ferbotz.momentic.data.billing.StoreProduct
import com.ferbotz.momentic.data.model.SessionState
import com.ferbotz.momentic.data.model.SignInResult
import com.ferbotz.momentic.data.model.User
import com.ferbotz.momentic.data.repository.AuthRepository
import com.ferbotz.momentic.data.repository.PendingPurchase
import com.ferbotz.momentic.data.repository.PublishKind
import com.ferbotz.momentic.data.repository.PublishRepository
import com.ferbotz.momentic.data.repository.PublishResult
import com.ferbotz.momentic.data.result.AppError
import com.ferbotz.momentic.data.result.AppResult
import com.ferbotz.momentic.presentation.publish.PublishStage
import com.ferbotz.momentic.presentation.publish.PublishViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** The paywall's summary and prices, and a paid purchase whose confirmation doesn't land at first. */
@OptIn(ExperimentalCoroutinesApi::class)
class PublishViewModelTest {

    private val gift = TemplateFixtures.template(TemplateFixtures.giftReveal)

    private class SignedInAuth : AuthRepository {
        override val session: StateFlow<SessionState> = MutableStateFlow(SessionState.SignedIn(User("u1", null, null, null, null)))
        override suspend fun signInWithGoogle(idToken: String): AppResult<SignInResult> = error("not used")
        override suspend fun refreshProfile(): AppResult<User> = error("not used")
        override suspend fun signOut() = Unit
        override suspend fun deleteAccount(): AppResult<Unit> = error("not used")
    }

    private class Store : StoreBilling {
        var purchases = 0
        override val isAvailable = true
        override suspend fun identify(appUserId: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun reset(): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun products(productIds: List<String>): AppResult<List<StoreProduct>> = AppResult.Success(
            listOf(StoreProduct("publish_tierA_7d", "7 days", "₹599.00", amountMicros = 599_000_000), StoreProduct("publish_tierA_1d", "1 day", "₹149.00")),
        )
        override suspend fun purchase(productId: String): PurchaseOutcome {
            purchases++
            return PurchaseOutcome.Purchased("tx$purchases")
        }
    }

    /** Records a purchase as pending before each call, as the real repository does, and answers with [results] in turn. */
    private class Publishing(vararg results: AppResult<PublishResult>) : PublishRepository {
        val queue = ArrayDeque(results.toList())
        val transactions = mutableListOf<String>()
        private val pending = mutableListOf<PendingPurchase>()
        override suspend fun publish(kind: PublishKind, creationId: String, storeTransactionId: String, productId: String): AppResult<PublishResult> {
            transactions += storeTransactionId
            if (pending.none { it.storeTransactionId == storeTransactionId }) pending += PendingPurchase(kind, creationId, storeTransactionId, productId, 0)
            val result = queue.removeFirst()
            if (result is AppResult.Success) pending.removeAll { it.storeTransactionId == storeTransactionId }
            return result
        }
        override fun pendingPurchases(): List<PendingPurchase> = pending.toList()
        override suspend fun completePending(): List<Pair<PendingPurchase, AppResult<PublishResult>>> = emptyList()
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun paywall(publishing: Publishing, store: Store = Store()): PublishViewModel {
        val creations = FakeCreations().apply {
            stored = creation("c1", gift.id, Json.parseToJsonElement("""{"recipientName":"Asha","message":"Happy birthday!"}""").jsonObject)
        }
        return PublishViewModel("c1", creations, FakeCatalogue(gift), publishing, store, SignedInAuth(), FakeRemoteConfig())
    }

    private val live = PublishResult(creation = null, shareCode = "aK3x9Qm2", shareUrl = "https://momentic.ferbotz.com/aK3x9Qm2", expiresAt = null, alreadyPublished = false)

    @Test
    fun theSummaryShowsTheTemplate_andEachDurationWhatItComesToADay() {
        val state = paywall(Publishing()).uiState.value
        assertEquals(gift.id, state.template?.id)
        val week = state.options.single { it.days == 7 }
        assertEquals("₹599.00", week.price)
        assertEquals("₹86", week.perDay)
        assertNull(state.options.single { it.days == 1 }.perDay, "no amount from the store, no sum")
        assertEquals(week.productId, state.selectedProductId, "a week is chosen to start with")
    }

    @Test
    fun aPaidPurchaseThatDoesntConfirm_saysSoInFull_thenFinishesWithTheSameTransaction() = runTest {
        val store = Store()
        val publishing = Publishing(AppResult.Failure(AppError.Network()), AppResult.Success(live))
        val vm = paywall(publishing, store)

        vm.purchase()
        val failed = vm.uiState.value
        assertEquals(PublishStage.CHOOSING, failed.stage)
        assertNotNull(failed.pendingPurchase)
        assertNotNull(failed.confirmError)
        assertNull(failed.message, "the screen says it, not a snackbar")

        vm.purchase()
        assertEquals(1, store.purchases, "never charged twice")
        assertEquals(listOf("tx1", "tx1"), publishing.transactions)
        assertEquals(PublishStage.DONE, vm.uiState.value.stage)
        assertNull(vm.uiState.value.confirmError)
    }
}
