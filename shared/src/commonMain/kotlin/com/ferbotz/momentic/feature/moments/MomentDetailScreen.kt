package com.ferbotz.momentic.feature.moments

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ferbotz.momentic.data.model.Creation
import com.ferbotz.momentic.data.model.CreationStats
import com.ferbotz.momentic.data.model.RsvpSummary
import com.ferbotz.momentic.designsystem.components.ArtImage
import com.ferbotz.momentic.designsystem.components.ButtonStyle
import com.ferbotz.momentic.designsystem.components.EmptyState
import com.ferbotz.momentic.designsystem.components.InlineBanner
import com.ferbotz.momentic.designsystem.components.MessageTone
import com.ferbotz.momentic.designsystem.components.MomenticButton
import com.ferbotz.momentic.designsystem.components.MomenticTopBar
import com.ferbotz.momentic.designsystem.components.PrimaryButton
import com.ferbotz.momentic.designsystem.components.SecondaryButton
import com.ferbotz.momentic.designsystem.components.SkeletonBlock
import com.ferbotz.momentic.designsystem.components.StatusChip
import com.ferbotz.momentic.designsystem.components.ViewsBarChart
import com.ferbotz.momentic.designsystem.icons.MomenticIcons
import com.ferbotz.momentic.designsystem.icons.MomenticIllustrations
import com.ferbotz.momentic.presentation.moments.MomentDetailUiState
import com.ferbotz.momentic.presentation.moments.MomentStatus
import com.ferbotz.momentic.presentation.moments.interimTitle
import com.ferbotz.momentic.presentation.moments.momentStatus
import com.ferbotz.momentic.presentation.util.dayMonthLabel
import com.ferbotz.momentic.presentation.util.longLabel
import com.ferbotz.momentic.resources.Res
import com.ferbotz.momentic.resources.action_try_again
import com.ferbotz.momentic.resources.moment_back
import com.ferbotz.momentic.resources.moment_bring_back
import com.ferbotz.momentic.resources.moment_copy
import com.ferbotz.momentic.resources.moment_days
import com.ferbotz.momentic.resources.moment_days_left_of
import com.ferbotz.momentic.resources.moment_delete
import com.ferbotz.momentic.resources.moment_draft_body
import com.ferbotz.momentic.resources.moment_draft_title
import com.ferbotz.momentic.resources.moment_edit
import com.ferbotz.momentic.resources.moment_ended_body
import com.ferbotz.momentic.resources.moment_ended_body_plain
import com.ferbotz.momentic.resources.moment_ended_title
import com.ferbotz.momentic.resources.moment_ends_today
import com.ferbotz.momentic.resources.moment_extend
import com.ferbotz.momentic.resources.moment_guests
import com.ferbotz.momentic.resources.moment_link_offline
import com.ferbotz.momentic.resources.moment_live_until
import com.ferbotz.momentic.resources.moment_load_failed
import com.ferbotz.momentic.resources.moment_preview
import com.ferbotz.momentic.resources.moment_publish
import com.ferbotz.momentic.resources.moment_replies
import com.ferbotz.momentic.resources.moment_replies_none
import com.ferbotz.momentic.resources.moment_replies_summary
import com.ferbotz.momentic.resources.moment_replies_waiting
import com.ferbotz.momentic.resources.moment_share
import com.ferbotz.momentic.resources.moment_stats_failed
import com.ferbotz.momentic.resources.moment_time_left
import com.ferbotz.momentic.resources.moment_views
import com.ferbotz.momentic.resources.moment_views_since
import com.ferbotz.momentic.resources.moment_views_while_live
import com.ferbotz.momentic.resources.moment_your_link
import com.ferbotz.momentic.theme.Momentic
import com.ferbotz.momentic.theme.MomenticElevation
import com.ferbotz.momentic.theme.MomenticShapes
import com.ferbotz.momentic.theme.MomenticSizes
import com.ferbotz.momentic.theme.MomenticSpacing
import com.ferbotz.momentic.theme.literal
import com.ferbotz.momentic.theme.momenticShadow
import com.ferbotz.momentic.theme.occasionTint
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * One moment. Live: its link, views by day, time left and — for an invitation — the guests' replies,
 * with Extend, Edit, Preview and Delete. Ended: a way to bring it back on the same link. A draft: edit,
 * preview, publish.
 */
