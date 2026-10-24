package com.ferbotz.momentic.designsystem.fields

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ferbotz.momentic.data.model.AspectRatio
import com.ferbotz.momentic.designsystem.components.ProgressRing
import com.ferbotz.momentic.designsystem.components.SecondaryButton
import com.ferbotz.momentic.designsystem.icons.MomenticIcons
import com.ferbotz.momentic.presentation.editor.PhotoSlot
import com.ferbotz.momentic.resources.Res
import com.ferbotz.momentic.resources.photo_add
import com.ferbotz.momentic.resources.photo_add_more
import com.ferbotz.momentic.resources.photo_add_short
import com.ferbotz.momentic.resources.photo_change
import com.ferbotz.momentic.resources.photo_change_long
import com.ferbotz.momentic.resources.photo_choose_another
import com.ferbotz.momentic.resources.photo_cropped
import com.ferbotz.momentic.resources.photo_crop_hint
import com.ferbotz.momentic.resources.photo_failed
import com.ferbotz.momentic.resources.photo_numbered
import com.ferbotz.momentic.resources.photo_remove
import com.ferbotz.momentic.resources.photo_remove_numbered
import com.ferbotz.momentic.resources.photo_try_again
import com.ferbotz.momentic.resources.photo_uploaded
import com.ferbotz.momentic.resources.photo_uploading
import com.ferbotz.momentic.resources.photos_count
import com.ferbotz.momentic.resources.photos_hint
import com.ferbotz.momentic.resources.photos_hint_no_limit
import com.ferbotz.momentic.theme.Momentic
import com.ferbotz.momentic.theme.MomenticElevation
import com.ferbotz.momentic.theme.momenticShadow
import org.jetbrains.compose.resources.stringResource
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import kotlin.math.ceil

private val PhotoCorner = 14.dp
private val PhotoShape = RoundedCornerShape(PhotoCorner)
private const val ADD_TILE = "add-photo"

/** The ratio the field asks for, or 4:5 — the most common one in the templates. */
private fun AspectRatio?.orDefault(): Float = this?.ratio ?: (4f / 5f)

/**
 * One photo at the field's aspect ratio (160 wide). Empty, it's a dashed slot that opens the picker;
 * with a photo, "Change" sits on it; while uploading a ring turns over it; a failed upload says so and
 * offers "Try again" when that can work.
 */
@Composable
fun PhotoField(
    slot: PhotoSlot?,
    label: String,
    aspectRatio: AspectRatio?,
    onPick: () -> Unit,
    onRetry: (String) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    helper: String? = null,
    error: String? = null,
    width: Dp = 160.dp,
) {
    val c = Momentic.colors
    val ratio = aspectRatio.orDefault()
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel(label, required)
        Box(Modifier.width(width).aspectRatio(ratio)) {
            when (slot) {
                null -> AddPhotoTile(
                    label = stringResource(Res.string.photo_add),
                    large = true,
                    hasError = error != null,
                    onClick = onPick,
                    modifier = Modifier.fillMaxSize(),
                )
                else -> {
                    PhotoTile(
                        slot = slot,
                        description = label,
                        onRetry = { onRetry(slot.slotId) },
                        modifier = Modifier.fillMaxSize(),
                        ringLarge = true,
                    )
                    RemoveButton(stringResource(Res.string.photo_remove), onClick = { onRemove(slot.slotId) })
                    if (slot is PhotoSlot.Uploaded) {
                        ChangePill(onClick = onPick, modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp))
                    }
                }
            }
        }
        when (slot) {
            null -> FieldFooter(helper = helper ?: aspectRatio?.let { stringResource(Res.string.photo_crop_hint, it.toString()) }, error = error)
            is PhotoSlot.Uploaded -> if (error != null) FieldFooter(helper = null, error = error) else StatusLine(uploaded = true)
            is PhotoSlot.Uploading -> FieldFooter(helper = stringResource(Res.string.photo_uploading), error = error)
            is PhotoSlot.Failed -> FailedLine(
                message = slot.message,
                actionLabel = stringResource(if (slot.retryable) Res.string.photo_try_again else Res.string.photo_choose_another),
                onAction = if (slot.retryable) ({ onRetry(slot.slotId) }) else onPick,
            )
        }
    }
}

