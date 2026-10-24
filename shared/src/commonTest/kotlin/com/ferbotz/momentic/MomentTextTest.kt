package com.ferbotz.momentic

import com.ferbotz.momentic.presentation.moments.DayAgo
import com.ferbotz.momentic.presentation.moments.dayAgo
import com.ferbotz.momentic.presentation.moments.interimTitle
import com.ferbotz.momentic.presentation.util.initials
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

/** What a moment row says until creations carry a title, and how long ago it changed. */
class MomentTextTest {

    private fun data(json: String) = Json.parseToJsonElement(json).jsonObject

    @Test
    fun theFirstShortLineNamesTheMoment() {
        assertEquals("Divya", creation("c1", "t", data("""{"partnerName":" Divya ","memories":[],"question":"Will you marry me?"}""")).interimTitle())
        assertEquals("Asha", creation("c1", "t", data("""{"age":3,"message":"A very long message that goes on and on, well past forty characters","recipientName":"Asha"}""")).interimTitle())
        assertNull(creation("c1", "t", data("""{"photos":[{"imageId":"x"}],"note":"line one\nline two"}""")).interimTitle())
        assertEquals("Ananya", creation("c1", "t", data("""{"ceremonyAt":"2026-12-12T19:30:00+05:30","ribbon":"#B84D66","partnerOne":"Ananya"}""")).interimTitle())
    }

    @Test
    fun daysAgo_inCalendarDays() {
        val ist = TimeZone.of("Asia/Kolkata")
        val now = Instant.parse("2026-10-24T06:00:00Z")
        assertEquals(DayAgo.Today, dayAgo(Instant.parse("2026-10-23T20:00:00Z"), now, ist), "01:30 on the 24th in India")
        assertEquals(DayAgo.Yesterday, dayAgo(Instant.parse("2026-10-23T06:00:00Z"), now, ist))
        assertEquals(DayAgo.DaysAgo(3), dayAgo(Instant.parse("2026-10-21T06:00:00Z"), now, ist))
        assertEquals(DayAgo.On(LocalDate(2026, 10, 2)), dayAgo(Instant.parse("2026-10-02T06:00:00Z"), now, ist))
        assertEquals(DayAgo.Today, dayAgo(Instant.parse("2026-10-25T06:00:00Z"), now, ist), "a clock that's ahead")
    }

    @Test
    fun initials_firstAndLastName() {
        assertEquals("MI", initials("Meera Iyer"))
        assertEquals("RK", initials(" Rahul  Kumar Verma Khanna "))
        assertEquals("A", initials("arjun"))
        assertEquals("?", initials("  "))
    }
}
