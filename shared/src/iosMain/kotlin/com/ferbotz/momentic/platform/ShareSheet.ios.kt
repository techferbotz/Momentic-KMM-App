package com.ferbotz.momentic.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIActivityViewController
import platform.UIKit.popoverPresentationController

@Composable
actual fun rememberShareSheet(): ShareSheet = remember { IosShareSheet() }

private class IosShareSheet : ShareSheet {

    @OptIn(ExperimentalForeignApi::class)
    override fun share(text: String): Boolean {
        val host = topViewController() ?: return false
        val sheet = UIActivityViewController(activityItems = listOf(text), applicationActivities = null)
        // On iPad the sheet is a popover and must be anchored, or UIKit raises; the centre of the screen will do.
        sheet.popoverPresentationController?.let { popover ->
            val view = host.view
            popover.sourceView = view
            view.bounds.useContents { popover.sourceRect = CGRectMake(size.width / 2, size.height / 2, 0.0, 0.0) }
        }
        host.presentViewController(sheet, animated = true, completion = null)
        return true
    }
}
