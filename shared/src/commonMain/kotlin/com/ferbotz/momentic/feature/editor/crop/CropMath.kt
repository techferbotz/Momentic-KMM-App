package com.ferbotz.momentic.feature.editor.crop

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.ferbotz.momentic.data.media.CropRect

/*
 * The crop screen's geometry, in screen pixels. The photo (already turned) is drawn centred on the frame,
 * shifted by an offset and scaled so it always covers the frame; the frame keeps the field's aspect ratio.
 */

/** How far the photo can be zoomed in past "just covers the frame". */
internal const val MAX_ZOOM = 4f

/** The largest frame of [aspect] (width / height) that fits [area] with [margin] on every side. */
internal fun frameIn(area: Size, aspect: Float, margin: Float): Size {
    val availableWidth = (area.width - margin * 2).coerceAtLeast(1f)
    val availableHeight = (area.height - margin * 2).coerceAtLeast(1f)
    return if (availableWidth / availableHeight > aspect) {
        Size(availableHeight * aspect, availableHeight)
    } else {
        Size(availableWidth, availableWidth / aspect)
    }
}

/** The scale at which a [photo] just covers [frame] — zoom 1. */
internal fun coverScale(photo: Size, frame: Size): Float = maxOf(frame.width / photo.width, frame.height / photo.height)

/** [offset] (the photo's centre from the frame's) pulled back so the photo, shown at [shown], still covers [frame]. */
internal fun clampOffset(offset: Offset, shown: Size, frame: Size): Offset {
    val slackX = ((shown.width - frame.width) / 2).coerceAtLeast(0f)
    val slackY = ((shown.height - frame.height) / 2).coerceAtLeast(0f)
    return Offset(offset.x.coerceIn(-slackX, slackX), offset.y.coerceIn(-slackY, slackY))
}

/** The part of the photo inside [frame], as fractions of the photo, with the photo [shown] at [offset]. */
internal fun cropFor(offset: Offset, shown: Size, frame: Size): CropRect {
    val left = ((shown.width - frame.width) / 2 - offset.x) / shown.width
    val top = ((shown.height - frame.height) / 2 - offset.y) / shown.height
    return CropRect(
        left = left.coerceIn(0f, 1f),
        top = top.coerceIn(0f, 1f),
        width = (frame.width / shown.width).coerceIn(0.0001f, 1f),
        height = (frame.height / shown.height).coerceIn(0.0001f, 1f),
    )
}
