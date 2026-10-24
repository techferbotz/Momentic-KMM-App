package com.ferbotz.momentic.theme

import androidx.compose.ui.unit.dp

/** The 8-point grid. Screens keep a 20 dp gutter. */
object MomenticSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val gutter = 20.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 40.dp
    val xxxl = 48.dp
    val huge = 64.dp
}

/** Fixed sizes the components share. Anything tappable is at least [minTouchTarget]. */
object MomenticSizes {
    val minTouchTarget = 48.dp
    val buttonHeight = 52.dp
    val compactButtonHeight = 44.dp
    val chipHeight = 48.dp
    val iconButton = 48.dp
    val icon = 24.dp
    val iconSmall = 20.dp
    val iconTiny = 16.dp
    val topBarHeight = 56.dp
    val bottomBarHeight = 64.dp
    val carouselCardWidth = 160.dp
    val momentCover = 72.dp
}
