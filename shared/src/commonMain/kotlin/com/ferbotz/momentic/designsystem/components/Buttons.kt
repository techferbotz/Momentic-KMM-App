package com.ferbotz.momentic.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ferbotz.momentic.resources.Res
import com.ferbotz.momentic.resources.cd_loading
import com.ferbotz.momentic.theme.Momentic
import com.ferbotz.momentic.theme.MomenticColors
import com.ferbotz.momentic.theme.MomenticElevation
import com.ferbotz.momentic.theme.MomenticMotion
import com.ferbotz.momentic.theme.MomenticShapes
import com.ferbotz.momentic.theme.MomenticSizes
import com.ferbotz.momentic.theme.momenticShadow
import org.jetbrains.compose.resources.stringResource

/** The four button looks from the canvas. One primary per screen. */
enum class ButtonStyle {
    /** Petal fill, ink label, soft shadow. */
    Primary,

    /** Soft-rose outline, rose label. */
    Secondary,

    /** Rose label only, 48 tall — for the quieter action beside a primary. */
    Quiet,

    /** Soft-red outline, red label, always with an icon and words. */
    Destructive,

    /** Red label only, 48 tall, with an icon — "Remove this moment" under a sheet's main action. */
    QuietDestructive,
}

/**
 * A fully rounded button, 52 tall (44 when [compact], which also sets a 14 sp label; [ButtonStyle.Quiet]
 * is always 48). Pressed it deepens and settles to 98 %; disabled it goes quiet; [loading] swaps the icon
 * for a spinner, keeps the width and stops further taps. A label too long for one line wraps onto a
 * second rather than being cut, so it still reads at large text sizes.
 */
@Composable
fun MomenticButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ButtonStyle = ButtonStyle.Primary,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    compact: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val active = enabled && !loading
    val palette = buttonPalette(style, Momentic.colors, pressed = pressed && active, enabled = enabled)
    val scale by animateFloatAsState(if (pressed && active) 0.98f else 1f, MomenticMotion.quick(), label = "press")
    val quiet = style == ButtonStyle.Quiet || style == ButtonStyle.QuietDestructive
    val height = when {
        quiet -> MomenticSizes.minTouchTarget
        compact -> MomenticSizes.compactButtonHeight
        else -> MomenticSizes.buttonHeight
    }
    val loadingLabel = stringResource(Res.string.cd_loading)
    val shape = MomenticShapes.pill

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (style == ButtonStyle.Primary && active && !pressed) {
                    Modifier.momenticShadow(MomenticElevation.Rest, shape)
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .background(palette.background)
            .then(palette.border?.let { Modifier.border(1.5.dp, it, shape) } ?: Modifier)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = active,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { if (loading) stateDescription = loadingLabel }
            .heightIn(min = height)
            .padding(horizontal = if (compact || quiet) 16.dp else 24.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when {
                loading -> Spinner(color = palette.content, size = if (compact) 16.dp else 18.dp)
                icon != null -> Icon(icon, contentDescription = null, tint = palette.content, modifier = Modifier.size(20.dp))
            }
            Text(
                text = text,
                style = if (compact) Momentic.type.label else Momentic.type.button,
                color = palette.content,
                // A wrapped label stays beside its icon; without one it centres.
                textAlign = if (icon != null || loading) TextAlign.Start else TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    compact: Boolean = false,
) = MomenticButton(text, onClick, modifier, ButtonStyle.Primary, icon, enabled, loading, compact)

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    compact: Boolean = false,
) = MomenticButton(text, onClick, modifier, ButtonStyle.Secondary, icon, enabled, loading, compact)

@Composable
fun QuietButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    compact: Boolean = false,
) = MomenticButton(text, onClick, modifier, ButtonStyle.Quiet, icon, enabled, loading, compact)

@Composable
fun DestructiveButton(
    text: String,
    onClick: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    compact: Boolean = false,
) = MomenticButton(text, onClick, modifier, ButtonStyle.Destructive, icon, enabled, loading, compact)

private class ButtonPalette(val background: Color, val content: Color, val border: Color?)

private fun buttonPalette(style: ButtonStyle, c: MomenticColors, pressed: Boolean, enabled: Boolean): ButtonPalette =
    when (style) {
        ButtonStyle.Primary -> when {
            !enabled -> ButtonPalette(c.line, c.inkSoft, null)
            pressed -> ButtonPalette(c.petalPressed, c.ink, null)
            else -> ButtonPalette(c.petal, c.ink, null)
        }
        ButtonStyle.Secondary -> when {
            !enabled -> ButtonPalette(Color.Transparent, c.inkSoft, c.line)
            pressed -> ButtonPalette(c.blush100, c.roseDeep, c.roseDeep)
            else -> ButtonPalette(Color.Transparent, c.rose, c.roseSoft)
        }
        ButtonStyle.Quiet -> when {
            !enabled -> ButtonPalette(Color.Transparent, c.inkSoft, null)
            pressed -> ButtonPalette(c.blush100, c.roseDeep, null)
            else -> ButtonPalette(Color.Transparent, c.rose, null)
        }
        ButtonStyle.Destructive -> when {
            !enabled -> ButtonPalette(Color.Transparent, c.inkSoft, c.line)
            pressed -> ButtonPalette(c.errorFill, c.error, c.error)
            else -> ButtonPalette(Color.Transparent, c.error, c.errorSoft)
        }
        ButtonStyle.QuietDestructive -> when {
            !enabled -> ButtonPalette(Color.Transparent, c.inkSoft, null)
            pressed -> ButtonPalette(c.errorFill, c.error, null)
            else -> ButtonPalette(Color.Transparent, c.error, null)
        }
    }

/** The looks of a round, 48 dp icon button. */
enum class IconButtonStyle {
    /** No container — top bars. */
    Plain,

    /** Cream disc with a hairline — on paper. */
    Outlined,

    /** Cream disc with a soft shadow — floating over template art. */
    OnArt,

    /** Soft-rose ring, rose glyph — a tappable action in a list row (call a guest). */
    Accent,
}

/** A round icon-only button. [contentDescription] is required: it's the button's only name. */
@Composable
fun MomenticIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: IconButtonStyle = IconButtonStyle.Plain,
    enabled: Boolean = true,
    size: Dp = MomenticSizes.iconButton,
) {
    val c = Momentic.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val (background, border, tint) = when (style) {
        IconButtonStyle.Plain -> Triple(if (pressed) c.blush100 else Color.Transparent, null, c.ink)
        IconButtonStyle.Outlined -> Triple(if (pressed) c.blush100 else c.surface, c.line, c.ink)
        IconButtonStyle.OnArt -> Triple(if (pressed) c.blush100 else c.surface.copy(alpha = 0.94f), null, c.ink)
        IconButtonStyle.Accent -> Triple(if (pressed) c.blush100 else Color.Transparent, c.roseSoft, c.rose)
    }
    Box(
        modifier = modifier
            .size(size)
            .then(if (style == IconButtonStyle.OnArt) Modifier.momenticShadow(MomenticElevation.Rest, CircleShape) else Modifier)
            .clip(CircleShape)
            .background(background)
            .then(border?.let { Modifier.border(if (style == IconButtonStyle.Accent) 1.5.dp else 1.dp, it, CircleShape) } ?: Modifier)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription }
            .graphicsLayer { alpha = if (enabled) 1f else 0.5f },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (enabled) tint else c.inkSoft, modifier = Modifier.size(MomenticSizes.icon))
    }
}
