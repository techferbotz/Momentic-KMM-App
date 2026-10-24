package com.ferbotz.momentic.feature.editor

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ferbotz.momentic.designsystem.components.MomenticIconButton
import com.ferbotz.momentic.designsystem.components.PrimaryButton
import com.ferbotz.momentic.designsystem.components.QuietButton
import com.ferbotz.momentic.designsystem.components.SecondaryButton
import com.ferbotz.momentic.designsystem.icons.MomenticIcons
import com.ferbotz.momentic.feature.web.WebFrame
import com.ferbotz.momentic.platform.rememberShareSheet
import com.ferbotz.momentic.platform.rememberWebPageState
import com.ferbotz.momentic.platform.shareText
import com.ferbotz.momentic.resources.Res
import com.ferbotz.momentic.resources.web_back_to_editing
import com.ferbotz.momentic.resources.web_edit
import com.ferbotz.momentic.resources.web_preview_note
import com.ferbotz.momentic.resources.web_preview_region
import com.ferbotz.momentic.resources.web_preview_share_text
import com.ferbotz.momentic.resources.web_publish
import com.ferbotz.momentic.resources.web_send_preview
import com.ferbotz.momentic.theme.Momentic
import com.ferbotz.momentic.theme.MomenticShapes
import com.ferbotz.momentic.theme.MomenticSpacing
import org.jetbrains.compose.resources.stringResource

/**
 * The creator's draft as guests will see it — only the creator can open this link, and it lasts about
 * 30 minutes. "Send a preview link" shares it (to ask someone's opinion); Edit goes back to the form,
 * Publish on to choosing how long.
 */
@Composable
fun PreviewRoute(
    url: String,
    creationId: String,
    onEdit: () -> Unit,
    onPublish: (creationId: String) -> Unit,
) {
    val c = Momentic.colors
    val state = rememberWebPageState(url)
    val share = rememberShareSheet()
    val shareLine = stringResource(Res.string.web_preview_share_text)
    WebFrame(
        state = state,
        regionDescription = stringResource(Res.string.web_preview_region),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(c.surface)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .height(56.dp)
                    .padding(horizontal = MomenticSpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(MomenticSpacing.xxs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MomenticIconButton(MomenticIcons.Back, stringResource(Res.string.web_back_to_editing), onClick = onEdit)
                Text(
                    text = stringResource(Res.string.web_preview_note),
                    style = Momentic.type.label,
                    color = c.ink,
                    modifier = Modifier
                        .clip(MomenticShapes.pill)
                        .background(c.blush100)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        },
        bottomBar = {
            QuietButton(
                text = stringResource(Res.string.web_send_preview),
                onClick = { share.share(shareText(shareLine, url)) },
                icon = MomenticIcons.Link,
                compact = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth().padding(top = MomenticSpacing.xxs), horizontalArrangement = Arrangement.spacedBy(MomenticSpacing.sm)) {
                SecondaryButton(stringResource(Res.string.web_edit), onClick = onEdit, icon = MomenticIcons.Pencil, modifier = Modifier.weight(1f))
                PrimaryButton(stringResource(Res.string.web_publish), onClick = { onPublish(creationId) }, modifier = Modifier.weight(1f))
            }
        },
    )
}
