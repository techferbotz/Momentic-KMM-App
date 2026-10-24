package com.ferbotz.momentic.designsystem.fields

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ferbotz.momentic.data.model.ChoiceOption
import com.ferbotz.momentic.designsystem.icons.MomenticIcons
import com.ferbotz.momentic.presentation.editor.FormEngine
import com.ferbotz.momentic.resources.Res
import com.ferbotz.momentic.resources.colour_blush
import com.ferbotz.momentic.resources.colour_custom
import com.ferbotz.momentic.resources.colour_gold
import com.ferbotz.momentic.resources.colour_hex
import com.ferbotz.momentic.resources.colour_hex_hint
import com.ferbotz.momentic.resources.colour_lavender
import com.ferbotz.momentic.resources.colour_midnight
import com.ferbotz.momentic.resources.colour_pick_one
import com.ferbotz.momentic.resources.colour_rose
import com.ferbotz.momentic.resources.colour_sage
import com.ferbotz.momentic.resources.colour_sky
import com.ferbotz.momentic.resources.field_required
import com.ferbotz.momentic.theme.Momentic
import com.ferbotz.momentic.theme.MomenticMotion
import com.ferbotz.momentic.theme.MomenticShapes
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Yes / no as a whole row that toggles: the label and its caption on the left, the switch on the right,
 * 64 tall. A switch that's off is an outline with a small dot; on, it fills rose with a checked thumb.
 */
@Composable
fun ToggleRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    required: Boolean = false,
    error: String? = null,
) {
    val c = Momentic.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .fieldContainer(fieldTone(focused, error != null), c)
                .toggleable(value = checked, interactionSource = interaction, indication = null, role = Role.Switch, onValueChange = onCheckedChange)
                .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                FieldLabel(label, required)
                if (caption != null) Text(caption, style = Momentic.type.caption, color = c.inkSoft)
            }
            MomenticSwitch(checked)
        }
        FieldFooter(helper = null, error = error)
    }
}

/** The switch itself, 52 × 32 — drawn only; the row around it takes the tap. */
@Composable
fun MomenticSwitch(checked: Boolean, modifier: Modifier = Modifier) {
    val c = Momentic.colors
    val spec: AnimationSpec<Dp> = if (Momentic.reduceMotion) snap() else MomenticMotion.quick()
    val thumb by animateDpAsState(if (checked) 24.dp else 16.dp, spec, label = "thumb")
    val x by animateDpAsState(if (checked) 24.dp else 8.dp, spec, label = "thumbX")
    Box(
        modifier = modifier
            .size(52.dp, 32.dp)
            .clip(MomenticShapes.pill)
            .background(if (checked) c.rose else c.surface)
            .border(2.dp, if (checked) c.rose else c.inkSoft, MomenticShapes.pill),
    ) {
        Box(
            modifier = Modifier
                .offset(x = x, y = (32.dp - thumb) / 2)
                .size(thumb)
                .clip(CircleShape)
                .background(if (checked) c.white else c.inkSoft),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) Icon(MomenticIcons.Check, contentDescription = null, tint = c.rose, modifier = Modifier.size(14.dp))
        }
    }
}

/**
 * One pick from a few options, as chips that wrap (48 tall). The chosen one is blush with a rose-deep
 * ring and a check. Tapping the chosen chip of an optional field clears it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChoiceChips(
    options: List<ChoiceOption>,
    selected: String?,
    onSelect: (String?) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    helper: String? = null,
    error: String? = null,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel(label, required)
        FlowRow(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { option ->
                val isSelected = option.value == selected
                ChoiceChip(
                    label = option.label,
                    selected = isSelected,
                    hasError = error != null,
                    onClick = { onSelect(if (isSelected && !required) null else option.value) },
                )
            }
        }
        FieldFooter(helper = helper, error = error)
    }
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, hasError: Boolean, onClick: () -> Unit) {
    val c = Momentic.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val (border, width) = when {
        focused -> c.rose to 2.dp
        selected -> c.roseDeep to 1.5.dp
        hasError -> c.error to 1.dp
        else -> c.line to 1.dp
    }
    Row(
        modifier = Modifier
            .height(48.dp)
            .then(if (focused) Modifier.focusHalo(c.rose.copy(alpha = 0.16f), 24.dp) else Modifier)
            .clip(MomenticShapes.pill)
            .background(if (selected) c.blush100 else c.surface)
            .border(width, border, MomenticShapes.pill)
            .selectable(selected = selected, interactionSource = interaction, indication = null, role = Role.RadioButton, onClick = onClick)
            .padding(start = if (selected) 12.dp else 16.dp, end = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) Icon(MomenticIcons.Check, contentDescription = null, tint = c.roseDeep, modifier = Modifier.size(16.dp))
        Text(
            text = label,
            style = if (selected) Momentic.type.label.copy(fontWeight = FontWeight.Bold) else Momentic.type.label,
            color = c.ink,
            maxLines = 1,
        )
    }
}

/** A preset for colour fields — the field contract carries no options yet, so the app offers these. */
data class ColourPreset(val hex: String, val name: StringResource)

