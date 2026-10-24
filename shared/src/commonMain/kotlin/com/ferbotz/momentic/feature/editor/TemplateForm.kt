package com.ferbotz.momentic.feature.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.ferbotz.momentic.data.model.FieldDefinition
import com.ferbotz.momentic.data.model.FieldType
import com.ferbotz.momentic.designsystem.components.ButtonStyle
import com.ferbotz.momentic.designsystem.components.InlineBanner
import com.ferbotz.momentic.designsystem.components.MessageTone
import com.ferbotz.momentic.designsystem.components.MomenticBottomSheet
import com.ferbotz.momentic.designsystem.components.MomenticButton
import com.ferbotz.momentic.designsystem.components.MomenticIconButton
import com.ferbotz.momentic.designsystem.components.PrimaryButton
import com.ferbotz.momentic.designsystem.fields.ChoiceChips
import com.ferbotz.momentic.designsystem.fields.ColourSwatches
import com.ferbotz.momentic.designsystem.fields.DateInput
import com.ferbotz.momentic.designsystem.fields.DateTimeInput
import com.ferbotz.momentic.designsystem.fields.GroupEntryCard
import com.ferbotz.momentic.designsystem.fields.GroupListField
import com.ferbotz.momentic.designsystem.fields.InlinePhotoField
import com.ferbotz.momentic.designsystem.fields.MomenticTextField
import com.ferbotz.momentic.designsystem.fields.NumberStepper
import com.ferbotz.momentic.designsystem.fields.PhotoField
import com.ferbotz.momentic.designsystem.fields.PhotoListField
import com.ferbotz.momentic.designsystem.fields.ToggleRow
import com.ferbotz.momentic.designsystem.icons.MomenticIcons
import com.ferbotz.momentic.presentation.editor.EditorUiState
import com.ferbotz.momentic.presentation.editor.EntryNoun
import com.ferbotz.momentic.presentation.editor.FieldPath
import com.ferbotz.momentic.presentation.editor.FieldValue
import com.ferbotz.momentic.presentation.editor.FormEngine
import com.ferbotz.momentic.presentation.editor.GroupEntry
import com.ferbotz.momentic.resources.Res
import com.ferbotz.momentic.resources.cd_close
import com.ferbotz.momentic.resources.field_example
import com.ferbotz.momentic.resources.field_helper_optional
import com.ferbotz.momentic.resources.field_optional
import com.ferbotz.momentic.resources.field_unsupported
import com.ferbotz.momentic.resources.group_add
import com.ferbotz.momentic.resources.group_edit
import com.ferbotz.momentic.resources.group_edit_named
import com.ferbotz.momentic.resources.group_empty
import com.ferbotz.momentic.resources.group_hold_to_reorder
import com.ferbotz.momentic.resources.group_needs_attention
import com.ferbotz.momentic.resources.group_new
import com.ferbotz.momentic.resources.group_position
import com.ferbotz.momentic.resources.group_remove
import com.ferbotz.momentic.resources.group_reorder
import com.ferbotz.momentic.resources.group_save
import com.ferbotz.momentic.resources.group_tap_to_fill
import com.ferbotz.momentic.resources.group_untitled
import com.ferbotz.momentic.resources.photos_count
import com.ferbotz.momentic.resources.place_placeholder
import com.ferbotz.momentic.theme.Momentic
import com.ferbotz.momentic.theme.MomenticSpacing
import org.jetbrains.compose.resources.stringResource

/**
 * What the form can ask for. The editor screen hands over its view model's intents; the design gallery a
 * stand-in. [pickPhotos] asks the screen to run the photo picker (and the crop) for [path] — [room] is how
 * many more fit, 1 for a single photo, null for no limit — and then call `addPhotos` with the result.
 */
interface FormActions {
    fun update(path: FieldPath, value: FieldValue)
    fun pickPhotos(path: FieldPath, room: Int?)
    fun retryPhoto(path: FieldPath, slotId: String)
    fun removePhoto(path: FieldPath, slotId: String)
    fun movePhoto(path: FieldPath, from: Int, to: Int)
    fun addEntry(groupKey: String)
    fun removeEntry(groupKey: String, entryId: String)
    fun moveEntry(groupKey: String, from: Int, to: Int)
}

/**
 * The form generated from a template's fields — one input per field, in the template's order, stacked
 * 24 dp apart. A repeating group's entries open in a sheet. When a save or "Continue" turns up new
 * problems, the first field with one is scrolled into view.
 */
