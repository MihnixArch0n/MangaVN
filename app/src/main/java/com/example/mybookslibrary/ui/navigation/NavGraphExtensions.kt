package com.example.mybookslibrary.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.mybookslibrary.ui.screens.DiscoverScreen
import com.example.mybookslibrary.ui.screens.LibraryScreen
import com.example.mybookslibrary.ui.screens.MangaReviewScreen
import com.example.mybookslibrary.ui.screens.SearchScreen
import com.example.mybookslibrary.ui.screens.SettingScreen
import com.example.mybookslibrary.ui.screens.auth.LoginScreen
import com.example.mybookslibrary.ui.screens.auth.RegisterScreen
import com.example.mybookslibrary.ui.screens.detail.MangaDetailScreen
import com.example.mybookslibrary.ui.screens.reader.ReaderScreen

internal fun NavGraphBuilder.authGraph(navController: NavHostController) {
    composable(AuthDestination.Login) {
        LoginScreen(
            onLoginSuccess = {
                navController.navigate(BottomNavDestination.Discover.route) {
                    popUpTo(AuthDestination.Login) { inclusive = true }
                }
            },
            onNavigateToRegister = {
                navController.navigate(AuthDestination.Register)
            },
        )
    }
    composable(AuthDestination.Register) {
        RegisterScreen(
            onRegisterSuccess = { navController.popBackStack() },
            onNavigateToLogin = { navController.popBackStack() },
        )
    }
}

internal fun NavGraphBuilder.mainTabsGraph(navController: NavHostController) {
    composable(
        route = BottomNavDestination.Discover.route,
        enterTransition = { fadeIn(navTween()) + scaleIn(initialScale = 0.95f, animationSpec = navTween()) },
        exitTransition = { fadeOut(navTween()) },
    ) {
        CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this@composable) {
            DiscoverScreen(
                onMangaClick = { manga ->
                    navController.navigate(
                        MangaDetailDestination.createRoute(
                            mangaId = manga.id,
                            title = manga.title,
                            coverArt = manga.coverArt,
                            description = manga.description,
                            tags = manga.tags,
                        ),
                    )
                },
                onSearchClick = {
                    navController.navigateToBottomTab(BottomNavDestination.Search)
                },
                onLibraryClick = {
                    navController.navigateToBottomTab(BottomNavDestination.Library)
                },
                onProfileClick = {
                    navController.navigateToBottomTab(BottomNavDestination.Setting)
                },
            )
        }
    }
    composable(
        route = BottomNavDestination.Search.route,
        enterTransition = { fadeIn(navTween()) + scaleIn(initialScale = 0.95f, animationSpec = navTween()) },
        exitTransition = { fadeOut(navTween()) },
    ) {
        CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this@composable) {
            SearchScreen(
                onMangaClick = { manga ->
                    navController.navigate(
                        MangaDetailDestination.createRoute(
                            mangaId = manga.id,
                            title = manga.title,
                            coverArt = manga.coverArt,
                            description = manga.description,
                            tags = manga.tags,
                        ),
                    )
                },
            )
        }
    }
    composable(
        route = BottomNavDestination.Library.route,
        enterTransition = { fadeIn(navTween()) + scaleIn(initialScale = 0.95f, animationSpec = navTween()) },
        exitTransition = { fadeOut(navTween()) },
    ) {
        CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this@composable) {
            LibraryScreen(
                onOpenDetail = { mangaId, title, coverUrl ->
                    navController.navigate(
                        MangaDetailDestination.createRoute(mangaId, title, coverUrl, "", emptyList()),
                    )
                },
            )
        }
    }
    composable(
        route = BottomNavDestination.Setting.route,
        enterTransition = { fadeIn(navTween()) + scaleIn(initialScale = 0.95f, animationSpec = navTween()) },
        exitTransition = { fadeOut(navTween()) },
    ) {
        SettingScreen()
    }
}

internal fun NavGraphBuilder.mangaDetailGraph(navController: NavHostController) {
    composable(
        route = MangaDetailDestination.routePattern,
        arguments =
            listOf(
                navArgument(MangaDetailDestination.mangaIdArgumentName) { type = NavType.StringType },
                navArgument(MangaDetailDestination.titleArgumentName) { type = NavType.StringType },
                navArgument(MangaDetailDestination.coverArtArgumentName) { type = NavType.StringType },
                navArgument(MangaDetailDestination.descriptionArgumentName) { type = NavType.StringType },
                navArgument(MangaDetailDestination.tagsArgumentName) { type = NavType.StringType },
            ),
        enterTransition = {
            scaleIn(initialScale = 0.9f, animationSpec = navTween()) + fadeIn(animationSpec = navTween())
        },
        popExitTransition = {
            scaleOut(targetScale = 0.9f, animationSpec = navTween()) + fadeOut(animationSpec = navTween())
        },
    ) { backStackEntry ->
        val args = backStackEntry.arguments ?: return@composable
        val tagsString = args.getString(MangaDetailDestination.tagsArgumentName).orEmpty()
        val tags = tagsString.split(",").filter { it.isNotBlank() }
        CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this@composable) {
            MangaDetailScreen(
                mangaId = args.getString(MangaDetailDestination.mangaIdArgumentName).orEmpty(),
                title = args.getString(MangaDetailDestination.titleArgumentName).orEmpty(),
                coverArt = args.getString(MangaDetailDestination.coverArtArgumentName).orEmpty(),
                description = args.getString(MangaDetailDestination.descriptionArgumentName).orEmpty(),
                tags = tags,
                onBackClick = { navController.popBackStack() },
                onReadChapter = { mangaId, chapterId, chapterTitle, startPageIndex ->
                    navController.navigate(
                        ReaderDestination.createRoute(mangaId, chapterId, chapterTitle, startPageIndex),
                    )
                },
                onReviewClick = { mangaId ->
                    navController.navigate(MangaReviewDestination.createRoute(mangaId))
                },
            )
        }
    }
}

internal fun NavGraphBuilder.reviewGraph(navController: NavHostController) {
    composable(
        route = MangaReviewDestination.routePattern,
        arguments =
            listOf(
                navArgument(MangaReviewDestination.mangaIdArgumentName) { type = NavType.StringType },
            ),
    ) {
        MangaReviewScreen(onBackClick = { navController.popBackStack() })
    }
}

internal fun NavGraphBuilder.readerGraph(navController: NavHostController) {
    composable(
        route = ReaderDestination.routePattern,
        arguments =
            listOf(
                navArgument(ReaderDestination.mangaIdArgumentName) { type = NavType.StringType },
                navArgument(ReaderDestination.chapterIdArgumentName) { type = NavType.StringType },
                navArgument(ReaderDestination.chapterTitleArgumentName) { type = NavType.StringType },
                navArgument(ReaderDestination.startPageIndexArgumentName) { type = NavType.IntType },
            ),
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = navTween(),
        ) + fadeIn(animationSpec = navTween())
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = navTween(),
        ) + fadeOut(animationSpec = navTween())
        },
    ) {
        ReaderScreen(onBackClick = { navController.popBackStack() })
    }
}

private fun NavHostController.navigateToBottomTab(destination: BottomNavDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun <T> navTween() = tween<T>(durationMillis = 300, easing = FastOutSlowInEasing)
