package com.ferbotz.momentic.designsystem.gallery

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import com.ferbotz.momentic.data.mapper.toDomain
import com.ferbotz.momentic.data.media.PickedPhoto
import com.ferbotz.momentic.data.media.platformImageProcessor
import com.ferbotz.momentic.data.model.AspectRatio
import com.ferbotz.momentic.data.model.ChoiceOption
import com.ferbotz.momentic.data.model.FieldDefinition
import com.ferbotz.momentic.data.model.FieldType
import com.ferbotz.momentic.data.model.Pricing
import com.ferbotz.momentic.data.model.TemplateDetail
import com.ferbotz.momentic.data.network.appJson
import com.ferbotz.momentic.data.network.dto.TemplateDetailDto
import com.ferbotz.momentic.data.result.AppResult
import com.ferbotz.momentic.designsystem.components.QuietButton
import com.ferbotz.momentic.designsystem.components.SecondaryButton
import com.ferbotz.momentic.designsystem.components.SegmentedControl
import com.ferbotz.momentic.feature.editor.FormActions
import com.ferbotz.momentic.feature.editor.TemplateForm
import com.ferbotz.momentic.feature.editor.crop.CropFlow
import com.ferbotz.momentic.feature.editor.crop.CropSession
import com.ferbotz.momentic.platform.decodePhotoPreview
import com.ferbotz.momentic.platform.rememberPhotoPicker
import com.ferbotz.momentic.presentation.editor.EditorUiState
import com.ferbotz.momentic.presentation.editor.FieldPath
import com.ferbotz.momentic.presentation.editor.FieldValue
import com.ferbotz.momentic.presentation.editor.FormEngine
import com.ferbotz.momentic.presentation.editor.PhotoSlot
import com.ferbotz.momentic.presentation.editor.definitionAt
import com.ferbotz.momentic.presentation.editor.valueAt
import com.ferbotz.momentic.presentation.editor.withValueAt
import com.ferbotz.momentic.theme.Momentic
import com.ferbotz.momentic.theme.MomenticSpacing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive

/**
 * The field kit at work: the form generated from a template — one with every field type, or three real
 * templates — against a stand-in for the editor's view model. "Add photo" opens the real photo picker and
 * crop screen; nothing is uploaded — each framed photo goes through the real image processor on the
 * device, and the last result shows under the buttons, to check it against the frame. Each "upload"
 * settles after a moment (every fourth one fails, to show that state). "Check the form" runs the same
 * validation as "Continue".
 */
@Composable
internal fun FieldKitDemo() {
    val scope = rememberCoroutineScope()
    var which by remember { mutableIntStateOf(0) }
    val templates = remember { listOf(EveryField, GalleryTemplates.proposal, GalleryTemplates.wedding, GalleryTemplates.giftReveal) }
    var lastFramed by remember { mutableStateOf<ImageBitmap?>(null) }
    var pickingFor by remember { mutableStateOf<FieldPath?>(null) }
    // Each tap on "Add photo" is a new request — the same field twice in a row (after a cancel) still opens the picker.
    var pickRequests by remember { mutableIntStateOf(0) }
    var cropping by remember { mutableStateOf<Pair<FieldPath, CropSession>?>(null) }
    val form = remember(which) {
        GalleryForm(templates[which], scope, onPick = { path, _ ->
            pickingFor = path
            pickRequests++
        })
    }
    val picker = rememberPhotoPicker { picked ->
        val path = pickingFor ?: return@rememberPhotoPicker
        pickingFor = null
        val ratio = form.state.fields.definitionAt(path)?.aspectRatio
        if (ratio == null) form.addPicked(path, picked.map { PickedPhoto(it) }) else cropping = path to CropSession(ratio, picked)
    }
    LaunchedEffect(pickRequests) {
        val path = pickingFor ?: return@LaunchedEffect
        val field = form.state.fields.definitionAt(path) ?: return@LaunchedEffect
        picker.pick(if (field.type == FieldType.IMAGE) 1 else 4)
    }
    Column(verticalArrangement = Arrangement.spacedBy(MomenticSpacing.md)) {
        SegmentedControl(listOf("All", "Question", "Invitation", "Surprise"), which, { which = it })
        Row(horizontalArrangement = Arrangement.spacedBy(MomenticSpacing.xs)) {
            SecondaryButton("Check the form", onClick = form::check, compact = true)
            QuietButton("Clear errors", onClick = form::clearErrors, compact = true)
        }
        lastFramed?.let { framed ->
            Text("The last photo as it would upload", style = Momentic.type.captionStrong, color = Momentic.colors.inkSoft)
            Image(framed, contentDescription = "The last framed photo", modifier = Modifier.width(160.dp).aspectRatio(framed.width.toFloat() / framed.height))
        }
        TemplateForm(form.state, form)
    }
    cropping?.let { (path, session) ->
        CropFlow(
            session = session,
            onAdvance = { next -> cropping = path to next },
            onDone = { framed ->
                cropping = null
                form.addPicked(path, framed)
                val first = framed.firstOrNull() ?: return@CropFlow
                scope.launch {
                    val prepared = platformImageProcessor().prepare(first.bytes, session.aspectRatio, maxBytes = 5_000_000, framing = first.framing)
                    lastFramed = (prepared as? AppResult.Success)?.data?.bytes?.let { decodePhotoPreview(it) }
                }
            },
            onUnreadable = { },
        )
    }
}

