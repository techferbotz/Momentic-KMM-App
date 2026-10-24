package com.ferbotz.momentic

import com.ferbotz.momentic.presentation.editor.EntryNoun
import com.ferbotz.momentic.presentation.util.clockLabel
import com.ferbotz.momentic.presentation.util.compactLabel
import com.ferbotz.momentic.presentation.util.fullLabel
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class FieldKitTextTest {

    @Test
    fun aDateFieldReadsTheWayGuestsWillSeeIt() {
        assertEquals("Sat, 12 December 2026", LocalDate(2026, 12, 12).fullLabel())
        assertEquals("Sat, 12 Dec 2026", LocalDate(2026, 12, 12).compactLabel())
    }

    @Test
    fun timesAreOnATwelveHourClock() {
        assertEquals("7:30 pm", LocalTime(19, 30).clockLabel())
        assertEquals("12:05 am", LocalTime(0, 5).clockLabel())
        assertEquals("12:00 pm", LocalTime(12, 0).clockLabel())
        assertEquals("9:00 am", LocalTime(9, 0).clockLabel())
    }

    @Test
    fun aGroupsEntryIsNamedAfterItsLabel() {
        assertEquals(EntryNoun("moment", "moments"), EntryNoun.forLabel("Your moments"))
        assertEquals(EntryNoun("memory", "memories"), EntryNoun.forLabel("Memories"))
        assertEquals(EntryNoun("wish", "wishes"), EntryNoun.forLabel("Wishes"))
        assertEquals(EntryNoun("favourite song", "favourite songs"), EntryNoun.forLabel("Favourite songs"))
        assertEquals(EntryNoun("photo", "photos"), EntryNoun.forLabel("Photos"))
    }

    @Test
    fun anythingElseIsAnItem() {
        assertEquals(EntryNoun("item", "items"), EntryNoun.forLabel("Reasons I love you"))
        assertEquals(EntryNoun("item", "items"), EntryNoun.forLabel("Dress"))
        assertEquals(EntryNoun("item", "items"), EntryNoun.forLabel("Your"))
    }

    @Test
    fun theArticleFollowsTheSound() {
        assertEquals("a moment", EntryNoun.forLabel("Moments").withArticle)
        assertEquals("an item", EntryNoun.forLabel("Reasons I love you").withArticle)
        assertEquals("an evening", EntryNoun.forLabel("Evenings").withArticle)
    }
}
