package com.ferbotz.momentic.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Gentle, 200–300 ms, ease-out. The only celebration in the app is the falling petals on "Your moment
 * is live"; when the system asks for reduced motion ([Momentic.reduceMotion]) loops stand still.
 */
object MomenticMotion {
    /** Presses, chips, toggles. */
    const val QUICK_MS = 200

    /** Screen transitions. */
    const val SCREEN_MS = 250

    /** Sheets rising and settling. */
    const val SHEET_MS = 300

    val easeOut: Easing = CubicBezierEasing(0f, 0f, 0.2f, 1f)

    fun <T> quick(): TweenSpec<T> = tween(QUICK_MS, easing = easeOut)
}

internal val LocalReduceMotion = staticCompositionLocalOf { false }
