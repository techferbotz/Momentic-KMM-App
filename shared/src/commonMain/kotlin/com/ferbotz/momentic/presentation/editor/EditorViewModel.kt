package com.ferbotz.momentic.presentation.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ferbotz.momentic.data.media.ImageProcessor
import com.ferbotz.momentic.data.media.PickedPhoto
import com.ferbotz.momentic.data.model.AspectRatio
import com.ferbotz.momentic.data.model.FieldDefinition
import com.ferbotz.momentic.data.model.FieldType
import com.ferbotz.momentic.data.model.TemplateDetail
import com.ferbotz.momentic.data.repository.CatalogueRepository
import com.ferbotz.momentic.data.repository.CreationRepository
import com.ferbotz.momentic.data.repository.ImageRepository
import com.ferbotz.momentic.data.repository.RemoteConfigRepository
import com.ferbotz.momentic.data.repository.RenderRepository
import com.ferbotz.momentic.data.result.AppError
import com.ferbotz.momentic.data.result.AppResult
import com.ferbotz.momentic.data.result.ErrorCode
import com.ferbotz.momentic.presentation.util.EventChannel
import com.ferbotz.momentic.presentation.util.toUserMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * What the editor renders.
 *
 * @param template the field contract the form is built from (`template.fields`), never hard-coded.
 * @param creationId null until the first save creates the draft.
 * @param values what has been entered, by field key — read with [valueOf].
 * @param errors problems by input; set when a save or "Continue" is refused, cleared as inputs change.
 * @param examples sample values from the template's shopfront ("e.g. Divya"), by [FieldPath.exampleKey].
 * @param hasUnsavedChanges something changed since the last save (ask before leaving).
 * @param message a one-off note for a snackbar; clear it with `messageShown()`.
 */
data class EditorUiState(
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val template: TemplateDetail? = null,
    val creationId: String? = null,
    val values: Map<String, FieldValue> = emptyMap(),
    val errors: Map<FieldPath, String> = emptyMap(),
    val examples: Map<String, String> = emptyMap(),
    val isSaving: Boolean = false,
    val isOpeningPreview: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val message: String? = null,
) {
    val fields: List<FieldDefinition> get() = template?.fields.orEmpty()

    /** Photos still on their way up; preview and "Continue" wait for them. */
    val uploadsInFlight: Int get() = values.values.sumOf(::uploadingIn)

    fun valueOf(path: FieldPath): FieldValue? = values.valueAt(path)

    fun errorFor(path: FieldPath): String? = errors[path]

    fun exampleFor(path: FieldPath): String? = examples[path.exampleKey]
}

/** The key [EditorUiState.examples] uses: `key`, or `groupKey.childKey` for a group's child. */
val FieldPath.exampleKey: String get() = if (childKey == null) key else "$key.$childKey"

private fun uploadingIn(value: FieldValue): Int = when (value) {
    is FieldValue.Photo -> if (value.photo is PhotoSlot.Uploading) 1 else 0
    is FieldValue.Photos -> value.photos.count { it is PhotoSlot.Uploading }
    is FieldValue.Group -> value.entries.sumOf { entry -> entry.values.values.sumOf(::uploadingIn) }
    else -> 0
}

/** One-off things the editor asks its screen to do. */
sealed interface EditorEvent {
    /** Open the creator's own draft on the web (a fresh `d_…` link, valid about 30 minutes). */
    data class OpenPreview(val url: String) : EditorEvent

    /** The draft is complete and saved: go to the paywall for it. */
    data class ContinueToPublish(val creationId: String) : EditorEvent

    /** The draft was deleted (or a never-saved one discarded): close the editor. */
    data object Closed : EditorEvent
}

/**
 * Backs the editor — the input form generated from a template's field contract (`API.md`), for a new
 * creation or a draft being continued.
 *
 * - **Photos upload as they're picked**, one at a time (uploads are limited to 20 an hour per IP),
 *   after being made upright, cropped to the field's aspect ratio and shrunk on the device.
 * - **Save** keeps a partial draft (only malformed values are refused). **Continue** requires every
 *   required field first, then saves and hands over to publishing. **Preview** saves and opens the
 *   draft on the web.
 * - A photo removed from the form is freed on the server (it counts towards the 60-photo limit): at
 *   once if no save ever used it, otherwise after the next save stops using it.
 */
