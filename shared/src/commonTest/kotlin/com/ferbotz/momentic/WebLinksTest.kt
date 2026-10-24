package com.ferbotz.momentic

import com.ferbotz.momentic.data.util.isEmailAddress
import com.ferbotz.momentic.data.util.isWebUrl
import com.ferbotz.momentic.data.util.mailtoUri
import com.ferbotz.momentic.data.util.resolveWebUrl
import com.ferbotz.momentic.data.util.telUri
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WebLinksTest {

    private val origin = "https://momentic.ferbotz.com"

    @Test
    fun templateArtResolvesAgainstTheWebOrigin_notTheApiBase() {
        // Contract BE-002: thumbnails are web paths served by the renderer.
        assertEquals(
            "https://momentic.ferbotz.com/templates/greeting_open_letter/thumb.webp",
            resolveWebUrl(origin, "/templates/greeting_open_letter/thumb.webp"),
        )
        assertEquals("https://momentic.ferbotz.com/samples/wed-1.webp", resolveWebUrl("$origin/", "samples/wed-1.webp"))
    }

    @Test
    fun absoluteUrlsPassThrough_blankPathsResolveToNull() {
        val s3 = "https://momentica-media.s3.ap-south-1.amazonaws.com/img/abc/full.webp"
        assertEquals(s3, resolveWebUrl(origin, s3))
        assertNull(resolveWebUrl(origin, null))
        assertNull(resolveWebUrl(origin, "   "))
    }

    @Test
    fun onlyHttpUrlsWithAHostAreWebUrls() {
        assertTrue(isWebUrl("https://momentic.ferbotz.com/aK3x9Qm2"))
        assertFalse(isWebUrl("javascript:alert(1)"))
        assertFalse(isWebUrl("https:///nohost"))
        assertFalse(isWebUrl("https://a b.com"))
    }

    @Test
    fun mailtoIsBuiltOnlyFromPlainAddresses() {
        assertTrue(isEmailAddress("support@ferbotz.com"))
        assertFalse(isEmailAddress("a@b.com?cc=evil@x.com"))
        assertEquals("mailto:support@ferbotz.com?subject=Hi%20there", mailtoUri("support@ferbotz.com", subject = "Hi there"))
    }

    @Test
    fun telUri_keepsTheDigitsAndALeadingPlus() {
        assertEquals("tel:+919876543210", telUri("+91 98765 43210"))
        assertEquals("tel:02227654321", telUri("(022) 2765-4321"))
        assertEquals(null, telUri("call me"))
    }
}
