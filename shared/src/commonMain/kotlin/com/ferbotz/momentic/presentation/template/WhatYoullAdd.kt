package com.ferbotz.momentic.presentation.template

import com.ferbotz.momentic.data.model.FieldDefinition
import com.ferbotz.momentic.data.model.FieldType

/** What a line of "What you'll add" is about — it picks the line's icon. */
enum class AddKind { Person, Photos, Words, Note, When, Place, Choice }

/**
 * One line of "What you'll add" on the template screen: a field's [label], and for a list how many it
 * takes ([min]–[max], either may be open).
 */
data class AddItem(val kind: AddKind, val label: String, val min: Int? = null, val max: Int? = null)

/**
 * The template screen's summary of what the form will ask, one line per top-level field, in the
 * contract's order. Switches (`boolean`) are settings rather than content and are left out, as is a
 * type this build doesn't know.
 */
fun whatYoullAdd(fields: List<FieldDefinition>): List<AddItem> = fields.mapNotNull { field ->
    val kind = when (field.type) {
        FieldType.TEXT -> if (field.isAName()) AddKind.Person else AddKind.Words
        FieldType.NUMBER -> AddKind.Words
        FieldType.LONG_TEXT -> AddKind.Note
        FieldType.DATE, FieldType.DATE_TIME -> AddKind.When
        FieldType.LOCATION -> AddKind.Place
        FieldType.IMAGE, FieldType.IMAGE_LIST -> AddKind.Photos
        FieldType.GROUP_LIST -> if (field.fields.any { it.type == FieldType.IMAGE || it.type == FieldType.IMAGE_LIST }) AddKind.Photos else AddKind.Words
        FieldType.CHOICE, FieldType.COLOR -> AddKind.Choice
        FieldType.BOOLEAN, FieldType.UNKNOWN -> return@mapNotNull null
    }
    val counted = field.type == FieldType.IMAGE_LIST || field.type == FieldType.GROUP_LIST
    AddItem(
        kind = kind,
        label = field.label,
        min = field.minItems?.takeIf { counted && it > 0 },
        max = field.maxItems?.takeIf { counted && it > 0 },
    )
}

private fun FieldDefinition.isAName(): Boolean =
    key.endsWith("name", ignoreCase = true) || label.contains("name", ignoreCase = true)