internal class GalleryForm(
    template: TemplateDetail,
    private val scope: CoroutineScope,
    private val onPick: (FieldPath, Int?) -> Unit,
) : FormActions {
    private var lastId = 0
    private var picks = 0
    private fun newId(): String = "g${++lastId}"

    var state by mutableStateOf(
        EditorUiState(
            isLoading = false,
            template = template,
            values = FormEngine.initialValues(template.fields, data = null, newId = ::newId),
            examples = mapOf("theirName" to "Divya", "partnerName" to "Divya", "recipientName" to "Aarav", "venue" to "Amber Lawns, MI Road, Jaipur"),
        ),
    )
        private set

    fun check() {
        state = state.copy(errors = FormEngine.validate(state.fields, state.values, requireComplete = true))
    }

    fun clearErrors() {
        state = state.copy(errors = emptyMap())
    }

    override fun update(path: FieldPath, value: FieldValue) {
        state = state.copy(values = state.values.withValueAt(path, value), errors = state.errors - path)
    }

    override fun pickPhotos(path: FieldPath, room: Int?) = onPick(path, room)

    /** Photos back from the picker (and the crop screen) — each "uploads" as the real editor's would. */
    fun addPicked(path: FieldPath, picked: List<PickedPhoto>) {
        val field = state.fields.definitionAt(path) ?: return
        if (picked.isEmpty()) return
        val count = if (field.type == FieldType.IMAGE) 1 else picked.size
        val slots = List(count) { PhotoSlot.Uploading(newId()) }
        state = state.copy(
            values = when (val current = state.values.valueAt(path)) {
                is FieldValue.Photos -> state.values.withValueAt(path, FieldValue.Photos(current.photos + slots))
                else -> state.values.withValueAt(path, FieldValue.Photo(slots.first()))
            },
            errors = state.errors - path,
        )
        slots.forEach { finishUpload(path, it.slotId, fail = ++picks % 4 == 0) }
    }

    override fun retryPhoto(path: FieldPath, slotId: String) {
        replace(path, slotId) { PhotoSlot.Uploading(slotId) }
        finishUpload(path, slotId, fail = false)
    }

    override fun removePhoto(path: FieldPath, slotId: String) = replace(path, slotId) { null }

    override fun movePhoto(path: FieldPath, from: Int, to: Int) {
        val photos = (state.values.valueAt(path) as? FieldValue.Photos)?.photos ?: return
        update(path, FieldValue.Photos(photos.moved(from, to)))
    }

    override fun addEntry(groupKey: String) {
        val group = state.fields.firstOrNull { it.key == groupKey } ?: return
        val entries = (state.values[groupKey] as? FieldValue.Group)?.entries.orEmpty()
        update(FieldPath(groupKey), FieldValue.Group(entries + FormEngine.newEntry(group, ::newId)))
    }

    override fun removeEntry(groupKey: String, entryId: String) {
        val entries = (state.values[groupKey] as? FieldValue.Group)?.entries ?: return
        update(FieldPath(groupKey), FieldValue.Group(entries.filterNot { it.id == entryId }))
        state = state.copy(errors = state.errors.filterKeys { it.entryId != entryId })
    }

    override fun moveEntry(groupKey: String, from: Int, to: Int) {
        val entries = (state.values[groupKey] as? FieldValue.Group)?.entries ?: return
        update(FieldPath(groupKey), FieldValue.Group(entries.moved(from, to)))
    }

    private fun finishUpload(path: FieldPath, slotId: String, fail: Boolean) {
        scope.launch {
            delay(1400)
            replace(path, slotId) {
                if (fail) {
                    PhotoSlot.Failed(slotId, "No connection.", retryable = true)
                } else {
                    PhotoSlot.Uploaded(slotId, imageId = "img_$slotId", thumbUrl = null, fullUrl = null)
                }
            }
        }
    }

    private fun replace(path: FieldPath, slotId: String, transform: (PhotoSlot) -> PhotoSlot?) {
        val next = when (val value = state.values.valueAt(path)) {
            is FieldValue.Photo -> FieldValue.Photo(value.photo?.let { if (it.slotId == slotId) transform(it) else it })
            is FieldValue.Photos -> FieldValue.Photos(value.photos.mapNotNull { if (it.slotId == slotId) transform(it) else it })
            else -> return
        }
        state = state.copy(values = state.values.withValueAt(path, next))
    }

    private fun <T> List<T>.moved(from: Int, to: Int): List<T> {
        if (from !in indices || to !in indices || from == to) return this
        return toMutableList().apply { add(to, removeAt(from)) }
    }
}


