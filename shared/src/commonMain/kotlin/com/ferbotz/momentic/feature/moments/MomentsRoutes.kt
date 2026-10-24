package com.ferbotz.momentic.feature.moments

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ferbotz.momentic.data.model.Attendance
import com.ferbotz.momentic.designsystem.components.ConfirmDialog
import com.ferbotz.momentic.designsystem.components.MessageTone
import com.ferbotz.momentic.designsystem.icons.MomenticIcons
import com.ferbotz.momentic.feature.publish.ExtendSheet
import com.ferbotz.momentic.feature.shell.LocalMessenger
import com.ferbotz.momentic.feature.shell.MessageEffect
import com.ferbotz.momentic.platform.rememberShareSheet
import com.ferbotz.momentic.platform.rememberTextCopier
import com.ferbotz.momentic.platform.shareText
import com.ferbotz.momentic.presentation.config.LocalRemoteConfig
import com.ferbotz.momentic.presentation.di.koinViewModel
import com.ferbotz.momentic.presentation.links.rememberLinkOpener
import com.ferbotz.momentic.presentation.moments.MomentDetailEvent
import com.ferbotz.momentic.presentation.moments.MomentDetailViewModel
import com.ferbotz.momentic.presentation.moments.MomentsViewModel
import com.ferbotz.momentic.presentation.moments.RsvpsViewModel
import com.ferbotz.momentic.presentation.util.longLabel
import com.ferbotz.momentic.resources.Res
import com.ferbotz.momentic.resources.live_copied
import com.ferbotz.momentic.resources.live_until
import com.ferbotz.momentic.resources.moment_delete
import com.ferbotz.momentic.resources.moment_delete_body_draft
import com.ferbotz.momentic.resources.moment_delete_body_live
import com.ferbotz.momentic.resources.moment_delete_keep
import com.ferbotz.momentic.resources.moment_delete_title
import com.ferbotz.momentic.resources.notice_cant_call
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.core.parameter.parametersOf

/** The My Moments tab: drafts, live and ended. It keeps itself current after saves, publishes and deletes elsewhere. */
@Composable
fun MomentsRoute(onOpenMoment: (creationId: String) -> Unit, onNewMoment: () -> Unit) {
    val viewModel: MomentsViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MessageEffect(state.message, viewModel::messageShown)
    // Until a tab is picked: Live when something is live, else Drafts.
    var picked by rememberSaveable { mutableStateOf<MomentsTab?>(null) }
    val tab = picked ?: if (state.live.isNotEmpty()) MomentsTab.Live else MomentsTab.Drafts
    MomentsScreen(
        state = state,
        tab = tab,
        maxDrafts = LocalRemoteConfig.current.limits.maxDrafts,
        onTab = { picked = it },
        onOpenMoment = onOpenMoment,
        onNewMoment = onNewMoment,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::retry,
        onLoadMore = viewModel::loadMore,
        onFinishPending = viewModel::finishPendingPurchases,
    )
}

/**
 * One moment. Its events lead on: open the page (a draft's fresh preview link goes to the preview, with
 * Edit and Publish under it), share, edit, publish a draft, the guests' replies — and, once deleted,
 * back to the list. A published moment adds time in a sheet over this screen.
 */
@Composable
fun MomentRoute(
    creationId: String,
    onBack: () -> Unit,
    onOpenPage: (url: String) -> Unit,
    onPreview: (url: String, templateId: String, creationId: String) -> Unit,
    onEdit: (templateId: String, creationId: String) -> Unit,
    onPublish: (creationId: String) -> Unit,
    onViewRsvps: (creationId: String, momentTitle: String) -> Unit,
) {
    val viewModel: MomentDetailViewModel = koinViewModel { parametersOf(creationId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MessageEffect(state.message, viewModel::messageShown)
    val share = rememberShareSheet()
    val copier = rememberTextCopier()
    val messenger = LocalMessenger.current
    val scope = rememberCoroutineScope()
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }
    var extending by rememberSaveable { mutableStateOf(false) }
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
                is MomentDetailEvent.ViewRsvps -> rsvps(event.creationId, viewModel.uiState.value.momentTitle())
                MomentDetailEvent.Deleted -> back()
            }
        }
    }
    val copied = stringResource(Res.string.live_copied)
    val creation = state.creation
    MomentDetailScreen(
        state = state,
        onBack = onBack,
        onCopy = {
            creation?.shareUrl?.let { url ->
                copier.copy(url)
                messenger.show(copied, MessageTone.Success)
            }
        },
        onShare = viewModel::share,
        onEdit = viewModel::edit,
        onOpen = viewModel::open,
        onPublishOrRenew = { if (creation?.isPublished == true) extending = true else viewModel.publishOrRenew() },
        onViewRsvps = viewModel::viewRsvps,
        onDelete = { confirmingDelete = true },
        onRetry = viewModel::refresh,
    )
    if (extending && creation != null) {
        ExtendSheet(
            creationId = creation.id,
            momentTitle = state.momentTitle(),
            onDismiss = { extending = false },
            onExtended = { until ->
                viewModel.refresh()
                scope.launch {
                    val day = until?.toLocalDateTime(TimeZone.currentSystemDefault())?.date?.longLabel()
                    if (day != null) messenger.show(getString(Res.string.live_until, day), MessageTone.Success)
                }
            },
        )
    }
    if (confirmingDelete) {
        ConfirmDialog(
            title = stringResource(Res.string.moment_delete_title),
            message = stringResource(if (creation?.isPublished == true) Res.string.moment_delete_body_live else Res.string.moment_delete_body_draft),
            confirmLabel = stringResource(Res.string.moment_delete),
            onConfirm = {
                confirmingDelete = false
                viewModel.delete()
            },
            dismissLabel = stringResource(Res.string.moment_delete_keep),
            onDismiss = { confirmingDelete = false },
            icon = MomenticIcons.Trash,
            confirming = state.isDeleting,
        )
    }
}

/** The guests' replies to one moment ([momentTitle] under the bar), filtered by answer on the phone. */
@Composable
fun RsvpsRoute(creationId: String, momentTitle: String?, onBack: () -> Unit) {
    val viewModel: RsvpsViewModel = koinViewModel { parametersOf(creationId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MessageEffect(state.message, viewModel::messageShown)
    val links = rememberLinkOpener()
    val messenger = LocalMessenger.current
    val cantCall = stringResource(Res.string.notice_cant_call)
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    RsvpsScreen(
        state = state,
        momentTitle = momentTitle,
        filter = filter?.let { Attendance.valueOf(it) },
        onFilter = { filter = it?.name },
        onBack = onBack,
        onCall = { phone -> if (!links.call(phone)) messenger.show(cantCall) },
        onLoadMore = viewModel::loadMore,
        onRetry = viewModel::refresh,
    )
}
