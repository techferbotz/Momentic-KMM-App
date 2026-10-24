package com.ferbotz.momentic.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

@Composable
actual fun MomenticDialogWindow(onDismissRequest: () -> Unit, scrim: Color, content: @Composable () -> Unit) {
    Dialog(onDismissRequest, DialogProperties(usePlatformDefaultWidth = false)) {
        // Android dims behind a dialog in black; only the amount can follow the scrim.
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.setDimAmount(scrim.alpha) }
        content()
    }
}
