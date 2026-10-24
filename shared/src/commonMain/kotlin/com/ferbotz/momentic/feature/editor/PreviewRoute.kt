package com.ferbotz.momentic.feature.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ferbotz.momentic.designsystem.components.PrimaryButton
import com.ferbotz.momentic.designsystem.components.SecondaryButton
import com.ferbotz.momentic.designsystem.icons.MomenticIcons
import com.ferbotz.momentic.feature.shell.StandInNote
import com.ferbotz.momentic.feature.shell.StandInScreen
import com.ferbotz.momentic.presentation.links.rememberLinkOpener
import com.ferbotz.momentic.resources.Res
import com.ferbotz.momentic.resources.action_open_in_browser
import com.ferbotz.momentic.theme.MomenticSpacing
import org.jetbrains.compose.resources.stringResource

/** The draft on the web, only the creator can see it; Edit goes back to the form, Publish on to paying. */
@Composable
fun PreviewRoute(
    url: String,
    creationId: String,
    onEdit: () -> Unit,
    onPublish: (creationId: String) -> Unit,
) {
    val links = rememberLinkOpener()
    StandInScreen(title = "Preview", onBack = onEdit) {
        StandInNote(url)
        SecondaryButton(stringResource(Res.string.action_open_in_browser), onClick = { links.openPage(url) }, modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MomenticSpacing.xs)) {
            SecondaryButton("Edit", onClick = onEdit, icon = MomenticIcons.Pencil, modifier = Modifier.weight(1f))
            PrimaryButton("Publish", onClick = { onPublish(creationId) }, modifier = Modifier.weight(1.4f))
        }
    }
}
