package com.ferbotz.momentic.feature.web

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ferbotz.momentic.designsystem.components.MomenticIconButton
import com.ferbotz.momentic.designsystem.components.PrimaryButton
import com.ferbotz.momentic.designsystem.components.TopBarBadge
import com.ferbotz.momentic.designsystem.icons.MomenticIcons
import com.ferbotz.momentic.platform.rememberWebPageState
import com.ferbotz.momentic.resources.Res
import com.ferbotz.momentic.resources.web_close_example
import com.ferbotz.momentic.resources.web_example
import com.ferbotz.momentic.resources.web_example_region
import com.ferbotz.momentic.resources.web_make_it_yours
import com.ferbotz.momentic.theme.Momentic
import com.ferbotz.momentic.theme.MomenticSpacing
import org.jetbrains.compose.resources.stringResource

/**
 * A template's live example, with sample content, in the app's frame: close, the template's name and an
 * "Example" badge on top; "Make it yours" under the page when [onMakeItYours] is given.
 */
@Composable
fun WebPageRoute(url: String, title: String?, onClose: () -> Unit, onMakeItYours: (() -> Unit)?) {
    val c = Momentic.colors
    val state = rememberWebPageState(url)
    WebFrame(
        state = state,
        regionDescription = stringResource(Res.string.web_example_region, title.orEmpty()),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(c.surface)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .height(56.dp)
                    .padding(horizontal = MomenticSpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(MomenticSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MomenticIconButton(MomenticIcons.Close, stringResource(Res.string.web_close_example), onClick = onClose)
                if (title != null) {
                    Text(title, style = Momentic.type.label, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                }
                TopBarBadge(stringResource(Res.string.web_example))
            }
        },
        bottomBar = onMakeItYours?.let { make ->
            { PrimaryButton(stringResource(Res.string.web_make_it_yours), onClick = make, modifier = Modifier.fillMaxWidth()) }
        },
    )
}
