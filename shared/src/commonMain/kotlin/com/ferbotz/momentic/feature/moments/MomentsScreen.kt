package com.ferbotz.momentic.feature.moments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ferbotz.momentic.data.model.Creation
import com.ferbotz.momentic.data.model.TemplateCard
import com.ferbotz.momentic.designsystem.components.EmptyState
import com.ferbotz.momentic.designsystem.components.InlineBanner
import com.ferbotz.momentic.designsystem.components.MessageTone
import com.ferbotz.momentic.designsystem.components.MomentRow
import com.ferbotz.momentic.designsystem.components.PrimaryButton
import com.ferbotz.momentic.designsystem.components.SecondaryButton
import com.ferbotz.momentic.designsystem.components.SegmentedControl
import com.ferbotz.momentic.designsystem.components.SkeletonBlock
import com.ferbotz.momentic.designsystem.components.Spinner
import com.ferbotz.momentic.designsystem.components.TabHeader
import com.ferbotz.momentic.designsystem.icons.MomenticIcons
import com.ferbotz.momentic.designsystem.icons.MomenticIllustrations
import com.ferbotz.momentic.presentation.moments.DayAgo
import com.ferbotz.momentic.presentation.moments.MomentsUiState
import com.ferbotz.momentic.presentation.moments.dayAgo
import com.ferbotz.momentic.presentation.moments.interimTitle
import com.ferbotz.momentic.presentation.moments.momentStatus
import com.ferbotz.momentic.presentation.util.dayMonthLabel
import com.ferbotz.momentic.presentation.util.longLabel
import com.ferbotz.momentic.resources.Res
import com.ferbotz.momentic.resources.action_try_again
import com.ferbotz.momentic.resources.day_days_ago
import com.ferbotz.momentic.resources.day_on
import com.ferbotz.momentic.resources.day_today
import com.ferbotz.momentic.resources.day_yesterday
import com.ferbotz.momentic.resources.moments_drafts_note
import com.ferbotz.momentic.resources.moments_edited
import com.ferbotz.momentic.resources.moments_empty_drafts_body
import com.ferbotz.momentic.resources.moments_empty_drafts_title
import com.ferbotz.momentic.resources.moments_empty_ended_body
import com.ferbotz.momentic.resources.moments_empty_ended_title
import com.ferbotz.momentic.resources.moments_empty_live_body
import com.ferbotz.momentic.resources.moments_empty_live_title
import com.ferbotz.momentic.resources.moments_ended_on
import com.ferbotz.momentic.resources.moments_error_title
import com.ferbotz.momentic.resources.moments_line
import com.ferbotz.momentic.resources.moments_loading
import com.ferbotz.momentic.resources.moments_new
import com.ferbotz.momentic.resources.moments_pending
import com.ferbotz.momentic.resources.moments_pending_retry
import com.ferbotz.momentic.resources.moments_published
import com.ferbotz.momentic.resources.moments_tab_drafts
import com.ferbotz.momentic.resources.moments_tab_ended
import com.ferbotz.momentic.resources.moments_tab_live
import com.ferbotz.momentic.resources.moments_tabs
import com.ferbotz.momentic.resources.tab_moments
import com.ferbotz.momentic.theme.Momentic
import com.ferbotz.momentic.theme.MomenticShapes
import com.ferbotz.momentic.theme.MomenticSizes
import com.ferbotz.momentic.theme.MomenticSpacing
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.Instant

/** The three shelves of My Moments. */
enum class MomentsTab { Drafts, Live, Ended }

