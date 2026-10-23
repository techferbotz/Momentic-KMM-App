package com.ferbotz.momentic

import com.ferbotz.momentic.data.model.FieldDefinition
import com.ferbotz.momentic.data.model.FieldType
import com.ferbotz.momentic.presentation.editor.FieldPath
import com.ferbotz.momentic.presentation.editor.FieldValue
import com.ferbotz.momentic.presentation.editor.FormEngine
import com.ferbotz.momentic.presentation.editor.GroupEntry
import com.ferbotz.momentic.presentation.editor.PhotoSlot
import com.ferbotz.momentic.presentation.editor.withValueAt
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.UtcOffset
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The form engine against Momentic's real field contracts: what a new form starts with, what it
 * writes into `data`, and what it refuses before a save or a publish.
 */
class FormEngineTest {

    private var lastId = 0
    private fun newId() = "id${++lastId}"

    private val proposal = TemplateFixtures.template(TemplateFixtures.proposal)
    private val wedding = TemplateFixtures.template(TemplateFixtures.wedding)
    private val gift = TemplateFixtures.template(TemplateFixtures.giftReveal)

    @Test
    fun aNewFormStartsFromEachFieldsDefault() {
        val proposalValues = FormEngine.initialValues(proposal.fields, data = null, newId = ::newId)
        assertEquals(FieldValue.Text("Will you marry me?"), proposalValues["question"])
        val memories = proposalValues["memories"] as FieldValue.Group
        assertEquals(3, memories.entries.size, "a group starts with its minimum, so its shape is visible")
        assertEquals(FieldValue.Text(""), memories.entries.first().values["title"])
        assertEquals(FieldValue.Photo(null), memories.entries.first().values["image"])

        val weddingValues = FormEngine.initialValues(wedding.fields, data = null, newId = ::newId)
        assertEquals(FieldValue.Toggle(true), weddingValues["rsvpEnabled"])
        assertEquals(FieldValue.Photos(emptyList()), weddingValues["photos"])
        assertEquals(FieldValue.DateTime(null), weddingValues["ceremonyAt"])

        assertEquals(FieldValue.Choice("blush"), FormEngine.initialValues(gift.fields, null, ::newId)["accent"])
    }

    @Test
    fun emptyFieldsAreOmitted_neverSentAsNull() {
        val values = FormEngine.initialValues(wedding.fields, data = null, newId = ::newId)
        assertEquals(JsonObject(mapOf("rsvpEnabled" to JsonPrimitive(true))), FormEngine.toWireData(wedding.fields, values))
    }

    @Test
    fun valuesAreWrittenInTheirWireShapes() {
        var values = FormEngine.initialValues(wedding.fields, data = null, newId = ::newId)
        values = values
            .withValueAt(FieldPath("partnerOne"), FieldValue.Text("  Sarah  "))
            .withValueAt(FieldPath("ceremonyAt"), FieldValue.DateTime(LocalDateTime(2026, 12, 6, 18, 30), UtcOffset(hours = 5, minutes = 30)))
            .withValueAt(FieldPath("venue"), FieldValue.Location("The Leela Palace, Bengaluru", 12.9606, 77.6486))
            .withValueAt(
                FieldPath("photos"),
                FieldValue.Photos(
                    listOf(
                        PhotoSlot.Uploaded("s1", "img-1", null, null),
                        PhotoSlot.Uploading("s2"),
                        PhotoSlot.Uploaded("s3", "img-3", null, null),
                    ),
                ),
            )
            .withValueAt(FieldPath("rsvpEnabled"), FieldValue.Toggle(false))

        val data = FormEngine.toWireData(wedding.fields, values)
        assertEquals("Sarah", data["partnerOne"]?.jsonPrimitive?.content, "text is trimmed")
        assertEquals("2026-12-06T18:30:00+05:30", data["ceremonyAt"]?.jsonPrimitive?.content)
        val venue = data["venue"]!!.jsonObject
        assertEquals("The Leela Palace, Bengaluru", venue["address"]?.jsonPrimitive?.content)
        assertEquals(JsonPrimitive(12.9606), venue["latitude"])
        assertEquals(
            JsonArray(listOf(JsonObject(mapOf("imageId" to JsonPrimitive("img-1"))), JsonObject(mapOf("imageId" to JsonPrimitive("img-3"))))),
            data["photos"],
            "photos are ids, never URLs — and only the uploaded ones",
        )
        assertEquals(JsonPrimitive(false), data["rsvpEnabled"])
        assertNull(data["dressCode"])

        val giftData = FormEngine.toWireData(
            gift.fields,
            FormEngine.initialValues(gift.fields, null, ::newId).withValueAt(FieldPath("age"), FieldValue.Number("12")),
        )
        assertEquals(12L, giftData["age"]?.jsonPrimitive?.long, "a whole number is written as an integer")
        assertEquals(2.5, FormEngine.parseNumber("2,5"))
    }

    @Test
    fun groupsWriteOnlyTheirFilledEntries() {
        var values = FormEngine.initialValues(proposal.fields, data = null, newId = ::newId)
        val entries = (values["memories"] as FieldValue.Group).entries
        values = values
            .withValueAt(FieldPath("memories", entries[0].id, "title"), FieldValue.Text("The day we met"))
            .withValueAt(FieldPath("memories", entries[0].id, "text"), FieldValue.Text("You laughed first."))
            .withValueAt(FieldPath("memories", entries[1].id, "title"), FieldValue.Text("Goa"))

        val memories = FormEngine.toWireData(proposal.fields, values)["memories"] as JsonArray
        assertEquals(2, memories.size, "the blank third entry is dropped")
        assertEquals(setOf("title", "text"), memories[0].jsonObject.keys, "an empty optional photo is omitted")
        assertEquals(setOf("title"), memories[1].jsonObject.keys)
    }

