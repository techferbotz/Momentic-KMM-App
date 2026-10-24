package com.ferbotz.momentic.feature.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ferbotz.momentic.data.model.FeedSection
import com.ferbotz.momentic.designsystem.components.EmptyState
import com.ferbotz.momentic.designsystem.components.FeaturedTemplateCard
import com.ferbotz.momentic.designsystem.components.InlineBanner
import com.ferbotz.momentic.designsystem.components.MessageTone
import com.ferbotz.momentic.designsystem.components.OccasionChip
import com.ferbotz.momentic.designsystem.components.QuietButton
import com.ferbotz.momentic.designsystem.components.SecondaryButton
import com.ferbotz.momentic.designsystem.components.SkeletonBlock
import com.ferbotz.momentic.designsystem.components.StatusBarScrim
import com.ferbotz.momentic.designsystem.components.TemplateTile
import com.ferbotz.momentic.designsystem.components.Tier
import com.ferbotz.momentic.designsystem.icons.MomenticIcons
import com.ferbotz.momentic.designsystem.icons.MomenticIllustrations
import com.ferbotz.momentic.feature.gate.UpdateBanner
import com.ferbotz.momentic.feature.shell.UpdateNudge
import com.ferbotz.momentic.presentation.feed.FeedUiState
import com.ferbotz.momentic.resources.Res
import com.ferbotz.momentic.resources.action_try_again
import com.ferbotz.momentic.resources.explore_empty_body
import com.ferbotz.momentic.resources.explore_empty_title
import com.ferbotz.momentic.resources.explore_error_title
import com.ferbotz.momentic.resources.explore_loading
import com.ferbotz.momentic.resources.explore_offline_banner
import com.ferbotz.momentic.resources.explore_offline_body
import com.ferbotz.momentic.resources.explore_offline_lead
import com.ferbotz.momentic.resources.explore_offline_title
import com.ferbotz.momentic.resources.explore_see_all
import com.ferbotz.momentic.resources.explore_see_all_named
import com.ferbotz.momentic.resources.explore_title
import com.ferbotz.momentic.theme.Momentic
import com.ferbotz.momentic.theme.MomenticShapes
import com.ferbotz.momentic.theme.MomenticSizes
import com.ferbotz.momentic.theme.MomenticSpacing
import org.jetbrains.compose.resources.stringResource

