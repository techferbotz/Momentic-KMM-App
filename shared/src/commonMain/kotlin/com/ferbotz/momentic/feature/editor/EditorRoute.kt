package com.ferbotz.momentic.feature.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ferbotz.momentic.designsystem.components.PrimaryButton
import com.ferbotz.momentic.designsystem.components.SecondaryButton
import com.ferbotz.momentic.feature.shell.LocalMessenger
import com.ferbotz.momentic.feature.shell.MessageEffect
import com.ferbotz.momentic.feature.shell.StandInScreen
import com.ferbotz.momentic.feature.shell.StandInStatus
import com.ferbotz.momentic.presentation.di.koinViewModel
import com.ferbotz.momentic.presentation.editor.EditorEvent
import com.ferbotz.momentic.presentation.editor.EditorViewModel
import com.ferbotz.momentic.presentation.editor.FieldPath
import com.ferbotz.momentic.presentation.editor.FieldValue
import com.ferbotz.momentic.theme.MomenticSpacing
import org.koin.core.parameter.parametersOf

/**
 * The form for one template — a new creation, or [creationId]'s draft. Preview opens the draft on the
 * web; Continue goes to publishing once everything required is there; a deleted draft closes the form.
 */
@Composable
fun EditorRoute(
    templateId: String,
    creationId: String?,
    onClose: () -> Unit,
    onPreview: (url: String, templateId: String, creationId: String) -> Unit,
    onContinue: (creationId: String) -> Unit,
) {
    val viewModel: EditorViewModel = koinViewModel { parametersOf(templateId, creationId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MessageEffect(state.message, viewModel::messageShown)

    val preview by rememberUpdatedState(onPreview)
    val next by rememberUpdatedState(onContinue)
    val close by rememberUpdatedState(onClose)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                // The preview is saved first, so the draft has an id by now.
                is EditorEvent.OpenPreview -> viewModel.uiState.value.creationId?.let { id -> preview(event.url, templateId, id) }
                is EditorEvent.ContinueToPublish -> next(event.creationId)
                EditorEvent.Closed -> close()
            }
        }
    }

    val messenger = LocalMessenger.current
    val actions = remember(viewModel) {
        EditorFormActions(viewModel) { _, _ -> messenger.show("Adding photos arrives with the photo picker.") }
    }

    StandInScreen(title = state.template?.name.orEmpty(), onBack = onClose) {
        StandInStatus(state.isLoading, state.loadError, viewModel::retryLoad)
        if (state.template != null) {
            TemplateForm(state, actions)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MomenticSpacing.xs)) {
                SecondaryButton("Preview", onClick = viewModel::preview, loading = state.isOpeningPreview, modifier = Modifier.weight(1f))
                PrimaryButton("Continue", onClick = viewModel::continueToPublish, loading = state.isSaving, modifier = Modifier.weight(1f))
            }
        }
    }
}

/**
 * The form's intents, as the editor's view model takes them. Photos are the exception: picking them is
 * the screen's job ([onPickPhotos] — the picker, then the crop), and the picked bytes go to `addPhotos`.
 */
class EditorFormActions(
    private val viewModel: EditorViewModel,
    private val onPickPhotos: (path: FieldPath, room: Int?) -> Unit,
) : FormActions {
    override fun update(path: FieldPath, value: FieldValue) = viewModel.update(path, value)
    override fun pickPhotos(path: FieldPath, room: Int?) = onPickPhotos(path, room)
    override fun retryPhoto(path: FieldPath, slotId: String) = viewModel.retryPhoto(path, slotId)
    override fun removePhoto(path: FieldPath, slotId: String) = viewModel.removePhoto(path, slotId)
    override fun movePhoto(path: FieldPath, from: Int, to: Int) = viewModel.movePhoto(path, from, to)
    override fun addEntry(groupKey: String) = viewModel.addEntry(groupKey)
    override fun removeEntry(groupKey: String, entryId: String) = viewModel.removeEntry(groupKey, entryId)
    override fun moveEntry(groupKey: String, from: Int, to: Int) = viewModel.moveEntry(groupKey, from, to)
}
