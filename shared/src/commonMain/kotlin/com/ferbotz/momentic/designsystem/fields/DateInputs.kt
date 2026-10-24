package com.ferbotz.momentic.designsystem.fields

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ferbotz.momentic.designsystem.components.PrimaryButton
import com.ferbotz.momentic.designsystem.components.QuietButton
import com.ferbotz.momentic.designsystem.icons.MomenticIcons
import com.ferbotz.momentic.platform.MomenticDialogWindow
import com.ferbotz.momentic.presentation.util.clockLabel
import com.ferbotz.momentic.presentation.util.compactLabel
import com.ferbotz.momentic.presentation.util.fullLabel
import com.ferbotz.momentic.resources.Res
import com.ferbotz.momentic.resources.date_choose
import com.ferbotz.momentic.resources.date_missing
import com.ferbotz.momentic.resources.date_part
import com.ferbotz.momentic.resources.field_cancel
import com.ferbotz.momentic.resources.field_clear
import com.ferbotz.momentic.resources.field_done
import com.ferbotz.momentic.resources.field_required
import com.ferbotz.momentic.resources.time_choose
import com.ferbotz.momentic.resources.time_missing
import com.ferbotz.momentic.resources.time_next
import com.ferbotz.momentic.resources.time_part
import com.ferbotz.momentic.theme.Momentic
import com.ferbotz.momentic.theme.MomenticElevation
import com.ferbotz.momentic.theme.MomenticShapes
import com.ferbotz.momentic.theme.MomenticSpacing
import com.ferbotz.momentic.theme.momenticShadow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant

/**
 * A date, shown the way guests will read it ("Sat, 12 December 2026"); tapping opens the date picker.
 * [onClear] adds "Clear" to the picker — for an optional date that's been set.
 */
@Composable
fun DateInput(
    value: LocalDate?,
    onValueChange: (LocalDate) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    helper: String? = null,
    error: String? = null,
    onClear: (() -> Unit)? = null,
) {
    var picking by rememberSaveable { mutableStateOf(false) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel(label, required)
        PickerButton(
            text = value?.fullLabel(),
            placeholder = stringResource(Res.string.date_choose),
            icon = MomenticIcons.Calendar,
            hasError = error != null,
            accessibilityLabel = accessibilityLabel(label, required),
            modifier = Modifier.fillMaxWidth(),
            onClick = { picking = true },
        )
        FieldFooter(helper = helper, error = error)
    }
    if (picking) {
        DatePickerWindow(
            title = label,
            initial = value,
            onDismiss = { picking = false },
            onConfirm = onValueChange,
            onClear = onClear?.takeIf { value != null },
        )
    }
}

/**
 * A date and a time under one label — two pickers side by side ("Sat, 12 Dec 2026" · "7:30 pm"). The
 * value is written only once both halves are chosen; until then the half that's set is held here.
 */
@Composable
fun DateTimeInput(
    value: LocalDateTime?,
    onValueChange: (LocalDateTime) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    helper: String? = null,
    error: String? = null,
) {
    var pendingDate by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingTime by rememberSaveable { mutableStateOf<String?>(null) }
    var picking by rememberSaveable { mutableStateOf<String?>(null) }
    val date = value?.date ?: pendingDate?.let(LocalDate::parse)
    val time = value?.time ?: pendingTime?.let(LocalTime::parse)

    fun commit(newDate: LocalDate?, newTime: LocalTime?) {
        if (newDate != null && newTime != null) {
            pendingDate = null
            pendingTime = null
            onValueChange(LocalDateTime(newDate, newTime))
        } else {
            pendingDate = newDate?.toString()
            pendingTime = newTime?.toString()
        }
    }

    val missingTime = date != null && time == null
    val missingDate = time != null && date == null
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel(label, required)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PickerButton(
                text = date?.compactLabel(),
                placeholder = stringResource(Res.string.date_part),
                icon = MomenticIcons.Calendar,
                hasError = error != null && (date == null || !missingTime),
                accessibilityLabel = "${accessibilityLabel(label, required)}, ${stringResource(Res.string.date_part)}",
                modifier = Modifier.weight(1.35f),
                compact = true,
                onClick = { picking = "date" },
            )
            PickerButton(
                text = time?.clockLabel(),
                placeholder = stringResource(Res.string.time_part),
                icon = MomenticIcons.Clock,
                hasError = error != null && (time == null || !missingDate),
                accessibilityLabel = "${accessibilityLabel(label, required)}, ${stringResource(Res.string.time_part)}",
                modifier = Modifier.weight(1f),
                compact = true,
                onClick = { picking = "time" },
            )
        }
        FieldFooter(
            helper = if (missingTime) stringResource(Res.string.time_next) else helper,
            error = when {
                error == null -> null
                missingTime -> stringResource(Res.string.time_missing)
                missingDate -> stringResource(Res.string.date_missing)
                else -> error
            },
        )
    }
    when (picking) {
        "date" -> DatePickerWindow(
            title = label,
            initial = date,
            onDismiss = { picking = null },
            onConfirm = { commit(it, time) },
            onClear = null,
        )
        "time" -> TimePickerWindow(
            title = label,
            initial = time,
            onDismiss = { picking = null },
            onConfirm = { commit(date, it) },
        )
    }
}