/**
 * My Moments: drafts, live and ended moments on three tabs, newest first, each a row with its cover
 * (the template's art for now), a name, when it last changed, its status and views. Pull down to look
 * again; the list asks for more at its end.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MomentsScreen(
    state: MomentsUiState,
    tab: MomentsTab,
    maxDrafts: Int,
    onTab: (MomentsTab) -> Unit,
    onOpenMoment: (creationId: String) -> Unit,
    onNewMoment: () -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onFinishPending: () -> Unit,
    modifier: Modifier = Modifier,
    now: Instant = Clock.System.now(),
) {
    val c = Momentic.colors
    val shown = when (tab) {
        MomentsTab.Drafts -> state.drafts
        MomentsTab.Live -> state.live
        MomentsTab.Ended -> state.ended
    }
    Column(modifier.fillMaxSize()) {
        TabHeader(
            title = stringResource(Res.string.tab_moments),
            action = { PrimaryButton(stringResource(Res.string.moments_new), onClick = onNewMoment, icon = MomenticIcons.Plus, compact = true) },
        )
        val tabsLabel = stringResource(Res.string.moments_tabs)
        SegmentedControl(
            options = listOf(
                stringResource(Res.string.moments_tab_drafts, state.drafts.size),
                stringResource(Res.string.moments_tab_live, state.live.size),
                stringResource(Res.string.moments_tab_ended, state.ended.size),
            ),
            selectedIndex = tab.ordinal,
            onSelect = { onTab(MomentsTab.entries[it]) },
            modifier = Modifier
                .padding(start = MomenticSpacing.gutter, end = MomenticSpacing.gutter, top = MomenticSpacing.xs)
                .semantics { contentDescription = tabsLabel },
        )
        if (state.pendingPurchases > 0) {
            InlineBanner(
                text = pluralStringResource(Res.plurals.moments_pending, state.pendingPurchases, state.pendingPurchases),
                tone = MessageTone.Info,
                actionLabel = stringResource(Res.string.moments_pending_retry),
                onAction = onFinishPending,
                modifier = Modifier.padding(start = MomenticSpacing.gutter, end = MomenticSpacing.gutter, top = MomenticSpacing.sm),
            )
        }
        val pull = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            state = pull,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pull,
                    isRefreshing = state.isRefreshing,
                    containerColor = c.surface,
                    color = c.rose,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            },
        ) {
            when {
                state.isLoading -> MomentsSkeleton()
                state.error != null && state.creations.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    EmptyState(
                        illustration = MomenticIllustrations.BrokenLink,
                        title = stringResource(Res.string.moments_error_title),
                        body = state.error,
                        action = { SecondaryButton(stringResource(Res.string.action_try_again), onClick = onRetry, icon = MomenticIcons.Retry) },
                        modifier = Modifier.padding(MomenticSpacing.gutter).padding(top = MomenticSpacing.xl),
                    )
                }
                shown.isEmpty() -> TabEmpty(tab, onNewMoment)
                else -> MomentList(shown, tab, state, maxDrafts, onOpenMoment, onLoadMore, now)
            }
        }
    }
}

@Composable
private fun MomentList(
    shown: List<Creation>,
    tab: MomentsTab,
    state: MomentsUiState,
    maxDrafts: Int,
    onOpenMoment: (String) -> Unit,
    onLoadMore: () -> Unit,
    now: Instant,
) {
    val list = rememberLazyListState()
    // Near the end of what's loaded: ask for the next page.
    val nearEnd by remember { derivedStateOf { (list.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= list.layoutInfo.totalItemsCount - 3 } }
    LaunchedEffect(nearEnd, state.canLoadMore) {
        if (nearEnd && state.canLoadMore) onLoadMore()
    }
    val zone = TimeZone.currentSystemDefault()
    LazyColumn(
        state = list,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = MomenticSpacing.gutter, end = MomenticSpacing.gutter, top = MomenticSpacing.md, bottom = MomenticSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(MomenticSpacing.sm),
    ) {
        items(shown, key = { it.id }) { creation ->
            val template = state.templates[creation.templateId]
            val status = creation.momentStatus(now, zone)
            MomentRow(
                title = creation.interimTitle() ?: template?.name ?: creation.templateName,
                subtitle = momentLine(creation, template, now, zone),
                coverUrl = template?.thumbnailUrl,
                categoryId = template?.categoryId,
                status = status,
                views = if (creation.isPublished) creation.viewCount else null,
                onClick = { onOpenMoment(creation.id) },
            )
        }
        if (state.isLoadingMore) {
            item(key = "more") {
                Box(Modifier.fillMaxWidth().padding(MomenticSpacing.md), contentAlignment = Alignment.Center) { Spinner(color = Momentic.colors.rose, size = 22.dp) }
            }
        }
        if (tab == MomentsTab.Drafts) {
            item(key = "drafts-note") {
                Text(
                    stringResource(Res.string.moments_drafts_note, maxDrafts),
                    style = Momentic.type.caption,
                    color = Momentic.colors.inkSoft,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(MomenticSpacing.xs),
                )
            }
        }
    }
}

/**
 * The row's second line: the template, and when the moment last changed — "The Question · edited
 * yesterday". Without a name of its own the row is already titled by the template, so it's left out.
 */