/** One of every field type, as the canvas's field kit board draws them. */
private val EveryField = TemplateDetail(
    id = "field_kit",
    categoryId = "greeting",
    name = "The Field Kit",
    tagline = "",
    description = "",
    thumbnailUrl = null,
    previewImageUrls = emptyList(),
    tierKey = "A",
    displayPerDay = "",
    tags = emptyList(),
    previewCode = "pv_field_kit",
    supportsRsvp = true,
    pricing = Pricing(tierKey = "A", label = "Simple", displayPerDay = "", bundles = emptyList()),
    fields = listOf(
        FieldDefinition("theirName", FieldType.TEXT, "Their name", required = true, helpText = "As you'd say it to them", maxLength = 40),
        FieldDefinition("message", FieldType.LONG_TEXT, "Message", required = false, helpText = "A few lines in your own words", maxLength = 600, placeholder = "Write it the way you'd say it"),
        FieldDefinition("turning", FieldType.NUMBER, "Turning", required = true, min = 1.0, max = 12.0),
        FieldDefinition("rsvp", FieldType.BOOLEAN, "Collect RSVPs", required = false, helpText = "Guests reply yes, maybe or no", defaultValue = JsonPrimitive(false)),
        FieldDefinition("ceremonyDate", FieldType.DATE, "Ceremony date", required = true),
        FieldDefinition("ceremonyAt", FieldType.DATE_TIME, "Ceremony date and time", required = true),
        FieldDefinition("ribbon", FieldType.COLOR, "Ribbon colour", required = false),
        FieldDefinition(
            "occasion", FieldType.CHOICE, "Occasion", required = true,
            options = listOf("Thank you", "Sorry", "Congratulations", "Good luck", "Thinking of you").map { ChoiceOption(it.lowercase(), it) },
        ),
        FieldDefinition("venue", FieldType.LOCATION, "Venue", required = true, helpText = "Guests get directions from this"),
        FieldDefinition("cover", FieldType.IMAGE, "Cover photo", required = true, aspectRatio = AspectRatio(4, 5)),
        FieldDefinition("photos", FieldType.IMAGE_LIST, "Photos", required = false, maxItems = 8, aspectRatio = AspectRatio(4, 5)),
        FieldDefinition(
            "moments", FieldType.GROUP_LIST, "Moments", required = true, minItems = 1, maxItems = 8,
            helpText = "A photo, a title and a short caption for each",
            fields = listOf(
                FieldDefinition("photo", FieldType.IMAGE, "Photo", required = true, aspectRatio = AspectRatio(4, 5)),
                FieldDefinition("title", FieldType.TEXT, "Title", required = true, maxLength = 40),
                FieldDefinition("caption", FieldType.LONG_TEXT, "Caption", required = false, helpText = "A line or two", maxLength = 140),
            ),
        ),
        FieldDefinition("song", FieldType.UNKNOWN, "Background song", required = false),
    ),
)

