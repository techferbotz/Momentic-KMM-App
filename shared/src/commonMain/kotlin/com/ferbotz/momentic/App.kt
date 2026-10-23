package com.ferbotz.momentic

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import com.ferbotz.momentic.data.local.preferences.AppPreferences
import com.ferbotz.momentic.data.repository.DraftSessionRepository
import com.ferbotz.momentic.data.util.resolveWebUrl
import com.ferbotz.momentic.navigation.EditorRoute
import com.ferbotz.momentic.navigation.FeedRoute
import com.ferbotz.momentic.navigation.TemplateRoute
import com.ferbotz.momentic.presentation.config.LocalRemoteConfig
import com.ferbotz.momentic.presentation.config.RemoteConfigViewModel
import com.ferbotz.momentic.presentation.di.koinGet
import com.ferbotz.momentic.presentation.di.koinViewModel
import com.ferbotz.momentic.presentation.feed.FeedViewModel
import com.ferbotz.momentic.presentation.links.rememberLinkOpener
import com.ferbotz.momentic.presentation.template.TemplateDetailViewModel
import com.ferbotz.momentic.theme.MomenticTheme
import com.ferbotz.momentic.ui.screens.FeedScreen
import com.ferbotz.momentic.ui.screens.MaintenanceScreen
import com.ferbotz.momentic.ui.screens.TemplateDetailScreen
import com.ferbotz.momentic.ui.screens.UpdateNudgeDialog
import com.ferbotz.momentic.ui.screens.UpdateRequiredScreen
import org.koin.core.parameter.parametersOf

/** Duration of the screen push transitions. */
private const val NAV_ANIM_MS = 320

/**
 * App root. Everything sits under the [RemoteConfigGate] (maintenance / forced update / feature
 * switches from `GET /config`); below it, the feed → template → editor flow.
 */
@Composable
fun App() {
    MomenticTheme(darkTheme = isSystemInDarkTheme()) {
        setSingletonImageLoaderFactory { context ->
            ImageLoader.Builder(context)
                .components { add(KtorNetworkFetcherFactory()) }
                .crossfade(true)
                .build()
        }
        DraftSessionBootstrap()
        RemoteConfigGate {
            MomenticNavHost()
        }
    }
}

/**
 * Mints the anonymous draft session on first launch (contract: "call once on first launch and store
 * the token"), off the critical path. If this attempt fails — offline on first launch — the editor
 * mints one before its first save instead.
 */
@Composable
private fun DraftSessionBootstrap() {
    val prefs: AppPreferences = koinGet()
    val draftSessions: DraftSessionRepository = koinGet()
    LaunchedEffect(Unit) {
        if (!prefs.isSignedIn) draftSessions.ensureDraftToken()
    }
}

/**
 * Remote-config gate around the whole UI (HOA protocol 11 §3). The config is fetched on every cold
 * start and re-fetched on foreground once its TTL lapses; until it lands the cached copy (or the
 * compiled-in defaults) applies, so launch never waits on it. Maintenance and a required update
 * *replace* the app; a recommended update is a dismissible dialog over it. Everything inside reads the
 * live document through [LocalRemoteConfig].
 */
@Composable
private fun RemoteConfigGate(content: @Composable () -> Unit) {
    val viewModel: RemoteConfigViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Each return to the foreground re-checks the TTL (the cold-start fetch runs in the VM's init).
    LifecycleStartEffect(viewModel) {
        viewModel.onAppForegrounded()
        onStopOrDispose { }
    }

    CompositionLocalProvider(LocalRemoteConfig provides state.config) {
        when {
            state.isUnderMaintenance -> MaintenanceScreen(
                message = state.maintenanceMessage,
                onRetry = viewModel::retry,
            )
            state.isUpdateRequired -> UpdateRequiredScreen(
                message = state.updateMessage,
                storeUrl = state.storeUrl,
            )
            else -> {
                content()
                if (state.showUpdateNudge) {
                    UpdateNudgeDialog(
                        message = state.updateMessage,
                        storeUrl = state.storeUrl,
                        onDismiss = viewModel::dismissUpdateNudge,
                    )
                }
            }
        }
    }
}

@Composable
private fun MomenticNavHost() {
    val navController = rememberNavController()
    val linkOpener = rememberLinkOpener()
    val webOrigin = LocalRemoteConfig.current.web.origin

    NavHost(
        navController = navController,
        startDestination = FeedRoute,
        enterTransition = { slideInHorizontally(tween(NAV_ANIM_MS)) { it / 4 } + fadeIn(tween(NAV_ANIM_MS)) },
        exitTransition = { fadeOut(tween(NAV_ANIM_MS)) },
        popEnterTransition = { fadeIn(tween(NAV_ANIM_MS)) },
        popExitTransition = { slideOutHorizontally(tween(NAV_ANIM_MS)) { it / 4 } + fadeOut(tween(NAV_ANIM_MS)) },
    ) {
        composable<FeedRoute> {
            val viewModel: FeedViewModel = koinViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            FeedScreen(
                state = state,
                onTemplateClick = { templateId -> navController.navigate(TemplateRoute(templateId)) },
                onRefresh = viewModel::refresh,
                onRetry = viewModel::retry,
                onMessageShown = viewModel::messageShown,
            )
        }
        composable<TemplateRoute> { entry ->
            val route = entry.toRoute<TemplateRoute>()
            val viewModel: TemplateDetailViewModel = koinViewModel { parametersOf(route.templateId) }
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            TemplateDetailScreen(
                state = state,
                onBack = { navController.popBackStack() },
                // The shopfront lives on the web origin at /<previewCode> (contract § Previewing on the web).
                onPreview = { code -> resolveWebUrl(webOrigin, code)?.let(linkOpener::openPage) },
                onUseTemplate = { navController.navigate(EditorRoute(templateId = route.templateId)) },
                onRetry = viewModel::retry,
            )
        }
        composable<EditorRoute> {
            // Stand-in until the editor screen arrives with the app's UI; EditorViewModel is ready for it.
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("The editor is on its way.")
            }
        }
    }
}