@Composable
fun MomentDetailScreen(
    state: MomentDetailUiState,
    onBack: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onOpen: () -> Unit,
    onPublishOrRenew: () -> Unit,
    onViewRsvps: () -> Unit,
    onDelete: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    now: Instant = Clock.System.now(),
) {
    val c = Momentic.colors
    val creation = state.creation
    Column(modifier.fillMaxSize().background(c.background)) {
        MomenticTopBar(onBack = onBack, navigationLabel = stringResource(Res.string.moment_back), background = c.background)
        when {
            creation != null -> Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = MomenticSpacing.gutter, end = MomenticSpacing.gutter, top = MomenticSpacing.xs, bottom = MomenticSpacing.xl)
                    .windowInsetsPadding(WindowInsets.navigationBars),
                verticalArrangement = Arrangement.spacedBy(MomenticSpacing.md),
            ) {
                val zone = TimeZone.currentSystemDefault()
                val status = creation.momentStatus(now, zone)
                MomentHeader(state, creation, status)
                when (status) {
                    MomentStatus.Draft, null -> DraftBody(state, onEdit, onOpen, onPublishOrRenew)
                    MomentStatus.Ended -> EndedBody(state, creation, zone, onCopy, onShare, onPublishOrRenew)
                    else -> LiveBody(state, creation, now, zone, onCopy, onShare, onEdit, onOpen, onPublishOrRenew, onViewRsvps)
                }
                MomenticButton(
                    text = stringResource(Res.string.moment_delete),
                    onClick = onDelete,
                    style = ButtonStyle.QuietDestructive,
                    icon = MomenticIcons.Trash,
                    loading = state.isDeleting,
                    modifier = Modifier.fillMaxWidth().height(MomenticSizes.minTouchTarget),
                )
            }
            state.isLoading -> DetailSkeleton()
            else -> EmptyState(
                illustration = MomenticIllustrations.BrokenLink,
                title = stringResource(Res.string.moment_load_failed),
                body = state.error.orEmpty(),
                action = { SecondaryButton(stringResource(Res.string.action_try_again), onClick = onRetry, icon = MomenticIcons.Retry) },
                modifier = Modifier.padding(MomenticSpacing.gutter).padding(top = MomenticSpacing.xl),
            )
        }
    }
}

/** The name a moment goes by: its page's own title once it's live, else its first short line, else its template's name. */
internal fun MomentDetailUiState.momentTitle(): String {
    val creation = creation
    return linkPreview?.title?.takeIf { it.isNotBlank() }
        ?: creation?.interimTitle()
        ?: template?.name
        ?: creation?.templateName.orEmpty()
}

@Composable
private fun MomentHeader(state: MomentDetailUiState, creation: Creation, status: MomentStatus?) {
    val c = Momentic.colors
    Row(horizontalArrangement = Arrangement.spacedBy(MomenticSpacing.md), verticalAlignment = Alignment.CenterVertically) {
        ArtImage(
            url = state.template?.thumbnailUrl,
            categoryId = state.template?.categoryId,
            contentDescription = null,
            muted = status == MomentStatus.Ended,
            modifier = Modifier
                .size(width = 96.dp, height = 120.dp)
                .momenticShadow(MomenticElevation.Rest, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp)),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(state.momentTitle(), style = Momentic.type.h1, color = c.ink, modifier = Modifier.semantics { heading() })
            Text(state.template?.name ?: creation.templateName, style = Momentic.type.bodySmall, color = c.inkSoft)
            if (status != null) StatusChip(status)
        }
    }
}