/**
 * Explore: "What are we celebrating?", the occasions, the featured experience and one carousel per
 * occasion. Pull down to look again. Offline, it says so and [onRetry]s by itself (the route's job).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    state: FeedUiState,
    nudge: UpdateNudge?,
    onOpenCategory: (categoryId: String) -> Unit,
    onOpenTemplate: (templateId: String) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Momentic.colors
    val pull = rememberPullToRefreshState()
    Box(modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            state = pull,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pull,
                    isRefreshing = state.isRefreshing,
                    containerColor = c.surface,
                    color = c.rose,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = MomenticSpacing.huge),
                )
            },
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = MomenticSpacing.xl),
            ) {
                item(key = "top") { Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars)) }
                if (nudge != null) {
                    item(key = "nudge") {
                        UpdateBanner(nudge, Modifier.padding(start = MomenticSpacing.gutter, end = MomenticSpacing.gutter, top = MomenticSpacing.xs))
                    }
                }
                item(key = "title") {
                    Text(
                        text = stringResource(Res.string.explore_title),
                        style = Momentic.type.display,
                        color = c.ink,
                        modifier = Modifier
                            .padding(start = MomenticSpacing.gutter, end = MomenticSpacing.gutter, top = MomenticSpacing.md, bottom = MomenticSpacing.gutter)
                            .semantics { heading() },
                    )
                }
                when {
                    state.sections.isNotEmpty() -> feed(state.sections, onOpenCategory, onOpenTemplate)
                    state.isLoading -> item(key = "loading") { ExploreSkeleton() }
                    state.error != null -> item(key = "error") {
                        if (state.isOffline) OfflineState(onRetry) else FailedState(state.error, onRetry)
                    }
                    else -> item(key = "empty") {
                        EmptyState(
                            illustration = MomenticIllustrations.Tending,
                            title = stringResource(Res.string.explore_empty_title),
                            body = stringResource(Res.string.explore_empty_body),
                            modifier = Modifier.padding(top = MomenticSpacing.lg),
                        )
                    }
                }
            }
        }
        StatusBarScrim()
    }
}

private fun LazyListScope.feed(
    sections: List<FeedSection>,
    onOpenCategory: (String) -> Unit,
    onOpenTemplate: (String) -> Unit,
) {
    item(key = "occasions") {
        LazyRow(
            contentPadding = PaddingValues(horizontal = MomenticSpacing.gutter),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(sections, key = { it.category.id }) { section ->
                OccasionChip(
                    emoji = section.category.emoji,
                    label = section.category.name,
                    categoryId = section.category.id,
                    onClick = { onOpenCategory(section.category.id) },
                )
            }
        }
    }
    // Featured: the first experience of the first occasion, until the backend names one.
    val featuredSection = sections.firstOrNull { it.templates.isNotEmpty() }
    val featured = featuredSection?.templates?.first()
    if (featured != null) {
        item(key = "featured") {
            FeaturedTemplateCard(
                name = featured.name,
                tagline = featured.tagline,
                priceLine = featured.displayPerDay,
                artUrl = featured.thumbnailUrl,
                categoryId = featured.categoryId,
                tier = Tier.fromKey(featured.tierKey),
                onClick = { onOpenTemplate(featured.id) },
                emoji = featuredSection.category.emoji,
                modifier = Modifier.padding(start = MomenticSpacing.gutter, end = MomenticSpacing.gutter, top = MomenticSpacing.lg),
            )
        }
    }
    sections.filter { it.templates.isNotEmpty() }.forEach { section ->
        item(key = "section-${section.category.id}") {
            Column(Modifier.padding(top = MomenticSpacing.xl), verticalArrangement = Arrangement.spacedBy(MomenticSpacing.xs)) {
                SectionHeader(section, onSeeAll = { onOpenCategory(section.category.id) })
                LazyRow(
                    contentPadding = PaddingValues(horizontal = MomenticSpacing.gutter),
                    horizontalArrangement = Arrangement.spacedBy(MomenticSpacing.md),
                ) {
                    items(section.templates, key = { it.id }) { card ->
                        TemplateTile(
                            card = card,
                            onClick = { onOpenTemplate(card.id) },
                            emoji = section.category.emoji,
                            modifier = Modifier.width(MomenticSizes.carouselCardWidth),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(section: FeedSection, onSeeAll: () -> Unit) {
    val c = Momentic.colors
    val seeAll = stringResource(Res.string.explore_see_all_named, section.category.name)
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = MomenticSpacing.gutter, end = MomenticSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${section.category.emoji} ${section.category.name}",
            style = Momentic.type.h2,
            color = c.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        QuietButton(
            text = stringResource(Res.string.explore_see_all),
            onClick = onSeeAll,
            compact = true,
            modifier = Modifier.semantics { contentDescription = seeAll },
        )
    }
}

/** What loads: the occasions, the featured card and a carousel, as blush shapes. */
@Composable
private fun ExploreSkeleton() {
    val c = Momentic.colors
    val label = stringResource(Res.string.explore_loading)
    Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = label }) {
        Row(Modifier.padding(horizontal = MomenticSpacing.gutter), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(118.dp, 120.dp, 96.dp).forEach { width ->
                SkeletonBlock(Modifier.width(width).height(MomenticSizes.chipHeight), MomenticShapes.pill)
            }
        }
        Column(
            Modifier
                .padding(start = MomenticSpacing.gutter, end = MomenticSpacing.gutter, top = MomenticSpacing.lg)
                .fillMaxWidth()
                .clip(MomenticShapes.card)
                .background(c.surface),
        ) {
            SkeletonBlock(Modifier.fillMaxWidth().height(180.dp), RectangleShape)
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(MomenticSpacing.gutter),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SkeletonBlock(Modifier.width(64.dp).height(10.dp), RoundedCornerShape(5.dp))
                SkeletonBlock(Modifier.width(190.dp).height(22.dp), RoundedCornerShape(11.dp))
                SkeletonBlock(Modifier.width(236.dp).height(12.dp), RoundedCornerShape(6.dp))
                SkeletonBlock(Modifier.width(120.dp).height(12.dp), RoundedCornerShape(6.dp))
            }
        }
        SkeletonBlock(
            Modifier
                .padding(start = MomenticSpacing.gutter, top = 36.dp, bottom = MomenticSpacing.md)
                .width(110.dp)
                .height(20.dp),
            RoundedCornerShape(10.dp),
        )
        Row(Modifier.padding(horizontal = MomenticSpacing.gutter), horizontalArrangement = Arrangement.spacedBy(MomenticSpacing.md)) {
            repeat(3) {
                SkeletonBlock(Modifier.size(width = MomenticSizes.carouselCardWidth, height = 200.dp), MomenticShapes.card)
            }
        }
    }
}

@Composable
private fun OfflineState(onRetry: () -> Unit) {
    Column(Modifier.padding(horizontal = MomenticSpacing.gutter), verticalArrangement = Arrangement.spacedBy(MomenticSpacing.sm)) {
        InlineBanner(
            text = stringResource(Res.string.explore_offline_banner),
            lead = stringResource(Res.string.explore_offline_lead),
            tone = MessageTone.Offline,
        )
        EmptyState(
            illustration = MomenticIllustrations.Offline,
            title = stringResource(Res.string.explore_offline_title),
            body = stringResource(Res.string.explore_offline_body),
            action = { SecondaryButton(stringResource(Res.string.action_try_again), onClick = onRetry, icon = MomenticIcons.Retry) },
            modifier = Modifier.padding(top = MomenticSpacing.xl),
        )
    }
}

@Composable
private fun FailedState(message: String, onRetry: () -> Unit) {
    EmptyState(
        illustration = MomenticIllustrations.BrokenLink,
        title = stringResource(Res.string.explore_error_title),
        body = message,
        action = { SecondaryButton(stringResource(Res.string.action_try_again), onClick = onRetry, icon = MomenticIcons.Retry) },
        modifier = Modifier.padding(horizontal = MomenticSpacing.gutter).padding(top = MomenticSpacing.lg),
    )
}