class EditorViewModel(
    private val templateId: String,
    creationId: String?,
    private val catalogue: CatalogueRepository,
    private val creations: CreationRepository,
    private val images: ImageRepository,
    private val render: RenderRepository,
    private val processor: ImageProcessor,
    private val remoteConfig: RemoteConfigRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState(creationId = creationId))
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val _events = EventChannel<EditorEvent>()
    val events: Flow<EditorEvent> = _events.events

    private var lastId = 0
    private fun newId(): String = "f${++lastId}"

    /** The picked photo behind each slot still uploading or failed, so "Try again" can redo it — framed the same way. */
    private val sources = mutableMapOf<String, PickedPhoto>()

    /** One photo uploads at a time — gentle on the hourly allowance and on a phone's connection. */
    private val uploadQueue = Mutex()
    private val saveLock = Mutex()

    /** Photos the last save wrote into the draft. */
    private var savedImageIds: Set<String> = emptySet()

    /** Photos removed from the form that the last save still used; freed after the next save. */
    private val releasedAfterSave = mutableSetOf<String>()

    init {
        load()
    }

    // ---------------------------------------------------------------------------------------------
    // Loading
    // ---------------------------------------------------------------------------------------------

    fun retryLoad() = load()

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadError = null) }
            val template = when (val result = catalogue.getTemplate(templateId)) {
                is AppResult.Success -> result.data
                is AppResult.Failure -> return@launch fail(result.error)
            }
            val creationId = _uiState.value.creationId
            var data: JsonObject? = null
            var known: Map<String, ImageRef> = emptyMap()
            if (creationId != null) {
                data = when (val result = creations.get(creationId)) {
                    is AppResult.Success -> result.data.data
                    is AppResult.Failure -> return@launch fail(result.error)
                }
                // A draft stores photos as bare ids; its own render resolves them to thumbnails.
                if (data.containsImages()) known = resolveImages(creationId)
            }
            val values = FormEngine.initialValues(template.fields, data, ::newId, known)
            savedImageIds = if (data != null) FormEngine.imageIds(template.fields, values).toSet() else emptySet()
            _uiState.update { it.copy(isLoading = false, template = template, values = values) }
            loadExamples(template)
        }
    }

    private fun fail(error: AppError) {
        _uiState.update { it.copy(isLoading = false, loadError = error.toUserMessage()) }
    }

    private suspend fun resolveImages(creationId: String): Map<String, ImageRef> {
        val link = creations.previewLink(creationId).getOrNull() ?: return emptyMap()
        val page = render.render(link.code).getOrNull() ?: return emptyMap()
        return buildMap { page.values?.let { collectImages(it, this) } }
    }

    private fun collectImages(element: JsonElement, into: MutableMap<String, ImageRef>) {
        when (element) {
            is JsonObject -> {
                val id = element["id"].text()
                if (id != null && (element["thumb"] != null || element["full"] != null)) {
                    into[id] = ImageRef(thumbUrl = element["thumb"].text(), fullUrl = element["full"].text())
                } else {
                    element.values.forEach { collectImages(it, into) }
                }
            }
            is JsonArray -> element.forEach { collectImages(it, into) }
            else -> Unit
        }
    }

    /** "e.g." hints from the shopfront's sample values — the same example the web shows (BE-002). */
    private fun loadExamples(template: TemplateDetail) {
        viewModelScope.launch {
            val sample = render.examplePage(template.previewCode).getOrNull()?.values ?: return@launch
            val examples = buildMap {
                for (field in template.fields) {
                    when (field.type) {
                        FieldType.GROUP_LIST -> {
                            val first = (sample[field.key] as? JsonArray)?.firstOrNull() as? JsonObject ?: continue
                            for (child in field.fields) exampleText(child, first[child.key])?.let { put("${field.key}.${child.key}", it) }
                        }
                        else -> exampleText(field, sample[field.key])?.let { put(field.key, it) }
                    }
                }
            }
            _uiState.update { it.copy(examples = examples) }
        }
    }

    private fun exampleText(field: FieldDefinition, value: JsonElement?): String? = when (field.type) {
        FieldType.TEXT, FieldType.LONG_TEXT, FieldType.NUMBER -> value.text()
        FieldType.LOCATION -> (value as? JsonObject)?.get("address").text()
        else -> null
    }

    // ---------------------------------------------------------------------------------------------
    // Editing
    // ---------------------------------------------------------------------------------------------

    /**
     * Sets a single value — text, number, toggle, date, date-time, colour, choice, location. Photos
     * and group entries have their own intents below.
     */
    fun update(path: FieldPath, value: FieldValue) {
        _uiState.update { state ->
            state.copy(
                values = state.values.withValueAt(path, value),
                errors = state.errors - path,
                hasUnsavedChanges = true,
            )
        }
    }

    /**
     * Adds picked photos to an `image` or `imageList` field (or a group child of one). Each is
     * prepared and uploaded in turn; extra photos beyond the field's `maxItems` are left out. A
     * single-photo field keeps only the first and replaces what was there. Each is centre-cropped to
     * the field's aspect ratio; [addFramedPhotos] takes the crop screen's framing instead.
     */
    fun addPhotos(path: FieldPath, picked: List<ByteArray>) = addFramedPhotos(path, picked.map { PickedPhoto(it) })

    /** [addPhotos], with each photo turned and cropped the way the person framed it on the crop screen. */
    fun addFramedPhotos(path: FieldPath, picked: List<PickedPhoto>) {
        val state = _uiState.value
        val field = state.fields.definitionAt(path) ?: return
        if (picked.isEmpty()) return
        when (field.type) {
            FieldType.IMAGE -> {
                (state.valueOf(path) as? FieldValue.Photo)?.photo?.let(::releaseSlot)
                val slot = PhotoSlot.Uploading(newId())
                sources[slot.slotId] = picked.first()
                setValue(path, FieldValue.Photo(slot))
                startUpload(path, slot.slotId, field.aspectRatio)
            }
            FieldType.IMAGE_LIST -> {
                val current = (state.valueOf(path) as? FieldValue.Photos)?.photos.orEmpty()
                val room = (field.maxItems ?: Int.MAX_VALUE) - current.size
                if (room <= 0) return note("You can add up to ${field.maxItems} photos here.")
                val accepted = picked.take(room)
                if (accepted.size < picked.size) note("Only ${field.maxItems} photos fit here, so ${picked.size - accepted.size} were left out.")
                val slots = accepted.map { photo -> PhotoSlot.Uploading(newId()).also { sources[it.slotId] = photo } }
                setValue(path, FieldValue.Photos(current + slots))
                slots.forEach { startUpload(path, it.slotId, field.aspectRatio) }
            }
            else -> Unit
        }
    }

    /** "Try again" on a photo that failed to upload. */
    fun retryPhoto(path: FieldPath, slotId: String) {
        val field = _uiState.value.fields.definitionAt(path) ?: return
        if (sources[slotId] == null) return
        replaceSlot(path, slotId) { PhotoSlot.Uploading(slotId) }
        startUpload(path, slotId, field.aspectRatio)
    }

    fun removePhoto(path: FieldPath, slotId: String) {
        val value = _uiState.value.valueOf(path) ?: return
        val slot = when (value) {
            is FieldValue.Photo -> value.photo?.takeIf { it.slotId == slotId }
            is FieldValue.Photos -> value.photos.firstOrNull { it.slotId == slotId }
            else -> null
        } ?: return
        releaseSlot(slot)
        replaceSlot(path, slotId) { null }
    }

    /** Reorders an `imageList` — the order photos appear on the page. */
    fun movePhoto(path: FieldPath, fromIndex: Int, toIndex: Int) {
        val photos = (_uiState.value.valueOf(path) as? FieldValue.Photos)?.photos ?: return
        setValue(path, FieldValue.Photos(photos.moved(fromIndex, toIndex)))
    }

    /** Adds an empty entry to a `groupList` (another memory, another moment). */
    fun addEntry(groupKey: String) {
        val state = _uiState.value
        val group = state.fields.firstOrNull { it.key == groupKey && it.type == FieldType.GROUP_LIST } ?: return
        val entries = (state.values[groupKey] as? FieldValue.Group)?.entries.orEmpty()
        val max = group.maxItems
        if (max != null && entries.size >= max) return note("You can add up to $max here.")
        setValue(FieldPath(groupKey), FieldValue.Group(entries + FormEngine.newEntry(group, ::newId)))
    }

    fun removeEntry(groupKey: String, entryId: String) {
        val entries = (_uiState.value.values[groupKey] as? FieldValue.Group)?.entries ?: return
        val removed = entries.firstOrNull { it.id == entryId } ?: return
        removed.values.values.forEach { value ->
            when (value) {
                is FieldValue.Photo -> value.photo?.let(::releaseSlot)
                is FieldValue.Photos -> value.photos.forEach(::releaseSlot)
                else -> Unit
            }
        }
        setValue(FieldPath(groupKey), FieldValue.Group(entries - removed))
        _uiState.update { state -> state.copy(errors = state.errors.filterKeys { it.entryId != entryId }) }
    }

    /** Reorders a `groupList` — the order the moments play on the page. */
    fun moveEntry(groupKey: String, fromIndex: Int, toIndex: Int) {
        val entries = (_uiState.value.values[groupKey] as? FieldValue.Group)?.entries ?: return
        setValue(FieldPath(groupKey), FieldValue.Group(entries.moved(fromIndex, toIndex)))
    }

    fun messageShown() {
        _uiState.update { it.copy(message = null) }
    }

    // ---------------------------------------------------------------------------------------------
    // Saving, previewing, continuing
    // ---------------------------------------------------------------------------------------------

    /** Saves the draft as it is; only malformed values (too long, out of range) stop it. */
    fun save() {
        viewModelScope.launch {
            if (persist(requireComplete = false) != null) note("Draft saved")
        }
    }

    /** Saves, then opens the draft on the web in the in-app browser. */
    fun preview() {
        viewModelScope.launch {
            if (_uiState.value.uploadsInFlight > 0) return@launch note("Wait a moment — your photos are still uploading.")
            _uiState.update { it.copy(isOpeningPreview = true) }
            val creationId = persist(requireComplete = false)
            if (creationId != null) {
                when (val link = creations.previewLink(creationId)) {
                    is AppResult.Success -> _events.send(EditorEvent.OpenPreview(link.data.url))
                    is AppResult.Failure -> note(link.error.toUserMessage())
                }
            }
            _uiState.update { it.copy(isOpeningPreview = false) }
        }
    }

    /** Requires every required field, saves, then hands the draft to publishing. */
    fun continueToPublish() {
        viewModelScope.launch {
            val creationId = persist(requireComplete = true) ?: return@launch
            _events.send(EditorEvent.ContinueToPublish(creationId))
        }
    }

    /** Deletes the draft (or discards a never-saved form, freeing its photos), then closes. */
    fun delete() {
        viewModelScope.launch {
            val state = _uiState.value
            val creationId = state.creationId
            if (creationId == null) {
                FormEngine.imageIds(state.fields, state.values).forEach { id -> viewModelScope.launch { images.delete(id) } }
                _events.send(EditorEvent.Closed)
                return@launch
            }
            when (val result = creations.delete(creationId)) {
                is AppResult.Success -> _events.send(EditorEvent.Closed)
                is AppResult.Failure -> note(result.error.toUserMessage())
            }
        }
    }

    /**
     * Validates, then creates or updates the draft. Returns its id, or null when it was refused (the
     * errors or the failure are already in the state). Nothing is sent when the draft is saved and
     * unchanged.
     */
    private suspend fun persist(requireComplete: Boolean): String? = saveLock.withLock {
        val state = _uiState.value
        val template = state.template ?: return@withLock null
        val errors = FormEngine.validate(template.fields, state.values, requireComplete)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(errors = errors) }
            note(
                if (requireComplete) "A few things still need filling in." else "A few things need another look.",
            )
            return@withLock null
        }
        if (!state.hasUnsavedChanges && state.creationId != null) return@withLock state.creationId

        _uiState.update { it.copy(isSaving = true, errors = emptyMap()) }
        val data = FormEngine.toWireData(template.fields, state.values)
        val usedImages = FormEngine.imageIds(template.fields, state.values)
        val result = state.creationId
            ?.let { creations.update(it, data, coverImageId = usedImages.firstOrNull()) }
            ?: creations.create(template.id, data, coverImageId = usedImages.firstOrNull())
        when (result) {
            is AppResult.Success -> {
                savedImageIds = usedImages.toSet()
                freeReleasedImages()
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        creationId = result.data.id,
                        // Values edited while the save was in flight are still unsaved.
                        hasUnsavedChanges = it.values != state.values,
                    )
                }
                result.data.id
            }
            is AppResult.Failure -> {
                _uiState.update { it.copy(isSaving = false) }
                note(result.error.toUserMessage())
                null
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Photos
    // ---------------------------------------------------------------------------------------------

    private fun startUpload(path: FieldPath, slotId: String, aspectRatio: AspectRatio?) {
        viewModelScope.launch {
            uploadQueue.withLock {
                val source = sources[slotId] ?: return@withLock
                // Removed (or its group entry removed) while waiting its turn.
                if (!hasSlot(path, slotId)) {
                    sources.remove(slotId)
                    return@withLock
                }
                val maxBytes = remoteConfig.config.limits.maxUploadBytes
                val prepared = when (val result = processor.prepare(source.bytes, aspectRatio, maxBytes, framing = source.framing)) {
                    is AppResult.Success -> result.data
                    is AppResult.Failure -> {
                        sources.remove(slotId)
                        replaceSlot(path, slotId) { PhotoSlot.Failed(slotId, result.error.toUserMessage(), retryable = false) }
                        return@withLock
                    }
                }
                when (val result = images.upload(prepared)) {
                    is AppResult.Success -> {
                        if (hasSlot(path, slotId)) {
                            sources.remove(slotId)
                            replaceSlot(path, slotId) {
                                PhotoSlot.Uploaded(slotId, result.data.id, result.data.thumbUrl, result.data.fullUrl)
                            }
                        } else {
                            // Removed while uploading: nothing will ever use it.
                            sources.remove(slotId)
                            images.delete(result.data.id)
                        }
                    }
                    is AppResult.Failure -> {
                        val retryable = result.error.isWorthRetrying()
                        if (!retryable) sources.remove(slotId)
                        replaceSlot(path, slotId) { PhotoSlot.Failed(slotId, result.error.toUserMessage(), retryable) }
                    }
                }
            }
        }
    }

    private fun AppError.isWorthRetrying(): Boolean = when (this) {
        is AppError.Network, is AppError.Unknown -> true
        is AppError.Api -> code == ErrorCode.RATE_LIMITED || code == ErrorCode.STORAGE_UNAVAILABLE || status >= 500
        is AppError.Invalid -> false
    }

    /** Lets go of a photo leaving the form: frees it now, or after the next save if the saved draft still uses it. */
    private fun releaseSlot(slot: PhotoSlot) {
        sources.remove(slot.slotId)
        val imageId = (slot as? PhotoSlot.Uploaded)?.imageId ?: return
        if (imageId in savedImageIds) {
            releasedAfterSave += imageId
        } else {
            viewModelScope.launch { images.delete(imageId) }
        }
    }

    private fun freeReleasedImages() {
        val stillUsed = savedImageIds
        val toFree = releasedAfterSave.filterNot { it in stillUsed }
        releasedAfterSave.removeAll(toFree.toSet())
        toFree.forEach { id -> viewModelScope.launch { images.delete(id) } }
    }

    private fun hasSlot(path: FieldPath, slotId: String): Boolean = when (val value = _uiState.value.valueOf(path)) {
        is FieldValue.Photo -> value.photo?.slotId == slotId
        is FieldValue.Photos -> value.photos.any { it.slotId == slotId }
        else -> false
    }

    /** Replaces (or, when [transform] returns null, removes) one photo slot at [path]. */
    private fun replaceSlot(path: FieldPath, slotId: String, transform: (PhotoSlot) -> PhotoSlot?) {
        _uiState.update { state ->
            val next = when (val value = state.valueOf(path)) {
                is FieldValue.Photo -> FieldValue.Photo(value.photo?.let { if (it.slotId == slotId) transform(it) else it })
                is FieldValue.Photos -> FieldValue.Photos(value.photos.mapNotNull { if (it.slotId == slotId) transform(it) else it })
                else -> return@update state
            }
            state.copy(values = state.values.withValueAt(path, next), errors = state.errors - path, hasUnsavedChanges = true)
        }
    }

    private fun setValue(path: FieldPath, value: FieldValue) {
        _uiState.update { state ->
            state.copy(values = state.values.withValueAt(path, value), errors = state.errors - path, hasUnsavedChanges = true)
        }
    }

    private fun note(message: String) {
        _uiState.update { it.copy(message = message) }
    }

    private fun JsonObject?.containsImages(): Boolean {
        fun scan(element: JsonElement?): Boolean = when (element) {
            is JsonObject -> "imageId" in element || element.values.any(::scan)
            is JsonArray -> element.any(::scan)
            else -> false
        }
        return scan(this)
    }

    private fun JsonElement?.text(): String? = (this as? JsonPrimitive)?.takeIf { it !is JsonNull }?.contentOrNull

    private fun <T> List<T>.moved(fromIndex: Int, toIndex: Int): List<T> {
        if (fromIndex !in indices || toIndex !in indices || fromIndex == toIndex) return this
        return toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
    }
}
