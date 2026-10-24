package com.ferbotz.momentic.feature.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ferbotz.momentic.designsystem.components.EmptyState
import com.ferbotz.momentic.designsystem.components.MomenticTopBar
import com.ferbotz.momentic.designsystem.components.SecondaryButton
import com.ferbotz.momentic.designsystem.components.Spinner
import com.ferbotz.momentic.designsystem.components.TabHeader
import com.ferbotz.momentic.designsystem.icons.MomenticIllustrations
import com.ferbotz.momentic.resources.Res
import com.ferbotz.momentic.resources.action_try_again
import com.ferbotz.momentic.theme.Momentic
import com.ferbotz.momentic.theme.MomenticSpacing
import org.jetbrains.compose.resources.stringResource

/**
 * The frame of a screen whose design isn't built yet (navigation shell, phase 3): its title, what it
 * shows, and its ways on — real buttons wired to the real view model and routes. Each is replaced by
 * the designed screen in phase 5.
 */
@Composable
fun StandInScreen(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    tabRoot: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxSize().background(Momentic.colors.background)) {
        if (tabRoot) TabHeader(title) else MomenticTopBar(title = title, onBack = onBack)
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(MomenticSpacing.gutter),
            verticalArrangement = Arrangement.spacedBy(MomenticSpacing.sm),
            content = content,
        )
    }
}

/** A line of what a stand-in screen holds. */
@Composable
fun StandInNote(text: String) {
    Text(text, style = Momentic.type.bodySmall, color = Momentic.colors.inkSoft)
}

/** The loading and failed states every screen has, until its own are built. */
@Composable
fun StandInStatus(isLoading: Boolean, error: String?, onRetry: (() -> Unit)?) {
    when {
        isLoading -> Box(Modifier.fillMaxWidth().padding(MomenticSpacing.xxl), contentAlignment = Alignment.Center) {
            Spinner(color = Momentic.colors.rose, size = 28.dp)
        }
        error != null -> EmptyState(
            illustration = MomenticIllustrations.Offline,
            title = error,
            body = "",
            action = onRetry?.let { retry -> { SecondaryButton(stringResource(Res.string.action_try_again), onClick = retry) } },
        )
    }
}
