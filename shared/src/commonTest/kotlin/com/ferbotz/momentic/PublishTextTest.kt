package com.ferbotz.momentic

import com.ferbotz.momentic.presentation.publish.liveUntil
import com.ferbotz.momentic.presentation.publish.perDayPrice
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

/** The paywall's "₹86 a day" and "live until 30 October". */
class PublishTextTest {

    @Test
    fun perDay_isWrittenTheWayTheStoreWroteThePrice() {
        assertEquals("₹86", perDayPrice("₹599.00", 599_000_000, 7))
        assertEquals("₹67", perDayPrice("₹1,999.00", 1_999_000_000, 30))
        assertEquals("₹149", perDayPrice("₹149", 149_000_000, 1))
        assertEquals("$0.71", perDayPrice("$4.99", 4_990_000, 7))
        assertEquals("0,71 €", perDayPrice("4,99 €", 4_990_000, 7))
        assertEquals("US$1.43", perDayPrice("US$9.99", 9_990_000, 7))
    }

    @Test
    fun perDay_needsANumberAndSomeDays() {
        assertNull(perDayPrice("Free", 0, 7))
        assertNull(perDayPrice("₹599.00", 599_000_000, 0))
    }

    @Test
    fun liveUntil_countsFromNow_orFromTheEndOfALiveLink() {
        val ist = TimeZone.of("Asia/Kolkata")
        val now = Instant.parse("2026-10-23T09:00:00Z")
        assertEquals(LocalDate(2026, 10, 30), liveUntil(7, now, currentExpiry = null, ist))
        assertEquals(LocalDate(2026, 11, 7), liveUntil(7, now, currentExpiry = Instant.parse("2026-10-31T09:00:00Z"), ist))
        assertEquals(LocalDate(2026, 10, 30), liveUntil(7, now, currentExpiry = Instant.parse("2026-10-01T09:00:00Z"), ist), "an ended link starts again from now")
    }
}
