package com.ferbotz.momentic.data.model

import kotlinx.serialization.json.JsonElement

/**
 * The input a field asks for (`API.md` § The field contract). The app renders one input per field
 * and writes the result into the creation's `data` under the field's key.
 */
enum class FieldType(val wire: String) {
    /** A string. */
    TEXT("text"),

    /** A multi-line string. */
    LONG_TEXT("longText"),

    /** A number, within `min`/`max`. */
    NUMBER("number"),

    /** A boolean. */
    BOOLEAN("boolean"),

    /** `"YYYY-MM-DD"`. */
    DATE("date"),

    /** ISO 8601 with an offset. */
    DATE_TIME("dateTime"),

    /** `"#rrggbb"`. */
    COLOR("color"),

    /** One of the field's `options[].value`. */
    CHOICE("choice"),

    /** `{ address, latitude?, longitude? }`. */
    LOCATION("location"),

    /** `{ "imageId": … }` — an id from `POST /images`, never a URL. */
    IMAGE("image"),

    /** An array of `{ "imageId": … }`. */
    IMAGE_LIST("imageList"),

    /** An array of objects keyed by the group's child fields — one level deep. */
    GROUP_LIST("groupList"),

    /** A type this build doesn't know yet (templates ship without an app release). */
    UNKNOWN("");

    companion object {
        fun fromWire(wire: String?): FieldType = entries.firstOrNull { it != UNKNOWN && it.wire == wire } ?: UNKNOWN
    }
}

/** One choice of a `choice` field: render [label], store [value]. */
data class ChoiceOption(
    val value: String,
    val label: String,
)

/** A cropper hint such as `4:5` (width : height). */
data class AspectRatio(val width: Int, val height: Int) {

    /** Width over height, for `Modifier.aspectRatio`. */
    val ratio: Float get() = width.toFloat() / height.toFloat()

    override fun toString(): String = "$width:$height"

    companion object {
        /** Parses `"4:5"`; anything else (or a zero side) is no hint at all. */
        fun parse(value: String?): AspectRatio? {
            val parts = value?.split(':') ?: return null
            if (parts.size != 2) return null
            val width = parts[0].trim().toIntOrNull() ?: return null
            val height = parts[1].trim().toIntOrNull() ?: return null
            return if (width > 0 && height > 0) AspectRatio(width, height) else null
        }
    }
}

/**
 * One field definition. Every property beyond [key], [type], [label] and [required] is optional and
 * only meaningful for the types noted.
 *
 * @param required whether `data` must carry it. An optional field left empty is **omitted**, never
 *   sent as `null`.
 * @param placeholder `text`, `longText`.
 * @param defaultValue prefill for any type, in the type's wire shape.
 * @param maxLength `text`, `longText` — enforced in the app too; the server also rejects.
 * @param min `number`.
 * @param max `number`.
 * @param minItems `imageList`, `groupList`.
 * @param maxItems `imageList`, `groupList`.
 * @param aspectRatio `image`, `imageList` — crop before upload.
 * @param options `choice`.
 * @param fields `groupList` — the child fields; a group never contains a group.
 */
data class FieldDefinition(
    val key: String,
    val type: FieldType,
    val label: String,
    val required: Boolean,
    val helpText: String? = null,
    val placeholder: String? = null,
    val defaultValue: JsonElement? = null,
    val maxLength: Int? = null,
    val min: Double? = null,
    val max: Double? = null,
    val minItems: Int? = null,
    val maxItems: Int? = null,
    val aspectRatio: AspectRatio? = null,
    val options: List<ChoiceOption> = emptyList(),
    val fields: List<FieldDefinition> = emptyList(),
)
