package com.ferbotz.momentic.feature.editor.crop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.ferbotz.momentic.data.media.PhotoFraming
import com.ferbotz.momentic.data.media.PickedPhoto
import com.ferbotz.momentic.data.model.AspectRatio
import com.ferbotz.momentic.designsystem.components.MomenticIconButton
import com.ferbotz.momentic.designsystem.components.PrimaryButton
import com.ferbotz.momentic.designsystem.components.QuietButton
import com.ferbotz.momentic.designsystem.components.Spinner
import com.ferbotz.momentic.designsystem.icons.MomenticIcons
import com.ferbotz.momentic.platform.FullScreenWindow
import com.ferbotz.momentic.platform.decodePhotoPreview
import com.ferbotz.momentic.resources.Res
import com.ferbotz.momentic.resources.crop_area
import com.ferbotz.momentic.resources.crop_cancel
import com.ferbotz.momentic.resources.crop_hint
import com.ferbotz.momentic.resources.crop_position
import com.ferbotz.momentic.resources.crop_ratio_note
import com.ferbotz.momentic.resources.crop_rotate
import com.ferbotz.momentic.resources.crop_title
import com.ferbotz.momentic.resources.crop_unreadable
import com.ferbotz.momentic.resources.crop_use
import com.ferbotz.momentic.resources.crop_use_rest
import com.ferbotz.momentic.resources.crop_zoom
import com.ferbotz.momentic.theme.Momentic
import com.ferbotz.momentic.theme.MomenticElevation
import com.ferbotz.momentic.theme.MomenticShapes
import com.ferbotz.momentic.theme.MomenticSpacing
import com.ferbotz.momentic.theme.momenticShadow
import org.jetbrains.compose.resources.stringResource

private val FrameMargin = 45.dp

/**
 * The photos picked for one field, framed one at a time: [photos] as picked, the one at [index] on
 * screen, and those [framed] so far.
 */
class CropSession(
    val aspectRatio: AspectRatio,
    val photos: List<ByteArray>,
    val index: Int = 0,
    val framed: List<PickedPhoto> = emptyList(),
) {
    val current: ByteArray get() = photos[index]
    val isLast: Boolean get() = index == photos.lastIndex

    fun next(framing: PhotoFraming?): CropSession =
        CropSession(aspectRatio, photos, index + 1, framed + PickedPhoto(current, framing))
}

/**
 * Frames the photos of [session] in turn on the [CropScreen]. [onDone] gets the ones to add — framed,
 * or as they are for "Use the rest as they are"; Cancel (or Back) stops, keeping those already framed.
 * A photo that can't be read is skipped with [onUnreadable].
 */
@Composable
fun CropFlow(
    session: CropSession,
    onAdvance: (CropSession) -> Unit,
    onDone: (List<PickedPhoto>) -> Unit,
    onUnreadable: (String) -> Unit,
) {
    var preview by remember(session.index) { mutableStateOf<ImageBitmap?>(null) }
    val unreadable = stringResource(Res.string.crop_unreadable)
    LaunchedEffect(session.index) {
        val decoded = decodePhotoPreview(session.current)
        if (decoded != null) {
            preview = decoded
        } else {
            onUnreadable(unreadable)
            val skipped = CropSession(session.aspectRatio, session.photos, session.index + 1, session.framed)
            if (session.isLast) onDone(session.framed) else onAdvance(skipped)
        }
    }
    fun use(framing: PhotoFraming?) {
        val next = session.next(framing)
        if (session.isLast) onDone(next.framed) else onAdvance(next)
    }
    val cancel = { onDone(session.framed) }
    // Its own window, so it covers the screen wherever it's composed from (a scrolling page, a sheet).
    FullScreenWindow(onDismissRequest = cancel) {
        CropScreen(
            photo = preview,
            aspectRatio = session.aspectRatio,
            position = if (session.photos.size > 1) session.index + 1 to session.photos.size else null,
            onCancel = cancel,
            onUse = ::use,
            onUseRestAsTheyAre = if (session.isLast) null else ({
                onDone(session.framed + session.photos.drop(session.index).map { PickedPhoto(it) })
            }),
        )
    }
}

