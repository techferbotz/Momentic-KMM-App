package com.ferbotz.momentic.feature.web

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ferbotz.momentic.designsystem.components.SecondaryButton
import com.ferbotz.momentic.feature.shell.StandInNote
import com.ferbotz.momentic.feature.shell.StandInScreen
import com.ferbotz.momentic.presentation.links.rememberLinkOpener
import com.ferbotz.momentic.resources.Res
import com.ferbotz.momentic.resources.action_open_in_browser
import org.jetbrains.compose.resources.stringResource

/** A web page in the app's own frame — a template's live example. */
@Composable
fun WebPageRoute(url: String, title: String?, onBack: () -> Unit) {
    val links = rememberLinkOpener()
    StandInScreen(title = title.orEmpty(), onBack = onBack) {
        StandInNote(url)
        SecondaryButton(stringResource(Res.string.action_open_in_browser), onClick = { links.openPage(url) }, modifier = Modifier.fillMaxWidth())
    }
}
