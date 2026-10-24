package com.ferbotz.momentic.feature.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ferbotz.momentic.designsystem.components.MessageTone
import com.ferbotz.momentic.feature.editor.crop.CropFlow
import com.ferbotz.momentic.feature.editor.crop.CropSession
import com.ferbotz.momentic.feature.shell.LocalMessenger
import com.ferbotz.momentic.feature.shell.MessageEffect
import com.ferbotz.momentic.platform.SystemBack
import com.ferbotz.momentic.platform.rememberPhotoPicker
import com.ferbotz.momentic.presentation.di.koinViewModel
import com.ferbotz.momentic.presentation.editor.EditorEvent
import com.ferbotz.momentic.presentation.editor.EditorViewModel
import com.ferbotz.momentic.presentation.editor.FieldPath
import com.ferbotz.momentic.presentation.editor.FieldValue
import com.ferbotz.momentic.presentation.editor.definitionAt
import org.koin.core.parameter.parametersOf

/**
 * The form for one template — a new creation, or [creationId]'s draft. It saves itself as you go; Back
 * (the bar's or the system's) saves what's left and closes. Preview opens the draft on the web once
 * everything required is there; publishing goes on from there.
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
    // Photos: the picker, then (for a field with an aspect ratio) the crop screen, one photo at a time. Which
    // field asked survives the app being let go while the picker is open, so the photo still finds its place.
    var pickingFor by rememberSaveable(stateSaver = FieldPathSaver) { mutableStateOf<FieldPath?>(null) }
    var cropping by remember { mutableStateOf<Pair<FieldPath, CropSession>?>(null) }
    val picker = rememberPhotoPicker { picked ->
        val path = pickingFor ?: return@rememberPhotoPicker
        pickingFor = null
        val ratio = viewModel.uiState.value.fields.definitionAt(path)?.aspectRatio
        if (ratio == null) viewModel.addPhotos(path, picked) else cropping = path to CropSession(ratio, picked)
    }
    val actions = remember(viewModel, picker) {
        EditorFormActions(viewModel) { path, room ->
            pickingFor = path
            picker.pick(room ?: MAX_PHOTOS_AT_ONCE)
        }
    }

    // The system's Back saves first too; the form closes when the view model says so.
    SystemBack(enabled = cropping == null) { viewModel.close() }
    EditorScreen(
        state = state,
        actions = actions,
        onBack = viewModel::close,
        onPreview = viewModel::preview,
        onRetry = viewModel::retryLoad,
        onKeepEditing = viewModel::keepEditing,
        onLeaveWithoutSaving = viewModel::leaveWithoutSaving,
    )

    cropping?.let { (path, session) ->
        CropFlow(
            session = session,
            onAdvance = { next -> cropping = path to next },
            onDone = { framed ->
                cropping = null
                viewModel.addFramedPhotos(path, framed)
            },
            onUnreadable = { messenger.show(it, MessageTone.Error) },
        )
    }
}

/** How many photos the picker offers at once for a list with no limit of its own. */
private const val MAX_PHOTOS_AT_ONCE = 10

/** A [FieldPath] as one string, for saved instance state. */
private val FieldPathSaver = Saver<FieldPath?, String>(
    save = { path -> path?.let { listOf(it.key, it.entryId.orEmpty(), it.childKey.orEmpty()).joinToString(SEPARATOR) } },
    restore = { saved ->
        val (key, entryId, childKey) = saved.split(SEPARATOR)
        FieldPath(key, entryId.ifEmpty { null }, childKey.ifEmpty { null })
    },
)

private const val SEPARATOR = "\u001F"

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
