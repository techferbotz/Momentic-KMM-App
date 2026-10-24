package com.ferbotz.momentic

import com.ferbotz.momentic.data.model.Creation
import com.ferbotz.momentic.data.model.CreationStatus
import com.ferbotz.momentic.presentation.moments.MomentStatus
import com.ferbotz.momentic.presentation.moments.momentStatus
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class MomentStatusTest {

    private val now = Instant.parse("2026-10-23T10:00:00Z")

    @Test
    fun aDraftIsADraft() {
        assertEquals(MomentStatus.Draft, creation(CreationStatus.DRAFT).momentStatus(now, TimeZone.UTC))
    }

    @Test
    fun anUnknownStatusShowsNoChip() {
        assertNull(creation(CreationStatus.UNKNOWN).momentStatus(now, TimeZone.UTC))
    }

    @Test
    fun anExpiredMomentHasEnded() {
        val flagged = creation(CreationStatus.PUBLISHED, expiresAt = "2026-10-30T10:00:00Z", isExpired = true)
        val lapsed = creation(CreationStatus.PUBLISHED, expiresAt = "2026-10-22T10:00:00Z", isExpired = false)
        assertEquals(MomentStatus.Ended, flagged.momentStatus(now, TimeZone.UTC))
        assertEquals(MomentStatus.Ended, lapsed.momentStatus(now, TimeZone.UTC))
    }

    @Test
    fun theLastTwoDaysWarn() {
        val tonight = creation(CreationStatus.PUBLISHED, expiresAt = "2026-10-23T20:00:00Z")
        val tomorrow = creation(CreationStatus.PUBLISHED, expiresAt = "2026-10-24T09:00:00Z")
        assertEquals(MomentStatus.EndingSoon(0), tonight.momentStatus(now, TimeZone.UTC))
        assertEquals(MomentStatus.EndingSoon(1), tomorrow.momentStatus(now, TimeZone.UTC))
    }

    @Test
    fun otherwiseItCountsTheDaysLeft() {
        val inFiveDays = creation(CreationStatus.PUBLISHED, expiresAt = "2026-10-28T09:00:00Z")
        val noEndDate = creation(CreationStatus.PUBLISHED, expiresAt = null)
        assertEquals(MomentStatus.Live(5), inFiveDays.momentStatus(now, TimeZone.UTC))
        assertEquals(MomentStatus.Live(null), noEndDate.momentStatus(now, TimeZone.UTC))
    }

    @Test
    fun daysAreCalendarDaysWhereTheCreatorIs() {
        // 01:30 on the 24th in India; the link ends at 22:30 that same Indian day.
        val lateEvening = Instant.parse("2026-10-23T20:00:00Z")
        val endsLaterToday = creation(CreationStatus.PUBLISHED, expiresAt = "2026-10-24T17:00:00Z")
        assertEquals(MomentStatus.EndingSoon(0), endsLaterToday.momentStatus(lateEvening, TimeZone.of("Asia/Kolkata")))
        assertEquals(MomentStatus.EndingSoon(1), endsLaterToday.momentStatus(lateEvening, TimeZone.UTC))
    }

    private fun creation(status: CreationStatus, expiresAt: String? = null, isExpired: Boolean = false) = Creation(
        id = "cr_1",
        templateId = "tpl_question",
        templateName = "The Question",
        status = status,
        data = JsonObject(emptyMap()),
        coverImageId = null,
        shareCode = if (status == CreationStatus.PUBLISHED) "aK3x9Qm2" else null,
        shareUrl = null,
        publishedAt = null,
        expiresAt = expiresAt?.let(Instant::parse),
        isExpired = isExpired,
        viewCount = 0,
        createdAt = null,
        updatedAt = null,
    )
}