/** The tappable box of a date or time field: the value (or a quiet placeholder) and its icon. */
@Composable
private fun PickerButton(
    text: String?,
    placeholder: String,
    icon: ImageVector,
    hasError: Boolean,
    accessibilityLabel: String,
    modifier: Modifier,
    onClick: () -> Unit,
    compact: Boolean = false,
) {
    val c = Momentic.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Row(
        modifier = modifier
            .height(FieldHeight)
            .fieldContainer(fieldTone(focused, hasError), c)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "$accessibilityLabel, ${text ?: placeholder}" }
            .padding(horizontal = if (compact) 14.dp else 16.dp),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text ?: placeholder,
            style = Momentic.type.body,
            color = if (text == null) c.inkSoft else c.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Icon(icon, contentDescription = null, tint = c.inkSoft, modifier = Modifier.size(if (compact) 20.dp else 22.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerWindow(
    title: String,
    initial: LocalDate?,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
    onClear: (() -> Unit)?,
) {
    val c = Momentic.colors
    val state = rememberDatePickerState(initialSelectedDateMillis = initial?.atStartOfDayIn(TimeZone.UTC)?.toEpochMilliseconds())
    val chosen = state.selectedDateMillis?.let { Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date }
    PickerWindow(
        onDismiss = onDismiss,
        confirmEnabled = chosen != null,
        onConfirm = {
            chosen?.let(onConfirm)
            onDismiss()
        },
        onClear = onClear?.let { clear -> { clear(); onDismiss() } },
    ) {
        DatePicker(
            state = state,
            colors = DatePickerDefaults.colors(
                containerColor = c.surface,
                titleContentColor = c.inkSoft,
                headlineContentColor = c.ink,
                weekdayContentColor = c.inkSoft,
                subheadContentColor = c.inkSoft,
                navigationContentColor = c.ink,
                yearContentColor = c.ink,
                currentYearContentColor = c.rose,
                selectedYearContentColor = c.white,
                selectedYearContainerColor = c.roseDeep,
                dayContentColor = c.ink,
                selectedDayContentColor = c.white,
                selectedDayContainerColor = c.roseDeep,
                todayContentColor = c.roseDeep,
                todayDateBorderColor = c.rose,
                dividerColor = c.line,
            ),
            title = {
                Text(title, style = Momentic.type.label, color = c.inkSoft, modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp))
            },
            headline = {
                Text(
                    text = chosen?.fullLabel() ?: stringResource(Res.string.date_choose),
                    style = Momentic.type.h2,
                    color = c.ink,
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, bottom = 12.dp),
                )
            },
            showModeToggle = false,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerWindow(
    title: String,
    initial: LocalTime?,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
) {
    val c = Momentic.colors
    val state = rememberTimePickerState(initialHour = initial?.hour ?: 18, initialMinute = initial?.minute ?: 0, is24Hour = false)
    PickerWindow(
        onDismiss = onDismiss,
        confirmEnabled = true,
        onConfirm = {
            onConfirm(LocalTime(state.hour, state.minute))
            onDismiss()
        },
        onClear = null,
    ) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = Momentic.type.label, color = c.inkSoft)
            Text(stringResource(Res.string.time_choose), style = Momentic.type.h2, color = c.ink)
        }
        // The picker sets its clock in Material's display and title styles — Lora here; keep it in the sans.
        MaterialTheme(
            typography = MaterialTheme.typography.copy(
                displayLarge = Momentic.type.hero.copy(fontWeight = FontWeight.Medium),
                titleMedium = Momentic.type.label,
            ),
        ) {
            TimeDial(state)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDial(state: TimePickerState) {
    val c = Momentic.colors
    TimePicker(
        state = state,
        modifier = Modifier.fillMaxWidth().wrapContentWidth().padding(top = 20.dp),
        colors = TimePickerDefaults.colors(
            clockDialColor = c.blush100,
            clockDialSelectedContentColor = c.white,
            clockDialUnselectedContentColor = c.ink,
            selectorColor = c.roseDeep,
            containerColor = c.surface,
            periodSelectorBorderColor = c.line,
            periodSelectorSelectedContainerColor = c.blush200,
            periodSelectorUnselectedContainerColor = c.surface,
            periodSelectorSelectedContentColor = c.ink,
            periodSelectorUnselectedContentColor = c.inkSoft,
            timeSelectorSelectedContainerColor = c.blush200,
            timeSelectorUnselectedContainerColor = c.blush100,
            timeSelectorSelectedContentColor = c.ink,
            timeSelectorUnselectedContentColor = c.ink,
        ),
    )
}

/** A cream card in the app's dialog window, with the picker above "Cancel" and "Done". */
@Composable
private fun PickerWindow(
    onDismiss: () -> Unit,
    confirmEnabled: Boolean,
    onConfirm: () -> Unit,
    onClear: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    val c = Momentic.colors
    MomenticDialogWindow(onDismissRequest = onDismiss, scrim = c.scrim) {
        Column(
            modifier = Modifier
                .padding(horizontal = MomenticSpacing.xs)
                .widthIn(max = 380.dp)
                .momenticShadow(MomenticElevation.Sheet, MomenticShapes.card)
                .clip(MomenticShapes.card)
                .background(c.surface),
        ) {
            content()
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onClear != null) {
                    QuietButton(stringResource(Res.string.field_clear), onClick = onClear, compact = true, modifier = Modifier.weight(1f).wrapContentWidth(Alignment.Start))
                }
                QuietButton(stringResource(Res.string.field_cancel), onClick = onDismiss, compact = true)
                PrimaryButton(stringResource(Res.string.field_done), onClick = onConfirm, enabled = confirmEnabled, compact = true)
            }
        }
    }
}

@Composable
private fun accessibilityLabel(label: String, required: Boolean): String =
    if (required) "$label, ${stringResource(Res.string.field_required)}" else label