@Composable
fun TemplateForm(state: EditorUiState, actions: FormActions, modifier: Modifier = Modifier) {
    val fields = state.fields
    val requesters = remember(fields) { fields.associate { it.key to BringIntoViewRequester() } }
    var seenErrors by remember { mutableStateOf(emptySet<FieldPath>()) }
    LaunchedEffect(state.errors.keys) {
        val fresh = state.errors.keys - seenErrors
        seenErrors = state.errors.keys
        if (fresh.isEmpty()) return@LaunchedEffect
        val first = fields.firstOrNull { field -> state.errors.keys.any { it.key == field.key } } ?: return@LaunchedEffect
        requesters[first.key]?.bringIntoView()
    }
    // The open group entry, as "groupKey/entryId", and whether it was just added (a blank one is let go on close).
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    var editingIsNew by rememberSaveable { mutableStateOf(false) }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(MomenticSpacing.lg)) {
        fields.forEach { field ->
            key(field.key) {
                Box(Modifier.bringIntoViewRequester(requesters.getValue(field.key))) {
                    if (field.type == FieldType.GROUP_LIST) {
                        GroupInput(
                            group = field,
                            state = state,
                            actions = actions,
                            onOpen = { entryId, isNew ->
                                editing = "${field.key}/$entryId"
                                editingIsNew = isNew
                            },
                        )
                    } else {
                        FieldInput(field, FieldPath(field.key), state, actions)
                    }
                }
            }
        }
    }

    val open = editing?.split('/', limit = 2)
    if (open != null) {
        val group = fields.firstOrNull { it.key == open[0] }
        val entries = (state.values[open[0]] as? FieldValue.Group)?.entries.orEmpty()
        val index = entries.indexOfFirst { it.id == open[1] }
        if (group == null || index < 0) {
            editing = null
        } else {
            EntrySheet(
                group = group,
                entry = entries[index],
                index = index,
                total = entries.size,
                isNew = editingIsNew,
                templateName = state.template?.name.orEmpty(),
                state = state,
                actions = actions,
                onClose = { removeIfBlank ->
                    val entry = entries.getOrNull(index)
                    if (removeIfBlank && entry != null && entry.values.values.all(FormEngine::isEmpty)) {
                        actions.removeEntry(group.key, entry.id)
                    }
                    editing = null
                },
            )
        }
    }
}

