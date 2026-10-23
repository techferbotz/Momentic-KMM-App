package com.ferbotz.momentic

import com.ferbotz.momentic.data.mapper.toDomain
import com.ferbotz.momentic.data.model.TemplateDetail
import com.ferbotz.momentic.data.network.appJson
import com.ferbotz.momentic.data.network.dto.TemplateDetailDto

/** Real `GET /api/v1/templates/:id` payloads (`data`), as served by momentic.ferbotz.com on 2026-10-23. */
object TemplateFixtures {

    const val ORIGIN = "https://momentic.ferbotz.com"

    val proposal: String = """
        {"id":"proposal_memory_walk","categoryId":"love","name":"The Question","tagline":"Memories, one by one, then the question","thumbnailUrl":"/templates/proposal_memory_walk/thumb.webp","tierKey":"C","displayPerDay":"from Rs.200/day","tags":["proposal","romantic","timeline","cinematic"],"previewCode":"pv_proposal_memory_walk","description":"Your story unfolds a memory at a time. At the end, the page asks the only question that matters.","previewImages":["/templates/proposal_memory_walk/p1.webp"],"supportsRsvp":false,"fields":[{"key":"partnerName","type":"text","label":"Their name","required":true,"maxLength":40},{"key":"memories","type":"groupList","label":"Your moments","helpText":"Three to seven moments, in the order they happened","required":true,"minItems":3,"maxItems":7,"fields":[{"key":"title","type":"text","label":"Moment","required":true,"maxLength":60},{"key":"text","type":"longText","label":"What happened","required":true,"maxLength":300},{"key":"image","type":"image","label":"Photo","required":false,"aspectRatio":"4:5"}]},{"key":"question","type":"text","label":"The question","required":true,"maxLength":80,"defaultValue":"Will you marry me?"},{"key":"closingNote","type":"longText","label":"After the question","required":false,"maxLength":400}],"pricing":{"tierKey":"C","label":"Premium","displayPerDay":"from Rs.200/day","bundles":[{"days":7,"productId":"publish_tierC_7d"},{"days":1,"productId":"publish_tierC_1d"},{"days":3,"productId":"publish_tierC_3d"},{"days":30,"productId":"publish_tierC_30d"},{"days":90,"productId":"publish_tierC_90d"}]}}
    """.trimIndent()

    val wedding: String = """
        {"id":"wedding_invitation_classic","categoryId":"wedding","name":"The Invitation","tagline":"An unfolding wedding invitation","thumbnailUrl":"/templates/wedding_invitation_classic/thumb.webp","tierKey":"C","displayPerDay":"from Rs.200/day","tags":["wedding","invitation","rsvp","map"],"previewCode":"pv_wedding_invitation_classic","description":"An envelope that opens into your ceremony details, a photo, a map and an RSVP the couple can read back.","previewImages":["/templates/wedding_invitation_classic/p1.webp"],"supportsRsvp":true,"fields":[{"key":"partnerOne","type":"text","label":"First name","required":true,"maxLength":40},{"key":"partnerTwo","type":"text","label":"Second name","required":true,"maxLength":40},{"key":"ceremonyAt","type":"dateTime","label":"Ceremony date and time","required":true},{"key":"venue","type":"location","label":"Venue","required":true},{"key":"invitationNote","type":"longText","label":"Invitation note","required":true,"maxLength":600},{"key":"photos","type":"imageList","label":"Photos","required":false,"minItems":0,"maxItems":8,"aspectRatio":"3:4"},{"key":"dressCode","type":"text","label":"Dress code","required":false,"maxLength":80},{"key":"rsvpEnabled","type":"boolean","label":"Collect RSVPs","required":false,"defaultValue":true}],"pricing":{"tierKey":"C","label":"Premium","displayPerDay":"from Rs.200/day","bundles":[{"days":1,"productId":"publish_tierC_1d"},{"days":3,"productId":"publish_tierC_3d"},{"days":7,"productId":"publish_tierC_7d"},{"days":30,"productId":"publish_tierC_30d"},{"days":90,"productId":"publish_tierC_90d"}]}}
    """.trimIndent()

    val giftReveal: String = """
        {"id":"birthday_gift_reveal","categoryId":"birthday","name":"Open the Surprise","tagline":"A gift box they tap to unwrap","thumbnailUrl":"/templates/birthday_gift_reveal/thumb.webp","tierKey":"A","displayPerDay":"from Rs.50/day","tags":["surprise","confetti","interactive"],"previewCode":"pv_birthday_gift_reveal","description":"The page opens as a wrapped box. Tapping it bursts into confetti and reveals your message and photos.","previewImages":["/templates/birthday_gift_reveal/p1.webp"],"supportsRsvp":false,"fields":[{"key":"recipientName","type":"text","label":"Their name","required":true,"maxLength":40},{"key":"age","type":"number","label":"Turning","required":false,"min":1,"max":120},{"key":"message","type":"longText","label":"Message inside","required":true,"maxLength":600},{"key":"photos","type":"imageList","label":"Photos","required":false,"minItems":0,"maxItems":6,"aspectRatio":"4:5"},{"key":"accent","type":"choice","label":"Ribbon colour","required":false,"defaultValue":"blush","options":[{"value":"blush","label":"Blush"},{"value":"gold","label":"Gold"},{"value":"midnight","label":"Midnight"}]}],"pricing":{"tierKey":"A","label":"Simple","displayPerDay":"from Rs.50/day","bundles":[{"days":1,"productId":"publish_tierA_1d"},{"days":3,"productId":"publish_tierA_3d"},{"days":7,"productId":"publish_tierA_7d"},{"days":30,"productId":"publish_tierA_30d"},{"days":90,"productId":"publish_tierA_90d"}]}}
    """.trimIndent()

    /** `GET /api/v1/feed` (`data`), trimmed to two sections. */
    val feed: String = """
        {"sections":[{"category":{"id":"birthday","name":"Birthday","emoji":"🎂"},"templates":[{"id":"birthday_for_babies","categoryId":"birthday","name":"Birthday for Babies","tagline":"A soft, playful first-birthday page","thumbnailUrl":"/templates/birthday_for_babies/thumb.webp","tierKey":"B","displayPerDay":"from Rs.100/day","tags":["baby","first birthday","pastel","photos"],"previewCode":"pv_birthday_for_babies"}]},{"category":{"id":"friendship","name":"Friendship","emoji":"👫"},"templates":[]},{"category":{"id":"greeting","name":"Greeting","emoji":"💌"},"templates":[{"id":"greeting_open_letter","categoryId":"greeting","name":"The Letter","tagline":"A letter that unseals itself","thumbnailUrl":"/templates/greeting_open_letter/thumb.webp","tierKey":"A","displayPerDay":"from Rs.50/day","tags":["greeting","letter"],"previewCode":"pv_greeting_open_letter"},{"categoryId":"greeting","name":"No id, dropped"}]}]}
    """.trimIndent()

    fun template(json: String): TemplateDetail =
        appJson.decodeFromString<TemplateDetailDto>(json).toDomain(ORIGIN)!!
}
