package com.ferbotz.momentic.presentation.links

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.ferbotz.momentic.data.util.isWebUrl
import com.ferbotz.momentic.data.util.mailtoUri
import com.ferbotz.momentic.platform.findActivity
import com.ferbotz.momentic.theme.MomenticTheme

/** Custom Tabs, in the palette of the screen the page was opened from. */
@Composable
actual fun rememberLinkOpener(): LinkOpener {
    val context = LocalContext.current
    val dark = MomenticTheme.isDark
    val toolbarColor = MaterialTheme.colorScheme.background.toArgb()
    return remember(context, dark, toolbarColor) { AndroidLinkOpener(context, dark, toolbarColor) }
}

private class AndroidLinkOpener(
    private val context: Context,
    private val dark: Boolean,
    private val toolbarColor: Int,
) : LinkOpener {

    override fun openPage(url: String): Boolean {
        val target = url.trim()
        if (!isWebUrl(target)) return false
        val tab = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setColorScheme(if (dark) CustomTabsIntent.COLOR_SCHEME_DARK else CustomTabsIntent.COLOR_SCHEME_LIGHT)
            .setDefaultColorSchemeParams(
                CustomTabColorSchemeParams.Builder().setToolbarColor(toolbarColor).build(),
            )
            .build()
        val activity = context.findActivity()
        // Outside an Activity (a preview, a wrapped context) the tab needs a task of its own.
        if (activity == null) tab.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            // A browser without Custom Tabs ignores the extras and opens the page itself, so the only
            // failure left is a device with no browser at all.
            tab.launchUrl(activity ?: context, Uri.parse(target))
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }

    override fun composeEmail(address: String, subject: String?, body: String?, onUnavailable: () -> Unit) {
        // SENDTO + mailto: only mail apps answer it.
        val email = Intent(Intent.ACTION_SENDTO, Uri.parse(mailtoUri(address, subject, body)))
        val activity = context.findActivity()
        if (activity == null) email.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            (activity ?: context).startActivity(email)
        } catch (e: ActivityNotFoundException) {
            onUnavailable()
        }
    }
}
