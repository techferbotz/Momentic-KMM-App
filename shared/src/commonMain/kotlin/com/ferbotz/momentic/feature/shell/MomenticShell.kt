package com.ferbotz.momentic.feature.shell

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.ferbotz.momentic.designsystem.components.MainTab
import com.ferbotz.momentic.designsystem.components.MomenticBottomBar
import com.ferbotz.momentic.designsystem.components.MomenticSnackbarHost
import com.ferbotz.momentic.feature.editor.EditorRoute as EditorScreenRoute
import com.ferbotz.momentic.feature.editor.PreviewRoute as PreviewScreenRoute
import com.ferbotz.momentic.feature.explore.CategoryRoute as CategoryScreenRoute
import com.ferbotz.momentic.feature.explore.ExploreRoute as ExploreScreenRoute
import com.ferbotz.momentic.feature.moments.MomentRoute as MomentScreenRoute
import com.ferbotz.momentic.feature.moments.MomentsRoute as MomentsScreenRoute
import com.ferbotz.momentic.feature.moments.RsvpsRoute as RsvpsScreenRoute
import com.ferbotz.momentic.feature.publish.PublishRoute as PublishScreenRoute
import com.ferbotz.momentic.feature.publish.PublishedRoute as PublishedScreenRoute
import com.ferbotz.momentic.feature.settings.DeleteAccountRoute as DeleteAccountScreenRoute
import com.ferbotz.momentic.feature.settings.PurchasesRoute as PurchasesScreenRoute
import com.ferbotz.momentic.feature.settings.SettingsRoute as SettingsScreenRoute
import com.ferbotz.momentic.feature.template.TemplateRoute as TemplateScreenRoute
import com.ferbotz.momentic.feature.web.WebPageRoute as WebPageScreenRoute
import com.ferbotz.momentic.navigation.CategoryRoute
import com.ferbotz.momentic.navigation.DeleteAccountRoute
import com.ferbotz.momentic.navigation.EditorRoute
import com.ferbotz.momentic.navigation.ExploreGraph
import com.ferbotz.momentic.navigation.ExploreRoute
import com.ferbotz.momentic.navigation.MomentRoute
import com.ferbotz.momentic.navigation.MomentsGraph
import com.ferbotz.momentic.navigation.MomentsRoute
import com.ferbotz.momentic.navigation.PreviewRoute
import com.ferbotz.momentic.navigation.PublishRoute
import com.ferbotz.momentic.navigation.PublishedRoute
import com.ferbotz.momentic.navigation.PurchasesRoute
import com.ferbotz.momentic.navigation.RsvpsRoute
import com.ferbotz.momentic.navigation.SettingsGraph
import com.ferbotz.momentic.navigation.SettingsRoute
import com.ferbotz.momentic.navigation.TemplateRoute
import com.ferbotz.momentic.navigation.WebPageRoute
import com.ferbotz.momentic.presentation.di.koinViewModel
import com.ferbotz.momentic.presentation.feed.FeedViewModel
import com.ferbotz.momentic.presentation.links.LinkOpener
import com.ferbotz.momentic.presentation.links.rememberLinkOpener
import com.ferbotz.momentic.presentation.session.AppSessionViewModel
import com.ferbotz.momentic.theme.Momentic
import com.ferbotz.momentic.theme.MomenticMotion

/**
 * The app below the gates: one snackbar host for every screen's messages, the bottom bar on the tab
 * roots (and Category), and the navigation host. Session notices — "You've been signed out…" after a
 * rejected token — show here, whatever screen is open.
 */
@Composable
fun MomenticShell() {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val messenger = remember(scope) { Messenger(SnackbarHostState(), scope) }
    val entry by navController.currentBackStackEntryAsState()
    val destination = entry?.destination

    val session: AppSessionViewModel = koinViewModel()
    LaunchedEffect(session) {
        session.notices.collect { notice -> messenger.show(notice) }
    }

    CompositionLocalProvider(LocalMessenger provides messenger) {
        Scaffold(
            containerColor = Momentic.colors.background,
            // Screens draw under the status bar themselves; the bottom is the bar's, or the gesture area's.
            contentWindowInsets = WindowInsets.navigationBars,
            snackbarHost = { MomenticSnackbarHost(messenger.hostState) },
            bottomBar = {
                if (destination.showsBottomBar()) {
                    MomenticBottomBar(selected = destination.tab() ?: MainTab.Explore, onSelect = navController::navigateToTab)
                }
            },
        ) { padding ->
            MomenticNavHost(navController, Modifier.padding(padding).consumeWindowInsets(padding))
        }
    }
}