/**
 * Crop one photo to the field's [aspectRatio]: the frame stays put while the photo pans (drag), zooms
 * (pinch, or the slider) and turns (the rotate button); outside the frame is veiled. "Use photo"
 * hands back how it was framed.
 */
@Composable
fun CropScreen(
    photo: ImageBitmap?,
    aspectRatio: AspectRatio,
    position: Pair<Int, Int>?,
    onCancel: () -> Unit,
    onUse: (PhotoFraming) -> Unit,
    onUseRestAsTheyAre: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val c = Momentic.colors
    val density = LocalDensity.current
    val marginPx = with(density) { FrameMargin.toPx() }
    var turns by remember(photo) { mutableIntStateOf(0) }
    var zoom by remember(photo) { mutableFloatStateOf(1f) }
    var offset by remember(photo) { mutableStateOf(Offset.Zero) }
    var area by remember { mutableStateOf(Size.Zero) }

    // The geometry of the moment, shared by the drawing, the gestures and "Use photo".
    fun photoSize(): Size? = photo?.let {
        if (turns % 2 == 1) Size(it.height.toFloat(), it.width.toFloat()) else Size(it.width.toFloat(), it.height.toFloat())
    }
    fun frame(): Size = frameIn(area, aspectRatio.ratio, marginPx)
    fun shownAt(zoomLevel: Float): Size? = photoSize()?.let { size ->
        val scale = coverScale(size, frame()) * zoomLevel
        Size(size.width * scale, size.height * scale)
    }
    fun zoomTo(level: Float, pan: Offset = Offset.Zero) {
        val target = level.coerceIn(1f, MAX_ZOOM)
        val shown = shownAt(target) ?: return
        // Zooming keeps the point under the frame's centre where it is.
        offset = clampOffset(offset * (target / zoom) + pan, shown, frame())
        zoom = target
    }

    Column(modifier.fillMaxSize().background(c.background).windowInsetsPadding(WindowInsets.safeDrawing)) {
        // Equal slots either side (as on the canvas), so the title sits in the middle.
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = MomenticSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(96.dp), contentAlignment = Alignment.CenterStart) {
                QuietButton(stringResource(Res.string.crop_cancel), onClick = onCancel)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(Res.string.crop_title), style = Momentic.type.h2, color = c.ink, textAlign = TextAlign.Center)
                if (position != null) {
                    Text(stringResource(Res.string.crop_position, position.first, position.second), style = Momentic.type.caption, color = c.inkSoft)
                }
            }
            Box(Modifier.width(96.dp), contentAlignment = Alignment.CenterEnd) {
                MomenticIconButton(
                    icon = MomenticIcons.Rotate,
                    contentDescription = stringResource(Res.string.crop_rotate),
                    onClick = {
                        turns = (turns + 1) % 4
                        zoom = 1f
                        offset = Offset.Zero
                    },
                    enabled = photo != null,
                )
            }
        }

        val areaDescription = stringResource(Res.string.crop_area, aspectRatio.toString())
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds()
                .onSizeChanged { area = it.toSize() }
                .semantics { contentDescription = areaDescription }
                .pointerInput(photo, turns) {
                    detectTransformGestures { _, pan, gestureZoom, _ -> zoomTo(zoom * gestureZoom, pan) }
                },
        ) {
            val bitmap = photo
            if (bitmap == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Spinner(color = c.rose, size = 28.dp) }
            } else {
                Canvas(Modifier.fillMaxSize()) {
                    val frame = frame()
                    val frameTopLeft = Offset((size.width - frame.width) / 2, (size.height - frame.height) / 2)
                    val photoSize = photoSize() ?: return@Canvas
                    val scale = coverScale(photoSize, frame) * zoom
                    translate(size.width / 2 + offset.x, size.height / 2 + offset.y) {
                        rotate(90f * turns, pivot = Offset.Zero) {
                            scale(scale, pivot = Offset.Zero) {
                                drawImage(
                                    bitmap,
                                    dstOffset = IntOffset(-bitmap.width / 2, -bitmap.height / 2),
                                    dstSize = IntSize(bitmap.width, bitmap.height),
                                    filterQuality = FilterQuality.High,
                                )
                            }
                        }
                    }
                    // Veil everything outside the frame.
                    val veil = Path().apply {
                        fillType = PathFillType.EvenOdd
                        addRect(Rect(Offset.Zero, size))
                        addRect(Rect(frameTopLeft, frame))
                    }
                    drawPath(veil, c.background.copy(alpha = 0.74f))
                    // The frame, its thirds and its corners.
                    val hairline = 1.dp.toPx()
                    for (third in 1..2) {
                        val x = frameTopLeft.x + frame.width * third / 3
                        val y = frameTopLeft.y + frame.height * third / 3
                        drawLine(c.surface.copy(alpha = 0.7f), Offset(x, frameTopLeft.y), Offset(x, frameTopLeft.y + frame.height), hairline)
                        drawLine(c.surface.copy(alpha = 0.7f), Offset(frameTopLeft.x, y), Offset(frameTopLeft.x + frame.width, y), hairline)
                    }
                    drawRect(c.surface, frameTopLeft, frame, style = Stroke(2.dp.toPx()))
                    val arm = 22.dp.toPx()
                    val weight = 3.dp.toPx()
                    val out = 2.dp.toPx()
                    val corners = listOf(
                        frameTopLeft to Offset(1f, 1f),
                        Offset(frameTopLeft.x + frame.width, frameTopLeft.y) to Offset(-1f, 1f),
                        Offset(frameTopLeft.x, frameTopLeft.y + frame.height) to Offset(1f, -1f),
                        Offset(frameTopLeft.x + frame.width, frameTopLeft.y + frame.height) to Offset(-1f, -1f),
                    )
                    corners.forEach { (corner, direction) ->
                        val start = Offset(corner.x - direction.x * out, corner.y - direction.y * out)
                        drawLine(c.surface, start, Offset(start.x + direction.x * arm, start.y), weight)
                        drawLine(c.surface, start, Offset(start.x, start.y + direction.y * arm), weight)
                    }
                }
            }
            Text(
                text = stringResource(Res.string.crop_ratio_note, aspectRatio.toString()),
                style = Momentic.type.captionStrong.copy(fontWeight = FontWeight.Bold),
                color = c.ink,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = MomenticSpacing.md)
                    .momenticShadow(MomenticElevation.Rest, MomenticShapes.pill)
                    .clip(MomenticShapes.pill)
                    .background(c.surface)
                    .padding(horizontal = MomenticSpacing.sm, vertical = 8.dp),
            )
            Text(
                text = stringResource(Res.string.crop_hint),
                style = Momentic.type.label,
                color = c.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = MomenticSpacing.md),
            )
        }

        Column(
            Modifier.fillMaxWidth().padding(horizontal = MomenticSpacing.gutter, vertical = MomenticSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(MomenticSpacing.xs),
        ) {
            val zoomLabel = stringResource(Res.string.crop_zoom)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MomenticSpacing.sm)) {
                Icon(MomenticIcons.Minus, contentDescription = null, tint = c.inkSoft, modifier = Modifier.size(20.dp))
                Slider(
                    value = zoom,
                    onValueChange = { zoomTo(it) },
                    valueRange = 1f..MAX_ZOOM,
                    enabled = photo != null,
                    colors = SliderDefaults.colors(thumbColor = c.rose, activeTrackColor = c.rose, inactiveTrackColor = c.blush200),
                    modifier = Modifier.weight(1f).height(48.dp).semantics { contentDescription = zoomLabel },
                )
                Icon(MomenticIcons.Plus, contentDescription = null, tint = c.inkSoft, modifier = Modifier.size(20.dp))
            }
            PrimaryButton(
                text = stringResource(Res.string.crop_use),
                onClick = {
                    val shown = shownAt(zoom) ?: return@PrimaryButton
                    onUse(PhotoFraming(quarterTurns = turns, crop = cropFor(offset, shown, frame())))
                },
                enabled = photo != null,
                modifier = Modifier.fillMaxWidth(),
            )
            if (onUseRestAsTheyAre != null) {
                QuietButton(stringResource(Res.string.crop_use_rest), onClick = onUseRestAsTheyAre, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
