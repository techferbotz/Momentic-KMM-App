package com.ferbotz.momentic.presentation.editor

import com.ferbotz.momentic.data.model.FieldDefinition
import com.ferbotz.momentic.data.model.FieldType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toInstant
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.put
import kotlin.math.abs

/** Where an already-uploaded photo can be seen, for drafts reopened in the editor. */
data class ImageRef(val thumbUrl: String?, val fullUrl: String?)

/**
 * The form engine: a template's field contract (`API.md` § The field contract) ⇄ editable values ⇄
 * the creation's `data`. Pure functions, no UI — so the rules are tested once and any UI can use them.
 *
 * Wire rules it follows:
 * - one value per field, under the field's `key`;
 * - an optional field left empty is **omitted**, never sent as `null`;
 * - photos are `{ "imageId": … }` — never a URL;
 * - `date` is `YYYY-MM-DD`, `dateTime` is ISO 8601 with an offset, `color` is `#rrggbb` lowercase;
 * - a `groupList` is an array of objects keyed by the child keys, one level deep.
 */
object FormEngine {

    /**
     * The starting values: from [data] when reopening a draft, otherwise from each field's
     * `defaultValue`. A `groupList` starts with `minItems` entries (at least one), so the shape of
     * what's asked is visible from the first look.
     *
     * @param images resolved URLs for photos already uploaded (keyed by image id), when known.
     */
    fun initialValues(
        fields: List<FieldDefinition>,
        data: JsonObject?,
        newId: () -> String,
        images: Map<String, ImageRef> = emptyMap(),
    ): Map<String, FieldValue> = fields.associate { field ->
        val source = if (data != null) data[field.key] else field.defaultValue
        field.key to read(field, source, isNew = data == null, newId = newId, images = images)
    }

    /** A fresh entry for a `groupList`, its children at their defaults. */
    fun newEntry(group: FieldDefinition, newId: () -> String): GroupEntry = GroupEntry(
        id = newId(),
        values = group.fields.associate { child ->
            child.key to read(child, child.defaultValue, isNew = true, newId = newId, images = emptyMap())
        },
    )

    /** The creation's `data` for these values. Empty optional fields are left out. */
    fun toWireData(fields: List<FieldDefinition>, values: Map<String, FieldValue>): JsonObject = buildJsonObject {
        for (field in fields) {
            toWire(field, values[field.key])?.let { put(field.key, it) }
        }
    }

    /**
     * Problems with the values, keyed by the input they belong to, in field order.
     *
     * @param requireComplete false for saving a draft: only what would make the data malformed (too
     *   long, out of range, too many). True before publishing: every required field and every minimum
     *   count as well.
     */
    fun validate(
        fields: List<FieldDefinition>,
        values: Map<String, FieldValue>,
        requireComplete: Boolean,
    ): Map<FieldPath, String> {
        val errors = linkedMapOf<FieldPath, String>()
        for (field in fields) {
            validate(field, values[field.key], FieldPath(field.key), requireComplete, errors)
        }
        return errors
    }

    /** Every uploaded photo, in form order — the first one becomes the creation's cover. */
    fun imageIds(fields: List<FieldDefinition>, values: Map<String, FieldValue>): List<String> =
        fields.flatMap { field -> imageIds(values[field.key]) }

    /** True when [value] holds nothing that would be written. */
    fun isEmpty(value: FieldValue?): Boolean = when (value) {
        null -> true
        is FieldValue.Text -> value.text.isBlank()
        is FieldValue.Number -> value.text.isBlank()
        is FieldValue.Toggle -> value.checked == null
        is FieldValue.Date -> value.date == null
        is FieldValue.DateTime -> value.dateTime == null
        is FieldValue.Color -> value.hex.isNullOrBlank()
        is FieldValue.Choice -> value.value == null
        is FieldValue.Location -> value.address.isBlank()
        is FieldValue.Photo -> value.photo == null
        is FieldValue.Photos -> value.photos.isEmpty()
        is FieldValue.Group -> value.entries.all { entry -> entry.values.values.all(::isEmpty) }
        is FieldValue.Unsupported -> value.raw == null || value.raw is JsonNull
    }

    // ---------------------------------------------------------------------------------------------
    // Reading
    // ---------------------------------------------------------------------------------------------

