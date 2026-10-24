package com.ferbotz.momentic.feature.publish

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ferbotz.momentic.designsystem.components.InlineBanner
import com.ferbotz.momentic.designsystem.components.MessageTone
import com.ferbotz.momentic.designsystem.components.MomenticBottomSheet
import com.ferbotz.momentic.designsystem.components.MomenticIconButton
import com.ferbotz.momentic.designsystem.components.PrimaryButton
import com.ferbotz.momentic.designsystem.components.SkeletonBlock
import com.ferbotz.momentic.designsystem.icons.MomenticIcons
import com.ferbotz.momentic.feature.account.SignInSheet
import com.ferbotz.momentic.presentation.config.LocalRemoteConfig
import com.ferbotz.momentic.presentation.di.koinViewModel
import com.ferbotz.momentic.presentation.links.rememberLinkOpener
import com.ferbotz.momentic.presentation.publish.PublishEvent
import com.ferbotz.momentic.presentation.publish.PublishStage
import com.ferbotz.momentic.presentation.publish.PublishUiState
import com.ferbotz.momentic.presentation.publish.PublishViewModel
import com.ferbotz.momentic.presentation.util.shortLabel
import com.ferbotz.momentic.resources.Res
import com.ferbotz.momentic.resources.cd_close
import com.ferbotz.momentic.resources.extend_same_link
import com.ferbotz.momentic.resources.extend_subtitle
import com.ferbotz.momentic.resources.extend_subtitle_ended
import com.ferbotz.momentic.resources.extend_title
import com.ferbotz.momentic.resources.paywall_choices
import com.ferbotz.momentic.resources.paywall_one_time
import com.ferbotz.momentic.resources.publishing_failed_body
import com.ferbotz.momentic.theme.Momentic
import com.ferbotz.momentic.theme.MomenticShapes
import com.ferbotz.momentic.theme.MomenticSpacing
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.core.parameter.parametersOf
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * "Add more time" over a published moment: the durations, each counted on from when the link would have
 * ended — the link itself never changes. [onExtended] runs once the server has the new end ([until]).
 */
@Composable
fun ExtendSheet(creationId: String, momentTitle: String, onDismiss: () -> Unit, onExtended: (until: Instant?) -> Unit) {
    // A fresh view model each time the sheet opens, so a finished renewal doesn't greet the next one —
    // but the same one across a rotation, so a purchase under way isn't lost.
    val opening = rememberSaveable { Random.nextLong().toString() }
    val viewModel: PublishViewModel = koinViewModel(key = "extend-$creationId-$opening") { parametersOf(creationId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val links = rememberLinkOpener()
    val config = LocalRemoteConfig.current
    var signingIn by remember { mutableStateOf(false) }
    val extended by rememberUpdatedState(onExtended)
    MomenticBottomSheet(onDismissRequest = onDismiss) {
        val close = dismiss
        LaunchedEffect(viewModel) {
            viewModel.events.collect { event ->
                when (event) {
                    PublishEvent.SignInRequired -> signingIn = true
                    is PublishEvent.Published -> {
                        extended(event.result.expiresAt)
                        close()
                    }
                    else -> Unit
                }
            }
        }
        ExtendContent(
            state = state,
            momentTitle = momentTitle,
            onSelect = { productId ->
                viewModel.messageShown()
                viewModel.select(productId)
            },
            onAdd = {
                viewModel.messageShown()
                viewModel.purchase()
            },
            onTerms = { links.openPage(config.links.terms) },
            onPrivacy = { links.openPage(config.links.privacyPolicy) },
            onClose = close,
        )
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

/** The sheet's content. Its notes show inside it — a snackbar would sit behind the sheet. */
@Composable
fun ExtendContent(
    state: PublishUiState,
    momentTitle: String,
    onSelect: (productId: String) -> Unit,
    onAdd: () -> Unit,
    onTerms: () -> Unit,
    onPrivacy: () -> Unit,
    onClose: () -> Unit,
    now: Instant = Clock.System.now(),
) {
    val c = Momentic.colors
    val expiry = state.creation?.expiresAt
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MomenticSpacing.xxs)) {
                Text(stringResource(Res.string.extend_title), style = Momentic.type.h1, color = c.ink)
                Text(
                    text = if (expiry != null && expiry > now) {
                        stringResource(Res.string.extend_subtitle, momentTitle, expiry.toLocalDateTime(TimeZone.currentSystemDefault()).date.shortLabel())
                    } else {
                        stringResource(Res.string.extend_subtitle_ended, momentTitle)
                    },
                    style = Momentic.type.caption,
                    color = c.inkSoft,
                )
            }
            MomenticIconButton(MomenticIcons.Close, stringResource(Res.string.cd_close), onClick = onClose)
        }
        Row(
            Modifier
                .padding(top = MomenticSpacing.sm)
                .fillMaxWidth()
                .clip(MomenticShapes.input)
                .background(c.blush100)
                .padding(horizontal = 14.dp, vertical = MomenticSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(MomenticIcons.Link, contentDescription = null, tint = c.roseDeep, modifier = Modifier.size(20.dp))
            Text(stringResource(Res.string.extend_same_link), style = Momentic.type.bodySmall, color = c.ink)
        }
        if (!state.isLoading && !state.isAvailable && state.unavailableReason != null) {
            InlineBanner(state.unavailableReason, MessageTone.Warning, modifier = Modifier.padding(top = MomenticSpacing.sm))
        }
        if (state.pendingPurchase != null) {
            InlineBanner(
                text = stringResource(Res.string.publishing_failed_body),
                lead = state.confirmError,
                tone = MessageTone.Error,
                modifier = Modifier.padding(top = MomenticSpacing.sm),
            )
        }
        state.message?.let { InlineBanner(it, MessageTone.Error, modifier = Modifier.padding(top = MomenticSpacing.sm)) }
        if (state.isLoading) {
            Column(Modifier.padding(top = MomenticSpacing.md), verticalArrangement = Arrangement.spacedBy(MomenticSpacing.xs)) {
                repeat(3) { SkeletonBlock(Modifier.fillMaxWidth().height(56.dp), MomenticShapes.input) }
            }
        } else {
            DurationChoices(
                rows = durationRows(state, now),
                selectedId = state.selectedProductId,
                onSelect = onSelect,
                label = stringResource(Res.string.paywall_choices),
                compact = true,
                modifier = Modifier.padding(top = MomenticSpacing.md),
            )
        }
        Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(MomenticSpacing.xs), verticalAlignment = Alignment.CenterVertically) {
            Icon(MomenticIcons.Shield, contentDescription = null, tint = c.success, modifier = Modifier.size(18.dp))
            Text(stringResource(Res.string.paywall_one_time), style = Momentic.type.label, color = c.ink)
        }
        PrimaryButton(
            text = purchaseLabel(state),
            onClick = onAdd,
            enabled = state.canPurchase || state.pendingPurchase != null,
            loading = state.stage == PublishStage.PURCHASING || state.stage == PublishStage.CONFIRMING,
            modifier = Modifier.fillMaxWidth().padding(top = MomenticSpacing.md),
        )
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            LegalLinks(onTerms, onPrivacy, Modifier.padding(top = MomenticSpacing.xxs))
        }
    }
}
