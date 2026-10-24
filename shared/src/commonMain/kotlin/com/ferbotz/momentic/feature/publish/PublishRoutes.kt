package com.ferbotz.momentic.feature.publish

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
import com.ferbotz.momentic.data.repository.PublishKind
import com.ferbotz.momentic.designsystem.components.InlineBanner
import com.ferbotz.momentic.designsystem.components.MessageTone
import com.ferbotz.momentic.designsystem.components.MomenticButton
import com.ferbotz.momentic.designsystem.components.ButtonStyle
import com.ferbotz.momentic.designsystem.components.PrimaryButton
import com.ferbotz.momentic.designsystem.components.SecondaryButton
import com.ferbotz.momentic.feature.account.SignInSheet
import com.ferbotz.momentic.feature.shell.MessageEffect
import com.ferbotz.momentic.feature.shell.StandInNote
import com.ferbotz.momentic.feature.shell.StandInScreen
import com.ferbotz.momentic.feature.shell.StandInStatus
import com.ferbotz.momentic.platform.rememberShareSheet
import com.ferbotz.momentic.platform.shareText
import com.ferbotz.momentic.presentation.di.koinViewModel
import com.ferbotz.momentic.presentation.moments.MomentDetailEvent
import com.ferbotz.momentic.presentation.moments.MomentDetailViewModel
import com.ferbotz.momentic.presentation.publish.PublishEvent
import com.ferbotz.momentic.presentation.publish.PublishStage
import com.ferbotz.momentic.presentation.publish.PublishViewModel
import org.koin.core.parameter.parametersOf

/**
 * Choose how long, then pay and publish — or, for a published moment, renew it (same link, more days).
 * Not signed in, it asks first ([SignInSheet]) and carries on with the purchase. A draft that's missing
 * something goes back to its form.
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

    StandInScreen(title = if (state.kind == PublishKind.RENEW) "Add more days" else "Choose how long", onBack = onBack) {
        StandInStatus(state.isLoading, state.loadError, viewModel::retryLoad)
        if (!state.isLoading && state.loadError == null) {
            StandInNote("${state.tierLabel} · ${state.displayPerDay}")
            if (!state.isAvailable) InlineBanner(state.unavailableReason ?: "Publishing isn't available right now.", MessageTone.Warning)
            if (state.missingFields.isNotEmpty()) {
                InlineBanner("Still to fill in: " + state.missingFields.joinToString(), MessageTone.Info)
                SecondaryButton("Finish the form", onClick = viewModel::editDraft, modifier = Modifier.fillMaxWidth())
            }
            state.options.forEach { option ->
                MomenticButton(
                    text = "${option.days} days" + (option.price?.let { " · $it" } ?: ""),
                    onClick = { viewModel.select(option.productId) },
                    style = if (option.productId == state.selectedProductId) ButtonStyle.Primary else ButtonStyle.Secondary,
                    modifier = Modifier.fillMaxWidth(),
                    compact = true,
                )
            }
            PrimaryButton(
                text = if (state.pendingPurchase != null) "Finish publishing" else "Publish",
                onClick = viewModel::purchase,
                enabled = state.canPurchase || state.pendingPurchase != null,
                loading = state.stage == PublishStage.PURCHASING || state.stage == PublishStage.CONFIRMING,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
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
    val open by rememberUpdatedState(onOpenPage)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is MomentDetailEvent.Share -> share.share(shareText(event.text, event.url))
                is MomentDetailEvent.OpenUrl -> open(event.url)
                else -> Unit
            }
        }
    }
    StandInScreen(title = "Your moment is live", onBack = onDone) {
        StandInStatus(state.isLoading, state.error, viewModel::refresh)
        state.creation?.shareUrl?.let { url ->
            StandInNote(url)
            PrimaryButton("Share the link", onClick = viewModel::share, modifier = Modifier.fillMaxWidth())
            SecondaryButton("Open your moment", onClick = viewModel::open, loading = state.isOpening, modifier = Modifier.fillMaxWidth())
        }
        SecondaryButton("Done", onClick = onDone, modifier = Modifier.fillMaxWidth())
    }
}
