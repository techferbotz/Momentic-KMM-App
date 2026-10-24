package com.ferbotz.momentic

import com.ferbotz.momentic.data.media.CropRect
import com.ferbotz.momentic.data.media.PhotoFraming
import com.ferbotz.momentic.data.media.framedCrop
import com.ferbotz.momentic.data.media.turnedSize
import com.ferbotz.momentic.data.model.AspectRatio
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class PhotoFramingTest {

    @Test
    fun withoutAFramingThePhotoIsCentred() {
        // A square photo at 4:5 keeps its full height and trims the sides to 800.
        assertContentEquals(intArrayOf(100, 0, 800, 1000), framedCrop(1000, 1000, crop = null, AspectRatio(4, 5)))
    }

    @Test
    fun theChosenCropIsKeptInPixels() {
        val crop = CropRect(left = 0.25f, top = 0.1f, width = 0.5f, height = 0.625f)
        assertContentEquals(intArrayOf(400, 100, 800, 625), framedCrop(1600, 1000, crop, AspectRatio(4, 5)))
    }

    @Test
    fun aCropPastTheEdgeIsPulledBackInside() {
        val crop = CropRect(left = 0.8f, top = 0.9f, width = 0.5f, height = 0.5f)
        assertContentEquals(intArrayOf(500, 500, 500, 500), framedCrop(1000, 1000, crop, aspectRatio = null))
    }

    @Test
    fun anOddNumberOfTurnsSwapsTheSides() {
        assertEquals(3000 to 4000, turnedSize(4000, 3000, quarterTurns = 1))
        assertEquals(4000 to 3000, turnedSize(4000, 3000, quarterTurns = 2))
        assertEquals(3000 to 4000, turnedSize(4000, 3000, quarterTurns = 3))
    }

    @Test
    fun turnsWrapAround() {
        assertEquals(1, PhotoFraming(quarterTurns = 5).turns)
        assertEquals(3, PhotoFraming(quarterTurns = -1).turns)
    }
}
