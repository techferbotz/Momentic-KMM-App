package com.ferbotz.momentic.platform

import androidx.compose.runtime.Composable

/**
 * Runs [onBack] in place of the system's Back — Android's back button or gesture, the iOS edge swipe —
 * while [enabled]. For a screen that has to do something before it goes (the form saves first).
 */
@Composable
expect fun SystemBack(enabled: Boolean = true, onBack: () -> Unit)
