package com.ferbotz.momentic.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Corner radii from the canvas: 20 for cards and sheets, 14 for inputs, fully round pills. */
object MomenticShapes {
    val card: Shape = RoundedCornerShape(20.dp)
    val sheet: Shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    val input: Shape = RoundedCornerShape(14.dp)

    /** Small photos inside rows (group entries, purchases). */
    val thumb: Shape = RoundedCornerShape(10.dp)

    /** Buttons, chips, badges, the segmented control. */
    val pill: Shape = CircleShape

    internal val material: Shapes = Shapes(
        extraSmall = RoundedCornerShape(10.dp),
        small = RoundedCornerShape(14.dp),
        medium = RoundedCornerShape(20.dp),
        large = RoundedCornerShape(20.dp),
        extraLarge = RoundedCornerShape(20.dp),
    )
}