/**
 * The single-photo field laid out side by side, for a group entry's sheet: the photo (120 wide) on the
 * left; the label, its state, the crop note and "Change photo" on the right.
 */
@Composable
fun InlinePhotoField(
    slot: PhotoSlot?,
    label: String,
    aspectRatio: AspectRatio?,
    onPick: () -> Unit,
    onRetry: (String) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    error: String? = null,
) {
    val ratio = aspectRatio.orDefault()
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Bottom) {
            Box(Modifier.width(120.dp).aspectRatio(ratio)) {
                if (slot == null) {
                    AddPhotoTile(stringResource(Res.string.photo_add), large = true, hasError = error != null, onClick = onPick, modifier = Modifier.fillMaxSize())
                } else {
                    PhotoTile(slot, description = label, onRetry = { onRetry(slot.slotId) }, modifier = Modifier.fillMaxSize(), ringLarge = true)
                    RemoveButton(stringResource(Res.string.photo_remove), onClick = { onRemove(slot.slotId) })
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FieldLabel(label, required)
                when (slot) {
                    is PhotoSlot.Uploaded -> StatusLine(uploaded = true)
                    is PhotoSlot.Uploading -> Text(stringResource(Res.string.photo_uploading), style = Momentic.type.caption, color = Momentic.colors.inkSoft)
                    is PhotoSlot.Failed -> FailedLine(
                        message = "",
                        actionLabel = stringResource(if (slot.retryable) Res.string.photo_try_again else Res.string.photo_choose_another),
                        onAction = if (slot.retryable) ({ onRetry(slot.slotId) }) else onPick,
                    )
                    null -> Unit
                }
                if (aspectRatio != null) {
                    when (slot) {
                        null -> Text(stringResource(Res.string.photo_crop_hint, aspectRatio.toString()), style = Momentic.type.caption, color = Momentic.colors.inkSoft)
                        is PhotoSlot.Uploaded -> Text(stringResource(Res.string.photo_cropped, aspectRatio.toString()), style = Momentic.type.caption, color = Momentic.colors.inkSoft)
                        else -> Unit
                    }
                }
                // Empty, the slot itself is the way to add one; with a photo, a button changes it.
                if (slot is PhotoSlot.Uploaded) {
                    SecondaryButton(
                        text = stringResource(Res.string.photo_change_long),
                        onClick = onPick,
                        icon = MomenticIcons.Photo,
                        compact = true,
                    )
                }
            }
        }
        if (error != null) FieldFooter(helper = null, error = error)
    }
}

/**
 * A grid of photos (three across, at the field's ratio) with a running count; long-press a photo to drag
 * it to a new place. The add tile sits last and goes away at the limit.
 */
