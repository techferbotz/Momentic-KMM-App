package com.ferbotz.momentic.feature.template

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ferbotz.momentic.data.model.TemplateDetail
import com.ferbotz.momentic.designsystem.components.ArtImage
import com.ferbotz.momentic.designsystem.components.EmptyState
import com.ferbotz.momentic.designsystem.components.IconButtonStyle
import com.ferbotz.momentic.designsystem.components.InlineBanner
import com.ferbotz.momentic.designsystem.components.MessageTone
import com.ferbotz.momentic.designsystem.components.MomenticIconButton
import com.ferbotz.momentic.designsystem.components.MomenticTopBar
import com.ferbotz.momentic.designsystem.components.PrimaryButton
import com.ferbotz.momentic.designsystem.components.SecondaryButton
import com.ferbotz.momentic.designsystem.components.SkeletonBlock
import com.ferbotz.momentic.designsystem.components.TagLabel
import com.ferbotz.momentic.designsystem.components.Tier
import com.ferbotz.momentic.designsystem.components.TierBadge
import com.ferbotz.momentic.designsystem.icons.MomenticIcons
import com.ferbotz.momentic.designsystem.icons.MomenticIllustrations
import com.ferbotz.momentic.presentation.template.AddItem
import com.ferbotz.momentic.presentation.template.AddKind
import com.ferbotz.momentic.presentation.template.TemplateDetailUiState
import com.ferbotz.momentic.presentation.template.whatYoullAdd
import com.ferbotz.momentic.resources.Res
import com.ferbotz.momentic.resources.action_try_again
import com.ferbotz.momentic.resources.cd_back
import com.ferbotz.momentic.resources.template_add_at_least
import com.ferbotz.momentic.resources.template_add_exactly
import com.ferbotz.momentic.resources.template_add_range
import com.ferbotz.momentic.resources.template_add_up_to
import com.ferbotz.momentic.resources.template_adds_title
import com.ferbotz.momentic.resources.template_error_title
import com.ferbotz.momentic.resources.template_make_it_yours
import com.ferbotz.momentic.resources.template_needs_update
import com.ferbotz.momentic.resources.template_preview_one
import com.ferbotz.momentic.resources.template_previews
import com.ferbotz.momentic.resources.template_rsvp_body
import com.ferbotz.momentic.resources.template_rsvp_title
import com.ferbotz.momentic.resources.template_see_example
import com.ferbotz.momentic.resources.template_tags
import com.ferbotz.momentic.theme.Momentic
import com.ferbotz.momentic.theme.MomenticShapes
import com.ferbotz.momentic.theme.MomenticSizes
import com.ferbotz.momentic.theme.MomenticSpacing
import com.ferbotz.momentic.theme.occasionTint
import org.jetbrains.compose.resources.stringResource

/**
 * One experience: its preview art to swipe through, what it is, a live example, what the form will ask
 * ("What you'll add", from its fields) and "Make it yours". [onLiveExample] is null when it has no
 * example page.
 */