    private fun read(
        field: FieldDefinition,
        source: JsonElement?,
        isNew: Boolean,
        newId: () -> String,
        images: Map<String, ImageRef>,
    ): FieldValue = when (field.type) {
        FieldType.TEXT, FieldType.LONG_TEXT -> FieldValue.Text(source.asText().orEmpty())
        FieldType.NUMBER -> FieldValue.Number(source.asNumberText().orEmpty())
        FieldType.BOOLEAN -> FieldValue.Toggle((source as? JsonPrimitive)?.booleanOrNull)
        FieldType.DATE -> FieldValue.Date(source.asText()?.let { runCatching { LocalDate.parse(it.trim().take(10)) }.getOrNull() })
        FieldType.DATE_TIME -> readDateTime(source.asText())
        FieldType.COLOR -> FieldValue.Color(source.asText()?.let(::normalizeHex))
        FieldType.CHOICE -> FieldValue.Choice(
            source.asText()?.takeIf { value -> field.options.isEmpty() || field.options.any { it.value == value } },
        )
        FieldType.LOCATION -> (source as? JsonObject)?.let { location ->
            FieldValue.Location(
                address = location["address"].asText().orEmpty(),
                latitude = (location["latitude"] as? JsonPrimitive)?.doubleOrNull,
                longitude = (location["longitude"] as? JsonPrimitive)?.doubleOrNull,
            )
        } ?: FieldValue.Location()
        FieldType.IMAGE -> FieldValue.Photo(source.asPhoto(newId, images))
        FieldType.IMAGE_LIST -> FieldValue.Photos((source as? JsonArray).orEmpty().mapNotNull { it.asPhoto(newId, images) })
        FieldType.GROUP_LIST -> {
            val stored = (source as? JsonArray).orEmpty().mapNotNull { element ->
                val entry = element as? JsonObject ?: return@mapNotNull null
                GroupEntry(
                    id = newId(),
                    values = field.fields.associate { child ->
                        child.key to read(child, entry[child.key], isNew = false, newId = newId, images = images)
                    },
                )
            }
            val entries = if (isNew && stored.isEmpty()) {
                List(maxOf(field.minItems ?: 0, 1)) { newEntry(field, newId) }
            } else {
                stored
            }
            FieldValue.Group(entries)
        }
        FieldType.UNKNOWN -> FieldValue.Unsupported(if (isNew) null else source)
    }

    private fun readDateTime(text: String?): FieldValue.DateTime {
        if (text.isNullOrBlank()) return FieldValue.DateTime()
        return runCatching {
            val components = DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET.parse(text.trim())
            FieldValue.DateTime(components.toLocalDateTime(), components.toUtcOffset())
        }.getOrElse {
            FieldValue.DateTime(runCatching { LocalDateTime.parse(text.trim()) }.getOrNull())
        }
    }

    /** A stored photo: `{ "imageId": … }` in a draft's data, or `{ id, full, thumb }` in a render. */
    private fun JsonElement?.asPhoto(newId: () -> String, images: Map<String, ImageRef>): PhotoSlot? {
        val obj = this as? JsonObject ?: return null
        val imageId = (obj["imageId"] ?: obj["id"]).asText()?.takeIf { it.isNotBlank() } ?: return null
        val known = images[imageId]
        return PhotoSlot.Uploaded(
            slotId = newId(),
            imageId = imageId,
            thumbUrl = obj["thumb"].asText() ?: known?.thumbUrl,
            fullUrl = obj["full"].asText() ?: known?.fullUrl,
        )
    }

    private fun JsonElement?.asText(): String? = (this as? JsonPrimitive)?.takeIf { it !is JsonNull }?.contentOrNull

    private fun JsonElement?.asNumberText(): String? {
        val primitive = this as? JsonPrimitive ?: return null
        if (primitive is JsonNull) return null
        return primitive.doubleOrNull?.let(::formatNumber) ?: primitive.contentOrNull
    }

    // ---------------------------------------------------------------------------------------------
    // Writing
    // ---------------------------------------------------------------------------------------------