@Composable
private fun MomenticNavHost(navController: NavHostController, modifier: Modifier) {
    val links = rememberLinkOpener()
    val still = Momentic.reduceMotion
    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        if (still) EnterTransition.None else slideInHorizontally(tween(MomenticMotion.SCREEN_MS, easing = MomenticMotion.easeOut)) { it / 6 } +
            fadeIn(tween(MomenticMotion.SCREEN_MS))
    }
    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        if (still) ExitTransition.None else fadeOut(tween(MomenticMotion.QUICK_MS))
    }
    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        if (still) EnterTransition.None else fadeIn(tween(MomenticMotion.QUICK_MS))
    }
    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        if (still) ExitTransition.None else slideOutHorizontally(tween(MomenticMotion.SCREEN_MS, easing = MomenticMotion.easeOut)) { it / 6 } +
            fadeOut(tween(MomenticMotion.SCREEN_MS))
    }

    NavHost(
        navController = navController,
        startDestination = ExploreGraph,
        modifier = modifier,
        enterTransition = enter,
        exitTransition = exit,
        popEnterTransition = popEnter,
        popExitTransition = popExit,
    ) {
        navigation<ExploreGraph>(startDestination = ExploreRoute) {
            composable<ExploreRoute> { entry ->
                ExploreScreenRoute(
                    feed = exploreFeed(navController, entry),
                    onOpenCategory = { navController.navigate(CategoryRoute(it)) },
                    onOpenTemplate = { navController.navigate(TemplateRoute(it)) },
                )
            }
            composable<CategoryRoute> { entry ->
                CategoryScreenRoute(
                    feed = exploreFeed(navController, entry),
                    categoryId = entry.toRoute<CategoryRoute>().categoryId,
                    onBack = navController::back,
                    onOpenTemplate = { navController.navigate(TemplateRoute(it)) },
                )
            }
        }
        navigation<MomentsGraph>(startDestination = MomentsRoute) {
            composable<MomentsRoute> {
                MomentsScreenRoute(
                    onOpenMoment = { navController.navigate(MomentRoute(it)) },
                    onNewMoment = { navController.navigateToTab(MainTab.Explore) },
                )
            }
        }
        navigation<SettingsGraph>(startDestination = SettingsRoute) {
            composable<SettingsRoute> {
                SettingsScreenRoute(
                    onPurchases = { navController.navigate(PurchasesRoute) },
                    onDeleteAccount = { navController.navigate(DeleteAccountRoute) },
                )
            }
        }

        composable<TemplateRoute> { entry ->
            TemplateScreenRoute(
                templateId = entry.toRoute<TemplateRoute>().templateId,
                onBack = navController::back,
                onLiveExample = { url, title, templateId -> navController.navigate(WebPageRoute(url, title, templateId)) },
                onMakeItYours = { navController.navigate(EditorRoute(templateId = it)) },
            )
        }
        composable<WebPageRoute> { entry ->
            val route = entry.toRoute<WebPageRoute>()
            WebPageScreenRoute(
                url = route.url,
                title = route.title,
                onClose = navController::back,
                onMakeItYours = route.templateId?.let { templateId -> { navController.navigate(EditorRoute(templateId = templateId)) } },
            )
        }
        composable<EditorRoute> { entry ->
            val route = entry.toRoute<EditorRoute>()
            EditorScreenRoute(
                templateId = route.templateId,
                creationId = route.creationId,
                onClose = navController::back,
                onPreview = { url, templateId, creationId -> navController.navigate(PreviewRoute(url, templateId, creationId)) },
                onContinue = { navController.navigate(PublishRoute(it)) },
            )
        }
        composable<PreviewRoute> { entry ->
            val route = entry.toRoute<PreviewRoute>()
            PreviewScreenRoute(
                url = route.url,
                creationId = route.creationId,
                onEdit = { navController.returnToEditor(route.templateId, route.creationId) },
                onPublish = { navController.navigate(PublishRoute(it)) },
            )
        }
        composable<PublishRoute> { entry ->
            PublishScreenRoute(
                creationId = entry.toRoute<PublishRoute>().creationId,
                onBack = navController::back,
                onEditDraft = { templateId, creationId -> navController.returnToEditor(templateId, creationId) },
                onPublished = navController::showPublished,
            )
        }
        composable<PublishedRoute> { entry ->
            PublishedScreenRoute(
                creationId = entry.toRoute<PublishedRoute>().creationId,
                onDone = navController::back,
                onOpenPage = links.page(),
            )
        }
        composable<MomentRoute> { entry ->
            MomentScreenRoute(
                creationId = entry.toRoute<MomentRoute>().creationId,
                onBack = navController::back,
                onOpenPage = links.page(),
                onPreview = { url, templateId, creationId -> navController.navigate(PreviewRoute(url, templateId, creationId)) },
                onEdit = { templateId, creationId -> navController.navigate(EditorRoute(templateId, creationId)) },
                onPublish = { navController.navigate(PublishRoute(it)) },
                onViewRsvps = { id, title -> navController.navigate(RsvpsRoute(id, title)) },
            )
        }
        composable<RsvpsRoute> { entry ->
            val route = entry.toRoute<RsvpsRoute>()
            RsvpsScreenRoute(creationId = route.creationId, momentTitle = route.momentTitle, onBack = navController::back)
        }
        composable<PurchasesRoute> {
            PurchasesScreenRoute(onBack = navController::back)
        }
        composable<DeleteAccountRoute> {
            DeleteAccountScreenRoute(onBack = navController::back, onDeleted = navController::restartAtExplore)
        }
    }
}

/** Explore and Category share one feed, held by the Explore graph's back-stack entry. */
@Composable
private fun exploreFeed(navController: NavHostController, entry: NavBackStackEntry): FeedViewModel {
    val graphEntry = remember(entry) { navController.getBackStackEntry<ExploreGraph>() }
    return koinViewModel(graphEntry)
}

/** A published moment's own page opens in the in-app browser — the real thing, as guests see it. */
private fun LinkOpener.page(): (String) -> Unit = { url -> openPage(url) }