@Composable
fun TemplateScreen(
    state: TemplateDetailUiState,
    onBack: () -> Unit,
    onLiveExample: (() -> Unit)?,
    onMakeItYours: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Momentic.colors
    val template = state.template
    val scroll = rememberScrollState()
    BoxWithConstraints(modifier.fillMaxSize().background(c.background)) {
        // When the art has scrolled up behind the status bar.
        val artBottom = with(LocalDensity.current) { (maxWidth * 5f / 4f).toPx() - WindowInsets.statusBars.getTop(this) }
        val pastArt by remember(artBottom) { derivedStateOf { scroll.value >= artBottom } }
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.weight(1f).verticalScroll(scroll)) {
                when {
                    template != null -> {
                        PreviewPager(template)
                        TemplateBody(template, onLiveExample)
                    }
                    state.isLoading -> TemplateSkeleton()
                    else -> Column(
                        Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(top = 72.dp, start = MomenticSpacing.gutter, end = MomenticSpacing.gutter),
                    ) {
                        EmptyState(
                            illustration = MomenticIllustrations.BrokenLink,
                            title = stringResource(Res.string.template_error_title),
                            body = state.error.orEmpty(),
                            action = { SecondaryButton(stringResource(Res.string.action_try_again), onClick = onRetry, icon = MomenticIcons.Retry) },
                        )
                    }
                }
            }
            if (template != null) {
                HorizontalDivider(thickness = 1.dp, color = c.line)
                Column(
                Modifier
                    .fillMaxWidth()
                    .background(c.surface)
                    .padding(horizontal = MomenticSpacing.gutter, vertical = MomenticSpacing.sm)
                    .windowInsetsPadding(WindowInsets.navigationBars),
            ) {
                    if (template.canBeFilledIn) {
                        PrimaryButton(stringResource(Res.string.template_make_it_yours), onClick = onMakeItYours, modifier = Modifier.fillMaxWidth())
                    } else {
                        InlineBanner(stringResource(Res.string.template_needs_update), MessageTone.Info)
                    }
                }
            }
        }
        if (pastArt && template != null) {
            // Past the art, a plain bar with the name takes over from the floating back button.
            MomenticTopBar(title = template.name, onBack = onBack, background = c.background, showDivider = true)
        } else {
            MomenticIconButton(
                icon = MomenticIcons.Back,
                contentDescription = stringResource(Res.string.cd_back),
                onClick = onBack,
                style = IconButtonStyle.OnArt,
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(start = MomenticSpacing.sm, top = MomenticSpacing.xxs),
            )
        }
    }
}