@Composable
private fun LiveBody(
    state: MomentDetailUiState,
    creation: Creation,
    now: Instant,
    zone: TimeZone,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onOpen: () -> Unit,
    onExtend: () -> Unit,
    onViewRsvps: () -> Unit,
) {
    creation.shareUrl?.let { url -> LinkCard(url, offline = false, onCopy, onShare) }
    if (state.showStats) ViewsCard(state, creation, zone)
    creation.expiresAt?.let { end -> TimeTiles(creation.publishedAt, end, now, zone) }
    val rsvp = state.stats?.rsvp
    if (rsvp != null || state.template?.supportsRsvp == true) RepliesCard(rsvp, onViewRsvps)
    Column(verticalArrangement = Arrangement.spacedBy(MomenticSpacing.sm)) {
        if (state.canRenew) {
            PrimaryButton(stringResource(Res.string.moment_extend), onClick = onExtend, icon = MomenticIcons.CalendarPlus, modifier = Modifier.fillMaxWidth())
        }
        Row(horizontalArrangement = Arrangement.spacedBy(MomenticSpacing.sm)) {
            SecondaryButton(stringResource(Res.string.moment_edit), onClick = onEdit, icon = MomenticIcons.Pencil, modifier = Modifier.weight(1f))
            SecondaryButton(stringResource(Res.string.moment_preview), onClick = onOpen, icon = MomenticIcons.Eye, loading = state.isOpening, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun EndedBody(
    state: MomentDetailUiState,
    creation: Creation,
    zone: TimeZone,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onBringBack: () -> Unit,
) {
    val c = Momentic.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(MomenticShapes.card)
            .background(c.surface)
            .border(1.dp, c.line, MomenticShapes.card)
            .padding(MomenticSpacing.gutter),
        verticalArrangement = Arrangement.spacedBy(MomenticSpacing.xs),
    ) {
        Text(stringResource(Res.string.moment_ended_title), style = Momentic.type.h2, color = c.ink, modifier = Modifier.semantics { heading() })
        val ended = creation.expiresAt?.toLocalDateTime(zone)?.date
        Text(
            text = ended?.let { stringResource(Res.string.moment_ended_body, it.longLabel()) } ?: stringResource(Res.string.moment_ended_body_plain),
            style = Momentic.type.bodySmall,
            color = c.ink,
        )
        if (state.canRenew) {
            PrimaryButton(
                stringResource(Res.string.moment_bring_back),
                onClick = onBringBack,
                icon = MomenticIcons.Retry,
                modifier = Modifier.fillMaxWidth().padding(top = MomenticSpacing.xs),
            )
        }
    }
    creation.shareUrl?.let { url -> LinkCard(url, offline = true, onCopy, onShare) }
    if (state.showStats) {
        val start = creation.publishedAt?.toLocalDateTime(zone)?.date
        val end = creation.expiresAt?.toLocalDateTime(zone)?.date
        Row(
            Modifier
                .fillMaxWidth()
                .momenticShadow(MomenticElevation.Rest, MomenticShapes.card)
                .clip(MomenticShapes.card)
                .background(c.surface)
                .padding(horizontal = MomenticSpacing.gutter, vertical = MomenticSpacing.md)
                .semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(Res.string.moment_views_while_live), style = Momentic.type.label, color = c.ink)
                if (start != null && end != null) {
                    Text("${start.longLabel()} – ${end.longLabel()}", style = Momentic.type.caption, color = c.inkSoft)
                }
            }
            Text((state.stats?.totalViews ?: creation.viewCount).toString(), style = Momentic.type.h1.copy(fontWeight = FontWeight.Bold, fontSize = 32.sp), color = c.ink)
        }
    }
}

@Composable
private fun DraftBody(state: MomentDetailUiState, onEdit: () -> Unit, onOpen: () -> Unit, onPublish: () -> Unit) {
    val c = Momentic.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(MomenticShapes.card)
            .background(c.surface)
            .border(1.dp, c.line, MomenticShapes.card)
            .padding(MomenticSpacing.gutter),
        verticalArrangement = Arrangement.spacedBy(MomenticSpacing.xs),
    ) {
        Text(stringResource(Res.string.moment_draft_title), style = Momentic.type.h2, color = c.ink)
        Text(stringResource(Res.string.moment_draft_body), style = Momentic.type.bodySmall, color = c.ink)
    }
    Column(verticalArrangement = Arrangement.spacedBy(MomenticSpacing.sm)) {
        if (state.canPublish) {
            PrimaryButton(stringResource(Res.string.moment_publish), onClick = onPublish, icon = MomenticIcons.Sparkle, modifier = Modifier.fillMaxWidth())
        }
        Row(horizontalArrangement = Arrangement.spacedBy(MomenticSpacing.sm)) {
            SecondaryButton(stringResource(Res.string.moment_edit), onClick = onEdit, icon = MomenticIcons.Pencil, modifier = Modifier.weight(1f))
            SecondaryButton(stringResource(Res.string.moment_preview), onClick = onOpen, icon = MomenticIcons.Eye, loading = state.isOpening, modifier = Modifier.weight(1f))
        }
    }
}

