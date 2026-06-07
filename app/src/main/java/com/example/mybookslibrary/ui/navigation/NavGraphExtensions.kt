package com.example.mybookslibrary.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.mybookslibrary.ui.screens.MangaReviewScreen
import com.example.mybookslibrary.ui.screens.auth.LoginScreen
import com.example.mybookslibrary.ui.screens.auth.RegisterScreen
import com.example.mybookslibrary.ui.screens.detail.MangaDetailScreen
import com.example.mybookslibrary.ui.screens.reader.ReaderScreen

internal fun NavGraphBuilder.authGraph(navController: NavHostController) {
    composable<Login> {
        LoginScreen(
            onLoginSuccess = {
                navController.navigate(Discover) {
                    popUpTo<Login> { inclusive = true }
                }
            },
            onNavigateToRegister = {
                navController.navigate(Register)
            },
        )
    }
    composable<Register> {
        RegisterScreen(
            onRegisterSuccess = { navController.popBackStack() },
            onNavigateToLogin = { navController.popBackStack() },
        )
    }
}

internal fun NavGraphBuilder.mangaDetailGraph(navController: NavHostController) {
    composable<MangaDetail>(
        enterTransition = {
            scaleIn(initialScale = 0.9f, animationSpec = navTween()) + fadeIn(animationSpec = navTween())
        },
        popExitTransition = {
            scaleOut(targetScale = 0.9f, animationSpec = navTween()) + fadeOut(animationSpec = navTween())
        },
    ) { backStackEntry ->
        val route = backStackEntry.toRoute<MangaDetail>()
        CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this@composable) {
            MangaDetailScreen(
                mangaId = route.mangaId,
                onBackClick = { navController.popBackStack() },
                onReadChapter = { mangaId, chapterId, chapterTitle, startPageIndex ->
                    navController.navigate(Reader(mangaId, chapterId, chapterTitle, startPageIndex))
                },
                onReviewClick = { mangaId ->
                    navController.navigate(MangaReview(mangaId))
                },
            )
        }
    }
}

internal fun NavGraphBuilder.reviewGraph(navController: NavHostController) {
    composable<MangaReview> {
        MangaReviewScreen(onBackClick = { navController.popBackStack() })
    }
}

internal fun NavGraphBuilder.readerGraph(navController: NavHostController) {
    composable<Reader>(
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

private fun <T> navTween() = tween<T>(durationMillis = 300, easing = FastOutSlowInEasing)