@Composable
internal fun momentLine(creation: Creation, template: TemplateCard?, now: Instant, zone: TimeZone): String {
    val templateName = template?.name ?: creation.templateName
    val ended = creation.isPublished && (creation.isExpired || creation.expiresAt?.let { it <= now } == true)
    val change = when {
        creation.isDraft -> (creation.updatedAt ?: creation.createdAt)?.let { stringResource(Res.string.moments_edited, dayText(dayAgo(it, now, zone))) }
        ended -> creation.expiresAt?.let { stringResource(Res.string.moments_ended_on, it.toLocalDateTime(zone).date.longLabel()) }
        else -> creation.publishedAt?.let { stringResource(Res.string.moments_published, dayText(dayAgo(it, now, zone))) }
    }
    return when {
        creation.interimTitle() == null -> change?.replaceFirstChar { it.uppercaseChar() } ?: templateName
        change == null -> templateName
        else -> stringResource(Res.string.moments_line, templateName, change)
    }
}

@Composable
internal fun dayText(day: DayAgo): String = when (day) {
    DayAgo.Today -> stringResource(Res.string.day_today)
    DayAgo.Yesterday -> stringResource(Res.string.day_yesterday)
    is DayAgo.DaysAgo -> stringResource(Res.string.day_days_ago, day.days)
    is DayAgo.On -> stringResource(Res.string.day_on, day.date.dayMonthLabel())
}

@Composable
private fun TabEmpty(tab: MomentsTab, onNewMoment: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        when (tab) {
            MomentsTab.Drafts -> EmptyState(
                illustration = MomenticIllustrations.Drafts,
                title = stringResource(Res.string.moments_empty_drafts_title),
                body = stringResource(Res.string.moments_empty_drafts_body),
                action = { PrimaryButton(stringResource(Res.string.moments_new), onClick = onNewMoment, icon = MomenticIcons.Plus) },
                modifier = Modifier.padding(top = 56.dp, start = MomenticSpacing.gutter, end = MomenticSpacing.gutter),
            )
            MomentsTab.Live -> EmptyState(
                illustration = MomenticIllustrations.Sent,
                title = stringResource(Res.string.moments_empty_live_title),
                body = stringResource(Res.string.moments_empty_live_body),
                modifier = Modifier.padding(top = 56.dp, start = MomenticSpacing.gutter, end = MomenticSpacing.gutter),
            )
            MomentsTab.Ended -> EmptyState(
                illustration = MomenticIllustrations.Sealed,
                title = stringResource(Res.string.moments_empty_ended_title),
                body = stringResource(Res.string.moments_empty_ended_body),
                modifier = Modifier.padding(top = 56.dp, start = MomenticSpacing.gutter, end = MomenticSpacing.gutter),
            )
        }
    }
}

@Composable
private fun MomentsSkeleton() {
    val label = stringResource(Res.string.moments_loading)
    Column(
        Modifier.fillMaxWidth().padding(MomenticSpacing.gutter).semantics(mergeDescendants = true) { contentDescription = label },
        verticalArrangement = Arrangement.spacedBy(MomenticSpacing.sm),
    ) {
        repeat(3) {
            Row(
                Modifier.fillMaxWidth().height(114.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SkeletonBlock(Modifier.size(width = MomenticSizes.momentCover, height = 90.dp), MomenticShapes.input)
                Column(verticalArrangement = Arrangement.spacedBy(MomenticSpacing.xs)) {
                    SkeletonBlock(Modifier.width(170.dp).height(16.dp), RoundedCornerShape(8.dp))
                    SkeletonBlock(Modifier.width(130.dp).height(12.dp), RoundedCornerShape(6.dp))
                    SkeletonBlock(Modifier.width(96.dp).height(24.dp), MomenticShapes.pill)
                }
            }
        }
    }
}
