package com.ferbotz.momentic.designsystem.fields

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ferbotz.momentic.designsystem.icons.MomenticIcons
import com.ferbotz.momentic.theme.Momentic
import com.ferbotz.momentic.theme.MomenticColors

/** Every input is 56 tall with 14 dp corners (the canvas's field kit). */
internal val FieldHeight = 56.dp
internal val FieldCorner = 14.dp
internal val FieldShape = RoundedCornerShape(FieldCorner)

/** How an input's container reads: at rest, focused (a rose ring and soft halo), or wrong (a red ring). */
enum class FieldTone { Rest, Focused, Error }

/** The tone for an input given its focus and error. An error wins: the person is told what to fix. */
fun fieldTone(focused: Boolean, hasError: Boolean): FieldTone = when {
    hasError -> FieldTone.Error
    focused -> FieldTone.Focused
    else -> FieldTone.Rest
}

/**
 * An input's container: cream, a hairline at rest, a 2 dp ring when focused or wrong, and with focus a
 * 4 dp rose halo drawn outside the bounds (so focusing never shifts the layout).
 */
fun Modifier.fieldContainer(tone: FieldTone, colors: MomenticColors, corner: Dp = FieldCorner): Modifier {
    val shape = RoundedCornerShape(corner)
    val ring = when (tone) {
        FieldTone.Rest -> colors.line
        FieldTone.Focused -> colors.rose
        FieldTone.Error -> colors.error
    }
    return this
        .then(if (tone == FieldTone.Focused) Modifier.focusHalo(colors.rose.copy(alpha = 0.16f), corner) else Modifier)
        .clip(shape)
        .background(colors.surface)
        .border(if (tone == FieldTone.Rest) 1.dp else 2.dp, ring, shape)
}

/** A soft ring [spread] outside the element's bounds — the canvas's `box-shadow: 0 0 0 4px`. */
fun Modifier.focusHalo(color: Color, corner: Dp, spread: Dp = 4.dp): Modifier = drawBehind {
    val outset = spread.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(-outset, -outset),
        size = Size(size.width + 2 * outset, size.height + 2 * outset),
        cornerRadius = CornerRadius(corner.toPx() + outset),
    )
}

/** A dashed outline — the empty photo slot, the drop target while dragging. */
fun Modifier.dashedBorder(width: Dp, color: Color, corner: Dp, dash: Dp = 6.dp, gap: Dp = 4.dp): Modifier = drawBehind {
    val stroke = width.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(stroke / 2, stroke / 2),
        size = Size(size.width - stroke, size.height - stroke),
        cornerRadius = CornerRadius(corner.toPx() - stroke / 2),
        style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash.toPx(), gap.toPx()))),
    )
}

/**
 * A field's label, 14 semibold, with a rose * when it's required, and an optional note on the right
 * ("3 of 8").
 */
@Composable
fun FieldLabel(text: String, required: Boolean, modifier: Modifier = Modifier, trailing: String? = null) {
    val c = Momentic.colors
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Text(
            text = buildAnnotatedString {
                append(text)
                if (required) {
                    append(" ")
                    withStyle(SpanStyle(color = c.rose)) { append("*") }
                }
            },
            style = Momentic.type.label,
            color = c.ink,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            Text(trailing, style = Momentic.type.caption, color = c.inkSoft)
        }
    }
}

/**
 * The line under an input: a helper or — instead — the error, which says what to do, with an alert icon;
 * and a counter on the right when there's a limit (red and bold once it's over).
 */
@Composable
fun FieldFooter(
    helper: String?,
    error: String?,
    modifier: Modifier = Modifier,
    counter: String? = null,
    counterOver: Boolean = false,
) {
    if (helper == null && error == null && counter == null) return
    val c = Momentic.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (error != null) {
            Row(
                modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite },
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(MomenticIcons.Alert, contentDescription = null, tint = c.error, modifier = Modifier.size(16.dp))
                Text(error, style = Momentic.type.captionStrong, color = c.error)
            }
        } else {
            Text(helper.orEmpty(), style = Momentic.type.caption, color = c.inkSoft, modifier = Modifier.weight(1f))
        }
        if (counter != null) {
            Text(
                text = counter,
                style = if (counterOver) Momentic.type.caption.copy(fontWeight = FontWeight.Bold) else Momentic.type.caption,
                color = if (counterOver) c.error else c.inkSoft,
            )
        }
    }
}
