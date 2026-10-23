package com.ferbotz.momentic.presentation.template

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ferbotz.momentic.data.model.TemplateDetail
import com.ferbotz.momentic.data.repository.CatalogueRepository
import com.ferbotz.momentic.data.result.AppResult
import com.ferbotz.momentic.presentation.util.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the template screen renders. */
data class TemplateDetailUiState(
    val template: TemplateDetail? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)

/** Backs the template screen: one `GET /templates/:id`, shared with the editor through the repository's memory. */
class TemplateDetailViewModel(
    private val templateId: String,
    private val catalogue: CatalogueRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TemplateDetailUiState())
    val uiState: StateFlow<TemplateDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = catalogue.getTemplate(templateId)) {
                is AppResult.Success -> _uiState.update { it.copy(template = result.data, isLoading = false) }
                is AppResult.Failure -> _uiState.update { it.copy(isLoading = false, error = result.error.toUserMessage()) }
            }
        }
    }
}
