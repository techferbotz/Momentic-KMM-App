package com.ferbotz.momentic.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.ferbotz.momentic.designsystem.components.ConfirmDialog
import com.ferbotz.momentic.designsystem.components.EmptyState
import com.ferbotz.momentic.designsystem.components.MomenticTopBar
import com.ferbotz.momentic.designsystem.components.PrimaryButton
import com.ferbotz.momentic.designsystem.components.SaveIndicator
import com.ferbotz.momentic.designsystem.components.SaveStatus
import com.ferbotz.momentic.designsystem.components.SecondaryButton
import com.ferbotz.momentic.designsystem.components.SkeletonBlock
import com.ferbotz.momentic.designsystem.icons.MomenticIcons
import com.ferbotz.momentic.designsystem.icons.MomenticIllustrations
import com.ferbotz.momentic.presentation.editor.AutosaveState
import com.ferbotz.momentic.presentation.editor.EditorUiState
import com.ferbotz.momentic.resources.Res
import com.ferbotz.momentic.resources.action_try_again
import com.ferbotz.momentic.resources.editor_leave_body_failed
import com.ferbotz.momentic.resources.editor_leave_body_invalid
import com.ferbotz.momentic.resources.editor_leave_body_offline
import com.ferbotz.momentic.resources.editor_leave_confirm
import com.ferbotz.momentic.resources.editor_leave_dismiss
import com.ferbotz.momentic.resources.editor_leave_title
import com.ferbotz.momentic.resources.editor_load_failed
import com.ferbotz.momentic.resources.editor_preview
import com.ferbotz.momentic.resources.editor_required_note
import com.ferbotz.momentic.resources.editor_title
import com.ferbotz.momentic.theme.Momentic
import com.ferbotz.momentic.theme.MomenticShapes
import com.ferbotz.momentic.theme.MomenticSpacing
import org.jetbrains.compose.resources.stringResource

/**
 * Personalise: the form generated from the template's fields, under a bar that says whether it's saved,
 * with Preview at the foot. Back ([onBack]) saves first; when it can't, the leave question shows.
 */
@Composable
fun EditorScreen(
    state: EditorUiState,
    actions: FormActions,
    onBack: () -> Unit,
    onPreview: () -> Unit,
    onRetry: () -> Unit,
    onKeepEditing: () -> Unit,
    onLeaveWithoutSaving: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Momentic.colors
    val template = state.template
    Column(modifier.fillMaxSize().background(c.background)) {
        MomenticTopBar(
            title = template?.name,
            onBack = onBack,
            background = c.background,
            actions = { state.autosave.status()?.let { SaveIndicator(it) } },
        )
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                template != null -> Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(start = MomenticSpacing.gutter, end = MomenticSpacing.gutter, top = MomenticSpacing.xs, bottom = MomenticSpacing.xl),
                ) {
                    Text(stringResource(Res.string.editor_title), style = Momentic.type.h1, color = c.ink, modifier = Modifier.semantics { heading() })
                    RequiredNote(Modifier.padding(top = MomenticSpacing.xxs, bottom = MomenticSpacing.lg))
                    TemplateForm(state, actions)
                }
                state.isLoading -> FormSkeleton()
                else -> EmptyState(
                    illustration = MomenticIllustrations.BrokenLink,
                    title = stringResource(Res.string.editor_load_failed),
                    body = state.loadError.orEmpty(),
                    action = { SecondaryButton(stringResource(Res.string.action_try_again), onClick = onRetry, icon = MomenticIcons.Retry) },
                    modifier = Modifier.padding(MomenticSpacing.gutter).padding(top = MomenticSpacing.xl),
                )
            }
        }
        if (template != null) {
            HorizontalDivider(thickness = 1.dp, color = c.line)
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(c.surface)
                    .padding(horizontal = MomenticSpacing.gutter, vertical = MomenticSpacing.sm)
                    .windowInsetsPadding(WindowInsets.navigationBars),
            ) {
                PrimaryButton(
                    text = stringResource(Res.string.editor_preview),
                    onClick = onPreview,
                    icon = MomenticIcons.Eye,
                    loading = state.isOpeningPreview,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
    if (state.confirmLeave) {
        ConfirmDialog(
            title = stringResource(Res.string.editor_leave_title),
            message = stringResource(
                when {
                    state.errors.isNotEmpty() -> Res.string.editor_leave_body_invalid
                    state.autosave == AutosaveState.Offline -> Res.string.editor_leave_body_offline
                    else -> Res.string.editor_leave_body_failed
                },
            ),
            confirmLabel = stringResource(Res.string.editor_leave_confirm),
            onConfirm = onLeaveWithoutSaving,
            dismissLabel = stringResource(Res.string.editor_leave_dismiss),
            onDismiss = onKeepEditing,
            icon = MomenticIcons.Alert,
        )
    }
}

/** "Fields marked * are needed before you preview." — the star in rose, as on every required label. */
@Composable
private fun RequiredNote(modifier: Modifier = Modifier) {
    val c = Momentic.colors
    val template = stringResource(Res.string.editor_required_note, STAR)
    val text = buildAnnotatedString {
        val at = template.indexOf(STAR)
        if (at < 0) {
            append(template)
        } else {
            append(template.substring(0, at))
            withStyle(SpanStyle(color = c.rose, fontWeight = FontWeight.Bold)) { append(STAR) }
            append(template.substring(at + STAR.length))
        }
    }
    Text(text, style = Momentic.type.bodySmall, color = c.inkSoft, modifier = modifier)
}

private const val STAR = "*"

private fun AutosaveState.status(): SaveStatus? = when (this) {
    AutosaveState.Idle -> null
    AutosaveState.Saving -> SaveStatus.Saving
    AutosaveState.Saved -> SaveStatus.Saved
    AutosaveState.Offline -> SaveStatus.Offline
    AutosaveState.NotSaved -> SaveStatus.NotSaved
}

@Composable
private fun FormSkeleton() {
    Column(Modifier.padding(MomenticSpacing.gutter), verticalArrangement = Arrangement.spacedBy(MomenticSpacing.sm)) {
        SkeletonBlock(Modifier.width(180.dp).height(28.dp), RoundedCornerShape(14.dp))
        SkeletonBlock(Modifier.width(260.dp).height(14.dp), RoundedCornerShape(7.dp))
        repeat(3) {
            SkeletonBlock(Modifier.padding(top = MomenticSpacing.md).width(96.dp).height(14.dp), RoundedCornerShape(7.dp))
            SkeletonBlock(Modifier.fillMaxWidth().height(56.dp), MomenticShapes.input)
        }
    }
}
