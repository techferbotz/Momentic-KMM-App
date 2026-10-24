package com.ferbotz.momentic.feature.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ferbotz.momentic.designsystem.components.QuietButton
import com.ferbotz.momentic.designsystem.components.SecondaryButton
import com.ferbotz.momentic.feature.gate.UpdateBanner
import com.ferbotz.momentic.feature.shell.LocalUpdateNudge
import com.ferbotz.momentic.feature.shell.MessageEffect
import com.ferbotz.momentic.feature.shell.StandInNote
import com.ferbotz.momentic.feature.shell.StandInScreen
import com.ferbotz.momentic.feature.shell.StandInStatus
import com.ferbotz.momentic.presentation.feed.FeedViewModel
import com.ferbotz.momentic.resources.Res
import com.ferbotz.momentic.resources.tab_explore
import com.ferbotz.momentic.theme.Momentic
import com.ferbotz.momentic.theme.MomenticSpacing
import org.jetbrains.compose.resources.stringResource

/** The Explore tab: the feed's categories and templates. [feed] is shared with [CategoryRoute]. */
@Composable
fun ExploreRoute(
    feed: FeedViewModel,
    onOpenCategory: (categoryId: String) -> Unit,
    onOpenTemplate: (templateId: String) -> Unit,
) {
    val state by feed.uiState.collectAsStateWithLifecycle()
    MessageEffect(state.message, feed::messageShown)
    val nudge = LocalUpdateNudge.current
    StandInScreen(title = stringResource(Res.string.tab_explore), onBack = null, tabRoot = true) {
        if (nudge != null) UpdateBanner(nudge)
        StandInStatus(state.isLoading, state.error, feed::retry)
        state.sections.forEach { section ->
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(MomenticSpacing.xxs)) {
                Row {
                    Text("${section.category.emoji} ${section.category.name}", style = Momentic.type.h2, modifier = Modifier.weight(1f))
                    QuietButton("See all", onClick = { onOpenCategory(section.category.id) }, compact = true)
                }
                section.templates.forEach { card ->
                    SecondaryButton(card.name, onClick = { onOpenTemplate(card.id) }, modifier = Modifier.fillMaxWidth(), compact = true)
                }
            }
        }
    }
}

/** One category's templates, from the feed Explore already loaded. */
@Composable
fun CategoryRoute(
    feed: FeedViewModel,
    categoryId: String,
    onBack: () -> Unit,
    onOpenTemplate: (templateId: String) -> Unit,
) {
    val state by feed.uiState.collectAsStateWithLifecycle()
    val section = state.sections.firstOrNull { it.category.id == categoryId }
    StandInScreen(title = section?.let { "${it.category.emoji} ${it.category.name}" } ?: "", onBack = onBack) {
        StandInStatus(state.isLoading && section == null, state.error, feed::retry)
        if (section != null && section.templates.isEmpty()) StandInNote("No experiences here yet.")
        section?.templates?.forEach { card ->
            SecondaryButton(card.name, onClick = { onOpenTemplate(card.id) }, modifier = Modifier.fillMaxWidth(), compact = true)
        }
    }
}
