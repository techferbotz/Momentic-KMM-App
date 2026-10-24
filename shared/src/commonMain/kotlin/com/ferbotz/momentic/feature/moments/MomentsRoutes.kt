package com.ferbotz.momentic.feature.moments

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ferbotz.momentic.designsystem.components.ConfirmDialog
import com.ferbotz.momentic.designsystem.components.DestructiveButton
import com.ferbotz.momentic.designsystem.components.PrimaryButton
import com.ferbotz.momentic.designsystem.components.SecondaryButton
import com.ferbotz.momentic.designsystem.icons.MomenticIcons
import com.ferbotz.momentic.feature.shell.MessageEffect
import com.ferbotz.momentic.feature.shell.StandInNote
import com.ferbotz.momentic.feature.shell.StandInScreen
import com.ferbotz.momentic.feature.shell.StandInStatus
import com.ferbotz.momentic.platform.rememberShareSheet
import com.ferbotz.momentic.platform.shareText
import com.ferbotz.momentic.presentation.di.koinViewModel
import com.ferbotz.momentic.presentation.moments.MomentDetailEvent
import com.ferbotz.momentic.presentation.moments.MomentDetailViewModel
import com.ferbotz.momentic.presentation.moments.MomentsViewModel
import com.ferbotz.momentic.presentation.moments.RsvpsViewModel
import com.ferbotz.momentic.resources.Res
import com.ferbotz.momentic.resources.tab_moments
import org.jetbrains.compose.resources.stringResource
import org.koin.core.parameter.parametersOf

/** The My Moments tab: drafts, live and ended. It keeps itself current after saves, publishes and deletes elsewhere. */
@Composable
fun MomentsRoute(onOpenMoment: (creationId: String) -> Unit, onNewMoment: () -> Unit) {
    val viewModel: MomentsViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MessageEffect(state.message, viewModel::messageShown)
    StandInScreen(title = stringResource(Res.string.tab_moments), onBack = null, tabRoot = true) {
        StandInStatus(state.isLoading, state.error, viewModel::retry)
        if (!state.isLoading && state.error == null && state.creations.isEmpty()) {
            StandInNote("Your first moment is one tap away.")
        }
        PrimaryButton("New moment", onClick = onNewMoment, icon = MomenticIcons.Plus, modifier = Modifier.fillMaxWidth())
        state.creations.forEach { creation ->
            val name = state.templates[creation.templateId]?.name ?: creation.templateName
            SecondaryButton("$name · ${creation.status.name.lowercase()}", onClick = { onOpenMoment(creation.id) }, modifier = Modifier.fillMaxWidth(), compact = true)
        }
        if (state.canLoadMore) SecondaryButton("More", onClick = viewModel::loadMore, loading = state.isLoadingMore, compact = true)
    }
}

/**
 * One moment. Its events lead on: open the page (a draft's fresh preview link goes to the preview, with
 * Edit and Publish under it), share, edit the draft, publish or renew, the guests' replies — and, once
 * deleted, back to the list.
 */
@Composable
fun MomentRoute(
    creationId: String,
    onBack: () -> Unit,
    onOpenPage: (url: String) -> Unit,
    onPreview: (url: String, templateId: String, creationId: String) -> Unit,
    onEdit: (templateId: String, creationId: String) -> Unit,
    onPublish: (creationId: String) -> Unit,
    onViewRsvps: (creationId: String) -> Unit,
) {
    val viewModel: MomentDetailViewModel = koinViewModel { parametersOf(creationId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MessageEffect(state.message, viewModel::messageShown)
    val share = rememberShareSheet()
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }
    val back by rememberUpdatedState(onBack)
    val open by rememberUpdatedState(onOpenPage)
    val preview by rememberUpdatedState(onPreview)
    val edit by rememberUpdatedState(onEdit)
    val publish by rememberUpdatedState(onPublish)
    val rsvps by rememberUpdatedState(onViewRsvps)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is MomentDetailEvent.OpenUrl -> {
                    val shown = viewModel.uiState.value.creation
                    if (shown != null && shown.isDraft) preview(event.url, shown.templateId, shown.id) else open(event.url)
                }
                is MomentDetailEvent.Share -> share.share(shareText(event.text, event.url))
                is MomentDetailEvent.Edit -> edit(event.templateId, event.creationId)
                is MomentDetailEvent.Publish -> publish(event.creationId)
                is MomentDetailEvent.ViewRsvps -> rsvps(event.creationId)
                MomentDetailEvent.Deleted -> back()
            }
        }
    }
    val creation = state.creation
    StandInScreen(title = state.template?.name ?: creation?.templateName.orEmpty(), onBack = onBack) {
        StandInStatus(state.isLoading, state.error, viewModel::refresh)
        if (creation != null) {
            StandInNote(creation.shareUrl ?: "Draft")
            state.stats?.let { StandInNote("${it.totalViews} views · ${it.daysRemaining ?: "–"} days left") }
            SecondaryButton(if (creation.isDraft) "Preview" else "Open", onClick = viewModel::open, loading = state.isOpening, modifier = Modifier.fillMaxWidth())
            if (creation.shareUrl != null) SecondaryButton("Share", onClick = viewModel::share, modifier = Modifier.fillMaxWidth())
            if (creation.isDraft) SecondaryButton("Edit", onClick = viewModel::edit, modifier = Modifier.fillMaxWidth())
            if (state.canPublish || state.canRenew) {
                PrimaryButton(if (state.canRenew) "Add more days" else "Publish", onClick = viewModel::publishOrRenew, modifier = Modifier.fillMaxWidth())
            }
            if (state.stats?.rsvp != null) SecondaryButton("Guest replies", onClick = viewModel::viewRsvps, modifier = Modifier.fillMaxWidth())
            DestructiveButton("Delete moment", onClick = { confirmingDelete = true }, icon = MomenticIcons.Trash, modifier = Modifier.fillMaxWidth())
        }
    }
    if (confirmingDelete) {
        ConfirmDialog(
            title = "Delete this moment?",
            message = if (creation?.isLive == true) "Its link will stop working straight away." else "This can’t be undone.",
            confirmLabel = "Delete moment",
            onConfirm = {
                confirmingDelete = false
                viewModel.delete()
            },
            dismissLabel = "Keep it",
            onDismiss = { confirmingDelete = false },
            icon = MomenticIcons.Trash,
            confirming = state.isDeleting,
        )
    }
}

/** The guests' replies to one moment. */
@Composable
fun RsvpsRoute(creationId: String, onBack: () -> Unit) {
    val viewModel: RsvpsViewModel = koinViewModel { parametersOf(creationId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MessageEffect(state.message, viewModel::messageShown)
    StandInScreen(title = "Guest replies", onBack = onBack) {
        StandInStatus(state.isLoading, state.error, viewModel::refresh)
        state.summary?.let { StandInNote("Yes ${it.yes} · Maybe ${it.maybe} · No ${it.no}") }
        state.rsvps.forEach { StandInNote("${it.guestName} · ${it.attending.name.lowercase()} · ${it.guestCount}") }
        if (state.canLoadMore) SecondaryButton("More", onClick = viewModel::loadMore, loading = state.isLoadingMore, compact = true)
    }
}
