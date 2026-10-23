package com.ferbotz.momentic.presentation.publish

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ferbotz.momentic.data.billing.PurchaseOutcome
import com.ferbotz.momentic.data.billing.StoreBilling
import com.ferbotz.momentic.data.model.Creation
import com.ferbotz.momentic.data.model.Pricing
import com.ferbotz.momentic.data.model.SessionState
import com.ferbotz.momentic.data.repository.AuthRepository
import com.ferbotz.momentic.data.repository.CatalogueRepository
import com.ferbotz.momentic.data.repository.CreationRepository
import com.ferbotz.momentic.data.repository.PendingPurchase
import com.ferbotz.momentic.data.repository.PublishKind
import com.ferbotz.momentic.data.repository.PublishRepository
import com.ferbotz.momentic.data.repository.PublishResult
import com.ferbotz.momentic.data.repository.RemoteConfigRepository
import com.ferbotz.momentic.data.result.AppResult
import com.ferbotz.momentic.presentation.editor.FormEngine
import com.ferbotz.momentic.presentation.util.EventChannel
import com.ferbotz.momentic.presentation.util.toUserMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One duration the moment can be bought for. [price] is the store's localised price, when known. */
data class DurationOption(
    val days: Int,
    val productId: String,
    val price: String?,
)

/** Where the purchase is. */
enum class PublishStage {
    /** Picking a duration. */
    CHOOSING,

    /** The store's purchase sheet is up. */
    PURCHASING,

    /** Paid; the backend is verifying the purchase and assigning the link. */
    CONFIRMING,

    /** Live — [PublishUiState.result] has the link. */
    DONE,
}

/**
 * What the paywall renders.
 *
 * @param kind [PublishKind.PUBLISH] for a draft, [PublishKind.RENEW] to extend a published link.
 * @param tierLabel / [displayPerDay] the backend's copy ("Signature", "from Rs.100/day").
 * @param isAvailable false when remote config has switched publishing (or renewal) off, or this build
 *   has no store set up; [unavailableReason] says which.
 * @param missingFields labels of required fields the draft hasn't filled yet — publishing waits for them.
 * @param pendingPurchase a purchase already paid for this moment whose confirmation hasn't landed; "Try
 *   again" finishes it with the same transaction instead of charging again.
 */
data class PublishUiState(
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val creation: Creation? = null,
    val kind: PublishKind = PublishKind.PUBLISH,
    val tierLabel: String = "",
    val displayPerDay: String = "",
    val options: List<DurationOption> = emptyList(),
    val selectedProductId: String? = null,
    val isSignedIn: Boolean = false,
    val isAvailable: Boolean = true,
    val unavailableReason: String? = null,
    val missingFields: List<String> = emptyList(),
    val stage: PublishStage = PublishStage.CHOOSING,
    val pendingPurchase: PendingPurchase? = null,
    val result: PublishResult? = null,
    val message: String? = null,
) {
    val selectedOption: DurationOption? get() = options.firstOrNull { it.productId == selectedProductId }

    val canPurchase: Boolean
        get() = !isLoading && isAvailable && missingFields.isEmpty() && selectedOption != null &&
            stage == PublishStage.CHOOSING
}

sealed interface PublishEvent {
    /** Publishing needs an account: show sign-in, then call `purchase()` again. */
    data object SignInRequired : PublishEvent

    /** The link is live. */
    data class Published(val result: PublishResult) : PublishEvent

    /** Share the link (the system share sheet). */
    data class Share(val url: String, val text: String) : PublishEvent

    /** The draft still needs filling in: reopen it in the editor. */
    data class EditDraft(val templateId: String, val creationId: String) : PublishEvent
}

/**
 * Backs the paywall for one creation: publishing a draft, or renewing a published link (the share
 * code never changes, and renewing early adds to the time left).
 *
 * The order matters and is fixed: the draft must be complete → the user signed in → the store customer
 * set to the backend user (purchases are verified per customer) → the store purchase → the backend's
 * confirmation, retried with the same transaction until it answers. A purchase is never made twice:
 * a paid-but-unconfirmed one is finished instead.
 */
