package com.ferbotz.momentic.presentation.moments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ferbotz.momentic.data.model.Creation
import com.ferbotz.momentic.data.model.CreationStats
import com.ferbotz.momentic.data.model.OpenGraph
import com.ferbotz.momentic.data.model.TemplateDetail
import com.ferbotz.momentic.data.repository.CatalogueRepository
import com.ferbotz.momentic.data.repository.CreationChange
import com.ferbotz.momentic.data.repository.CreationEvents
import com.ferbotz.momentic.data.repository.CreationRepository
import com.ferbotz.momentic.data.repository.RemoteConfigRepository
import com.ferbotz.momentic.data.repository.RenderRepository
import com.ferbotz.momentic.data.result.AppResult
import com.ferbotz.momentic.presentation.util.EventChannel
import com.ferbotz.momentic.presentation.util.toUserMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * What one moment's screen renders.
 *
 * @param stats views, days left and the RSVP summary — only for a published moment, and only while
 *   remote config's `features.stats` is on ([showStats]).
 * @param canPublish a draft, with publishing switched on.
 * @param canRenew published (live or ended), with renewal switched on.
 * @param linkPreview how the live link previews in a chat — the title and image the page sends to
 *   WhatsApp and the rest (the render endpoint's `og`); null for a draft, or until it has loaded.
 */
data class MomentDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val creation: Creation? = null,
    val template: TemplateDetail? = null,
    val stats: CreationStats? = null,
    val isLoadingStats: Boolean = false,
    val statsError: String? = null,
    val showStats: Boolean = false,
    val canPublish: Boolean = false,
    val canRenew: Boolean = false,
    val linkPreview: OpenGraph? = null,
    val isOpening: Boolean = false,
    val isDeleting: Boolean = false,
    val message: String? = null,
)

sealed interface MomentDetailEvent {
    /** Open a page in the in-app browser (the draft preview, or the live link). */
    data class OpenUrl(val url: String) : MomentDetailEvent

    /** Share the live link. */
    data class Share(val url: String, val text: String) : MomentDetailEvent

    data class Edit(val templateId: String, val creationId: String) : MomentDetailEvent

    /** To the paywall — publishing a draft, or renewing a published link. */
    data class Publish(val creationId: String) : MomentDetailEvent

    data class ViewRsvps(val creationId: String) : MomentDetailEvent

    data object Deleted : MomentDetailEvent
}

/** Backs one moment: its state, stats and RSVP summary, and every action on it. */
class MomentDetailViewModel(
    private val creationId: String,
    private val creations: CreationRepository,
    private val catalogue: CatalogueRepository,
    private val render: RenderRepository,
    private val remoteConfig: RemoteConfigRepository,
    events: CreationEvents,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MomentDetailUiState())
    val uiState: StateFlow<MomentDetailUiState> = _uiState.asStateFlow()

    private val _events = EventChannel<MomentDetailEvent>()
    val events: Flow<MomentDetailEvent> = _events.events

    init {
        viewModelScope.launch {
            events.changes.collect { change ->
                when (change) {
                    is CreationChange.Saved -> if (change.creation.id == creationId) show(change.creation)
                    is CreationChange.Deleted -> if (change.creationId == creationId) _events.send(MomentDetailEvent.Deleted)
                    CreationChange.OwnerChanged -> load()
                }
            }
        }
        load()
    }

    fun refresh() = load()

    /** A draft opens its (fresh) preview link; a published moment opens its share link. */
    fun open() {
        val creation = _uiState.value.creation ?: return
        val shareUrl = creation.shareUrl
        if (creation.isPublished && shareUrl != null) return _events.send(MomentDetailEvent.OpenUrl(shareUrl))
        viewModelScope.launch {
            _uiState.update { it.copy(isOpening = true) }
            when (val link = creations.previewLink(creationId)) {
                is AppResult.Success -> _events.send(MomentDetailEvent.OpenUrl(link.data.url))
                is AppResult.Failure -> _uiState.update { it.copy(message = link.error.toUserMessage()) }
            }
            _uiState.update { it.copy(isOpening = false) }
        }
    }

    fun share() {
        val url = _uiState.value.creation?.shareUrl ?: return
        _events.send(MomentDetailEvent.Share(url = url, text = "I made this for you 💌 $url"))
    }

    fun edit() {
        val creation = _uiState.value.creation ?: return
        _events.send(MomentDetailEvent.Edit(creation.templateId, creation.id))
    }

    fun publishOrRenew() {
        _events.send(MomentDetailEvent.Publish(creationId))
    }

    fun viewRsvps() {
        _events.send(MomentDetailEvent.ViewRsvps(creationId))
    }

    fun delete() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true) }
            when (val result = creations.delete(creationId)) {
                // The bus announces the deletion; the collector above sends Deleted.
                is AppResult.Success -> _uiState.update { it.copy(isDeleting = false) }
                is AppResult.Failure -> _uiState.update { it.copy(isDeleting = false, message = result.error.toUserMessage()) }
            }
        }
    }

    fun messageShown() {
        _uiState.update { it.copy(message = null) }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.creation == null, error = null) }
            when (val result = creations.get(creationId)) {
                is AppResult.Success -> show(result.data)
                is AppResult.Failure -> _uiState.update { it.copy(isLoading = false, error = result.error.toUserMessage()) }
            }
        }
    }

    private fun show(creation: Creation) {
        val features = remoteConfig.config.features
        val showStats = creation.isPublished && features.stats
        _uiState.update {
            it.copy(
                isLoading = false,
                creation = creation,
                showStats = showStats,
                canPublish = creation.isDraft && features.publishing,
                canRenew = creation.isPublished && features.renewal,
            )
        }
        if (_uiState.value.template?.id != creation.templateId) {
            viewModelScope.launch {
                catalogue.getTemplate(creation.templateId).getOrNull()?.let { template ->
                    _uiState.update { it.copy(template = template) }
                }
            }
        }
        if (showStats) loadStats()
        val shareCode = creation.shareCode
        if (creation.isPublished && shareCode != null && _uiState.value.linkPreview == null) {
            // A read of the public page: the view count only moves on its own POST, never on this.
            viewModelScope.launch {
                render.render(shareCode).getOrNull()?.og?.let { og -> _uiState.update { it.copy(linkPreview = og) } }
            }
        }
    }

    private fun loadStats() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingStats = true, statsError = null) }
            when (val result = creations.stats(creationId)) {
                is AppResult.Success -> _uiState.update { it.copy(isLoadingStats = false, stats = result.data) }
                is AppResult.Failure -> _uiState.update { it.copy(isLoadingStats = false, statsError = result.error.toUserMessage()) }
            }
        }
    }
}
