package com.ferbotz.momentic.designsystem.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.ferbotz.momentic.designsystem.icons.MomenticIcons
import com.ferbotz.momentic.resources.Res
import com.ferbotz.momentic.resources.cd_dismiss
import com.ferbotz.momentic.theme.Momentic
import com.ferbotz.momentic.theme.MomenticColors
import com.ferbotz.momentic.theme.MomenticElevation
import com.ferbotz.momentic.theme.MomenticShapes
import com.ferbotz.momentic.theme.MomenticSpacing
import com.ferbotz.momentic.theme.momenticShadow
import org.jetbrains.compose.resources.stringResource

/** How a message reads: its fill, icon and ink. Each always pairs an icon with words. */
enum class MessageTone {
    /** Blush note — "You can keep up to 20 drafts". */
    Info,

    /** Cream with a hairline — neutral facts ("You've added 60 photos"). */
    Neutral,

    /** Honey — limits that pass ("Try again in 12 minutes"). */
    Warning,

    /** Red — something failed. */
    Error,

    /** No connection. */
    Offline,

    /** Done — "Link copied". */
    Success,
}

private class TonePalette(val fill: Color, val border: Color?, val icon: Color, val text: Color, val glyph: ImageVector)

private fun MessageTone.palette(c: MomenticColors): TonePalette = when (this) {
    MessageTone.Info -> TonePalette(c.blush100, null, c.roseDeep, c.ink, MomenticIcons.Info)
    MessageTone.Neutral -> TonePalette(c.surface, c.line, c.inkSoft, c.ink, MomenticIcons.Info)
    MessageTone.Warning -> TonePalette(c.warningFill, null, c.warning, c.warning, MomenticIcons.Clock)
    MessageTone.Error -> TonePalette(c.errorFill, null, c.error, c.error, MomenticIcons.Alert)
    MessageTone.Offline -> TonePalette(c.surface, c.line, c.ink, c.ink, MomenticIcons.WifiOff)
    MessageTone.Success -> TonePalette(c.successFill, null, c.success, c.ink, MomenticIcons.CheckCircle)
}

/**
 * A message where the problem is — under a field, above a list. [lead] is set in bold before the
 * text ("You're offline. We'll keep trying."). An action and a dismiss button are optional.
 */
@Composable
fun InlineBanner(
    text: String,
    tone: MessageTone,
    modifier: Modifier = Modifier,
    lead: String? = null,
    icon: ImageVector? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
) {
    val p = tone.palette(Momentic.colors)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MomenticShapes.input)
            .background(p.fill)
            .then(p.border?.let { Modifier.border(1.dp, it, MomenticShapes.input) } ?: Modifier)
            .heightIn(min = 48.dp)
            .padding(start = MomenticSpacing.md, end = if (actionLabel != null || onDismiss != null) MomenticSpacing.xxs else MomenticSpacing.md)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(MomenticSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon ?: p.glyph, contentDescription = null, tint = p.icon, modifier = Modifier.size(20.dp))
        Text(
            text = buildAnnotatedString {
                if (lead != null) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(lead) }
                    append(' ')
                }
                append(text)
            },
            style = if (tone == MessageTone.Warning || tone == MessageTone.Error) {
                Momentic.type.bodySmall.copy(fontWeight = FontWeight.SemiBold)
            } else {
                Momentic.type.bodySmall
            },
            color = p.text,
            modifier = Modifier.weight(1f).padding(vertical = MomenticSpacing.sm),
        )
        if (actionLabel != null && onAction != null) {
            QuietButton(actionLabel, onAction, compact = true)
        }
        if (onDismiss != null) {
            MomenticIconButton(MomenticIcons.Close, stringResource(Res.string.cd_dismiss), onDismiss, size = 44.dp)
        }
    }
}

/** A snackbar message with a tone. Plain `showSnackbar(String)` calls read as [MessageTone.Neutral]. */
class MomenticSnackbarVisuals(
    override val message: String,
    val tone: MessageTone = MessageTone.Neutral,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = false,
    override val duration: SnackbarDuration = if (actionLabel == null) SnackbarDuration.Short else SnackbarDuration.Long,
) : SnackbarVisuals

/** The host every screen's `message` goes to. Snackbars sit 16 dp above the bottom bar. */
@Composable
fun MomenticSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState, modifier) { data -> MomenticSnackbar(data) }
}

/** Cream, a hairline, a raised shadow — never a dark bar: white text only sits on rose-deep. */
@Composable
fun MomenticSnackbar(data: SnackbarData, modifier: Modifier = Modifier) {
    val visuals = data.visuals
    val tone = (visuals as? MomenticSnackbarVisuals)?.tone ?: MessageTone.Neutral
    val c = Momentic.colors
    val p = tone.palette(c)
    Row(
        modifier = modifier
            .padding(horizontal = MomenticSpacing.gutter, vertical = MomenticSpacing.md)
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .momenticShadow(MomenticElevation.Raised, MomenticShapes.input)
            .clip(MomenticShapes.input)
            .background(c.surface)
            .border(1.dp, c.line, MomenticShapes.input)
            .heightIn(min = 52.dp)
            .padding(start = MomenticSpacing.md, end = if (visuals.actionLabel != null) MomenticSpacing.xxs else MomenticSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(MomenticSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(p.glyph, contentDescription = null, tint = if (tone == MessageTone.Neutral) c.inkSoft else p.icon, modifier = Modifier.size(20.dp))
        Text(visuals.message, style = Momentic.type.bodySmall, color = c.ink, modifier = Modifier.weight(1f).padding(vertical = MomenticSpacing.sm))
        visuals.actionLabel?.let { label -> QuietButton(label, onClick = { data.performAction() }, compact = true) }
    }
}

/**
 * An empty list's message: a soft illustration, one serif line, one sentence of help — and a way
 * forward when there is one.
 */
@Composable
fun EmptyState(
    illustration: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    val c = Momentic.colors
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = MomenticSpacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MomenticSpacing.sm),
    ) {
        Image(illustration, contentDescription = null, modifier = Modifier.size(140.dp))
        Text(title, style = Momentic.type.h1, color = c.ink, textAlign = TextAlign.Center, modifier = Modifier.padding(top = MomenticSpacing.xs))
        Text(body, style = Momentic.type.body, color = c.inkSoft, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 300.dp))
        if (action != null) {
            Column(Modifier.padding(top = MomenticSpacing.sm)) { action() }
        }
    }
}