val ColourPresets: List<ColourPreset> = listOf(
    ColourPreset("#efc9d2", Res.string.colour_blush),
    ColourPreset("#b84d66", Res.string.colour_rose),
    ColourPreset("#d9b26a", Res.string.colour_gold),
    ColourPreset("#9dbfa8", Res.string.colour_sage),
    ColourPreset("#a9c3e0", Res.string.colour_sky),
    ColourPreset("#c9b8e2", Res.string.colour_lavender),
    ColourPreset("#2e3a59", Res.string.colour_midnight),
)

/**
 * A colour: named swatches, each a 40 dp disc in a 52 dp ring, plus "Custom", which takes a colour code.
 * The chosen swatch has a rose-deep ring and a check; with an error every ring turns dashed red.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColourSwatches(
    hex: String?,
    onSelect: (String?) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    helper: String? = null,
    error: String? = null,
    presets: List<ColourPreset> = ColourPresets,
) {
    val normalized = hex?.let(FormEngine::normalizeHex)
    val preset = presets.firstOrNull { it.hex == normalized }
    val isCustom = normalized != null && preset == null
    var editingCustom by rememberSaveable { mutableStateOf(false) }
    var customText by rememberSaveable { mutableStateOf(if (isCustom) normalized.orEmpty() else "") }
    val requiredLabel = stringResource(Res.string.field_required)

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel(label, required)
        FlowRow(
            modifier = Modifier.selectableGroup().semantics { contentDescription = if (required) "$label, $requiredLabel" else label },
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            presets.forEach { option ->
                val selected = option.hex == normalized
                Swatch(
                    colour = parseHex(option.hex),
                    name = stringResource(option.name),
                    selected = selected,
                    hasError = error != null,
                    onClick = {
                        editingCustom = false
                        onSelect(if (selected && !required) null else option.hex)
                    },
                )
            }
            Swatch(
                colour = if (isCustom) parseHex(normalized!!) else null,
                name = stringResource(Res.string.colour_custom),
                selected = isCustom || editingCustom,
                hasError = error != null,
                onClick = { editingCustom = true },
            )
        }
        if (editingCustom || isCustom) {
            MomenticTextField(
                modifier = Modifier.padding(top = 4.dp),
                value = customText,
                onValueChange = { typed ->
                    customText = typed.take(7)
                    FormEngine.normalizeHex(typed)?.let(onSelect)
                },
                label = stringResource(Res.string.colour_hex),
                placeholder = "#e8a0b4",
                helper = stringResource(Res.string.colour_hex_hint),
                capitalization = KeyboardCapitalization.None,
            )
        }
        // The chosen preset's name, or "Pick one"; a custom colour already shows its code in the field above.
        FieldFooter(
            helper = helper ?: preset?.let { stringResource(it.name) } ?: if (normalized == null && !editingCustom) stringResource(Res.string.colour_pick_one) else null,
            error = error,
        )
    }
}

@Composable
private fun Swatch(colour: Color?, name: String, selected: Boolean, hasError: Boolean, onClick: () -> Unit) {
    val c = Momentic.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Column(
        modifier = Modifier
            .width(64.dp)
            .selectable(selected = selected, interactionSource = interaction, indication = null, role = Role.RadioButton, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val ring = Modifier.size(52.dp).let { base ->
            when {
                focused -> base.focusHalo(c.rose.copy(alpha = 0.16f), 26.dp).border(2.dp, c.rose, CircleShape)
                selected -> base.border(2.dp, c.roseDeep, CircleShape)
                hasError -> base.dashedBorder(2.dp, c.error, 26.dp)
                colour == null -> base.dashedBorder(1.5.dp, c.roseSoft, 26.dp)
                else -> base
            }
        }
        Box(ring, contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colour ?: c.surface)
                    .then(if (colour == null || colour.luminance() > 0.6f) Modifier.border(1.dp, c.ink.copy(alpha = 0.12f), CircleShape) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    selected && colour != null -> Icon(
                        MomenticIcons.Check,
                        contentDescription = null,
                        tint = if (colour.luminance() < 0.4f) c.white else c.ink,
                        modifier = Modifier.size(18.dp),
                    )
                    colour == null -> Icon(MomenticIcons.Plus, contentDescription = null, tint = c.roseDeep, modifier = Modifier.size(18.dp))
                }
            }
        }
        Text(
            text = name,
            style = if (selected) Momentic.type.captionStrong.copy(fontWeight = FontWeight.Bold) else Momentic.type.captionStrong,
            color = c.ink,
            maxLines = 1,
        )
    }
}

/** "#rrggbb" → a colour; assumes a normalised code. */
internal fun parseHex(hex: String): Color {
    val value = hex.removePrefix("#").toLong(16)
    return Color(0xFF000000 or value)
}
