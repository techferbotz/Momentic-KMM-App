package com.ferbotz.momentic.feature.template

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ferbotz.momentic.data.util.resolveWebUrl
import com.ferbotz.momentic.designsystem.components.PrimaryButton
import com.ferbotz.momentic.designsystem.components.SecondaryButton
import com.ferbotz.momentic.feature.shell.StandInNote
import com.ferbotz.momentic.feature.shell.StandInScreen
import com.ferbotz.momentic.feature.shell.StandInStatus
import com.ferbotz.momentic.presentation.config.LocalRemoteConfig
import com.ferbotz.momentic.presentation.di.koinViewModel
import com.ferbotz.momentic.presentation.template.TemplateDetailViewModel
import org.koin.core.parameter.parametersOf

/**
 * One template. "See a live example" opens its shopfront — `/<previewCode>` on the web origin (contract
 * § Previewing on the web) — in the app's web frame; "Make it yours" starts the form.
 */
@Composable
fun TemplateRoute(
    templateId: String,
    onBack: () -> Unit,
    onLiveExample: (url: String, title: String) -> Unit,
    onMakeItYours: (templateId: String) -> Unit,
) {
    val viewModel: TemplateDetailViewModel = koinViewModel { parametersOf(templateId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val origin = LocalRemoteConfig.current.web.origin
    val template = state.template
    StandInScreen(title = template?.name.orEmpty(), onBack = onBack) {
        StandInStatus(state.isLoading, state.error, viewModel::retry)
        if (template != null) {
            StandInNote(template.description)
            StandInNote("Asks for: " + template.fields.joinToString { it.label })
            val example = resolveWebUrl(origin, template.previewCode)
            if (example != null) {
                SecondaryButton("See a live example", onClick = { onLiveExample(example, template.name) }, modifier = Modifier.fillMaxWidth())
            }
            if (template.canBeFilledIn) {
                PrimaryButton("Make it yours", onClick = { onMakeItYours(template.id) }, modifier = Modifier.fillMaxWidth())
            } else {
                StandInNote("Update Momentic to use this experience.")
            }
        }
    }
}