/** One input for one field (or one child of a group entry), by the field's type. */
@Composable
private fun FieldInput(
    field: FieldDefinition,
    path: FieldPath,
    state: EditorUiState,
    actions: FormActions,
    inSheet: Boolean = false,
) {
    val value = state.valueOf(path)
    val error = state.errorFor(path)
    val helper = helperFor(field)
    when (field.type) {
        FieldType.TEXT, FieldType.LONG_TEXT -> MomenticTextField(
            value = (value as? FieldValue.Text)?.text.orEmpty(),
            onValueChange = { actions.update(path, FieldValue.Text(it)) },
            label = field.label,
            required = field.required,
            helper = helper,
            error = error,
            placeholder = placeholderFor(field, path, state),
            maxLength = field.maxLength,
            multiline = field.type == FieldType.LONG_TEXT,
        )
        FieldType.NUMBER -> NumberStepper(
            value = (value as? FieldValue.Number)?.text.orEmpty(),
            onValueChange = { actions.update(path, FieldValue.Number(it)) },
            label = field.label,
            required = field.required,
            helper = field.helpText?.let { optionalised(it, field.required) },
            error = error,
            min = field.min,
            max = field.max,
        )
        FieldType.BOOLEAN -> ToggleRow(
            checked = (value as? FieldValue.Toggle)?.checked == true,
            onCheckedChange = { actions.update(path, FieldValue.Toggle(it)) },
            label = field.label,
            caption = field.helpText,
            required = field.required,
            error = error,
        )
        FieldType.DATE -> DateInput(
            value = (value as? FieldValue.Date)?.date,
            onValueChange = { actions.update(path, FieldValue.Date(it)) },
            label = field.label,
            required = field.required,
            helper = helper,
            error = error,
            onClear = if (field.required) null else ({ actions.update(path, FieldValue.Date(null)) }),
        )
        FieldType.DATE_TIME -> {
            val current = value as? FieldValue.DateTime
            DateTimeInput(
                value = current?.dateTime,
                onValueChange = { actions.update(path, FieldValue.DateTime(it, current?.offset)) },
                label = field.label,
                required = field.required,
                helper = helper,
                error = error,
            )
        }
        FieldType.COLOR -> ColourSwatches(
            hex = (value as? FieldValue.Color)?.hex,
            onSelect = { actions.update(path, FieldValue.Color(it)) },
            label = field.label,
            required = field.required,
            helper = field.helpText?.let { optionalised(it, field.required) },
            error = error,
        )
        FieldType.CHOICE -> ChoiceChips(
            options = field.options,
            selected = (value as? FieldValue.Choice)?.value,
            onSelect = { actions.update(path, FieldValue.Choice(it)) },
            label = field.label,
            required = field.required,
            helper = helper,
            error = error,
        )
        FieldType.LOCATION -> MomenticTextField(
            value = (value as? FieldValue.Location)?.address.orEmpty(),
            // A new address no longer matches any coordinates that came with the old one.
            onValueChange = { actions.update(path, FieldValue.Location(address = it)) },
            label = field.label,
            required = field.required,
            helper = helper,
            error = error,
            placeholder = placeholderFor(field, path, state) ?: stringResource(Res.string.place_placeholder),
            leadingIcon = MomenticIcons.MapPin,
            capitalization = KeyboardCapitalization.Words,
        )
        FieldType.IMAGE -> {
            val slot = (value as? FieldValue.Photo)?.photo
            if (inSheet) {
                InlinePhotoField(
                    slot = slot,
                    label = field.label,
                    aspectRatio = field.aspectRatio,
                    onPick = { actions.pickPhotos(path, 1) },
                    onRetry = { actions.retryPhoto(path, it) },
                    onRemove = { actions.removePhoto(path, it) },
                    required = field.required,
                    error = error,
                )
            } else {
                PhotoField(
                    slot = slot,
                    label = field.label,
                    aspectRatio = field.aspectRatio,
                    onPick = { actions.pickPhotos(path, 1) },
                    onRetry = { actions.retryPhoto(path, it) },
                    onRemove = { actions.removePhoto(path, it) },
                    required = field.required,
                    helper = field.helpText?.let { optionalised(it, field.required) },
                    error = error,
                )
            }
        }
        FieldType.IMAGE_LIST -> PhotoListField(
            photos = (value as? FieldValue.Photos)?.photos.orEmpty(),
            label = field.label,
            aspectRatio = field.aspectRatio,
            maxItems = field.maxItems,
            onPick = { room -> actions.pickPhotos(path, room) },
            onRetry = { actions.retryPhoto(path, it) },
            onRemove = { actions.removePhoto(path, it) },
            onMove = { from, to -> actions.movePhoto(path, from, to) },
            required = field.required,
            helper = field.helpText?.let { optionalised(it, field.required) },
            error = error,
        )
        // A group inside a group isn't allowed by the contract; one at the top level has its own input.
        FieldType.GROUP_LIST -> Unit
        FieldType.UNKNOWN -> InlineBanner(stringResource(Res.string.field_unsupported, field.label), MessageTone.Neutral)
    }
}

