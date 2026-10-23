package com.ferbotz.momentic.presentation.moments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ferbotz.momentic.data.model.Rsvp
import com.ferbotz.momentic.data.model.RsvpSummary
import com.ferbotz.momentic.data.repository.CreationRepository
import com.ferbotz.momentic.data.result.AppResult
import com.ferbotz.momentic.presentation.util.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the RSVP list renders — guests' replies, readable only by the creator. */
data class RsvpsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val rsvps: List<Rsvp> = emptyList(),
    val summary: RsvpSummary? = null,
    val canLoadMore: Boolean = false,
    val isLoadingMore: Boolean = false,
    val message: String? = null,
)

/** Backs the RSVP list for one moment (`GET /creations/:id/rsvps`, cursor pagination). */
class RsvpsViewModel(
    private val creationId: String,
    private val creations: CreationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RsvpsUiState())
    val uiState: StateFlow<RsvpsUiState> = _uiState.asStateFlow()

    private var nextCursor: String? = null

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.rsvps.isEmpty(), error = null) }
            when (val result = creations.rsvps(creationId, cursor = null)) {
                is AppResult.Success -> {
                    nextCursor = result.data.page.nextCursor
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            rsvps = result.data.page.items,
                            summary = result.data.summary,
                            canLoadMore = result.data.page.hasMore,
                        )
                    }
                }
                is AppResult.Failure -> _uiState.update { it.copy(isLoading = false, error = result.error.toUserMessage()) }
            }
        }
    }

    fun loadMore() {
        val cursor = nextCursor ?: return
        if (_uiState.value.isLoadingMore) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            when (val result = creations.rsvps(creationId, cursor)) {
                is AppResult.Success -> {
                    nextCursor = result.data.page.nextCursor
                    _uiState.update {
                        it.copy(
                            rsvps = it.rsvps + result.data.page.items,
                            summary = result.data.summary ?: it.summary,
                            canLoadMore = result.data.page.hasMore,
                            isLoadingMore = false,
                        )
                    }
                }
                is AppResult.Failure -> _uiState.update { it.copy(isLoadingMore = false, message = result.error.toUserMessage()) }
            }
        }
    }

    fun messageShown() {
        _uiState.update { it.copy(message = null) }
    }
}
