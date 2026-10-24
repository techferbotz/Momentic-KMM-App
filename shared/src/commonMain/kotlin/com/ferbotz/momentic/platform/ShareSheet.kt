package com.ferbotz.momentic.platform

import androidx.compose.runtime.Composable

/** The system share sheet, for a moment's link and the line that goes with it. */
interface ShareSheet {

    /** Opens the share sheet with [text] (the message, ending in the link). False when nothing could take it. */
    fun share(text: String): Boolean
}

/** Remembers the platform [ShareSheet]: `ACTION_SEND` on Android, `UIActivityViewController` on iOS. */
@Composable
expect fun rememberShareSheet(): ShareSheet

/** "Text, then the link" — how every share reads, whether or not [text] already ends with the link. */
fun shareText(text: String, url: String): String =
    if (text.contains(url)) text else listOf(text.trim(), url).filter { it.isNotEmpty() }.joinToString(" ")
