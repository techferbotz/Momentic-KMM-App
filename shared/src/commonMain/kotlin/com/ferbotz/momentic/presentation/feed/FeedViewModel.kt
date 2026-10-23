package com.ferbotz.momentic.presentation.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ferbotz.momentic.data.model.FeedSection
import com.ferbotz.momentic.data.repository.CatalogueRepository
import com.ferbotz.momentic.data.result.AppResult
import com.ferbotz.momentic.presentation.util.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * What the feed renders.
 *
 * @param sections categories with their templates, in the backend's order.
 * @param isLoading the first load is in flight and there is nothing to show yet.
 * @param isRefreshing a pull-to-refresh is in flight (the sections stay on screen).
 * @param error a full-screen message when the first load failed.
 * @param message a one-off note for a snackbar (a refresh that failed while content is showing).
 */
data class FeedUiState(
    val sections: List<FeedSection> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val message: String? = null,
)

/** Backs the feed: network-first, loaded once per screen, re-pulled on demand. */
class FeedViewModel(private val catalogue: CatalogueRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            apply(catalogue.getFeed())
        }
    }

    /** "Try again" on the error state. */
    fun retry() = load()

    fun messageShown() {
        _uiState.update { it.copy(message = null) }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            apply(catalogue.getFeed())
        }
    }

    private fun apply(result: AppResult<List<FeedSection>>) {
        when (result) {
            is AppResult.Success -> _uiState.update {
                it.copy(sections = result.data, isLoading = false, isRefreshing = false, error = null)
            }
            is AppResult.Failure -> _uiState.update {
                val message = result.error.toUserMessage()
                if (it.sections.isEmpty()) {
                    it.copy(isLoading = false, isRefreshing = false, error = message)
                } else {
                    // Keep what's on screen; a failed refresh is a note, not a dead end.
                    it.copy(isLoading = false, isRefreshing = false, message = message)
                }
            }
        }
    }
}
