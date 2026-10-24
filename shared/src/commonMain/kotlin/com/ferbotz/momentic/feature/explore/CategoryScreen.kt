package com.ferbotz.momentic.feature.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ferbotz.momentic.data.model.Category
import com.ferbotz.momentic.data.model.FeedSection
import com.ferbotz.momentic.designsystem.components.EmptyState
import com.ferbotz.momentic.designsystem.components.MomenticIconButton
import com.ferbotz.momentic.designsystem.components.SecondaryButton
import com.ferbotz.momentic.designsystem.components.SkeletonBlock
import com.ferbotz.momentic.designsystem.components.TemplateTile
import com.ferbotz.momentic.designsystem.icons.MomenticIcons
import com.ferbotz.momentic.designsystem.icons.MomenticIllustrations
import com.ferbotz.momentic.resources.Res
import com.ferbotz.momentic.resources.action_try_again
import com.ferbotz.momentic.resources.category_about_anniversary
import com.ferbotz.momentic.resources.category_about_birthday
import com.ferbotz.momentic.resources.category_about_friendship
import com.ferbotz.momentic.resources.category_about_greeting
import com.ferbotz.momentic.resources.category_about_love
import com.ferbotz.momentic.resources.category_about_wedding
import com.ferbotz.momentic.resources.category_back
import com.ferbotz.momentic.resources.category_count
import com.ferbotz.momentic.resources.category_empty_body
import com.ferbotz.momentic.resources.category_empty_title
import com.ferbotz.momentic.resources.explore_error_title
import com.ferbotz.momentic.theme.Momentic
import com.ferbotz.momentic.theme.MomenticShapes
import com.ferbotz.momentic.theme.MomenticSpacing
import com.ferbotz.momentic.theme.occasionTint
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * One occasion's experiences in a two-column grid, under a header on the occasion's tint. [section] is
 * null while the feed loads (or when the occasion has gone); [error] is the feed's.
 */
@Composable
fun CategoryScreen(
    section: FeedSection?,
    isLoading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onOpenTemplate: (templateId: String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val full: LazyGridItemSpanScope.() -> GridItemSpan = { GridItemSpan(maxLineSpan) }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize().background(Momentic.colors.background),
        contentPadding = PaddingValues(bottom = MomenticSpacing.xl),
        horizontalArrangement = Arrangement.spacedBy(MomenticSpacing.md),
        verticalArrangement = Arrangement.spacedBy(MomenticSpacing.gutter),
    ) {
        item(key = "header", span = full) { CategoryHeader(section?.category, section?.templates?.size, onBack) }
        when {
            section != null && section.templates.isNotEmpty() -> itemsIndexed(section.templates, key = { _, card -> card.id }) { index, card ->
                TemplateTile(
                    card = card,
                    onClick = { onOpenTemplate(card.id) },
                    emoji = section.category.emoji,
                    modifier = Modifier.gridGutter(index),
                )
            }
            section == null && isLoading -> items(4, key = { "skeleton-$it" }) { index ->
                Column(Modifier.gridGutter(index), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SkeletonBlock(Modifier.fillMaxWidth().aspectRatio(4f / 5f), MomenticShapes.card)
                    SkeletonBlock(Modifier.width(110.dp).height(14.dp), RoundedCornerShape(7.dp))
                }
            }
            section == null && error != null -> item(key = "error", span = full) {
                EmptyState(
                    illustration = MomenticIllustrations.BrokenLink,
                    title = stringResource(Res.string.explore_error_title),
                    body = error,
                    action = { SecondaryButton(stringResource(Res.string.action_try_again), onClick = onRetry, icon = MomenticIcons.Retry) },
                    modifier = Modifier.padding(horizontal = MomenticSpacing.gutter),
                )
            }
            else -> item(key = "empty", span = full) {
                EmptyState(
                    illustration = MomenticIllustrations.Tending,
                    title = stringResource(Res.string.category_empty_title),
                    body = stringResource(Res.string.category_empty_body),
                    modifier = Modifier.padding(horizontal = MomenticSpacing.gutter),
                )
            }
        }
    }
}

/**
 * The screen's 20 dp gutters, inside the cells rather than round the grid, so the header above can run
 * edge to edge. Both columns lose the same width, so the tiles stay equal.
 */
private fun Modifier.gridGutter(index: Int): Modifier =
    padding(start = if (index % 2 == 0) MomenticSpacing.gutter else 0.dp, end = if (index % 2 == 1) MomenticSpacing.gutter else 0.dp)

/** The occasion's name in display serif on its tint, what it's for, and how many experiences it has. */
@Composable
private fun CategoryHeader(category: Category?, count: Int?, onBack: () -> Unit) {
    val c = Momentic.colors
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = MomenticSpacing.xxs)
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(occasionTint(category?.id).brush)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = MomenticSpacing.xs, end = MomenticSpacing.gutter, top = MomenticSpacing.xxs, bottom = MomenticSpacing.lg),
    ) {
        MomenticIconButton(MomenticIcons.Back, stringResource(Res.string.category_back), onClick = onBack)
        Column(Modifier.padding(start = MomenticSpacing.sm, top = MomenticSpacing.xs), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (category != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(MomenticSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
                    Text(category.emoji, fontSize = 30.sp)
                    Text(category.name, style = Momentic.type.display, color = c.roseDeep, modifier = Modifier.semantics { heading() })
                }
                categoryAbout(category.id)?.let { about ->
                    Text(stringResource(about), style = Momentic.type.body, color = c.ink)
                }
                if (count != null && count > 0) {
                    Text(pluralStringResource(Res.plurals.category_count, count, count), style = Momentic.type.captionStrong, color = c.ink)
                }
            } else {
                Box(Modifier.height(40.dp)) { SkeletonBlock(Modifier.width(160.dp).height(32.dp), RoundedCornerShape(16.dp)) }
            }
        }
    }
}

/** A line about an occasion the app knows. The feed carries no description, so an occasion it doesn't know shows none. */
private fun categoryAbout(categoryId: String): StringResource? = when (categoryId) {
    "birthday" -> Res.string.category_about_birthday
    "wedding" -> Res.string.category_about_wedding
    "love" -> Res.string.category_about_love
    "anniversary" -> Res.string.category_about_anniversary
    "friendship" -> Res.string.category_about_friendship
    "greeting" -> Res.string.category_about_greeting
    else -> null
}
