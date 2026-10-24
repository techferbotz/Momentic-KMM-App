package com.ferbotz.momentic.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * A dialog window that sizes itself (no platform width) and dims what's behind it with [scrim] instead of
 * the platform's darker default — iOS draws the scrim in Compose, Android dims the dialog's window.
 */
@Composable
expect fun MomenticDialogWindow(onDismissRequest: () -> Unit, scrim: Color, content: @Composable () -> Unit)