/** A repeating group: its entries as cards, "Add a moment", and the sheet each entry opens in. */
@Composable
private fun GroupInput(
    group: FieldDefinition,
    state: EditorUiState,
    actions: FormActions,
    onOpen: (entryId: String, isNew: Boolean) -> Unit,
) {
    val entries = (state.values[group.key] as? FieldValue.Group)?.entries.orEmpty()
    val noun = remember(group.label) { EntryNoun.forLabel(group.label) }
    val max = group.maxItems
    val titleChild = group.fields.firstOrNull { it.type == FieldType.TEXT } ?: group.fields.firstOrNull { it.type == FieldType.LONG_TEXT }
    val captionChild = group.fields.firstOrNull { it != titleChild && (it.type == FieldType.TEXT || it.type == FieldType.LONG_TEXT) }
    val photoChild = group.fields.firstOrNull { it.type == FieldType.IMAGE }

    // After "Add a moment", open the entry it made once it shows up.
    var awaitingNew by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(entries.size) {
        val before = awaitingNew ?: return@LaunchedEffect
        if (entries.size > before) {
            awaitingNew = null
            onOpen(entries.last().id, true)
        }
    }

    val hold = if (entries.size > 1) stringResource(Res.string.group_hold_to_reorder) else null
    val helper = listOfNotNull(group.helpText?.trimEnd('.', ' ')?.let { "$it." }, hold).joinToString(" ").ifEmpty { null }
    GroupListField(
        entries = entries,
        entryKey = { it.id },
        label = group.label,
        onMove = { from, to -> actions.moveEntry(group.key, from, to) },
        addLabel = stringResource(Res.string.group_add, noun.withArticle),
        onAdd = if (max == null || entries.size < max) {
            {
                awaitingNew = entries.size
                actions.addEntry(group.key)
            }
        } else {
            null
        },
        emptyText = stringResource(Res.string.group_empty, noun.plural),
        required = group.required,
        count = max?.let { stringResource(Res.string.photos_count, entries.size, it) },
        helper = helper,
        error = state.errorFor(FieldPath(group.key)),
    ) { entry, index, handle, isDragging ->
        val blank = entry.values.values.all(FormEngine::isEmpty)
        val title = (titleChild?.let { entry.values[it.key] } as? FieldValue.Text)?.text?.takeIf { it.isNotBlank() }
        val untitled = stringResource(Res.string.group_untitled, noun.singular.replaceFirstChar { it.uppercase() }, index + 1)
        val hasError = state.errors.keys.any { it.key == group.key && it.entryId == entry.id }
        val photoPath = photoChild?.let { FieldPath(group.key, entry.id, it.key) }
        val photo = photoChild?.let { (entry.values[it.key] as? FieldValue.Photo)?.photo }
        GroupEntryCard(
            title = title ?: untitled,
            titleIsPlaceholder = title == null,
            subtitle = when {
                hasError -> stringResource(Res.string.group_needs_attention)
                blank -> stringResource(Res.string.group_tap_to_fill)
                else -> (captionChild?.let { entry.values[it.key] } as? FieldValue.Text)?.text?.takeIf { it.isNotBlank() }
            },
            onOpen = { onOpen(entry.id, false) },
            handle = handle,
            reorderLabel = stringResource(Res.string.group_reorder, title ?: untitled),
            editLabel = stringResource(Res.string.group_edit_named, title ?: untitled),
            photo = photo,
            showPhoto = photoChild != null,
            hasError = hasError,
            isDragging = isDragging,
            onRetryPhoto = if (photo != null && photoPath != null) ({ actions.retryPhoto(photoPath, photo.slotId) }) else null,
        )
    }
}

/** One group entry in a sheet: its own fields, "Save" and "Remove this moment". */
@Composable
private fun EntrySheet(
    group: FieldDefinition,
    entry: GroupEntry,
    index: Int,
    total: Int,
    isNew: Boolean,
    templateName: String,
    state: EditorUiState,
    actions: FormActions,
    onClose: (removeIfBlank: Boolean) -> Unit,
) {
    val c = Momentic.colors
    val noun = remember(group.label) { EntryNoun.forLabel(group.label) }
    MomenticBottomSheet(onDismissRequest = { onClose(isNew) }) {
        val close = dismiss
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(if (isNew) Res.string.group_new else Res.string.group_edit, noun.singular),
                    style = Momentic.type.h1,
                    color = c.ink,
                )
                Text(
                    text = stringResource(Res.string.group_position, index + 1, total, templateName),
                    style = Momentic.type.caption,
                    color = c.inkSoft,
                )
            }
            MomenticIconButton(MomenticIcons.Close, stringResource(Res.string.cd_close), onClick = close)
        }
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(top = MomenticSpacing.md),
            verticalArrangement = Arrangement.spacedBy(MomenticSpacing.md),
        ) {
            group.fields.forEach { child ->
                key(child.key) {
                    FieldInput(child, FieldPath(group.key, entry.id, child.key), state, actions, inSheet = true)
                }
            }
            PrimaryButton(stringResource(Res.string.group_save), onClick = close, modifier = Modifier.fillMaxWidth().padding(top = MomenticSpacing.xs))
            MomenticButton(
                text = stringResource(Res.string.group_remove, noun.singular),
                onClick = {
                    actions.removeEntry(group.key, entry.id)
                    onClose(false)
                },
                style = ButtonStyle.QuietDestructive,
                icon = MomenticIcons.Trash,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** The helper under a field: its help text, with "optional" when it may be left empty. */
@Composable
private fun helperFor(field: FieldDefinition): String? = when {
    field.helpText != null -> optionalised(field.helpText, field.required)
    !field.required -> stringResource(Res.string.field_optional)
    else -> null
}

@Composable
private fun optionalised(text: String, required: Boolean): String =
    if (required) text else stringResource(Res.string.field_helper_optional, text.trimEnd('.'))

/** The field's own placeholder, or the shopfront's example ("e.g. Divya"). */
@Composable
private fun placeholderFor(field: FieldDefinition, path: FieldPath, state: EditorUiState): String? =
    field.placeholder ?: state.exampleFor(path)?.let { stringResource(Res.string.field_example, it) }