class PublishViewModel(
    private val creationId: String,
    private val creations: CreationRepository,
    private val catalogue: CatalogueRepository,
    private val publishing: PublishRepository,
    private val billing: StoreBilling,
    private val auth: AuthRepository,
    private val remoteConfig: RemoteConfigRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PublishUiState())
    val uiState: StateFlow<PublishUiState> = _uiState.asStateFlow()

    private val _events = EventChannel<PublishEvent>()
    val events: Flow<PublishEvent> = _events.events

    init {
        viewModelScope.launch {
            auth.session.collect { session ->
                _uiState.update { it.copy(isSignedIn = session is SessionState.SignedIn) }
            }
        }
        load()
    }

    fun retryLoad() = load()

    fun select(productId: String) {
        _uiState.update { state ->
            if (state.options.any { it.productId == productId }) state.copy(selectedProductId = productId) else state
        }
    }

    /** Buys the selected duration and publishes — or finishes an earlier paid purchase. */
    fun purchase() {
        viewModelScope.launch {
            val state = _uiState.value
            if (!state.isSignedIn) return@launch _events.send(PublishEvent.SignInRequired)
            state.pendingPurchase?.let { return@launch confirm(it.kind, it.storeTransactionId, it.productId) }
            if (!state.canPurchase) return@launch
            val option = state.selectedOption ?: return@launch

            _uiState.update { it.copy(stage = PublishStage.PURCHASING) }
            val userId = currentUserId()
            if (userId == null) {
                _uiState.update { it.copy(stage = PublishStage.CHOOSING, message = "Please sign in again to continue.") }
                return@launch
            }
            // Purchases verify per store customer, so the customer must be this backend user.
            val identified = billing.identify(userId)
            if (identified is AppResult.Failure) {
                _uiState.update { it.copy(stage = PublishStage.CHOOSING, message = identified.error.toUserMessage()) }
                return@launch
            }
            when (val outcome = billing.purchase(option.productId)) {
                is PurchaseOutcome.Purchased -> confirm(state.kind, outcome.storeTransactionId, option.productId)
                PurchaseOutcome.Cancelled -> _uiState.update { it.copy(stage = PublishStage.CHOOSING) }
                is PurchaseOutcome.Failed -> _uiState.update {
                    it.copy(stage = PublishStage.CHOOSING, message = outcome.error.toUserMessage())
                }
            }
        }
    }

    /** Opens the share sheet for the live link. */
    fun share() {
        val result = _uiState.value.result ?: return
        _events.send(PublishEvent.Share(url = result.shareUrl, text = "I made this for you 💌 ${result.shareUrl}"))
    }

    /** The draft isn't complete yet: back to the editor. */
    fun editDraft() {
        val creation = _uiState.value.creation ?: return
        _events.send(PublishEvent.EditDraft(creation.templateId, creation.id))
    }

    fun messageShown() {
        _uiState.update { it.copy(message = null) }
    }

    private suspend fun confirm(kind: PublishKind, storeTransactionId: String, productId: String) {
        _uiState.update { it.copy(stage = PublishStage.CONFIRMING) }
        when (val result = publishing.publish(kind, creationId, storeTransactionId, productId)) {
            is AppResult.Success -> {
                _uiState.update {
                    it.copy(
                        stage = PublishStage.DONE,
                        result = result.data,
                        creation = result.data.creation ?: it.creation,
                        pendingPurchase = null,
                    )
                }
                _events.send(PublishEvent.Published(result.data))
            }
            is AppResult.Failure -> {
                val pending = pendingFor(kind)
                _uiState.update {
                    it.copy(
                        stage = PublishStage.CHOOSING,
                        pendingPurchase = pending,
                        message = if (pending != null) {
                            "Your payment went through. We couldn't finish publishing just yet — tap to try again; you won't be charged twice."
                        } else {
                            result.error.toUserMessage()
                        },
                    )
                }
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadError = null) }
            val creation = when (val result = creations.get(creationId)) {
                is AppResult.Success -> result.data
                is AppResult.Failure -> {
                    _uiState.update { it.copy(isLoading = false, loadError = result.error.toUserMessage()) }
                    return@launch
                }
            }
            val kind = if (creation.isPublished) PublishKind.RENEW else PublishKind.PUBLISH
            val template = catalogue.getTemplate(creation.templateId).getOrNull()

            // Publishing an incomplete draft would be refused after payment, so check it first.
            val missing = if (kind == PublishKind.PUBLISH && template != null) {
                var id = 0
                val values = FormEngine.initialValues(template.fields, creation.data, newId = { "p${++id}" })
                val errors = FormEngine.validate(template.fields, values, requireComplete = true)
                template.fields.filter { field -> errors.keys.any { it.key == field.key } }.map { it.label }
            } else {
                emptyList()
            }

            val pricing: Pricing? = creations.pricing(creationId).getOrNull()?.takeIf { it.bundles.isNotEmpty() }
                ?: template?.pricing
            val bundles = pricing?.bundles.orEmpty()
            val prices = if (billing.isAvailable && bundles.isNotEmpty()) {
                billing.products(bundles.map { it.productId }).getOrNull().orEmpty().associateBy { it.productId }
            } else {
                emptyMap()
            }
            val options = bundles.map { DurationOption(it.days, it.productId, prices[it.productId]?.formattedPrice) }

            val features = remoteConfig.config.features
            val reason = when {
                kind == PublishKind.PUBLISH && !features.publishing -> "Publishing is paused right now. Your draft is saved."
                kind == PublishKind.RENEW && !features.renewal -> "Extending links is paused right now."
                !billing.isAvailable -> "Purchases aren't available in this version of the app yet."
                options.isEmpty() -> "This moment can't be published right now."
                else -> null
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    creation = creation,
                    kind = kind,
                    tierLabel = pricing?.label.orEmpty(),
                    displayPerDay = pricing?.displayPerDay ?: template?.displayPerDay.orEmpty(),
                    options = options,
                    // A week is the natural default for most moments; otherwise the shortest.
                    selectedProductId = (options.firstOrNull { o -> o.days == 7 } ?: options.firstOrNull())?.productId,
                    isAvailable = reason == null,
                    unavailableReason = reason,
                    missingFields = missing,
                    pendingPurchase = pendingFor(kind),
                )
            }
        }
    }

    private fun pendingFor(kind: PublishKind): PendingPurchase? =
        publishing.pendingPurchases().firstOrNull { it.creationId == creationId && it.kind == kind }

    private suspend fun currentUserId(): String? =
        (auth.session.value as? SessionState.SignedIn)?.user?.id ?: auth.refreshProfile().getOrNull()?.id
}
