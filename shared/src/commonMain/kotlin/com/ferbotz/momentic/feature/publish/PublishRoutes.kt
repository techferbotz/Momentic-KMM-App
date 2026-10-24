package com.ferbotz.momentic.feature.publish

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ferbotz.momentic.designsystem.components.MessageTone
import com.ferbotz.momentic.feature.account.SignInSheet
import com.ferbotz.momentic.feature.shell.LocalMessenger
import com.ferbotz.momentic.feature.shell.MessageEffect
import com.ferbotz.momentic.platform.SystemBack
import com.ferbotz.momentic.platform.rememberShareSheet
import com.ferbotz.momentic.platform.rememberTextCopier
import com.ferbotz.momentic.platform.shareText
import com.ferbotz.momentic.presentation.config.LocalRemoteConfig
import com.ferbotz.momentic.presentation.di.koinViewModel
import com.ferbotz.momentic.presentation.links.rememberLinkOpener
import com.ferbotz.momentic.presentation.moments.MomentDetailEvent
import com.ferbotz.momentic.presentation.moments.MomentDetailViewModel
import com.ferbotz.momentic.presentation.publish.PublishEvent
import com.ferbotz.momentic.presentation.publish.PublishStage
import com.ferbotz.momentic.presentation.publish.PublishViewModel
import com.ferbotz.momentic.resources.Res
import com.ferbotz.momentic.resources.live_copied
import com.ferbotz.momentic.resources.notice_no_mail_app
import com.ferbotz.momentic.resources.publishing_support_body
import com.ferbotz.momentic.resources.publishing_support_subject
import com.ferbotz.momentic.resources.share_line
import org.jetbrains.compose.resources.stringResource
import org.koin.core.parameter.parametersOf

/**
 * Choose how long, then pay and publish — or, for a published moment, renew it (same link, more days).
 * Not signed in, it asks first ([SignInSheet]) and carries on with the purchase. A draft that's missing
 * something goes back to its form. While the server confirms, Back waits.
 */
@Composable
fun PublishRoute(
    creationId: String,
    onBack: () -> Unit,
    onEditDraft: (templateId: String, creationId: String) -> Unit,
    onPublished: (creationId: String) -> Unit,
) {
    val viewModel: PublishViewModel = koinViewModel { parametersOf(creationId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MessageEffect(state.message, viewModel::messageShown)
    var signingIn by rememberSaveable { mutableStateOf(false) }
    val share = rememberShareSheet()
    val links = rememberLinkOpener()
    val messenger = LocalMessenger.current
    val config = LocalRemoteConfig.current
    val edit by rememberUpdatedState(onEditDraft)
    val published by rememberUpdatedState(onPublished)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                PublishEvent.SignInRequired -> signingIn = true
                is PublishEvent.Published -> published(creationId)
                is PublishEvent.Share -> share.share(shareText(event.text, event.url))
                is PublishEvent.EditDraft -> edit(event.templateId, event.creationId)
            }
        }
    }
    SystemBack(enabled = state.stage == PublishStage.CONFIRMING) {}

    val supportSubject = stringResource(Res.string.publishing_support_subject)
    val supportBody = stringResource(Res.string.publishing_support_body, creationId)
    val noMailApp = stringResource(Res.string.notice_no_mail_app, config.links.supportEmail)
    PaywallScreen(
        state = state,
        onBack = onBack,
        onSelect = viewModel::select,
        onPurchase = viewModel::purchase,
        onEditDraft = viewModel::editDraft,
        onTerms = { links.openPage(config.links.terms) },
        onPrivacy = { links.openPage(config.links.privacyPolicy) },
        onContactSupport = {
            links.composeEmail(config.links.supportEmail, supportSubject, supportBody, onUnavailable = { messenger.show(noMailApp) })
        },
        onRetryLoad = viewModel::retryLoad,
    )
    if (signingIn) {
        SignInSheet(
            onDismiss = { signingIn = false },
            onSignedIn = {
                signingIn = false
                viewModel.purchase()
            },
        )
    }
}

/**
 * "Your moment is live", over My Moments: the link to share, the QR code, and the page itself. The
 * moment is loaded fresh — the paywall that published it is gone from the back stack.
 */
@Composable
fun PublishedRoute(creationId: String, onDone: () -> Unit, onOpenPage: (url: String) -> Unit) {
    val viewModel: MomentDetailViewModel = koinViewModel { parametersOf(creationId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MessageEffect(state.message, viewModel::messageShown)
    val share = rememberShareSheet()
    val copier = rememberTextCopier()
    val messenger = LocalMessenger.current
    val open by rememberUpdatedState(onOpenPage)
    var showQr by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is MomentDetailEvent.Share -> share.share(shareText(event.text, event.url))
                is MomentDetailEvent.OpenUrl -> open(event.url)
                else -> Unit
            }
        }
    }
    val shareLine = stringResource(Res.string.share_line)
    val copied = stringResource(Res.string.live_copied)
    PublishedScreen(
        creation = state.creation,
        linkPreview = state.linkPreview,
        categoryId = state.template?.categoryId,
        isLoading = state.isLoading,
        error = state.error,
        onDone = onDone,
        onCopy = { url ->
            copier.copy(url)
            messenger.show(copied, MessageTone.Success)
        },
        onShareTo = { target ->
            val url = state.creation?.shareUrl ?: return@PublishedScreen
            val text = shareText(shareLine, url)
            if (target == null) share.share(text) else share.shareTo(target, text)
        },
        onShowQr = { showQr = true },
        onRetry = viewModel::refresh,
    )
    val url = state.creation?.shareUrl
    if (showQr && url != null) {
        QrSheet(url = url, momentTitle = state.linkPreview?.title, shareLine = shareLine, onDismiss = { showQr = false })
    }
}
