package com.ferbotz.momentic.platform

import androidx.compose.runtime.Composable

/**
 * Whether the system asks apps to reduce motion — Android's "Remove animations" (animator duration
 * scale 0), iOS's Reduce Motion. Read once per composition of the theme.
 */
@Composable
expect fun rememberReduceMotion(): Boolean