/** Three real templates as momentic.ferbotz.com served them on 2026-10-23. */
internal object GalleryTemplates {
    private const val ORIGIN = "https://momentic.ferbotz.com"

    val proposal = parse(
        """{"id":"proposal_memory_walk","categoryId":"love","name":"The Question","tagline":"Memories, one by one, then the question","tierKey":"C","displayPerDay":"from Rs.200/day","previewCode":"pv_proposal_memory_walk","description":"","supportsRsvp":false,"fields":[{"key":"partnerName","type":"text","label":"Their name","required":true,"maxLength":40},{"key":"memories","type":"groupList","label":"Your moments","helpText":"Three to seven moments, in the order they happened","required":true,"minItems":3,"maxItems":7,"fields":[{"key":"title","type":"text","label":"Moment","required":true,"maxLength":60},{"key":"text","type":"longText","label":"What happened","required":true,"maxLength":300},{"key":"image","type":"image","label":"Photo","required":false,"aspectRatio":"4:5"}]},{"key":"question","type":"text","label":"The question","required":true,"maxLength":80,"defaultValue":"Will you marry me?"},{"key":"closingNote","type":"longText","label":"After the question","required":false,"maxLength":400}],"pricing":{"tierKey":"C","label":"Premium","displayPerDay":"from Rs.200/day","bundles":[]}}""",
    )

    val wedding = parse(
        """{"id":"wedding_invitation_classic","categoryId":"wedding","name":"The Invitation","tagline":"An unfolding wedding invitation","tierKey":"C","displayPerDay":"from Rs.200/day","previewCode":"pv_wedding_invitation_classic","description":"","supportsRsvp":true,"fields":[{"key":"partnerOne","type":"text","label":"First name","required":true,"maxLength":40},{"key":"partnerTwo","type":"text","label":"Second name","required":true,"maxLength":40},{"key":"ceremonyAt","type":"dateTime","label":"Ceremony date and time","required":true},{"key":"venue","type":"location","label":"Venue","required":true},{"key":"invitationNote","type":"longText","label":"Invitation note","required":true,"maxLength":600},{"key":"photos","type":"imageList","label":"Photos","required":false,"minItems":0,"maxItems":8,"aspectRatio":"3:4"},{"key":"dressCode","type":"text","label":"Dress code","required":false,"maxLength":80},{"key":"rsvpEnabled","type":"boolean","label":"Collect RSVPs","required":false,"defaultValue":true}],"pricing":{"tierKey":"C","label":"Premium","displayPerDay":"from Rs.200/day","bundles":[]}}""",
    )

    val giftReveal = parse(
        """{"id":"birthday_gift_reveal","categoryId":"birthday","name":"Open the Surprise","tagline":"A gift box they tap to unwrap","tierKey":"A","displayPerDay":"from Rs.50/day","previewCode":"pv_birthday_gift_reveal","description":"","supportsRsvp":false,"fields":[{"key":"recipientName","type":"text","label":"Their name","required":true,"maxLength":40},{"key":"age","type":"number","label":"Turning","required":false,"min":1,"max":120},{"key":"message","type":"longText","label":"Message inside","required":true,"maxLength":600},{"key":"photos","type":"imageList","label":"Photos","required":false,"minItems":0,"maxItems":6,"aspectRatio":"4:5"},{"key":"accent","type":"choice","label":"Ribbon colour","required":false,"defaultValue":"blush","options":[{"value":"blush","label":"Blush"},{"value":"gold","label":"Gold"},{"value":"midnight","label":"Midnight"}]}],"pricing":{"tierKey":"A","label":"Simple","displayPerDay":"from Rs.50/day","bundles":[]}}""",
    )

    private fun parse(json: String): TemplateDetail = appJson.decodeFromString<TemplateDetailDto>(json).toDomain(ORIGIN)!!
}