    private fun toWire(field: FieldDefinition, value: FieldValue?): JsonElement? = when (value) {
        null -> null
        is FieldValue.Text -> value.text.trim().takeIf { it.isNotEmpty() }?.let(::JsonPrimitive)
        is FieldValue.Number -> parseNumber(value.text)?.let { number ->
            if (number % 1.0 == 0.0 && abs(number) < 1e15) JsonPrimitive(number.toLong()) else JsonPrimitive(number)
        }
        is FieldValue.Toggle -> value.checked?.let(::JsonPrimitive)
        is FieldValue.Date -> value.date?.let { JsonPrimitive(it.toString()) }
        is FieldValue.DateTime -> formatDateTime(value)?.let(::JsonPrimitive)
        is FieldValue.Color -> value.hex?.let(::normalizeHex)?.let(::JsonPrimitive)
        is FieldValue.Choice -> value.value
            ?.takeIf { chosen -> field.options.isEmpty() || field.options.any { it.value == chosen } }
            ?.let(::JsonPrimitive)
        is FieldValue.Location -> value.address.trim().takeIf { it.isNotEmpty() }?.let { address ->
            buildJsonObject {
                put("address", address)
                value.latitude?.let { put("latitude", it) }
                value.longitude?.let { put("longitude", it) }
            }
        }
        is FieldValue.Photo -> (value.photo as? PhotoSlot.Uploaded)?.let { imageRef(it.imageId) }
        is FieldValue.Photos -> value.photos
            .filterIsInstance<PhotoSlot.Uploaded>()
            .map { imageRef(it.imageId) }
            .takeIf { it.isNotEmpty() }
            ?.let(::JsonArray)
        is FieldValue.Group -> value.entries
            .map { entry -> entryToWire(field, entry) }
            .filter { it.isNotEmpty() }
            .takeIf { it.isNotEmpty() }
            ?.let(::JsonArray)
        is FieldValue.Unsupported -> value.raw?.takeIf { it !is JsonNull }
    }

    private fun entryToWire(group: FieldDefinition, entry: GroupEntry): JsonObject = buildJsonObject {
        for (child in group.fields) {
            toWire(child, entry.values[child.key])?.let { put(child.key, it) }
        }
    }

    private fun imageRef(imageId: String): JsonObject = buildJsonObject { put("imageId", imageId) }

    /** ISO 8601 with an offset, e.g. `2026-12-06T18:30:00+05:30`. */
    fun formatDateTime(value: FieldValue.DateTime): String? {
        val dateTime = value.dateTime ?: return null
        val offset = value.offset ?: deviceOffsetAt(dateTime)
        return "${dateTime.date}T${pad(dateTime.hour)}:${pad(dateTime.minute)}:${pad(dateTime.second)}${formatOffset(offset)}"
    }

    private fun deviceOffsetAt(dateTime: LocalDateTime): UtcOffset {
        val zone = TimeZone.currentSystemDefault()
        return zone.offsetAt(dateTime.toInstant(zone))
    }

    /** `+05:30`; zero is written `+00:00` rather than `Z`, so every value has the same shape. */
    private fun formatOffset(offset: UtcOffset): String {
        val total = offset.totalSeconds
        val sign = if (total < 0) "-" else "+"
        val minutes = abs(total) / 60
        return "$sign${pad(minutes / 60)}:${pad(minutes % 60)}"
    }

    private fun pad(value: Int): String = value.toString().padStart(2, '0')

    /** `#RRGGBB`, `RRGGBB` → `#rrggbb`; anything else is not a colour. */
    fun normalizeHex(value: String): String? {
        val hex = value.trim().removePrefix("#")
        if (hex.length != 6 || hex.any { it !in '0'..'9' && it.lowercaseChar() !in 'a'..'f' }) return null
        return "#" + hex.lowercase()
    }

    /** Accepts a comma as the decimal separator, as many keyboards type it. */
    fun parseNumber(text: String): Double? =
        text.trim().replace(',', '.').takeIf { it.isNotEmpty() }?.toDoubleOrNull()?.takeIf { it.isFinite() }

    private fun formatNumber(number: Double): String =
        if (number % 1.0 == 0.0 && abs(number) < 1e15) number.toLong().toString() else number.toString()

    // ---------------------------------------------------------------------------------------------
    // Validation
    // ---------------------------------------------------------------------------------------------

    private fun validate(
        field: FieldDefinition,
        value: FieldValue?,
        path: FieldPath,
        requireComplete: Boolean,
        errors: MutableMap<FieldPath, String>,
    ) {
        when (field.type) {
            FieldType.IMAGE_LIST -> validatePhotos(field, value as? FieldValue.Photos, path, requireComplete, errors)
            FieldType.GROUP_LIST -> validateGroup(field, value as? FieldValue.Group, path, requireComplete, errors)
            else -> {
                if (isEmpty(value)) {
                    if (requireComplete && field.required) errors[path] = requiredMessage(field)
                    return
                }
                problemWith(field, value!!, requireComplete)?.let { errors[path] = it }
            }
        }
    }

    /** What's wrong with a non-empty single value, or null. */
    private fun problemWith(field: FieldDefinition, value: FieldValue, requireComplete: Boolean): String? = when (value) {
        is FieldValue.Text -> field.maxLength?.takeIf { value.text.trim().length > it }?.let { "Keep it to $it characters" }
        is FieldValue.Number -> {
            val number = parseNumber(value.text)
            when {
                number == null -> "Enter a number"
                (field.min != null && number < field.min) || (field.max != null && number > field.max) -> rangeMessage(field)
                else -> null
            }
        }
        is FieldValue.Color -> if (value.hex?.let(::normalizeHex) == null) "Use a colour like #e8a0b4" else null
        is FieldValue.Choice -> if (field.options.isNotEmpty() && field.options.none { it.value == value.value }) "Pick one" else null
        is FieldValue.Photo -> photoProblem(value.photo, requireComplete)
        else -> null
    }

