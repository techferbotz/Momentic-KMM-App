package com.ferbotz.momentic.feature.publish

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ferbotz.momentic.data.repository.PublishKind
import com.ferbotz.momentic.designsystem.components.ArtImage
import com.ferbotz.momentic.designsystem.components.EmptyState
import com.ferbotz.momentic.designsystem.components.InlineBanner
import com.ferbotz.momentic.designsystem.components.MessageTone
import com.ferbotz.momentic.designsystem.components.MomenticTopBar
import com.ferbotz.momentic.designsystem.components.PrimaryButton
import com.ferbotz.momentic.designsystem.components.QuietButton
import com.ferbotz.momentic.designsystem.components.SecondaryButton
import com.ferbotz.momentic.designsystem.components.SkeletonBlock
import com.ferbotz.momentic.designsystem.components.Tier
import com.ferbotz.momentic.designsystem.components.TierBadge
import com.ferbotz.momentic.designsystem.icons.MomenticIcons
import com.ferbotz.momentic.designsystem.icons.MomenticIllustrations
import com.ferbotz.momentic.presentation.publish.DurationOption
import com.ferbotz.momentic.presentation.publish.PublishStage
import com.ferbotz.momentic.presentation.publish.PublishUiState
import com.ferbotz.momentic.presentation.publish.liveUntil
import com.ferbotz.momentic.presentation.util.longLabel
import com.ferbotz.momentic.resources.Res
import com.ferbotz.momentic.resources.action_try_again
import com.ferbotz.momentic.resources.extend_add
import com.ferbotz.momentic.resources.extend_add_no_price
import com.ferbotz.momentic.resources.extend_plus
import com.ferbotz.momentic.resources.extend_title
import com.ferbotz.momentic.resources.extend_until
import com.ferbotz.momentic.resources.paywall_choices
import com.ferbotz.momentic.resources.paywall_days
import com.ferbotz.momentic.resources.paywall_finish
import com.ferbotz.momentic.resources.paywall_finish_form
import com.ferbotz.momentic.resources.paywall_intro
import com.ferbotz.momentic.resources.paywall_load_failed
import com.ferbotz.momentic.resources.paywall_missing
import com.ferbotz.momentic.resources.paywall_one_time
import com.ferbotz.momentic.resources.paywall_per_day
import com.ferbotz.momentic.resources.paywall_per_day_until
import com.ferbotz.momentic.resources.paywall_publish
import com.ferbotz.momentic.resources.paywall_publish_no_price
import com.ferbotz.momentic.resources.paywall_title
import com.ferbotz.momentic.resources.publishing_body
import com.ferbotz.momentic.resources.publishing_contact
import com.ferbotz.momentic.resources.publishing_failed_body
import com.ferbotz.momentic.resources.publishing_failed_title
import com.ferbotz.momentic.resources.publishing_title
import com.ferbotz.momentic.resources.signin_privacy
import com.ferbotz.momentic.resources.signin_terms
import com.ferbotz.momentic.theme.Momentic
import com.ferbotz.momentic.theme.MomenticElevation
import com.ferbotz.momentic.theme.MomenticShapes
import com.ferbotz.momentic.theme.MomenticSizes
import com.ferbotz.momentic.theme.MomenticSpacing
import com.ferbotz.momentic.theme.momenticShadow
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Choose how long, and the publishing that follows: the paywall while choosing, "Making your link…"
 * while the server confirms, and "We couldn't make your link yet" when a paid purchase didn't confirm
 * ([onPurchase] then finishes it — never a second charge).
 */
