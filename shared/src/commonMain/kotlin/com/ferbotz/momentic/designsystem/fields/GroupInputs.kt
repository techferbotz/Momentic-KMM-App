package com.ferbotz.momentic.designsystem.fields

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ferbotz.momentic.designsystem.components.SecondaryButton
import com.ferbotz.momentic.designsystem.icons.MomenticIcons
import com.ferbotz.momentic.presentation.editor.PhotoSlot
import com.ferbotz.momentic.resources.Res
import com.ferbotz.momentic.resources.photo_failed
import com.ferbotz.momentic.resources.photo_try_again
import com.ferbotz.momentic.resources.photo_uploading_short
import com.ferbotz.momentic.theme.Momentic
import com.ferbotz.momentic.theme.MomenticElevation
import com.ferbotz.momentic.theme.momenticShadow
import org.jetbrains.compose.resources.stringResource
import sh.calvin.reorderable.ReorderableColumn

private val CardShape = RoundedCornerShape(14.dp)

/**
 * A repeating group ("Your moments"): the label with its running count, the entries as cards in order —
 * drag a card's handle to move it — and "Add a moment" below (gone at the limit). With no entries a
 * dashed box says so.
 *
 * @param entryContent draws one entry; [handle] is the drag handle's modifier, for its handle only.
 */
@Composable
fun <T> GroupListField(
    entries: List<T>,
    entryKey: (T) -> Any,
    label: String,
    onMove: (from: Int, to: Int) -> Unit,
    addLabel: String,
    onAdd: (() -> Unit)?,
    emptyText: String,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    count: String? = null,
    helper: String? = null,
    error: String? = null,
    entryContent: @Composable (entry: T, index: Int, handle: Modifier, isDragging: Boolean) -> Unit,
) {
    val c = Momentic.colors
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            FieldLabel(label, required, trailing = count)
            if (helper != null) Text(helper, style = Momentic.type.caption, color = c.inkSoft)
        }
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .clip(CardShape)
                    .then(if (error != null) Modifier.background(c.errorFill) else Modifier)
                    .dashedBorder(if (error != null) 2.dp else 1.5.dp, if (error != null) c.error else c.line, 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(emptyText, style = Momentic.type.bodySmall, color = if (error != null) c.ink else c.inkSoft, textAlign = TextAlign.Center)
            }
        } else {
            ReorderableColumn(
                list = entries,
                onSettle = onMove,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) { index, entry, isDragging ->
                key(entryKey(entry)) {
                    ReorderableItem {
                        entryContent(entry, index, Modifier.draggableHandle(), isDragging)
                    }
                }
            }
        }
        if (onAdd != null) {
            SecondaryButton(addLabel, onClick = onAdd, icon = MomenticIcons.Plus, modifier = Modifier.fillMaxWidth())
        }
        FieldFooter(helper = null, error = error)
    }
}

/**
 * One entry of a group as a card (84 tall): the drag handle, its photo when the group has one, its title
 * and a line under it — the caption, or the photo's upload state — and the pencil that opens it.
 */
@Composable
fun GroupEntryCard(
    title: String,
    titleIsPlaceholder: Boolean,
    subtitle: String?,
    onOpen: () -> Unit,
    handle: Modifier,
    reorderLabel: String,
    editLabel: String,
    modifier: Modifier = Modifier,
    photo: PhotoSlot? = null,
    showPhoto: Boolean = false,
    hasError: Boolean = false,
    isDragging: Boolean = false,
    onRetryPhoto: (() -> Unit)? = null,
) {
    val c = Momentic.colors
    val border = when {
        isDragging -> c.rose to 2.dp
        hasError -> c.error to 2.dp
        else -> c.line to 1.dp
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 84.dp)
            .then(if (isDragging) Modifier.momenticShadow(MomenticElevation.Raised, CardShape) else Modifier)
            .clip(CardShape)
            .background(c.surface)
            .border(border.second, border.first, CardShape)
            .clickable(role = Role.Button, onClickLabel = editLabel, onClick = onOpen)
            .padding(start = 2.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = handle.size(36.dp, 48.dp).semantics { contentDescription = reorderLabel },
            contentAlignment = Alignment.Center,
        ) {
            Icon(MomenticIcons.DragHandle, contentDescription = null, tint = if (isDragging) c.rose else c.inkSoft, modifier = Modifier.size(20.dp))
        }
        if (showPhoto) {
            if (photo == null) {
                Box(
                    Modifier.size(52.dp, 65.dp).clip(RoundedCornerShape(10.dp)).background(c.blush100),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(MomenticIcons.Photo, contentDescription = null, tint = c.roseSoft, modifier = Modifier.size(20.dp))
                }
            } else {
                PhotoTile(
                    slot = photo,
                    description = title,
                    onRetry = { onRetryPhoto?.invoke() },
                    modifier = Modifier.size(52.dp, 65.dp),
                    corner = 10.dp,
                )
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = Momentic.type.label,
                color = if (titleIsPlaceholder) c.inkSoft else c.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            when {
                photo is PhotoSlot.Uploading -> Text(stringResource(Res.string.photo_uploading_short), style = Momentic.type.caption, color = c.inkSoft)
                photo is PhotoSlot.Failed -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(Res.string.photo_failed), style = Momentic.type.captionStrong, color = c.error)
                    if (photo.retryable && onRetryPhoto != null) {
                        Text(
                            text = stringResource(Res.string.photo_try_again),
                            style = Momentic.type.captionStrong,
                            color = c.rose,
                            modifier = Modifier.clickable(role = Role.Button, onClick = onRetryPhoto).padding(horizontal = 4.dp, vertical = 8.dp),
                        )
                    }
                }
                subtitle != null -> Text(
                    text = subtitle,
                    style = if (hasError) Momentic.type.captionStrong else Momentic.type.caption,
                    color = if (hasError) c.error else c.inkSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(22.dp)).clickable(role = Role.Button, onClick = onOpen).semantics { contentDescription = editLabel },
            contentAlignment = Alignment.Center,
        ) {
            Icon(MomenticIcons.Pencil, contentDescription = null, tint = c.ink, modifier = Modifier.size(20.dp))
        }
    }
}