@Composable
fun PhotoListField(
    photos: List<PhotoSlot>,
    label: String,
    aspectRatio: AspectRatio?,
    maxItems: Int?,
    onPick: (room: Int?) -> Unit,
    onRetry: (String) -> Unit,
    onRemove: (String) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    helper: String? = null,
    error: String? = null,
) {
    val c = Momentic.colors
    val ratio = aspectRatio.orDefault()
    val room = maxItems?.let { it - photos.size }
    val canAdd = room == null || room > 0
    // The grid reorders from its own copy (it must see each move at once); the form's list catches up.
    var shown by remember { mutableStateOf(photos) }
    LaunchedEffect(photos) { shown = photos }
    val gridState = rememberLazyGridState()
    val reorder = rememberReorderableLazyGridState(gridState) { from, to ->
        if (from.key == ADD_TILE || to.key == ADD_TILE) return@rememberReorderableLazyGridState
        shown = shown.toMutableList().apply { add(to.index, removeAt(from.index)) }
        onMove(from.index, to.index)
    }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel(label, required, trailing = maxItems?.let { stringResource(Res.string.photos_count, photos.size, it) })
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val gap = 10.dp
            val cell = (maxWidth - gap * 2) / 3
            val tiles = shown.size + if (canAdd) 1 else 0
            val rows = ceil(tiles / 3.0).toInt().coerceAtLeast(1)
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                state = gridState,
                modifier = Modifier.fillMaxWidth().height(cell / ratio * rows + gap * (rows - 1)),
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalArrangement = Arrangement.spacedBy(gap),
                userScrollEnabled = false,
            ) {
                items(shown, key = { it.slotId }) { slot ->
                    val number = shown.indexOf(slot) + 1
                    ReorderableItem(reorder, key = slot.slotId) { dragging ->
                        Box(
                            Modifier
                                .aspectRatio(ratio)
                                .then(if (dragging) Modifier.momenticShadow(MomenticElevation.Raised, PhotoShape) else Modifier)
                                .then(if (dragging) Modifier.border(2.dp, c.rose, PhotoShape) else Modifier)
                                .longPressDraggableHandle(),
                        ) {
                            PhotoTile(
                                slot = slot,
                                description = stringResource(Res.string.photo_numbered, number),
                                onRetry = { onRetry(slot.slotId) },
                                modifier = Modifier.fillMaxSize(),
                                number = number,
                            )
                            RemoveButton(stringResource(Res.string.photo_remove_numbered, number), onClick = { onRemove(slot.slotId) })
                        }
                    }
                }
                if (canAdd) {
                    item(key = ADD_TILE) {
                        AddPhotoTile(
                            label = stringResource(Res.string.photo_add_short),
                            accessibilityLabel = stringResource(Res.string.photo_add_more),
                            large = false,
                            hasError = error != null && photos.isEmpty(),
                            onClick = { onPick(room) },
                            modifier = Modifier.aspectRatio(ratio),
                        )
                    }
                }
            }
        }
        val failed = photos.filterIsInstance<PhotoSlot.Failed>().firstOrNull()
        when {
            error != null -> FieldFooter(helper = null, error = error)
            failed != null -> FailedLine(
                message = failed.message,
                actionLabel = stringResource(if (failed.retryable) Res.string.photo_try_again else Res.string.photo_remove),
                onAction = if (failed.retryable) ({ onRetry(failed.slotId) }) else ({ onRemove(failed.slotId) }),
            )
            else -> FieldFooter(
                helper = helper ?: if (maxItems != null) stringResource(Res.string.photos_hint, maxItems) else stringResource(Res.string.photos_hint_no_limit),
                error = null,
            )
        }
    }
}

/** A photo in a slot: the image, or a ring while it uploads, or an alert when it couldn't. */
@Composable
fun PhotoTile(
    slot: PhotoSlot,
    description: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    number: Int? = null,
    ringLarge: Boolean = false,
    corner: Dp = PhotoCorner,
) {
    val c = Momentic.colors
    val shape = RoundedCornerShape(corner)
    Box(
        modifier = modifier
            .clip(shape)
            .background(c.blush100)
            .then(if (slot is PhotoSlot.Failed) Modifier.border(2.dp, c.error, shape) else Modifier)
            .then(
                if (slot is PhotoSlot.Failed && slot.retryable) {
                    Modifier.clickable(role = Role.Button, onClickLabel = stringResource(Res.string.photo_try_again), onClick = onRetry)
                } else {
                    Modifier
                },
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        when (slot) {
            is PhotoSlot.Uploaded -> {
                val url = slot.thumbUrl ?: slot.fullUrl
                // No address (an older draft) or one that won't load: the photo is still there, just not shown.
                var unavailable by remember(url) { mutableStateOf(url == null) }
                if (url != null) {
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        onSuccess = { unavailable = false },
                        onError = { unavailable = true },
                    )
                }
                if (unavailable) {
                    Icon(MomenticIcons.Photo, contentDescription = null, tint = c.roseSoft, modifier = Modifier.size(28.dp))
                }
                if (number != null) NumberBadge(number)
            }
            is PhotoSlot.Uploading -> {
                Box(Modifier.fillMaxSize().background(c.ink.copy(alpha = 0.08f)))
                Box(
                    Modifier.size(if (ringLarge) 64.dp else 40.dp).clip(CircleShape).background(c.surface),
                    contentAlignment = Alignment.Center,
                ) {
                    ProgressRing(progress = null, size = if (ringLarge) 56.dp else 32.dp, strokeWidth = if (ringLarge) 4.dp else 3.dp)
                }
            }
            is PhotoSlot.Failed -> {
                Box(Modifier.fillMaxSize().background(c.errorFill))
                Box(
                    Modifier.size(if (ringLarge) 48.dp else 36.dp).clip(CircleShape).background(c.surface),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(MomenticIcons.Alert, contentDescription = null, tint = c.error, modifier = Modifier.size(if (ringLarge) 24.dp else 20.dp))
                }
            }
        }
    }
}

