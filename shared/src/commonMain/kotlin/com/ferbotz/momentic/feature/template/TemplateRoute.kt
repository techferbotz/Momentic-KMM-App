package com.ferbotz.momentic.feature.template

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ferbotz.momentic.data.util.resolveWebUrl
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
    onLiveExample: (url: String, title: String, templateId: String) -> Unit,
    onMakeItYours: (templateId: String) -> Unit,
) {
    val viewModel: TemplateDetailViewModel = koinViewModel { parametersOf(templateId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val origin = LocalRemoteConfig.current.web.origin
    val template = state.template
    val example = template?.let { resolveWebUrl(origin, it.previewCode) }
    TemplateScreen(
        state = state,
        onBack = onBack,
        onLiveExample = if (template != null && example != null) {
            { onLiveExample(example, template.name, template.id) }
        } else {
            null
        },
        onMakeItYours = { onMakeItYours(templateId) },
        onRetry = viewModel::retry,
    )
}
