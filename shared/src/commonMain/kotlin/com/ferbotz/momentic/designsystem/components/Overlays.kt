package com.ferbotz.momentic.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ferbotz.momentic.platform.MomenticDialogWindow
import com.ferbotz.momentic.theme.Momentic
import com.ferbotz.momentic.theme.MomenticElevation
import com.ferbotz.momentic.theme.MomenticShapes
import com.ferbotz.momentic.theme.MomenticSpacing
import com.ferbotz.momentic.theme.momenticShadow
import kotlinx.coroutines.launch

/** What a sheet's content can do besides lay itself out: close the sheet, sliding it away first. */
class MomenticSheetScope internal constructor(
    column: ColumnScope,
    /** Slides the sheet down, then calls the sheet's `onDismissRequest` — after "Save", say. */
    val dismiss: () -> Unit,
) : ColumnScope by column

/**
 * A bottom sheet in the canvas's style: cream, 20 dp top corners, a soft handle, ink at 40 % behind.
 * It opens fully (never half-way); the content pads itself 20 dp at the sides and the sheet keeps clear
 * of the gesture area. Show it with `if (open) MomenticBottomSheet(…)`; close it from inside with
 * [MomenticSheetScope.dismiss] so it slides away.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MomenticBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable MomenticSheetScope.() -> Unit,
) {
    val c = Momentic.colors
    val sheetState = rememberBottomSheetState(SheetValue.Hidden, setOf(SheetValue.Hidden, SheetValue.Expanded))
    val scope = rememberCoroutineScope()
    val dismiss: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissRequest() }
    }
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = MomenticShapes.sheet,
        containerColor = c.surface,
        contentColor = c.ink,
        tonalElevation = 0.dp,
        scrimColor = c.scrim,
        dragHandle = { SheetHandle() },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = MomenticSpacing.gutter, end = MomenticSpacing.gutter, bottom = MomenticSpacing.lg),
        ) {
            MomenticSheetScope(this, dismiss).content()
        }
    }
}

/** The 36 × 4 handle at the top of every sheet. */
@Composable
fun SheetHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(top = MomenticSpacing.sm, bottom = MomenticSpacing.xs)
            .size(width = 36.dp, height = 4.dp)
            .clip(MomenticShapes.pill)
            .background(Momentic.colors.line),
    )
}

/**
 * A confirmation before something that can't be undone — "Delete this moment?". The destructive action
 * comes first as an outlined red button; the way out is a quiet rose one below it.
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    dismissLabel: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    confirmIcon: ImageVector? = icon,
    destructive: Boolean = true,
    confirming: Boolean = false,
) {
    val c = Momentic.colors
    MomenticDialogWindow(onDismissRequest = onDismiss, scrim = c.scrim) {
        Column(
            modifier = modifier
                .padding(horizontal = MomenticSpacing.lg)
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .momenticShadow(MomenticElevation.Sheet, MomenticShapes.card)
                .clip(MomenticShapes.card)
                .background(c.surface)
                .padding(MomenticSpacing.lg)
                .semantics { paneTitle = title },
            verticalArrangement = Arrangement.spacedBy(MomenticSpacing.xs),
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .padding(bottom = MomenticSpacing.xs)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (destructive) c.errorFill else c.blush100),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = if (destructive) c.error else c.roseDeep, modifier = Modifier.size(24.dp))
                }
            }
            Text(title, style = Momentic.type.h2, color = c.ink)
            Text(message, style = Momentic.type.body, color = c.inkSoft)
            Column(Modifier.padding(top = MomenticSpacing.md), verticalArrangement = Arrangement.spacedBy(MomenticSpacing.xs)) {
                if (destructive && confirmIcon != null) {
                    DestructiveButton(confirmLabel, onConfirm, confirmIcon, Modifier.fillMaxWidth(), loading = confirming)
                } else {
                    PrimaryButton(confirmLabel, onConfirm, Modifier.fillMaxWidth(), icon = confirmIcon, loading = confirming)
                }
                QuietButton(dismissLabel, onDismiss, Modifier.fillMaxWidth().height(48.dp), enabled = !confirming)
            }
        }
    }
}
