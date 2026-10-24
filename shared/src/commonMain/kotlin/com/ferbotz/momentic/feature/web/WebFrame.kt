package com.ferbotz.momentic.feature.web

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ferbotz.momentic.designsystem.components.EmptyState
import com.ferbotz.momentic.designsystem.components.SecondaryButton
import com.ferbotz.momentic.designsystem.icons.MomenticIcons
import com.ferbotz.momentic.designsystem.icons.MomenticIllustrations
import com.ferbotz.momentic.platform.WebPage
import com.ferbotz.momentic.platform.WebPageState
import com.ferbotz.momentic.resources.Res
import com.ferbotz.momentic.resources.action_try_again
import com.ferbotz.momentic.resources.web_failed_body
import com.ferbotz.momentic.resources.web_failed_title
import com.ferbotz.momentic.theme.Momentic
import com.ferbotz.momentic.theme.MomenticSpacing
import org.jetbrains.compose.resources.stringResource

/**
 * The app's frame around a web page: a cream bar on top ([topBar]), the page — with a thin rose progress
 * line while it loads, and a way back in when it fails — and the actions under it ([bottomBar]), which
 * keep clear of the gesture area.
 */
@Composable
fun WebFrame(
    state: WebPageState,
    regionDescription: String,
    topBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    bottomBar: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val c = Momentic.colors
    Column(modifier.fillMaxSize().background(c.surface)) {
        topBar()
        HorizontalDivider(thickness = 1.dp, color = c.line)
        Box(Modifier.weight(1f).fillMaxWidth().semantics { contentDescription = regionDescription }) {
            WebPage(state, Modifier.fillMaxSize())
            ProgressLine(state)
            if (state.failed) Failed(state)
        }
        if (bottomBar != null) {
            HorizontalDivider(thickness = 1.dp, color = c.line)
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(c.surface)
                    .padding(horizontal = MomenticSpacing.gutter, vertical = MomenticSpacing.sm)
                    .windowInsetsPadding(WindowInsets.navigationBars),
                content = bottomBar,
            )
        }
    }
}

@Composable
private fun BoxScope.ProgressLine(state: WebPageState) {
    val c = Momentic.colors
    AnimatedVisibility(state.isLoading && !state.failed, Modifier.align(Alignment.TopCenter), enter = fadeIn(), exit = fadeOut()) {
        LinearProgressIndicator(
            progress = { state.progress.coerceAtLeast(0.08f) },
            modifier = Modifier.fillMaxWidth().height(2.dp),
            color = c.rose,
            trackColor = c.blush100,
            drawStopIndicator = {},
        )
    }
}

@Composable
private fun Failed(state: WebPageState) {
    Box(Modifier.fillMaxSize().background(Momentic.colors.surface).padding(MomenticSpacing.gutter), contentAlignment = Alignment.Center) {
        EmptyState(
            illustration = MomenticIllustrations.Offline,
            title = stringResource(Res.string.web_failed_title),
            body = stringResource(Res.string.web_failed_body),
            action = { SecondaryButton(stringResource(Res.string.action_try_again), onClick = state::reload, icon = MomenticIcons.Retry) },
        )
    }
}
