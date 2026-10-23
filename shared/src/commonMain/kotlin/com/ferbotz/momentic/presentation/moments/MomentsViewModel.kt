package com.ferbotz.momentic.presentation.moments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ferbotz.momentic.data.model.Creation
import com.ferbotz.momentic.data.model.SessionState
import com.ferbotz.momentic.data.model.TemplateCard
import com.ferbotz.momentic.data.repository.AuthRepository
import com.ferbotz.momentic.data.repository.CatalogueRepository
import com.ferbotz.momentic.data.repository.CreationChange
import com.ferbotz.momentic.data.repository.CreationEvents
import com.ferbotz.momentic.data.repository.CreationRepository
import com.ferbotz.momentic.data.repository.PublishRepository
import com.ferbotz.momentic.data.result.AppResult
import com.ferbotz.momentic.presentation.util.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * What "My moments" renders: everything this device (or account) has made, newest first.
 *
 * @param templates the template card behind each creation (by template id), for its name and art.
 * @param pendingPurchases paid purchases still being confirmed — finished in the background.
 */
data class MomentsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val creations: List<Creation> = emptyList(),
    val templates: Map<String, TemplateCard> = emptyMap(),
    val canLoadMore: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isSignedIn: Boolean = false,
    val pendingPurchases: Int = 0,
    val message: String? = null,
) {
    val drafts: List<Creation> get() = creations.filter { it.isDraft }

    /** Published and inside their paid window. */
    val live: List<Creation> get() = creations.filter { it.isLive }

    /** Published, but the paid window has ended — renewable. */
    val ended: List<Creation> get() = creations.filter { it.isPublished && it.isExpired }
}

/**
 * Backs "My moments". Pages through `GET /creations` (cursor pagination), stays current through the
 * app's creation bus (a save, a publish or a delete anywhere shows up here), and reloads when the owner
 * changes — a sign-in adopts the device's drafts, a sign-out returns to them.
 */
class MomentsViewModel(
    private val creations: CreationRepository,
    private val catalogue: CatalogueRepository,
    private val publishing: PublishRepository,
    private val auth: AuthRepository,
    events: CreationEvents,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MomentsUiState())
    val uiState: StateFlow<MomentsUiState> = _uiState.asStateFlow()

    private var nextCursor: String? = null

    init {
        viewModelScope.launch {
            auth.session.collect { session ->
                _uiState.update { it.copy(isSignedIn = session is SessionState.SignedIn) }
            }
        }
        viewModelScope.launch {
            events.changes.collect { change ->
                when (change) {
                    is CreationChange.Saved -> upsert(change.creation)
                    is CreationChange.Deleted -> _uiState.update { state ->
                        state.copy(creations = state.creations.filterNot { it.id == change.creationId })
                    }
                    CreationChange.OwnerChanged -> load(showSpinner = true)
                }
            }
        }
        load(showSpinner = true)
        loadTemplates()
        finishPendingPurchases()
    }

    fun refresh() = load(showSpinner = false)

    fun retry() = load(showSpinner = true)

    fun loadMore() {
        val cursor = nextCursor ?: return
        if (_uiState.value.isLoadingMore) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            when (val result = creations.list(cursor)) {
                is AppResult.Success -> {
                    nextCursor = result.data.nextCursor
                    _uiState.update { state ->
                        val known = state.creations.map { it.id }.toSet()
                        state.copy(
                            creations = state.creations + result.data.items.filterNot { it.id in known },
                            canLoadMore = result.data.hasMore,
                            isLoadingMore = false,
                        )
                    }
                }
                is AppResult.Failure -> _uiState.update {
                    it.copy(isLoadingMore = false, message = result.error.toUserMessage())
                }
            }
        }
    }

    /** Deletes at once on screen; puts it back if the server refuses. */
    fun delete(creationId: String) {
        val before = _uiState.value.creations
        val removed = before.firstOrNull { it.id == creationId } ?: return
        _uiState.update { it.copy(creations = before - removed) }
        viewModelScope.launch {
            val result = creations.delete(creationId)
            if (result is AppResult.Failure) {
                _uiState.update { state ->
                    state.copy(
                        creations = (state.creations + removed).sortedByDescending { it.createdAt },
                        message = result.error.toUserMessage(),
                    )
                }
            }
        }
    }

    /** Retries paid purchases whose publish wasn't confirmed (needs a signed-in user). */
    fun finishPendingPurchases() {
        val pending = publishing.pendingPurchases()
        _uiState.update { it.copy(pendingPurchases = pending.size) }
        if (pending.isEmpty() || auth.session.value !is SessionState.SignedIn) return
        viewModelScope.launch {
            publishing.completePending()
            _uiState.update { it.copy(pendingPurchases = publishing.pendingPurchases().size) }
        }
    }

    fun messageShown() {
        _uiState.update { it.copy(message = null) }
    }

    private fun load(showSpinner: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = showSpinner && it.creations.isEmpty(), isRefreshing = !showSpinner, error = null) }
            when (val result = creations.list(cursor = null)) {
                is AppResult.Success -> {
                    nextCursor = result.data.nextCursor
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            creations = result.data.items,
                            canLoadMore = result.data.hasMore,
                        )
                    }
                }
                is AppResult.Failure -> _uiState.update {
                    val message = result.error.toUserMessage()
                    if (it.creations.isEmpty()) {
                        it.copy(isLoading = false, isRefreshing = false, error = message)
                    } else {
                        it.copy(isLoading = false, isRefreshing = false, message = message)
                    }
                }
            }
        }
    }

    private fun loadTemplates() {
        viewModelScope.launch {
            val feed = catalogue.getFeed().getOrNull() ?: return@launch
            _uiState.update { state ->
                state.copy(templates = feed.flatMap { it.templates }.associateBy { it.id })
            }
        }
    }

    private fun upsert(creation: Creation) {
        _uiState.update { state ->
            val exists = state.creations.any { it.id == creation.id }
            state.copy(
                creations = if (exists) {
                    state.creations.map { if (it.id == creation.id) creation else it }
                } else {
                    listOf(creation) + state.creations
                },
            )
        }
    }
}
