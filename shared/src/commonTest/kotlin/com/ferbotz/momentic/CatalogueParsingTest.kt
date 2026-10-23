package com.ferbotz.momentic

import com.ferbotz.momentic.data.mapper.toDomain
import com.ferbotz.momentic.data.model.AspectRatio
import com.ferbotz.momentic.data.model.FieldType
import com.ferbotz.momentic.data.network.appJson
import com.ferbotz.momentic.data.network.dto.FeedDto
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CatalogueParsingTest {

    @Test
    fun feedResolvesArtAgainstTheWebOrigin_andDropsWhatItCantUse() {
        val sections = appJson.decodeFromString<FeedDto>(TemplateFixtures.feed).toDomain(TemplateFixtures.ORIGIN)

        assertEquals(listOf("birthday", "greeting"), sections.map { it.category.id }, "an empty category has nothing to tap")
        val baby = sections.first().templates.single()
        assertEquals("https://momentic.ferbotz.com/templates/birthday_for_babies/thumb.webp", baby.thumbnailUrl)
        assertEquals("from Rs.100/day", baby.displayPerDay)
        assertEquals("pv_birthday_for_babies", baby.previewCode)
        assertEquals(1, sections.last().templates.size, "a card without an id is dropped, not fatal")
    }

    @Test
    fun theFieldContractMapsEveryProperty() {
        val template = TemplateFixtures.template(TemplateFixtures.proposal)

        assertEquals(listOf("partnerName", "memories", "question", "closingNote"), template.fields.map { it.key })
        val name = template.fields[0]
        assertEquals(FieldType.TEXT, name.type)
        assertTrue(name.required)
        assertEquals(40, name.maxLength)

        val memories = template.fields[1]
        assertEquals(FieldType.GROUP_LIST, memories.type)
        assertEquals(3, memories.minItems)
        assertEquals(7, memories.maxItems)
        assertEquals("Three to seven moments, in the order they happened", memories.helpText)
        assertEquals(listOf("title", "text", "image"), memories.fields.map { it.key })
        assertEquals(AspectRatio(4, 5), memories.fields[2].aspectRatio)
        assertFalse(memories.fields[2].required)

        assertEquals(JsonPrimitive("Will you marry me?"), template.fields[2].defaultValue)
        assertTrue(template.canBeFilledIn)
    }

    @Test
    fun pricingIsSortedByDuration_andArtIsAbsolute() {
        val template = TemplateFixtures.template(TemplateFixtures.proposal)

        assertEquals(listOf(1, 3, 7, 30, 90), template.pricing.bundles.map { it.days })
        assertEquals("publish_tierC_7d", template.pricing.bundles[2].productId)
        assertEquals("Premium", template.pricing.label)
        assertEquals(listOf("https://momentic.ferbotz.com/templates/proposal_memory_walk/p1.webp"), template.previewImageUrls)
    }

    @Test
    fun choiceOptionsAndCropHints() {
        val template = TemplateFixtures.template(TemplateFixtures.giftReveal)
        val accent = template.fields.single { it.key == "accent" }
        assertEquals(listOf("blush", "gold", "midnight"), accent.options.map { it.value })
        assertEquals("Gold", accent.options[1].label)
        assertEquals(0.8f, template.fields.single { it.key == "photos" }.aspectRatio?.ratio)
        assertEquals(1.0, template.fields.single { it.key == "age" }.min)
    }

    @Test
    fun aRequiredFieldOfAnUnknownTypeMeansThisBuildNeedsUpdating() {
        val required = TemplateFixtures.giftReveal.replace(
            """"fields":[""",
            """"fields":[{"key":"signature","type":"handwriting","label":"Sign it","required":true},""",
        )
        val optional = required.replace(""""required":true},{"key":"recipientName"""", """"required":false},{"key":"recipientName"""")

        assertFalse(TemplateFixtures.template(required).canBeFilledIn)
        assertEquals(FieldType.UNKNOWN, TemplateFixtures.template(required).fields.first().type)
        assertTrue(TemplateFixtures.template(optional).canBeFilledIn, "an optional unknown field can just be skipped")
    }

    @Test
    fun aGroupNeverContainsAGroup() {
        val nested = TemplateFixtures.proposal.replace(
            """{"key":"image","type":"image","label":"Photo","required":false,"aspectRatio":"4:5"}""",
            """{"key":"image","type":"image","label":"Photo","required":false,"aspectRatio":"4:5"},{"key":"inner","type":"groupList","label":"Inner","fields":[]}""",
        )
        val memories = TemplateFixtures.template(nested).fields.single { it.key == "memories" }
        assertEquals(listOf("title", "text", "image"), memories.fields.map { it.key })
        assertNull(AspectRatio.parse("4x5"))
    }
}
