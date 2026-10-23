package com.ferbotz.momentic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/*
 * The remote-config gate screens (HOA protocol 11 §3): what replaces — or sits over — the app when
 * `GET /config` says maintenance is on or this build must / should update.
 */

/** Shown instead of the app while `maintenance.enabled` is true. */
@Composable
fun MaintenanceScreen(message: String, onRetry: () -> Unit) {
    GateScaffold(emoji = "🕯️", title = "Back in a moment", message = message) {
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("Try again") }
    }
}

/** Hard block: the running build is below `update.minSupportedBuild`. No dismiss. */
@Composable
fun UpdateRequiredScreen(message: String?, storeUrl: String?) {
    val uriHandler = LocalUriHandler.current
    GateScaffold(
        emoji = "🎁",
        title = "Time to update",
        message = message ?: "This version of Momentic is too old to keep creating. Please update to continue.",
    ) {
        // No store listing yet ⇒ nothing to link to; the message alone has to do.
        if (storeUrl != null) {
            Button(
                onClick = { runCatching { uriHandler.openUri(storeUrl) } },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Update now") }
        }
    }
}

/** Dismissible nudge: the running build is below `update.recommendedBuild`. */
@Composable
fun UpdateNudgeDialog(message: String?, storeUrl: String?, onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("A new version of Momentic is here") },
        text = { Text(message ?: "Update for the latest fixes and new ways to make your moments.") },
        confirmButton = {
            if (storeUrl != null) {
                TextButton(onClick = { runCatching { uriHandler.openUri(storeUrl) }; onDismiss() }) { Text("Update") }
            } else {
                TextButton(onClick = onDismiss) { Text("Okay") }
            }
        },
        dismissButton = if (storeUrl != null) {
            { TextButton(onClick = onDismiss) { Text("Not now") } }
        } else {
            null
        },
    )
}

@Composable
private fun GateScaffold(
    emoji: String,
    title: String,
    message: String,
    actions: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier.size(112.dp).clip(CircleShape).background(colors.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = emoji, style = MaterialTheme.typography.displayMedium)
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = colors.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
            actions()
        }
    }
}