    private fun photoProblem(slot: PhotoSlot?, requireComplete: Boolean): String? = when (slot) {
        is PhotoSlot.Failed -> if (requireComplete) "This photo didn't upload. Try again or remove it." else null
        is PhotoSlot.Uploading -> if (requireComplete) "Still uploading this photo…" else null
        else -> null
    }

    private fun validatePhotos(
        field: FieldDefinition,
        value: FieldValue.Photos?,
        path: FieldPath,
        requireComplete: Boolean,
        errors: MutableMap<FieldPath, String>,
    ) {
        val photos = value?.photos.orEmpty()
        val max = field.maxItems
        val min = field.minItems ?: if (field.required) 1 else 0
        when {
            max != null && photos.size > max -> errors[path] = "Up to $max ${if (max == 1) "photo" else "photos"}"
            requireComplete && photos.any { it is PhotoSlot.Uploading } -> errors[path] = "Still uploading…"
            requireComplete && photos.any { it is PhotoSlot.Failed } -> errors[path] = "A photo didn't upload. Try again or remove it."
            requireComplete && photos.count { it is PhotoSlot.Uploaded } < min -> errors[path] = countMessage(min, field.maxItems, "photo", "photos")
        }
    }

    private fun validateGroup(
        field: FieldDefinition,
        value: FieldValue.Group?,
        path: FieldPath,
        requireComplete: Boolean,
        errors: MutableMap<FieldPath, String>,
    ) {
        val entries = value?.entries.orEmpty()
        val filled = entries.count { entry -> entry.values.values.any { !isEmpty(it) } }
        val min = field.minItems ?: if (field.required) 1 else 0
        val max = field.maxItems
        // "Your moments" → "moments", so the message reads "Add at least 3 moments".
        val noun = field.label.lowercase().removePrefix("your ").ifBlank { "entries" }
        when {
            max != null && filled > max -> errors[path] = "Up to $max $noun"
            requireComplete && filled < min -> errors[path] = countMessage(min, max, noun, noun)
        }
        entries.forEachIndexed { index, entry ->
            val entryFilled = entry.values.values.any { !isEmpty(it) }
            // A blank entry beyond what's needed is simply dropped on save; only check the ones that
            // count — the filled ones, and (before publishing) the blanks still needed to reach the minimum.
            val checkRequired = requireComplete && (entryFilled || index < min)
            if (!entryFilled && !checkRequired) return@forEachIndexed
            for (child in field.fields) {
                validate(
                    child,
                    entry.values[child.key],
                    FieldPath(key = field.key, entryId = entry.id, childKey = child.key),
                    requireComplete = checkRequired,
                    errors = errors,
                )
            }
        }
    }

    private fun requiredMessage(field: FieldDefinition): String = when (field.type) {
        FieldType.DATE -> "Pick a date"
        FieldType.DATE_TIME -> "Pick a date and time"
        FieldType.CHOICE -> "Pick one"
        FieldType.LOCATION -> "Add a place"
        FieldType.COLOR -> "Pick a colour"
        FieldType.BOOLEAN -> "Choose one"
        FieldType.IMAGE -> "Add a photo"
        FieldType.UNKNOWN -> "Update Momentic to fill this in"
        else -> "Required"
    }

    private fun rangeMessage(field: FieldDefinition): String = when {
        field.min != null && field.max != null -> "Enter a number from ${formatNumber(field.min)} to ${formatNumber(field.max)}"
        field.min != null -> "Enter ${formatNumber(field.min)} or more"
        else -> "Enter ${formatNumber(field.max!!)} or less"
    }

    private fun countMessage(min: Int, max: Int?, one: String, many: String): String = when {
        max != null && min == max -> "Add $min ${if (min == 1) one else many}"
        else -> "Add at least $min ${if (min == 1) one else many}"
    }

    private fun imageIds(value: FieldValue?): List<String> = when (value) {
        is FieldValue.Photo -> listOfNotNull((value.photo as? PhotoSlot.Uploaded)?.imageId)
        is FieldValue.Photos -> value.photos.filterIsInstance<PhotoSlot.Uploaded>().map { it.imageId }
        is FieldValue.Group -> value.entries.flatMap { entry -> entry.values.values.flatMap(::imageIds) }
        else -> emptyList()
    }
}
