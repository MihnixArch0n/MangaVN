package com.example.mybookslibrary.ui.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.mybookslibrary.domain.model.MangaModel
import com.example.mybookslibrary.ui.screens.DiscoverScreen
import com.example.mybookslibrary.ui.screens.LibraryScreen
import com.example.mybookslibrary.ui.screens.SearchScreen
import com.example.mybookslibrary.ui.screens.SettingScreen

internal fun NavGraphBuilder.mainTabsGraph(navController: NavHostController) {
    discoverTab(navController)
    searchTab(navController)
    libraryTab(navController)
    settingTab()
}

private fun NavGraphBuilder.discoverTab(navController: NavHostController) {
    composable<Discover>(
        enterTransition = { fadeIn(tabTween()) + scaleIn(initialScale = 0.95f, animationSpec = tabTween()) },
        exitTransition = { fadeOut(tabTween()) },
    ) {
        CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this@composable) {
            DiscoverScreen(
                onMangaClick = { manga -> navController.navigateToDetail(manga) },
                onSearchClick = { navController.navigateToBottomTab(Search) },
                onLibraryClick = { navController.navigateToBottomTab(Library) },
                onProfileClick = { navController.navigateToBottomTab(Setting) },
            )
        }
    }
}

private fun NavGraphBuilder.searchTab(navController: NavHostController) {
    composable<Search>(
        enterTransition = { fadeIn(tabTween()) + scaleIn(initialScale = 0.95f, animationSpec = tabTween()) },
        exitTransition = { fadeOut(tabTween()) },
    ) {
        CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this@composable) {
            SearchScreen(
                onMangaClick = { manga -> navController.navigateToDetail(manga) },
            )
        }
    }
}

private fun NavGraphBuilder.libraryTab(navController: NavHostController) {
    composable<Library>(
        enterTransition = { fadeIn(tabTween()) + scaleIn(initialScale = 0.95f, animationSpec = tabTween()) },
        exitTransition = { fadeOut(tabTween()) },
    ) {
        CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this@composable) {
            LibraryScreen(
                onOpenDetail = { mangaId ->
                    navController.navigate(MangaDetail(mangaId))
                },
            )
        }
    }
}

private fun NavGraphBuilder.settingTab() {
    composable<Setting>(
        enterTransition = { fadeIn(tabTween()) + scaleIn(initialScale = 0.95f, animationSpec = tabTween()) },
        exitTransition = { fadeOut(tabTween()) },
    ) {
        SettingScreen()
    }
}

private fun <T : Any> NavHostController.navigateToBottomTab(destination: T) {
    navigate(destination) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavHostController.navigateToDetail(manga: MangaModel) {
    navigate(MangaDetail(manga.id))
}

private fun <T> tabTween() = tween<T>(durationMillis = 300, easing = FastOutSlowInEasing)
