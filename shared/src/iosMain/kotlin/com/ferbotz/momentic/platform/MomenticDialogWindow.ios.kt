package com.ferbotz.momentic.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun MomenticDialogWindow(onDismissRequest: () -> Unit, scrim: Color, content: @Composable () -> Unit) {
    Dialog(onDismissRequest, DialogProperties(usePlatformDefaultWidth = false, scrimColor = scrim), content)
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun FullScreenWindow(onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            usePlatformInsets = false,
            scrimColor = Color.Transparent,
        ),
        content = content,
    )
}