/** The template's preview images, 4:5 and edge to edge under the status bar, with page dots. */
@Composable
private fun PreviewPager(template: TemplateDetail) {
    val images = template.previewImageUrls.ifEmpty { listOfNotNull(template.thumbnailUrl) }.ifEmpty { listOf(null) }
    val pager = rememberPagerState { images.size }
    val description = if (images.size > 1) {
        stringResource(Res.string.template_previews, pager.currentPage + 1, images.size)
    } else {
        stringResource(Res.string.template_preview_one, template.name)
    }
    Box(Modifier.fillMaxWidth().aspectRatio(4f / 5f).semantics(mergeDescendants = true) { contentDescription = description }) {
        HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
            ArtImage(images[page], template.categoryId, contentDescription = null, modifier = Modifier.fillMaxSize())
        }
        if (images.size > 1) {
            Row(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                repeat(images.size) { index ->
                    val current = index == pager.currentPage
                    Box(
                        Modifier
                            .size(width = if (current) 20.dp else 6.dp, height = 6.dp)
                            .alpha(if (current) 1f else 0.3f)
                            .clip(CircleShape)
                            .background(Momentic.colors.ink),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TemplateBody(template: TemplateDetail, onLiveExample: (() -> Unit)?) {
    val c = Momentic.colors
    Column(Modifier.fillMaxWidth().padding(start = MomenticSpacing.gutter, end = MomenticSpacing.gutter, top = MomenticSpacing.lg, bottom = MomenticSpacing.xl)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Tier.fromKey(template.tierKey)?.let { tier -> TierBadge(tier, label = template.pricing.label.ifBlank { null }) }
            Box(Modifier.weight(1f))
            Text(template.displayPerDay, style = Momentic.type.label.copy(fontWeight = FontWeight.Bold), color = c.ink)
        }
        Text(
            text = template.name,
            style = Momentic.type.display,
            color = c.ink,
            modifier = Modifier.padding(top = MomenticSpacing.sm).semantics { heading() },
        )
        if (template.tagline.isNotBlank()) {
            Text(template.tagline, style = Momentic.type.body, color = c.inkSoft, modifier = Modifier.padding(top = MomenticSpacing.xxs))
        }
        if (template.description.isNotBlank()) {
            Text(template.description, style = Momentic.type.body, color = c.ink, modifier = Modifier.padding(top = MomenticSpacing.md))
        }
        if (template.tags.isNotEmpty()) {
            val tagsLabel = stringResource(Res.string.template_tags)
            FlowRow(
                modifier = Modifier.padding(top = MomenticSpacing.md).semantics { contentDescription = tagsLabel },
                horizontalArrangement = Arrangement.spacedBy(MomenticSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(MomenticSpacing.xs),
            ) {
                template.tags.forEach { tag -> TagLabel(tag.replaceFirstChar { it.uppercaseChar() }) }
            }
        }
        if (onLiveExample != null) {
            SecondaryButton(
                text = stringResource(Res.string.template_see_example),
                onClick = onLiveExample,
                icon = MomenticIcons.Eye,
                modifier = Modifier.fillMaxWidth().padding(top = MomenticSpacing.lg),
            )
        }
        val adds = whatYoullAdd(template.fields)
        if (adds.isNotEmpty()) {
            Text(
                text = stringResource(Res.string.template_adds_title),
                style = Momentic.type.h2,
                color = c.ink,
                modifier = Modifier.padding(top = MomenticSpacing.xl).semantics { heading() },
            )
            Column(Modifier.padding(top = MomenticSpacing.md), verticalArrangement = Arrangement.spacedBy(MomenticSpacing.sm)) {
                adds.forEach { AddLine(it) }
            }
        }
        if (template.supportsRsvp) RsvpCard(Modifier.padding(top = MomenticSpacing.lg))
    }
}

@Composable
private fun AddLine(item: AddItem) {
    val c = Momentic.colors
    Row(Modifier.fillMaxWidth().heightIn(min = 44.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(c.blush100), contentAlignment = Alignment.Center) {
            Icon(item.kind.icon(), contentDescription = null, tint = c.roseDeep, modifier = Modifier.size(MomenticSizes.iconSmall))
        }
        Text(item.text(), style = Momentic.type.body, color = c.ink, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun AddItem.text(): String {
    val min = min
    val max = max
    return when {
        min != null && max != null && min == max -> stringResource(Res.string.template_add_exactly, label, max)
        min != null && max != null -> stringResource(Res.string.template_add_range, label, min, max)
        max != null -> stringResource(Res.string.template_add_up_to, label, max)
        min != null -> stringResource(Res.string.template_add_at_least, label, min)
        else -> label
    }
}

private fun AddKind.icon() = when (this) {
    AddKind.Person -> MomenticIcons.Person
    AddKind.Photos -> MomenticIcons.Photo
    AddKind.Words -> MomenticIcons.Bubble
    AddKind.Note -> MomenticIcons.Mail
    AddKind.When -> MomenticIcons.Calendar
    AddKind.Place -> MomenticIcons.MapPin
    AddKind.Choice -> MomenticIcons.Sparkle
}

/** "Guests can RSVP", on the wedding tint — for templates that collect answers. */
@Composable
private fun RsvpCard(modifier: Modifier = Modifier) {
    val c = Momentic.colors
    val tint = occasionTint("wedding")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MomenticShapes.card)
            .background(tint.brush)
            .padding(MomenticSpacing.md)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(c.surface), contentAlignment = Alignment.Center) {
            Icon(MomenticIcons.Users, contentDescription = null, tint = tint.accent, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(stringResource(Res.string.template_rsvp_title), style = Momentic.type.body.copy(fontWeight = FontWeight.Bold), color = c.ink)
            Text(stringResource(Res.string.template_rsvp_body), style = Momentic.type.bodySmall, color = c.ink)
        }
    }
}

@Composable
private fun TemplateSkeleton() {
    Column {
        SkeletonBlock(Modifier.fillMaxWidth().aspectRatio(4f / 5f), RoundedCornerShape(0.dp))
        Column(Modifier.padding(MomenticSpacing.gutter), verticalArrangement = Arrangement.spacedBy(MomenticSpacing.sm)) {
            SkeletonBlock(Modifier.width(96.dp).height(28.dp), MomenticShapes.pill)
            SkeletonBlock(Modifier.width(220.dp).height(32.dp), RoundedCornerShape(16.dp))
            SkeletonBlock(Modifier.fillMaxWidth().height(14.dp), RoundedCornerShape(7.dp))
            SkeletonBlock(Modifier.width(240.dp).height(14.dp), RoundedCornerShape(7.dp))
        }
    }
}
