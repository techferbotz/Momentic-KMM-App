package com.ferbotz.momentic.presentation.editor

import com.ferbotz.momentic.data.model.FieldDefinition
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.UtcOffset
import kotlinx.serialization.json.JsonElement

/**
 * Addresses one input in the form: a top-level field ([key]), or a child field ([childKey]) of one
 * entry ([entryId]) of a `groupList` field ([key]).
 */
data class FieldPath(
    val key: String,
    val entryId: String? = null,
    val childKey: String? = null,
) {
    val isGroupChild: Boolean get() = entryId != null

    override fun toString(): String = if (entryId == null) key else "$key[$entryId].$childKey"
}

/**
 * What the user has entered for one field — one subtype per kind of input. The form engine turns
 * these into the creation's `data` (`FormEngine.toWireData`) following the field contract.
 */
sealed interface FieldValue {

    /** `text`, `longText`. */
    data class Text(val text: String = "") : FieldValue

    /** `number`, as typed — parsed when validated and written. */
    data class Number(val text: String = "") : FieldValue

    /** `boolean`; null until chosen (unless the field has a default). */
    data class Toggle(val checked: Boolean? = null) : FieldValue

    /** `date`. */
    data class Date(val date: LocalDate? = null) : FieldValue

    /**
     * `dateTime` — the local wall-clock time plus the offset it is written with. A null [offset] means
     * the device's offset at that time.
     */
    data class DateTime(val dateTime: LocalDateTime? = null, val offset: UtcOffset? = null) : FieldValue

    /** `color`, as `#rrggbb`. */
    data class Color(val hex: String? = null) : FieldValue

    /** `choice` — one of the field's option values. */
    data class Choice(val value: String? = null) : FieldValue

    /** `location`. */
    data class Location(
        val address: String = "",
        val latitude: Double? = null,
        val longitude: Double? = null,
    ) : FieldValue

    /** `image`. */
    data class Photo(val photo: PhotoSlot? = null) : FieldValue

    /** `imageList`, in order. */
    data class Photos(val photos: List<PhotoSlot> = emptyList()) : FieldValue

    /** `groupList`, in order. */
    data class Group(val entries: List<GroupEntry> = emptyList()) : FieldValue

    /** A field of a type this build can't edit; whatever is stored is carried through untouched. */
    data class Unsupported(val raw: JsonElement? = null) : FieldValue
}

/** One entry of a `groupList` (a memory, a moment): its child values keyed by child field key. */
data class GroupEntry(
    val id: String,
    val values: Map<String, FieldValue>,
)

/**
 * One photo in an `image` or `imageList` field. Photos upload as they're picked, so a slot moves
 * [Uploading] → [Uploaded] (or [Failed], which can be retried). Only [Uploaded] photos are written to
 * `data`, as `{ "imageId": … }`.
 */
sealed interface PhotoSlot {

    /** Stable for the life of the editor — what the UI keys the slot on. */
    val slotId: String

    data class Uploading(override val slotId: String) : PhotoSlot

    /**
     * @param thumbUrl / [fullUrl] for showing it; null when unknown (an older draft whose photos
     *   couldn't be resolved).
     */
    data class Uploaded(
        override val slotId: String,
        val imageId: String,
        val thumbUrl: String?,
        val fullUrl: String?,
    ) : PhotoSlot

    /** [message] is ready to show; [retryable] says whether "Try again" can work. */
    data class Failed(
        override val slotId: String,
        val message: String,
        val retryable: Boolean,
    ) : PhotoSlot
}

/** The value at [path]: a top-level field, or one child of one group entry. */
fun Map<String, FieldValue>.valueAt(path: FieldPath): FieldValue? =
    if (path.entryId == null) {
        this[path.key]
    } else {
        (this[path.key] as? FieldValue.Group)?.entries?.firstOrNull { it.id == path.entryId }?.values?.get(path.childKey)
    }

/** These values with [value] at [path]. A path into a group entry that no longer exists changes nothing. */
fun Map<String, FieldValue>.withValueAt(path: FieldPath, value: FieldValue): Map<String, FieldValue> {
    if (path.entryId == null) return this + (path.key to value)
    val group = this[path.key] as? FieldValue.Group ?: return this
    val childKey = path.childKey ?: return this
    val entries = group.entries.map { entry ->
        if (entry.id == path.entryId) entry.copy(values = entry.values + (childKey to value)) else entry
    }
    return this + (path.key to group.copy(entries = entries))
}

/** The field definition behind [path]: the top-level field, or the group's child field. */
fun List<FieldDefinition>.definitionAt(path: FieldPath): FieldDefinition? {
    val field = firstOrNull { it.key == path.key } ?: return null
    return if (path.childKey == null) field else field.fields.firstOrNull { it.key == path.childKey }
}
