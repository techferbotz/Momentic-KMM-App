package com.ferbotz.momentic.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ferbotz.momentic.data.model.FeedSection
import com.ferbotz.momentic.data.model.TemplateCard
import com.ferbotz.momentic.presentation.feed.FeedUiState
import com.ferbotz.momentic.ui.components.ErrorState
import com.ferbotz.momentic.ui.components.PricePill
import com.ferbotz.momentic.ui.components.SkeletonBlock
import com.ferbotz.momentic.ui.components.TemplateArt

private val CardWidth = 212.dp

/** The feed: one row of template cards per category, in the backend's order. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    state: FeedUiState,
    onTemplateClick: (templateId: String) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onMessageShown: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            onMessageShown()
        }
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            state.sections.isEmpty() && state.isLoading -> FeedSkeleton(Modifier.padding(padding))
            state.sections.isEmpty() && state.error != null -> ErrorState(state.error, onRetry, Modifier.padding(padding))
            else -> PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp),
                ) {
                    item(key = "header") { FeedHeader() }
                    items(state.sections, key = { it.category.id }) { section ->
                        FeedSectionRow(section, onTemplateClick)
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedHeader() {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text(
            text = "Momentic",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Make a moment they'll keep.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FeedSectionRow(section: FeedSection, onTemplateClick: (String) -> Unit) {
    Column(Modifier.padding(top = 16.dp, bottom = 8.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (section.category.emoji.isNotBlank()) {
                Text(section.category.emoji, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.width(8.dp))
            }
            Text(section.category.name, style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(section.templates, key = { it.id }) { template ->
                TemplateCardView(template, section.category.emoji, onClick = { onTemplateClick(template.id) })
            }
        }
    }
}

@Composable
private fun TemplateCardView(template: TemplateCard, emoji: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(CardWidth)
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 5f)
                .clip(MaterialTheme.shapes.large),
        ) {
            TemplateArt(
                url = template.thumbnailUrl,
                categoryId = template.categoryId,
                emoji = emoji,
                contentDescription = template.name,
                modifier = Modifier.matchParentSize(),
            )
            if (template.displayPerDay.isNotBlank()) {
                PricePill(
                    text = template.displayPerDay,
                    modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = template.name,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        if (template.tagline.isNotBlank()) {
            Text(
                text = template.tagline,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun FeedSkeleton(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize()) {
        FeedHeader()
        repeat(2) {
            SkeletonBlock(
                Modifier.padding(start = 20.dp, top = 20.dp).width(140.dp).height(24.dp).clip(MaterialTheme.shapes.small),
            )
            Row(
                modifier = Modifier.padding(start = 20.dp, top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                repeat(2) {
                    SkeletonBlock(Modifier.width(CardWidth).aspectRatio(4f / 5f).clip(MaterialTheme.shapes.large))
                }
            }
        }
    }
}
