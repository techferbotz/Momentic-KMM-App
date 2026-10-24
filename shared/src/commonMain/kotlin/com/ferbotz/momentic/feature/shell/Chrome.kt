package com.ferbotz.momentic.feature.shell

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import com.ferbotz.momentic.designsystem.components.MessageTone
import com.ferbotz.momentic.designsystem.components.MomenticSnackbarVisuals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The app's one snackbar host, above the bottom bar (or the gesture area), and the scope its messages
 * run in — the shell's, so a message outlives the screen that sent it ("Draft deleted", then back to the
 * list). Messages queue; each shows in turn.
 */
@Stable
class Messenger(val hostState: SnackbarHostState, private val scope: CoroutineScope) {

    /** Shows [message]; [onAction] runs if its [actionLabel] is tapped. */
    fun show(
        message: String,
        tone: MessageTone = MessageTone.Neutral,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null,
    ) {
        scope.launch {
            val result = hostState.showSnackbar(MomenticSnackbarVisuals(message, tone, actionLabel))
            if (result == SnackbarResult.ActionPerformed) onAction?.invoke()
        }
    }
}

val LocalMessenger = staticCompositionLocalOf<Messenger> { error("No messenger: wrap the UI in MomenticShell") }

/**
 * A recommended update the person hasn't dismissed — shown as a banner at the top of Explore rather than
 * a dialog over the app. [onUpdate] is null when this platform's store listing isn't known yet.
 */
@Stable
class UpdateNudge(
    val message: String?,
    val onUpdate: (() -> Unit)?,
    val onDismiss: () -> Unit,
)

val LocalUpdateNudge = staticCompositionLocalOf<UpdateNudge?> { null }

/**
 * Shows a view model's one-off [message] as a snackbar, then tells the view model it was shown — the
 * contract every screen shares (`message` + `messageShown()`). The snackbar runs in the shell's scope,
 * so clearing the message doesn't take it down.
 */
@Composable
fun MessageEffect(message: String?, onShown: () -> Unit, tone: MessageTone = MessageTone.Neutral) {
    val messenger = LocalMessenger.current
    val shown by rememberUpdatedState(onShown)
    LaunchedEffect(message) {
        if (message == null) return@LaunchedEffect
        messenger.show(message, tone)
        shown()
    }
}