    @Test
    fun aDraftsDataSurvivesTheRoundTrip() {
        val stored = """
            {"partnerOne":"Sarah","partnerTwo":"Raj","ceremonyAt":"2026-12-06T18:30:00+05:30",
             "venue":{"address":"The Leela Palace","latitude":12.9606,"longitude":77.6486},
             "invitationNote":"Together with our families…","photos":[{"imageId":"a"},{"imageId":"b"}],
             "rsvpEnabled":false}
        """.trimIndent()
        val data = kotlinx.serialization.json.Json.parseToJsonElement(stored).jsonObject

        val values = FormEngine.initialValues(wedding.fields, data, ::newId)
        assertEquals(data, FormEngine.toWireData(wedding.fields, values))
        assertEquals(FieldValue.Text(""), values["dressCode"], "a field the draft left empty stays empty — no default")
        assertEquals(listOf("a", "b"), FormEngine.imageIds(wedding.fields, values))
    }

    @Test
    fun savingRefusesOnlyMalformedValues() {
        val values = FormEngine.initialValues(proposal.fields, data = null, newId = ::newId)
            .withValueAt(FieldPath("partnerName"), FieldValue.Text("x".repeat(41)))

        val errors = FormEngine.validate(proposal.fields, values, requireComplete = false)
        assertEquals(mapOf(FieldPath("partnerName") to "Keep it to 40 characters"), errors, "missing required fields are fine in a draft")
    }

    @Test
    fun publishingNeedsEveryRequiredFieldAndEachMinimum() {
        val values = FormEngine.initialValues(proposal.fields, data = null, newId = ::newId)
        val errors = FormEngine.validate(proposal.fields, values, requireComplete = true)

        assertEquals("Required", errors[FieldPath("partnerName")])
        assertEquals("Add at least 3 moments", errors[FieldPath("memories")])
        val firstEntry = (values["memories"] as FieldValue.Group).entries.first().id
        assertEquals("Required", errors[FieldPath("memories", firstEntry, "title")])
        assertNull(errors[FieldPath("memories", firstEntry, "image")], "the photo is optional")
        assertNull(errors[FieldPath("question")], "the question has its default")
        assertNull(errors[FieldPath("closingNote")])
    }

    @Test
    fun numbersChoicesAndColoursAreChecked() {
        val values = FormEngine.initialValues(gift.fields, null, ::newId)
        assertEquals(
            "Enter a number from 1 to 120",
            FormEngine.validate(gift.fields, values.withValueAt(FieldPath("age"), FieldValue.Number("130")), false)[FieldPath("age")],
        )
        assertEquals(
            "Enter a number",
            FormEngine.validate(gift.fields, values.withValueAt(FieldPath("age"), FieldValue.Number("ten")), false)[FieldPath("age")],
        )
        assertEquals(
            "Pick one",
            FormEngine.validate(gift.fields, values.withValueAt(FieldPath("accent"), FieldValue.Choice("neon")), false)[FieldPath("accent")],
        )
        assertEquals("#e8a0b4", FormEngine.normalizeHex("E8A0B4"))
        assertNull(FormEngine.normalizeHex("#e8a0b"))
    }

    @Test
    fun photoCountsFollowMinAndMaxItems() {
        val babyPhotos = FieldDefinition(key = "photos", type = FieldType.IMAGE_LIST, label = "Photos", required = true, minItems = 4, maxItems = 4)
        val three = FieldValue.Photos(List(3) { PhotoSlot.Uploaded("s$it", "i$it", null, null) })
        val five = FieldValue.Photos(List(5) { PhotoSlot.Uploaded("s$it", "i$it", null, null) })

        assertEquals("Add 4 photos", FormEngine.validate(listOf(babyPhotos), mapOf("photos" to three), true)[FieldPath("photos")])
        assertTrue(FormEngine.validate(listOf(babyPhotos), mapOf("photos" to three), false).isEmpty())
        assertEquals("Up to 4 photos", FormEngine.validate(listOf(babyPhotos), mapOf("photos" to five), false)[FieldPath("photos")])
    }

    @Test
    fun theCoverIsTheFirstPhotoInFormOrder() {
        val values = FormEngine.initialValues(proposal.fields, null, ::newId)
        val entries = (values["memories"] as FieldValue.Group).entries
        val withPhotos = values.withValueAt(
            FieldPath("memories"),
            FieldValue.Group(
                listOf(
                    GroupEntry(entries[0].id, entries[0].values + ("image" to FieldValue.Photo(PhotoSlot.Uploaded("a", "first", null, null)))),
                    GroupEntry(entries[1].id, entries[1].values + ("image" to FieldValue.Photo(PhotoSlot.Uploaded("b", "second", null, null)))),
                ),
            ),
        )
        assertEquals("first", FormEngine.imageIds(proposal.fields, withPhotos).first())
    }

    @Test
    fun dateTimesKeepTheirOffset() {
        val values = FormEngine.initialValues(
            wedding.fields,
            kotlinx.serialization.json.Json.parseToJsonElement("""{"ceremonyAt":"2026-12-06T18:30:00-04:00"}""").jsonObject,
            ::newId,
        )
        val ceremony = values["ceremonyAt"] as FieldValue.DateTime
        assertEquals(LocalDate(2026, 12, 6), ceremony.dateTime?.date)
        assertEquals("2026-12-06T18:30:00-04:00", FormEngine.formatDateTime(ceremony))
        assertEquals("2026-12-06T18:30:00+00:00", FormEngine.formatDateTime(ceremony.copy(offset = UtcOffset(hours = 0))))
    }
}
