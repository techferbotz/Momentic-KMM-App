package com.ferbotz.momentic

import com.ferbotz.momentic.presentation.template.AddItem
import com.ferbotz.momentic.presentation.template.AddKind
import com.ferbotz.momentic.presentation.template.whatYoullAdd
import kotlin.test.Test
import kotlin.test.assertEquals

/** The template screen's "What you'll add", from the live templates' real field contracts. */
class WhatYoullAddTest {

    @Test
    fun theQuestion_namesTheMomentsAndHowManyItTakes() {
        assertEquals(
            listOf(
                AddItem(AddKind.Person, "Their name"),
                AddItem(AddKind.Photos, "Your moments", min = 3, max = 7),
                AddItem(AddKind.Words, "The question"),
                AddItem(AddKind.Note, "After the question"),
            ),
            whatYoullAdd(TemplateFixtures.template(TemplateFixtures.proposal).fields),
        )
    }

    @Test
    fun theInvitation_leavesTheRsvpSwitchToItsOwnCard() {
        assertEquals(
            listOf(
                AddItem(AddKind.Person, "First name"),
                AddItem(AddKind.Person, "Second name"),
                AddItem(AddKind.When, "Ceremony date and time"),
                AddItem(AddKind.Place, "Venue"),
                AddItem(AddKind.Note, "Invitation note"),
                AddItem(AddKind.Photos, "Photos", max = 8),
                AddItem(AddKind.Words, "Dress code"),
            ),
            whatYoullAdd(TemplateFixtures.template(TemplateFixtures.wedding).fields),
        )
    }

    @Test
    fun openTheSurprise_countsOnlyLists() {
        assertEquals(
            listOf(
                AddItem(AddKind.Person, "Their name"),
                AddItem(AddKind.Words, "Turning"),
                AddItem(AddKind.Note, "Message inside"),
                AddItem(AddKind.Photos, "Photos", max = 6),
                AddItem(AddKind.Choice, "Ribbon colour"),
            ),
            whatYoullAdd(TemplateFixtures.template(TemplateFixtures.giftReveal).fields),
        )
    }
}