/** "Your link" with Copy and Share — greyed when the moment has ended and the link is offline. */
@Composable
private fun LinkCard(url: String, offline: Boolean, onCopy: () -> Unit, onShare: () -> Unit) {
    val c = Momentic.colors
    Column(
        Modifier
            .fillMaxWidth()
            .then(if (offline) Modifier else Modifier.momenticShadow(MomenticElevation.Rest, MomenticShapes.card))
            .clip(MomenticShapes.card)
            .background(c.surface)
            .then(if (offline) Modifier.border(1.dp, c.line, MomenticShapes.card) else Modifier)
            .padding(MomenticSpacing.md),
        verticalArrangement = Arrangement.spacedBy(MomenticSpacing.sm),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(if (offline) Res.string.moment_link_offline else Res.string.moment_your_link), style = Momentic.type.caption, color = c.inkSoft)
            Text(
                text = url.removePrefix("https://").removePrefix("http://"),
                style = Momentic.type.label.copy(fontSize = 15.sp).literal(),
                color = if (offline) c.inkSoft else c.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton(stringResource(Res.string.moment_copy), onClick = onCopy, icon = MomenticIcons.Copy, compact = true, modifier = Modifier.weight(1f))
            SecondaryButton(stringResource(Res.string.moment_share), onClick = onShare, icon = MomenticIcons.Share, compact = true, enabled = !offline, modifier = Modifier.weight(1f))
        }
    }
}

/** Views: the total, since when, and the bar chart of the last days (tap a day for its count). */
@Composable
private fun ViewsCard(state: MomentDetailUiState, creation: Creation, zone: TimeZone) {
    val c = Momentic.colors
    val stats: CreationStats? = state.stats
    Column(
        Modifier
            .fillMaxWidth()
            .momenticShadow(MomenticElevation.Rest, MomenticShapes.card)
            .clip(MomenticShapes.card)
            .background(c.surface)
            .padding(MomenticSpacing.gutter),
    ) {
        Text(stringResource(Res.string.moment_views), style = Momentic.type.label, color = c.ink, modifier = Modifier.semantics { heading() })
        Text(
            text = (stats?.totalViews ?: creation.viewCount).toString(),
            style = Momentic.type.body.copy(fontSize = 48.sp, lineHeight = 56.sp, fontWeight = FontWeight.Bold),
            color = c.ink,
            modifier = Modifier.padding(top = 2.dp),
        )
        creation.publishedAt?.let { since ->
            Text(stringResource(Res.string.moment_views_since, since.toLocalDateTime(zone).date.longLabel()), style = Momentic.type.caption, color = c.inkSoft)
        }
        when {
            stats != null && stats.dailyViews.isNotEmpty() -> ViewsBarChart(stats.dailyViews, Modifier.padding(top = MomenticSpacing.md))
            state.isLoadingStats -> SkeletonBlock(Modifier.padding(top = MomenticSpacing.md).fillMaxWidth().height(160.dp), MomenticShapes.input)
            state.statsError != null -> InlineBanner(stringResource(Res.string.moment_stats_failed), MessageTone.Warning, modifier = Modifier.padding(top = MomenticSpacing.md))
        }
    }
}

