package com.ferbotz.momentic

import com.ferbotz.momentic.data.mapper.toCreationDto
import com.ferbotz.momentic.data.mapper.toDomain
import com.ferbotz.momentic.data.mapper.toRsvpPage
import com.ferbotz.momentic.data.model.Attendance
import com.ferbotz.momentic.data.model.CreationStatus
import com.ferbotz.momentic.data.model.DailyViews
import com.ferbotz.momentic.data.model.RsvpSummary
import com.ferbotz.momentic.data.network.appJson
import com.ferbotz.momentic.data.network.dto.CreationsPageDto
import com.ferbotz.momentic.data.network.dto.StatsDto
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CreationParsingTest {

    private val creationJson = """
        {"id":"c1","templateId":"greeting_open_letter","templateName":"The Letter","status":"PUBLISHED",
         "data":{"recipientName":"Divya"},"coverImageId":null,"shareCode":"aK3x9Qm2",
         "shareUrl":"https://momentic.ferbotz.com/aK3x9Qm2","publishedAt":"2026-10-20T10:00:00Z",
         "expiresAt":"2026-10-27T10:00:00Z","isExpired":false,"viewCount":42,
         "createdAt":"2026-10-19T09:00:00Z","updatedAt":"2026-10-20T10:00:00Z"}
    """.trimIndent()

    private fun json(text: String): JsonObject = appJson.parseToJsonElement(text).jsonObject

    @Test
    fun aCreationMapsItsServerOwnedFields() {
        val creation = json(creationJson).toCreationDto().toDomain()!!
        assertEquals(CreationStatus.PUBLISHED, creation.status)
        assertTrue(creation.isLive)
        assertEquals("aK3x9Qm2", creation.shareCode)
        assertEquals(42L, creation.viewCount)
        assertEquals("Divya", creation.data["recipientName"]?.toString()?.trim('"'))
        assertEquals("2026-10-27T10:00:00Z", creation.expiresAt.toString())
    }

    @Test
    fun aCreationWrappedInTheResponseIsReadTheSame() {
        val bare = json(creationJson).toCreationDto()
        val wrapped = json("""{"creation":$creationJson,"shareCode":"aK3x9Qm2"}""").toCreationDto()
        assertEquals(bare, wrapped)
    }

    @Test
    fun anUnknownStatusOrMissingIdIsHandled() {
        assertEquals(CreationStatus.UNKNOWN, json("""{"id":"c2","status":"ARCHIVED"}""").toCreationDto().toDomain()?.status)
        assertNull(json("""{"templateId":"x"}""").toCreationDto().toDomain())
    }

    @Test
    fun aPageCarriesItsOpaqueCursor() {
        val page = appJson.decodeFromString<CreationsPageDto>("""{"creations":[$creationJson],"nextCursor":"bzox"}""").toDomain()
        assertEquals(1, page.items.size)
        assertEquals("bzox", page.nextCursor)
        assertTrue(page.hasMore)
        assertFalse(appJson.decodeFromString<CreationsPageDto>("""{"creations":[],"nextCursor":null}""").toDomain().hasMore)
    }

    @Test
    fun statsReadBareCountsOrDatedEntries() {
        val bare = appJson.decodeFromString<StatsDto>(
            """{"totalViews":12,"daysRemaining":3,"expiresAt":"2026-10-27T10:00:00Z","isExpired":false,"series":[1,0,5],"rsvp":null}""",
        ).toDomain()
        assertEquals(listOf(1L, 0L, 5L), bare.dailyViews.map { it.views })
        assertNull(bare.rsvp)

        val dated = appJson.decodeFromString<StatsDto>(
            """{"totalViews":3,"series":[{"date":"2026-10-20","views":2},{"day":"2026-10-21","count":1}],"rsvp":{"YES":3,"no":1,"maybe":2,"guests":7}}""",
        ).toDomain()
        assertEquals(DailyViews(LocalDate(2026, 10, 20), 2), dated.dailyViews.first())
        assertEquals(RsvpSummary(yes = 3, no = 1, maybe = 2, guests = 7), dated.rsvp)
        assertEquals(6, dated.rsvp?.responses)
    }

    @Test
    fun rsvpPagesReadTheirItemsAndSummary() {
        val page = json(
            """
            {"rsvps":[{"id":"r1","guestName":"Asha","attending":"YES","guestCount":2,"message":"Can't wait!","createdAt":"2026-10-21T08:00:00Z"},
                      {"guestName":"Ravi","attending":"maybe"}],
             "nextCursor":null,"summary":{"yes":1,"no":0,"maybe":1}}
            """.trimIndent(),
        ).toRsvpPage()

        assertEquals(2, page.page.items.size)
        assertEquals(Attendance.YES, page.page.items[0].attending)
        assertEquals(2, page.page.items[0].guestCount)
        assertEquals(Attendance.MAYBE, page.page.items[1].attending)
        assertEquals(1, page.page.items[1].guestCount, "a guest is one person unless they say otherwise")
        assertNotNull(page.summary)
        assertFalse(page.page.hasMore)
    }
}
