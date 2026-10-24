package com.ferbotz.momentic

import com.ferbotz.momentic.data.media.CropRect
import com.ferbotz.momentic.data.media.PhotoFraming
import com.ferbotz.momentic.data.media.PickedPhoto
import com.ferbotz.momentic.data.model.AspectRatio
import com.ferbotz.momentic.data.result.AppError
import com.ferbotz.momentic.data.result.ErrorCode
import com.ferbotz.momentic.presentation.editor.AUTOSAVE_DELAY_MS
import com.ferbotz.momentic.presentation.editor.AUTOSAVE_RETRY_MS
import com.ferbotz.momentic.presentation.editor.AutosaveState
import com.ferbotz.momentic.presentation.editor.EditorEvent
import com.ferbotz.momentic.presentation.editor.EditorViewModel
import com.ferbotz.momentic.presentation.editor.FieldPath
import com.ferbotz.momentic.presentation.editor.FieldValue
import com.ferbotz.momentic.presentation.editor.PhotoSlot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The editor's flows against the "Open the Surprise" template's real field contract. */
@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelTest {

    private val gift = TemplateFixtures.template(TemplateFixtures.giftReveal)
    private val photos = FieldPath("photos")

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun editor(
        creations: FakeCreations = FakeCreations(),
        images: FakeImages = FakeImages(),
        processor: FakeProcessor = FakeProcessor(),
        creationId: String? = null,
    ) = EditorViewModel(
        templateId = gift.id,
        creationId = creationId,
        catalogue = FakeCatalogue(gift),
        creations = creations,
        images = images,
        render = FakeRender(),
        processor = processor,
        remoteConfig = FakeRemoteConfig(),
    )

    private fun EditorViewModel.fillRequired() {
        update(FieldPath("recipientName"), FieldValue.Text("Divya"))
        update(FieldPath("message"), FieldValue.Text("Happy birthday!"))
    }

    private fun EditorViewModel.photoSlots(): List<PhotoSlot> = (uiState.value.valueOf(photos) as FieldValue.Photos).photos

    @Test
    fun continueWaitsForEveryRequiredField_thenSavesTheWireData() = runTest {
        val creations = FakeCreations()
        val vm = editor(creations)
        val events = mutableListOf<EditorEvent>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) { vm.events.collect { events += it } }

        vm.continueToPublish()
        assertEquals(setOf(FieldPath("recipientName"), FieldPath("message")), vm.uiState.value.errors.keys)
        assertTrue(creations.created.isEmpty(), "nothing is sent while required fields are empty")

        vm.fillRequired()
        assertTrue(vm.uiState.value.errors.isEmpty(), "editing clears the field's error")
        vm.continueToPublish()

        val expected = JsonObject(
            mapOf(
                "recipientName" to JsonPrimitive("Divya"),
                "message" to JsonPrimitive("Happy birthday!"),
                "accent" to JsonPrimitive("blush"),
            ),
        )
        assertEquals(expected, creations.created.single().first)
        assertEquals(listOf<EditorEvent>(EditorEvent.ContinueToPublish("c1")), events)
        assertEquals("c1", vm.uiState.value.creationId)
        assertFalse(vm.uiState.value.hasUnsavedChanges)
        collector.cancel()
    }

    @Test
    fun photosUploadAsTheyArePicked_croppedToTheFieldsRatio_andAreWrittenAsIds() = runTest {
        val creations = FakeCreations()
        val processor = FakeProcessor()
        val vm = editor(creations, processor = processor)

        vm.addPhotos(photos, listOf(byteArrayOf(1), byteArrayOf(2)))

        assertEquals(listOf("img1", "img2"), vm.photoSlots().map { (it as PhotoSlot.Uploaded).imageId })
        assertEquals(listOf<AspectRatio?>(AspectRatio(4, 5), AspectRatio(4, 5)), processor.aspectRatios)
        vm.fillRequired()
        vm.save()

        val (data, cover) = creations.created.single()
        assertEquals(
            JsonArray(listOf(JsonObject(mapOf("imageId" to JsonPrimitive("img1"))), JsonObject(mapOf("imageId" to JsonPrimitive("img2"))))),
            data["photos"],
        )
        assertEquals("img1", cover, "the first photo is the cover")
    }

    @Test
    fun aFramedPhotoIsPreparedAsFramed_andTryAgainKeepsTheFraming() = runTest {
        val images = FakeImages().apply { failNext = AppError.Network() }
        val processor = FakeProcessor()
        val vm = editor(images = images, processor = processor)
        val framing = PhotoFraming(quarterTurns = 1, crop = CropRect(0.1f, 0.2f, 0.5f, 0.625f))

        vm.addFramedPhotos(photos, listOf(PickedPhoto(byteArrayOf(1), framing)))
        val failed = vm.photoSlots().single() as PhotoSlot.Failed
        assertTrue(failed.retryable, "a dropped connection can be retried")

        vm.retryPhoto(photos, failed.slotId)

        assertEquals("img1", (vm.photoSlots().single() as PhotoSlot.Uploaded).imageId)
        assertEquals(listOf<PhotoFraming?>(framing, framing), processor.framings, "the retry is framed like the first try")
    }

    @Test
    fun photosAddedWithoutFramingAreCentred() = runTest {
        val processor = FakeProcessor()
        val vm = editor(processor = processor)
        vm.addPhotos(photos, listOf(byteArrayOf(1)))
        assertEquals(listOf<PhotoFraming?>(null), processor.framings)
    }

    @Test
    fun aRemovedPhotoIsFreedAtOnce_unlessTheSavedDraftStillUsesIt() = runTest {
        val creations = FakeCreations()
        val images = FakeImages()
        val vm = editor(creations, images)
        vm.fillRequired()

        vm.addPhotos(photos, listOf(byteArrayOf(1)))
        vm.removePhoto(photos, vm.photoSlots().single().slotId)
        assertEquals(listOf("img1"), images.deleted, "never saved, so nothing uses it")

        vm.addPhotos(photos, listOf(byteArrayOf(2), byteArrayOf(3)))
        vm.save()
        vm.removePhoto(photos, vm.photoSlots().first().slotId)
        assertEquals(listOf("img1"), images.deleted, "the saved draft still points at img2")

        vm.save()
        assertEquals(listOf("img1", "img2"), images.deleted)
        assertEquals("img3", creations.updated.single().second, "the cover moves to the next photo")
    }

    @Test
    fun photosBeyondTheFieldsMaximumAreLeftOut() = runTest {
        val vm = editor()
        vm.addPhotos(photos, List(8) { byteArrayOf(it.toByte()) })

        assertEquals(6, vm.photoSlots().size)
        assertTrue(vm.uiState.value.message.orEmpty().contains("6"))
    }

    @Test
    fun aDraftReopensWithItsValues_andIsntResentUnchanged() = runTest {
        val creations = FakeCreations().apply {
            stored = creation(
                id = "c9",
                templateId = gift.id,
                data = Json.parseToJsonElement("""{"recipientName":"Asha","message":"Hi!","photos":[{"imageId":"x1"}]}""").jsonObject,
            )
        }
        val vm = editor(creations, creationId = "c9")

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(FieldValue.Text("Asha"), state.valueOf(FieldPath("recipientName")))
        assertEquals("x1", (vm.photoSlots().single() as PhotoSlot.Uploaded).imageId)
        assertEquals(FieldValue.Choice(null), state.valueOf(FieldPath("accent")), "a draft's empty field isn't refilled with the default")

        vm.continueToPublish()
        assertTrue(creations.updated.isEmpty() && creations.created.isEmpty(), "nothing changed, nothing sent")
    }

    @Test
    fun aPauseInEditingSavesTheDraftQuietly_once() = runTest {
        val creations = FakeCreations()
        val vm = editor(creations)
        vm.update(FieldPath("recipientName"), FieldValue.Text("D"))
        vm.update(FieldPath("recipientName"), FieldValue.Text("Div"))
        assertEquals(AutosaveState.Saving, vm.uiState.value.autosave)

        advanceTimeBy(AUTOSAVE_DELAY_MS - 100)
        assertTrue(creations.created.isEmpty(), "still typing")
        vm.update(FieldPath("recipientName"), FieldValue.Text("Divya"))
        advanceTimeBy(AUTOSAVE_DELAY_MS + 1)

        assertEquals("Divya", creations.created.single().first["recipientName"]?.let { (it as JsonPrimitive).content })
        assertEquals(AutosaveState.Saved, vm.uiState.value.autosave)
        assertEquals(null, vm.uiState.value.message, "an autosave says nothing")
        assertTrue(vm.uiState.value.errors.isEmpty(), "required fields aren't pointed out by an autosave")
    }

    @Test
    fun aMalformedValueWaits_withoutMarkingIt() = runTest {
        val creations = FakeCreations()
        val vm = editor(creations)
        vm.update(FieldPath("recipientName"), FieldValue.Text("x".repeat(41)))
        advanceTimeBy(AUTOSAVE_DELAY_MS + 1)

        assertTrue(creations.created.isEmpty())
        assertEquals(AutosaveState.NotSaved, vm.uiState.value.autosave)
        assertTrue(vm.uiState.value.errors.isEmpty())
    }

    @Test
    fun offline_theAutosaveTriesAgainByItself() = runTest {
        val creations = FakeCreations().apply { failWith = AppError.Network() }
        val vm = editor(creations)
        vm.update(FieldPath("recipientName"), FieldValue.Text("Divya"))
        advanceTimeBy(AUTOSAVE_DELAY_MS + 1)
        assertEquals(AutosaveState.Offline, vm.uiState.value.autosave)
        assertEquals(null, vm.uiState.value.message)

        creations.failWith = null
        advanceTimeBy(AUTOSAVE_RETRY_MS + 1)
        assertEquals(1, creations.created.size)
        assertEquals(AutosaveState.Saved, vm.uiState.value.autosave)
    }

    @Test
    fun aRefusedAutosaveSaysWhy_andWaitsForASaveThatWorks() = runTest {
        val creations = FakeCreations().apply { failWith = AppError.Api(403, ErrorCode.DRAFT_LIMIT_REACHED, null) }
        val vm = editor(creations)
        vm.update(FieldPath("recipientName"), FieldValue.Text("Divya"))
        advanceTimeBy(AUTOSAVE_DELAY_MS + 1)
        assertTrue(vm.uiState.value.message.orEmpty().contains("drafts"))
        vm.messageShown()

        creations.failWith = null
        vm.update(FieldPath("recipientName"), FieldValue.Text("Divya R"))
        advanceTimeBy(AUTOSAVE_DELAY_MS + 1)
        assertTrue(creations.created.isEmpty(), "paused after the refusal")

        vm.save()
        assertEquals(1, creations.created.size)
        vm.update(FieldPath("recipientName"), FieldValue.Text("Divya Rao"))
        advanceTimeBy(AUTOSAVE_DELAY_MS + 1)
        assertEquals(1, creations.updated.size, "autosave is back once a save worked")
    }

    @Test
    fun backSavesWhatChanged_thenCloses() = runTest {
        val creations = FakeCreations()
        val vm = editor(creations)
        val events = mutableListOf<EditorEvent>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) { vm.events.collect { events += it } }

        vm.update(FieldPath("recipientName"), FieldValue.Text("Divya"))
        vm.close()
        vm.close()

        assertEquals(1, creations.created.size, "saved at once, not after the pause")
        assertEquals(listOf<EditorEvent>(EditorEvent.Closed), events, "a second Back doesn't close twice")
        advanceTimeBy(AUTOSAVE_DELAY_MS + 1)
        assertEquals(1, creations.created.size)
        collector.cancel()
    }

    @Test
    fun backWithNothingChanged_closesWithoutSaving() = runTest {
        val creations = FakeCreations()
        val vm = editor(creations)
        val events = mutableListOf<EditorEvent>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) { vm.events.collect { events += it } }

        vm.close()
        assertEquals(listOf<EditorEvent>(EditorEvent.Closed), events)
        assertTrue(creations.created.isEmpty(), "an untouched form leaves no draft behind")
        collector.cancel()
    }

    @Test
    fun backThatCantSave_asks_andLeavingLetsTheNewPhotosGo() = runTest {
        val creations = FakeCreations()
        val images = FakeImages()
        val vm = editor(creations, images)
        val events = mutableListOf<EditorEvent>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) { vm.events.collect { events += it } }

        vm.addPhotos(photos, listOf(byteArrayOf(1)))
        vm.update(FieldPath("recipientName"), FieldValue.Text("x".repeat(41)))
        vm.close()
        assertTrue(vm.uiState.value.confirmLeave)
        assertTrue(events.isEmpty())
        assertEquals(setOf(FieldPath("recipientName")), vm.uiState.value.errors.keys, "what stopped the save is marked")

        vm.keepEditing()
        assertFalse(vm.uiState.value.confirmLeave)
        vm.close()
        vm.leaveWithoutSaving()
        assertEquals(listOf<EditorEvent>(EditorEvent.Closed), events)
        assertEquals(listOf("img1"), images.deleted, "never saved, so nothing uses it")
        collector.cancel()
    }

    @Test
    fun previewNeedsEveryRequiredField() = runTest {
        val creations = FakeCreations()
        val vm = editor(creations)
        val events = mutableListOf<EditorEvent>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) { vm.events.collect { events += it } }

        vm.update(FieldPath("recipientName"), FieldValue.Text("Divya"))
        vm.preview()
        assertTrue(events.isEmpty())
        assertEquals(setOf(FieldPath("message")), vm.uiState.value.errors.keys)

        vm.update(FieldPath("message"), FieldValue.Text("Happy birthday!"))
        vm.preview()
        assertEquals(listOf<EditorEvent>(EditorEvent.OpenPreview("https://momentic.ferbotz.com/d_test")), events)
        collector.cancel()
    }

    @Test
    fun deletingCancelsAPendingAutosave() = runTest {
        val creations = FakeCreations()
        val vm = editor(creations)
        vm.fillRequired()
        vm.save()
        vm.update(FieldPath("recipientName"), FieldValue.Text("Divya R"))
        vm.delete()
        advanceTimeBy(AUTOSAVE_DELAY_MS + 1)
        assertEquals(listOf("c1"), creations.deleted)
        assertTrue(creations.updated.isEmpty(), "the deleted draft isn't saved again")
    }
}