/** Time left (with how much of the paid window remains) and the day it ends. */
@Composable
private fun TimeTiles(publishedAt: Instant?, end: Instant, now: Instant, zone: TimeZone) {
    val c = Momentic.colors
    val endDay = end.toLocalDateTime(zone).date
    val daysLeft = now.toLocalDateTime(zone).date.daysUntil(endDay).coerceAtLeast(0)
    val totalDays = publishedAt?.toLocalDateTime(zone)?.date?.daysUntil(endDay)?.coerceAtLeast(1)
    Row(horizontalArrangement = Arrangement.spacedBy(MomenticSpacing.sm)) {
        Tile(Modifier.weight(1f)) {
            Text(stringResource(Res.string.moment_time_left), style = Momentic.type.caption, color = c.inkSoft)
            Text(
                text = if (daysLeft == 0) stringResource(Res.string.moment_ends_today) else pluralStringResource(Res.plurals.moment_days, daysLeft, daysLeft),
                style = Momentic.type.h2.copy(fontFamily = Momentic.type.body.fontFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
                color = c.ink,
            )
            if (totalDays != null) {
                val fraction = (daysLeft.toFloat() / totalDays).coerceIn(0f, 1f)
                val label = stringResource(Res.string.moment_days_left_of, daysLeft, totalDays)
                Box(
                    Modifier
                        .padding(top = MomenticSpacing.xxs)
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(MomenticShapes.pill)
                        .background(c.blush100)
                        .semantics { contentDescription = label },
                ) {
                    Box(Modifier.fillMaxWidth(fraction).height(6.dp).clip(MomenticShapes.pill).background(c.rose))
                }
            }
        }
        Tile(Modifier.weight(1f)) {
            Text(stringResource(Res.string.moment_live_until), style = Momentic.type.caption, color = c.inkSoft)
            Text(
                text = endDay.dayMonthLabel(),
                style = Momentic.type.h2.copy(fontFamily = Momentic.type.body.fontFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
                color = c.ink,
            )
            Text(endDay.dayOfWeek.fullName(), style = Momentic.type.caption, color = c.inkSoft)
        }
    }
}

@Composable
private fun Tile(modifier: Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .momenticShadow(MomenticElevation.Rest, MomenticShapes.card)
            .clip(MomenticShapes.card)
            .background(Momentic.colors.surface)
            .padding(MomenticSpacing.md)
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(MomenticSpacing.xxs),
    ) { content() }
}

/** "Guest replies · 32 — Yes 24 · Maybe 5 · No 3 · 61 guests", on the wedding tint; opens the list. */
@Composable
private fun RepliesCard(summary: RsvpSummary?, onClick: () -> Unit) {
    val c = Momentic.colors
    val tint = occasionTint("wedding")
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MomenticShapes.card)
            .background(tint.brush)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = MomenticSpacing.md, end = MomenticSpacing.sm, top = MomenticSpacing.md, bottom = MomenticSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(c.surface), contentAlignment = Alignment.Center) {
            Icon(MomenticIcons.Users, contentDescription = null, tint = tint.accent, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = if (summary != null && summary.responses > 0) stringResource(Res.string.moment_replies, summary.responses) else stringResource(Res.string.moment_replies_none),
                style = Momentic.type.body.copy(fontWeight = FontWeight.Bold),
                color = c.ink,
            )
            Text(
                text = if (summary != null && summary.responses > 0) {
                    val counts = stringResource(Res.string.moment_replies_summary, summary.yes, summary.maybe, summary.no)
                    summary.guests?.let { "$counts · ${pluralStringResource(Res.plurals.moment_guests, it, it)}" } ?: counts
                } else {
                    stringResource(Res.string.moment_replies_waiting)
                },
                style = Momentic.type.bodySmall,
                color = c.ink,
            )
        }
        Icon(MomenticIcons.ChevronRight, contentDescription = null, tint = c.ink, modifier = Modifier.size(MomenticSizes.iconSmall))
    }
}

@Composable
private fun DetailSkeleton() {
    Column(Modifier.padding(MomenticSpacing.gutter), verticalArrangement = Arrangement.spacedBy(MomenticSpacing.md)) {
        Row(horizontalArrangement = Arrangement.spacedBy(MomenticSpacing.md), verticalAlignment = Alignment.CenterVertically) {
            SkeletonBlock(Modifier.size(width = 96.dp, height = 120.dp), RoundedCornerShape(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(MomenticSpacing.xs)) {
                SkeletonBlock(Modifier.width(180.dp).height(26.dp), RoundedCornerShape(13.dp))
                SkeletonBlock(Modifier.width(110.dp).height(14.dp), RoundedCornerShape(7.dp))
                SkeletonBlock(Modifier.width(120.dp).height(28.dp), MomenticShapes.pill)
            }
        }
        SkeletonBlock(Modifier.fillMaxWidth().height(120.dp), MomenticShapes.card)
        SkeletonBlock(Modifier.fillMaxWidth().height(260.dp), MomenticShapes.card)
    }
}

private fun DayOfWeek.fullName(): String = name.lowercase().replaceFirstChar { it.uppercaseChar() }
