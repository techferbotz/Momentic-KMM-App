package com.ferbotz.momentic.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * A web page shown inside the app's own frame — a template's live example, the creator's preview. What
 * the page is doing (loading, how far, or failed) is here for the frame to show.
 */
@Stable
class WebPageState(val url: String) {
    var isLoading by mutableStateOf(true)
        internal set
    var progress by mutableFloatStateOf(0f)
        internal set
    var failed by mutableStateOf(false)
        internal set

    /** Bumped by [reload]; the platform view reloads when it changes. */
    internal var reloads by mutableIntStateOf(0)
        private set

    /** Loads the page again — after it failed, say. */
    fun reload() {
        failed = false
        isLoading = true
        reloads++
    }
}

@Composable
fun rememberWebPageState(url: String): WebPageState = remember(url) { WebPageState(url) }

/**
 * The page: `WebView` on Android, `WKWebView` on iOS — JavaScript and storage on, media allowed to play
 * inline (the templates animate and some play music). Links to other apps (`tel:`, `mailto:`, WhatsApp,
 * maps) leave for those apps; web links stay in the page.
 */
@Composable
expect fun WebPage(state: WebPageState, modifier: Modifier = Modifier)
