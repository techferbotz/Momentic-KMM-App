package com.ferbotz.momentic.feature.shell

import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.ferbotz.momentic.designsystem.components.MainTab
import com.ferbotz.momentic.navigation.CategoryRoute
import com.ferbotz.momentic.navigation.EditorRoute
import com.ferbotz.momentic.navigation.ExploreGraph
import com.ferbotz.momentic.navigation.ExploreRoute
import com.ferbotz.momentic.navigation.MomentsGraph
import com.ferbotz.momentic.navigation.MomentsRoute
import com.ferbotz.momentic.navigation.PublishedRoute
import com.ferbotz.momentic.navigation.SettingsGraph
import com.ferbotz.momentic.navigation.SettingsRoute

private val MainTab.graph: Any
    get() = when (this) {
        MainTab.Explore -> ExploreGraph
        MainTab.Moments -> MomentsGraph
        MainTab.Settings -> SettingsGraph
    }

private val MainTab.root: Any
    get() = when (this) {
        MainTab.Explore -> ExploreRoute
        MainTab.Moments -> MomentsRoute
        MainTab.Settings -> SettingsRoute
    }

/** The tab a destination sits in, or null for a screen pushed over the tabs. */
fun NavDestination?.tab(): MainTab? {
    val destination = this ?: return null
    return when {
        destination.hierarchy.any { it.hasRoute<ExploreGraph>() } -> MainTab.Explore
        destination.hierarchy.any { it.hasRoute<MomentsGraph>() } -> MainTab.Moments
        destination.hierarchy.any { it.hasRoute<SettingsGraph>() } -> MainTab.Settings
        else -> null
    }
}

/** The bottom bar shows on the three tab roots and on Category; pushed screens hide it. */
fun NavDestination?.showsBottomBar(): Boolean {
    val destination = this ?: return false
    return destination.hasRoute<ExploreRoute>() || destination.hasRoute<CategoryRoute>() ||
        destination.hasRoute<MomentsRoute>() || destination.hasRoute<SettingsRoute>()
}

/**
 * Goes to [tab], keeping each tab's own back stack: the one being left is saved and the one being
 * entered comes back as it was. Choosing the tab already shown goes back to its root.
 */
fun NavController.navigateToTab(tab: MainTab) {
    if (currentBackStackEntry?.destination.tab() == tab) {
        popBackStack(tab.root, inclusive = false)
        return
    }
    navigate(tab.graph) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * After publishing: "Your moment is live" goes over My Moments, so Back from it lands there — and the
 * template, the form and the paywall behind it are let go rather than saved for later.
 */
fun NavController.showPublished(creationId: String) {
    navigate(MomentsGraph) {
        popUpTo(graph.findStartDestination().id)
        launchSingleTop = true
    }
    navigate(PublishedRoute(creationId))
}

/** Back one screen — never past the first one, which would leave nothing showing (a double tap on Back). */
fun NavController.back() {
    if (previousBackStackEntry != null) popBackStack()
}

/** After the account is deleted: a fresh Explore, with nothing of the account left on the back stack. */
fun NavController.restartAtExplore() {
    navigate(ExploreGraph) {
        popUpTo(graph.id) { inclusive = true }
    }
}

/** Back to the form for [templateId] / [creationId] when it's on the back stack; otherwise opens it. */
fun NavController.returnToEditor(templateId: String, creationId: String) {
    val onStack = runCatching { getBackStackEntry<EditorRoute>() }.getOrNull()
    if (onStack != null && popBackStack<EditorRoute>(inclusive = false)) return
    navigate(EditorRoute(templateId, creationId))
}