@Composable
fun PaywallScreen(
    state: PublishUiState,
    onBack: () -> Unit,
    onSelect: (productId: String) -> Unit,
    onPurchase: () -> Unit,
    onEditDraft: () -> Unit,
    onTerms: () -> Unit,
    onPrivacy: () -> Unit,
    onContactSupport: () -> Unit,
    onRetryLoad: () -> Unit,
    modifier: Modifier = Modifier,
    now: Instant = Clock.System.now(),
) {
    when {
        state.stage == PublishStage.CONFIRMING || state.stage == PublishStage.DONE -> PublishingProgress(modifier)
        state.pendingPurchase != null -> PublishingFailed(state.confirmError, onTryAgain = onPurchase, onContactSupport, modifier)
        else -> Paywall(state, onBack, onSelect, onPurchase, onEditDraft, onTerms, onPrivacy, onRetryLoad, now, modifier)
    }
}

@Composable
private fun Paywall(
    state: PublishUiState,
    onBack: () -> Unit,
    onSelect: (String) -> Unit,
    onPurchase: () -> Unit,
    onEditDraft: () -> Unit,
    onTerms: () -> Unit,
    onPrivacy: () -> Unit,
    onRetryLoad: () -> Unit,
    now: Instant,
    modifier: Modifier,
) {
    val c = Momentic.colors
    val renewing = state.kind == PublishKind.RENEW
    Column(modifier.fillMaxSize().background(c.background)) {
        MomenticTopBar(
            title = stringResource(if (renewing) Res.string.extend_title else Res.string.paywall_title),
            onBack = onBack,
            background = c.background,
        )
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.isLoading -> PaywallSkeleton()
                state.loadError != null -> EmptyState(
                    illustration = MomenticIllustrations.BrokenLink,
                    title = stringResource(Res.string.paywall_load_failed),
                    body = state.loadError,
                    action = { SecondaryButton(stringResource(Res.string.action_try_again), onClick = onRetryLoad, icon = MomenticIcons.Retry) },
                    modifier = Modifier.padding(MomenticSpacing.gutter).padding(top = MomenticSpacing.xl),
                )
                else -> Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(start = MomenticSpacing.gutter, end = MomenticSpacing.gutter, top = MomenticSpacing.xs, bottom = MomenticSpacing.lg),
                ) {
                    MomentSummary(state)
                    Text(
                        stringResource(Res.string.paywall_intro),
                        style = Momentic.type.bodySmall,
                        color = c.inkSoft,
                        modifier = Modifier.padding(top = MomenticSpacing.gutter, bottom = MomenticSpacing.sm),
                    )
                    if (state.missingFields.isNotEmpty()) {
                        InlineBanner(
                            text = stringResource(Res.string.paywall_missing, state.missingFields.joinToString()),
                            tone = MessageTone.Info,
                            actionLabel = stringResource(Res.string.paywall_finish_form),
                            onAction = onEditDraft,
                            modifier = Modifier.padding(bottom = MomenticSpacing.sm),
                        )
                    }
                    if (!state.isAvailable && state.unavailableReason != null) {
                        InlineBanner(state.unavailableReason, MessageTone.Warning, modifier = Modifier.padding(bottom = MomenticSpacing.sm))
                    }
                    DurationChoices(
                        rows = durationRows(state, now),
                        selectedId = state.selectedProductId,
                        onSelect = onSelect,
                        label = stringResource(Res.string.paywall_choices),
                    )
                    Row(
                        Modifier.padding(top = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(MomenticSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(MomenticIcons.Shield, contentDescription = null, tint = c.success, modifier = Modifier.size(18.dp))
                        Text(stringResource(Res.string.paywall_one_time), style = Momentic.type.label, color = c.ink)
                    }
                    LegalLinks(onTerms, onPrivacy, Modifier.padding(top = MomenticSpacing.xxs))
                }
            }
        }
        if (!state.isLoading && state.loadError == null) {
            HorizontalDivider(thickness = 1.dp, color = c.line)
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(c.surface)
                    .padding(horizontal = MomenticSpacing.gutter, vertical = MomenticSpacing.sm)
                    .windowInsetsPadding(WindowInsets.navigationBars),
            ) {
                PrimaryButton(
                    text = purchaseLabel(state),
                    onClick = onPurchase,
                    enabled = state.canPurchase,
                    loading = state.stage == PublishStage.PURCHASING,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** The moment being published: its template's art, name and tier. */
@Composable
private fun MomentSummary(state: PublishUiState) {
    val c = Momentic.colors
    val template = state.template
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .momenticShadow(MomenticElevation.Rest, MomenticShapes.card)
            .clip(MomenticShapes.card)
            .background(c.surface)
            .padding(MomenticSpacing.sm)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtImage(
            url = template?.thumbnailUrl,
            categoryId = template?.categoryId,
            contentDescription = null,
            modifier = Modifier.size(width = 56.dp, height = 70.dp).clip(MomenticShapes.thumb),
        )
        Column(verticalArrangement = Arrangement.spacedBy(MomenticSpacing.xxs)) {
            Text(template?.name ?: state.creation?.templateName.orEmpty(), style = Momentic.type.cardTitle, color = c.ink)
            Tier.fromKey(template?.tierKey)?.let { tier -> TierBadge(tier, label = state.tierLabel.ifBlank { null }) }
        }
    }
}

/** Terms & Conditions · Privacy Policy — from remote config, as every paywall must show them. */
@Composable
internal fun LegalLinks(onTerms: () -> Unit, onPrivacy: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        QuietButton(stringResource(Res.string.signin_terms), onClick = onTerms, compact = true)
        Text("·", style = Momentic.type.caption, color = Momentic.colors.inkSoft)
        QuietButton(stringResource(Res.string.signin_privacy), onClick = onPrivacy, compact = true)
    }
}

/** The durations as the paywall and the Extend sheet write them. A week is "Most chosen", until the backend says otherwise. */
@Composable
internal fun durationRows(state: PublishUiState, now: Instant): List<DurationRow> {
    val zone = TimeZone.currentSystemDefault()
    val renewing = state.kind == PublishKind.RENEW
    val expiry = if (renewing) state.creation?.expiresAt else null
    return state.options.map { option ->
        val until = liveUntil(option.days, now, expiry, zone).longLabel()
        val days = daysText(option)
        if (renewing) {
            val line = stringResource(Res.string.extend_until, until)
            DurationRow(option.productId, stringResource(Res.string.extend_plus, days), line, line, option.price, option.days == MOST_CHOSEN_DAYS)
        } else {
            val perDay = option.perDay
            DurationRow(
                productId = option.productId,
                title = days,
                subtitle = perDay?.let { stringResource(Res.string.paywall_per_day, it) } ?: stringResource(Res.string.extend_until, until),
                selectedSubtitle = perDay?.let { stringResource(Res.string.paywall_per_day_until, it, until) } ?: stringResource(Res.string.extend_until, until),
                price = option.price,
                mostChosen = option.days == MOST_CHOSEN_DAYS,
            )
        }
    }
}

@Composable
private fun daysText(option: DurationOption): String = pluralStringResource(Res.plurals.paywall_days, option.days, option.days)

/** "Publish for 7 days · ₹599", "Add 7 days · ₹599" — or "Finish publishing" for a paid purchase still to confirm. */
@Composable
internal fun purchaseLabel(state: PublishUiState): String {
    if (state.pendingPurchase != null) return stringResource(Res.string.paywall_finish)
    val option = state.selectedOption ?: return stringResource(if (state.kind == PublishKind.RENEW) Res.string.extend_title else Res.string.paywall_title)
    val days = daysText(option)
    val price = option.price
    return when (state.kind) {
        PublishKind.RENEW -> if (price != null) stringResource(Res.string.extend_add, days, price) else stringResource(Res.string.extend_add_no_price, days)
        PublishKind.PUBLISH -> if (price != null) stringResource(Res.string.paywall_publish, days, price) else stringResource(Res.string.paywall_publish_no_price, days)
    }
}

private const val MOST_CHOSEN_DAYS = 7

/** "Making your link…": a turning ring round the link, on a soft blush disc. */
@Composable
internal fun PublishingProgress(modifier: Modifier = Modifier) {
    val c = Momentic.colors
    val still = Momentic.reduceMotion
    BoxWithConstraints(modifier.fillMaxSize().background(c.background)) {
        Box(
            Modifier
                .offset(x = -(60.dp), y = 170.dp)
                .requiredSize(maxWidth + 120.dp)
                .alpha(0.55f)
                .clip(CircleShape)
                .background(c.blush100),
        )
        Column(
            Modifier.fillMaxSize().padding(horizontal = MomenticSpacing.xl).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MomenticSpacing.gutter, Alignment.CenterVertically),
        ) {
            Box(Modifier.size(104.dp), contentAlignment = Alignment.Center) {
                if (still) {
                    CircularProgressIndicator(progress = { 0.3f }, modifier = Modifier.fillMaxSize(), color = c.rose, trackColor = c.blush200, strokeWidth = 4.dp, gapSize = 0.dp)
                } else {
                    CircularProgressIndicator(modifier = Modifier.fillMaxSize(), color = c.rose, trackColor = c.blush200, strokeWidth = 4.dp, gapSize = 0.dp)
                }
                Box(
                    Modifier.size(72.dp).momenticShadow(MomenticElevation.Raised, CircleShape).clip(CircleShape).background(c.surface),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(MomenticIcons.Link, contentDescription = null, tint = c.roseDeep, modifier = Modifier.size(30.dp))
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(MomenticSpacing.xs)) {
                Text(stringResource(Res.string.publishing_title), style = Momentic.type.h1, color = c.ink, textAlign = TextAlign.Center)
                Text(stringResource(Res.string.publishing_body), style = Momentic.type.body, color = c.inkSoft, textAlign = TextAlign.Center)
            }
        }
    }
}

/** A paid purchase that didn't confirm: the payment is safe, "Try again" finishes it, support is a tap away. */
@Composable
private fun PublishingFailed(reason: String?, onTryAgain: () -> Unit, onContactSupport: () -> Unit, modifier: Modifier) {
    val c = Momentic.colors
    Column(modifier.fillMaxSize().background(c.background).windowInsetsPadding(WindowInsets.statusBars)) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = MomenticSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MomenticSpacing.gutter, Alignment.CenterVertically),
        ) {
            Image(MomenticIllustrations.BrokenLink, contentDescription = null, modifier = Modifier.size(120.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(Res.string.publishing_failed_title),
                    style = Momentic.type.h1,
                    color = c.ink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
                Text(stringResource(Res.string.publishing_failed_body), style = Momentic.type.body, color = c.inkSoft, textAlign = TextAlign.Center)
            }
            if (reason != null) {
                Row(
                    Modifier
                        .clip(MomenticShapes.input)
                        .background(c.errorFill)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                    horizontalArrangement = Arrangement.spacedBy(MomenticSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(MomenticIcons.Alert, contentDescription = null, tint = c.error, modifier = Modifier.size(18.dp))
                    Text(reason, style = Momentic.type.label, color = c.error)
                }
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = MomenticSpacing.gutter, vertical = MomenticSpacing.md)
                .windowInsetsPadding(WindowInsets.navigationBars),
            verticalArrangement = Arrangement.spacedBy(MomenticSpacing.xs),
        ) {
            PrimaryButton(stringResource(Res.string.action_try_again), onClick = onTryAgain, icon = MomenticIcons.Retry, modifier = Modifier.fillMaxWidth())
            QuietButton(stringResource(Res.string.publishing_contact), onClick = onContactSupport, modifier = Modifier.fillMaxWidth().height(MomenticSizes.minTouchTarget))
        }
    }
}

@Composable
private fun PaywallSkeleton() {
    Column(Modifier.padding(MomenticSpacing.gutter), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SkeletonBlock(Modifier.fillMaxWidth().height(94.dp), MomenticShapes.card)
        SkeletonBlock(Modifier.padding(top = MomenticSpacing.md).width(280.dp).height(14.dp), RoundedCornerShape(7.dp))
        repeat(5) { SkeletonBlock(Modifier.fillMaxWidth().height(64.dp), RoundedCornerShape(16.dp)) }
    }
}