/** The dashed "Add photo" slot. */
@Composable
fun AddPhotoTile(
    label: String,
    large: Boolean,
    hasError: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accessibilityLabel: String = label,
) {
    val c = Momentic.colors
    Column(
        modifier = modifier
            .clip(PhotoShape)
            .background(if (hasError) c.errorFill else c.surface)
            .dashedBorder(1.5.dp, if (hasError) c.error else c.roseSoft, PhotoCorner)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = accessibilityLabel },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (large) 10.dp else 6.dp, Alignment.CenterVertically),
    ) {
        if (large) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(c.blush100), contentAlignment = Alignment.Center) {
                Icon(MomenticIcons.Photo, contentDescription = null, tint = c.roseDeep, modifier = Modifier.size(24.dp))
            }
            Text(label, style = Momentic.type.label, color = c.rose)
        } else {
            Icon(MomenticIcons.Plus, contentDescription = null, tint = c.rose, modifier = Modifier.size(24.dp))
            Text(label, style = Momentic.type.captionStrong, color = c.rose)
        }
    }
}

@Composable
private fun BoxScope.NumberBadge(number: Int) {
    val c = Momentic.colors
    Box(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(6.dp)
            .size(24.dp)
            .clip(CircleShape)
            .background(c.surface),
        contentAlignment = Alignment.Center,
    ) {
        Text(number.toString(), style = Momentic.type.captionStrong, color = c.ink)
    }
}

/** A small × in the photo's corner, with a full 40 dp target. */
@Composable
private fun BoxScope.RemoveButton(label: String, onClick: () -> Unit) {
    val c = Momentic.colors
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .size(40.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(28.dp).momenticShadow(MomenticElevation.Rest, CircleShape).clip(CircleShape).background(c.surface.copy(alpha = 0.94f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(MomenticIcons.Close, contentDescription = null, tint = c.ink, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun ChangePill(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Momentic.colors
    Row(
        modifier = modifier
            .height(44.dp)
            .momenticShadow(MomenticElevation.Rest, RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
            .background(c.surface.copy(alpha = 0.94f))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 12.dp, end = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(MomenticIcons.Retry, contentDescription = null, tint = c.ink, modifier = Modifier.size(16.dp))
        Text(stringResource(Res.string.photo_change), style = Momentic.type.label, color = c.ink)
    }
}

/** "✓ Uploaded" in green under a photo that's safely up. */
@Composable
private fun StatusLine(uploaded: Boolean) {
    val c = Momentic.colors
    if (!uploaded) return
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(MomenticIcons.CheckCircle, contentDescription = null, tint = c.success, modifier = Modifier.size(16.dp))
        Text(stringResource(Res.string.photo_uploaded), style = Momentic.type.captionStrong, color = c.success)
    }
}

/** "Couldn't upload — Try again": the reason in red, then the way forward. */
@Composable
fun FailedLine(message: String, actionLabel: String, onAction: () -> Unit, modifier: Modifier = Modifier) {
    val c = Momentic.colors
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(MomenticIcons.Alert, contentDescription = null, tint = c.error, modifier = Modifier.size(16.dp).padding(end = 2.dp))
        Text(
            text = "${stringResource(Res.string.photo_failed)} $message".trim(),
            style = Momentic.type.captionStrong,
            color = c.error,
            modifier = Modifier.weight(1f, fill = false),
        )
        Text(
            text = actionLabel,
            style = Momentic.type.label,
            color = c.rose,
            modifier = Modifier
                .clickable(role = Role.Button, onClick = onAction)
                .padding(horizontal = 8.dp, vertical = 12.dp),
        )
    }
}
