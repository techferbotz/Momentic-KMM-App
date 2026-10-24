package com.ferbotz.momentic.feature.explore

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ferbotz.momentic.feature.shell.LocalUpdateNudge
import com.ferbotz.momentic.feature.shell.MessageEffect
import com.ferbotz.momentic.presentation.feed.FeedViewModel
import kotlinx.coroutines.delay

/** How often Explore looks again by itself while it's offline ("We'll keep trying"). */
private const val OFFLINE_RETRY_MS = 15_000L

/** The Explore tab: the feed's categories and templates. [feed] is shared with [CategoryRoute]. */
@Composable
fun ExploreRoute(
    feed: FeedViewModel,
    onOpenCategory: (categoryId: String) -> Unit,
    onOpenTemplate: (templateId: String) -> Unit,
) {
    val state by feed.uiState.collectAsStateWithLifecycle()
    MessageEffect(state.message, feed::messageShown)
    if (state.isOffline) {
        // A quiet look again: refresh keeps the offline state on screen until something arrives.
        LaunchedEffect(feed) {
            while (true) {
                delay(OFFLINE_RETRY_MS)
                feed.refresh()
            }
        }
    }
    ExploreScreen(
        state = state,
        nudge = LocalUpdateNudge.current,
        onOpenCategory = onOpenCategory,
        onOpenTemplate = onOpenTemplate,
        onRefresh = feed::refresh,
        onRetry = feed::retry,
    )
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
    CategoryScreen(
        section = state.sections.firstOrNull { it.category.id == categoryId },
        isLoading = state.isLoading,
        error = state.error,
        onBack = onBack,
        onOpenTemplate = onOpenTemplate,
        onRetry = feed::retry,
    )
}
