package com.ferbotz.momentic.designsystem.fields

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ferbotz.momentic.designsystem.icons.MomenticIcons
import com.ferbotz.momentic.presentation.editor.FormEngine
import com.ferbotz.momentic.resources.Res
import com.ferbotz.momentic.resources.field_counter
import com.ferbotz.momentic.resources.field_over_limit
import com.ferbotz.momentic.resources.field_required
import com.ferbotz.momentic.resources.number_at_least
import com.ferbotz.momentic.resources.number_at_most
import com.ferbotz.momentic.resources.number_between
import com.ferbotz.momentic.resources.number_less
import com.ferbotz.momentic.resources.number_more
import com.ferbotz.momentic.theme.Momentic
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs

/**
 * A text input from the field kit: label (with * when [required]), a 56-tall cream box — or a long-text
 * box that grows to six lines and then scrolls — and a footer with the helper or error and, when there's
 * a [maxLength], a counter. Typing past the limit is allowed but flagged, as the canvas shows it.
 *
 * The label and footer sit inside the text field's own decoration, so a screen reader reads them with it.
 */
@Composable
fun MomenticTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    helper: String? = null,
    error: String? = null,
    placeholder: String? = null,
    maxLength: Int? = null,
    multiline: Boolean = false,
    leadingIcon: ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    enabled: Boolean = true,
) {
    val c = Momentic.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val over = maxLength != null && value.length > maxLength
    val shownError = error ?: if (over) stringResource(Res.string.field_over_limit, maxLength) else null
    val tone = fieldTone(focused, shownError != null)
    val requiredLabel = stringResource(Res.string.field_required)
    val counter = maxLength?.let { stringResource(Res.string.field_counter, value.length, it) }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = if (required) "$label, $requiredLabel" else label },
        enabled = enabled,
        textStyle = Momentic.type.body.copy(color = c.ink),
        cursorBrush = SolidColor(c.rose),
        keyboardOptions = KeyboardOptions(
            capitalization = capitalization,
            keyboardType = keyboardType,
            imeAction = if (multiline) ImeAction.Default else ImeAction.Next,
        ),
        singleLine = !multiline,
        minLines = if (multiline) 4 else 1,
        maxLines = if (multiline) 6 else 1,
        interactionSource = interaction,
        decorationBox = { inner ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldLabel(label, required)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = FieldHeight)
                        .fieldContainer(tone, c)
                        .padding(horizontal = 16.dp, vertical = if (multiline) 14.dp else 0.dp),
                    verticalAlignment = if (multiline) Alignment.Top else Alignment.CenterVertically,
                ) {
                    if (leadingIcon != null) {
                        Icon(leadingIcon, contentDescription = null, tint = c.inkSoft, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(12.dp))
                    }
                    Box(Modifier.weight(1f).padding(vertical = if (multiline) 0.dp else 16.dp)) {
                        if (value.isEmpty() && placeholder != null) {
                            Text(placeholder, style = Momentic.type.body, color = c.inkSoft, maxLines = if (multiline) 4 else 1)
                        }
                        inner()
                    }
                }
                FieldFooter(helper = helper, error = shownError, counter = counter, counterOver = over)
            }
        },
    )
}

/**
 * A number with − and + either side (a stepper, 200 wide); the number itself can also be typed. A button
 * goes quiet at the end of the range; "–" stands in until there's a value.
 */
@Composable
fun NumberStepper(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    helper: String? = null,
    error: String? = null,
    min: Double? = null,
    max: Double? = null,
) {
    val c = Momentic.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val number = FormEngine.parseNumber(value)
    val canLower = number != null && (min == null || number - 1 >= min)
    val canRaise = max == null || number == null || number + 1 <= max
    val requiredLabel = stringResource(Res.string.field_required)
    val range = when {
        min != null && max != null -> stringResource(Res.string.number_between, plain(min), plain(max))
        min != null -> stringResource(Res.string.number_at_least, plain(min))
        max != null -> stringResource(Res.string.number_at_most, plain(max))
        else -> null
    }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel(label, required)
        Row(
            modifier = Modifier
                .width(200.dp)
                .height(FieldHeight)
                .fieldContainer(fieldTone(focused, error != null), c),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepButton(MomenticIcons.Minus, stringResource(Res.string.number_less), enabled = canLower) {
                number?.let { onValueChange(plain(stepped(it - 1, min, max))) }
            }
            BasicTextField(
                value = value,
                onValueChange = { typed -> onValueChange(typed.filter { it.isDigit() || it == '.' || it == ',' || it == '-' }) },
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = if (required) "$label, $requiredLabel" else label },
                textStyle = Momentic.type.h2.copy(color = c.ink, textAlign = TextAlign.Center),
                cursorBrush = SolidColor(c.rose),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                singleLine = true,
                interactionSource = interaction,
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.Center) {
                        if (value.isEmpty()) Text("–", style = Momentic.type.h2, color = c.inkSoft)
                        inner()
                    }
                },
            )
            StepButton(MomenticIcons.Plus, stringResource(Res.string.number_more), enabled = canRaise) {
                val next = if (number == null) min ?: 0.0 else number + 1
                onValueChange(plain(stepped(next, min, max)))
            }
        }
        FieldFooter(helper = helper ?: range, error = error)
    }
}

@Composable
private fun StepButton(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    val c = Momentic.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .size(56.dp)
            .fillMaxHeight()
            .background(if (pressed && enabled) c.blush100 else Color.Transparent)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label }
            .graphicsLayer { alpha = if (enabled) 1f else 0.5f },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (pressed && enabled) c.roseDeep else if (enabled) c.ink else c.inkSoft, modifier = Modifier.size(22.dp))
    }
}

/** Keeps a stepped value inside the field's range. */
private fun stepped(value: Double, min: Double?, max: Double?): Double {
    var result = value
    if (min != null && result < min) result = min
    if (max != null && result > max) result = max
    return result
}

/** 12.0 → "12", 2.5 → "2.5". */
private fun plain(value: Double): String =
    if (value % 1.0 == 0.0 && abs(value) < 1e15) value.toLong().toString() else value.toString()
