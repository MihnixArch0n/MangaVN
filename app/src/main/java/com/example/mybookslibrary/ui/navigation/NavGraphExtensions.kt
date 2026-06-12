package com.example.mybookslibrary.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.example.mybookslibrary.ui.screens.MangaReviewScreen
import com.example.mybookslibrary.ui.screens.DownloadsScreen
import com.example.mybookslibrary.ui.screens.EditProfileScreen
import com.example.mybookslibrary.ui.screens.ProfileScreen
import com.example.mybookslibrary.ui.screens.ReadingHistoryScreen
import com.example.mybookslibrary.ui.screens.StatisticsScreen
import com.example.mybookslibrary.ui.screens.auth.ChangePasswordScreen
import com.example.mybookslibrary.ui.screens.auth.LoginScreen
import com.example.mybookslibrary.ui.screens.auth.RegisterScreen
import com.example.mybookslibrary.ui.screens.onboarding.WelcomeCarouselScreen
import com.example.mybookslibrary.ui.screens.detail.MangaDetailScreen
import com.example.mybookslibrary.ui.screens.reader.ReaderScreen
import com.example.mybookslibrary.util.shareManga

internal fun NavGraphBuilder.onboardingGraph(navController: NavHostController, onWelcomeDone: () -> Unit) {
    composable<Onboarding> {
        WelcomeCarouselScreen(
            onFinish = {
                onWelcomeDone()
                navController.navigate(Login) {
                    popUpTo<Onboarding> { inclusive = true }
                }
            },
        )
    }
}

internal fun NavGraphBuilder.authGraph(navController: NavHostController, isReturningUser: Boolean = false) {
    composable<Login> {
        LoginScreen(
            isReturningUser = isReturningUser,
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
        deepLinks = listOf(
            // Pattern 1: https://mangadex.org/title/{mangaId}
            navDeepLink { uriPattern = "https://mangadex.org/title/{mangaId}" },
            // Pattern 2: https://mangadex.org/title/{mangaId}/some-slug
            navDeepLink { uriPattern = "https://mangadex.org/title/{mangaId}/.*" },
        ),
        enterTransition = {
            scaleIn(initialScale = 0.9f, animationSpec = navTween()) + fadeIn(animationSpec = navTween())
        },
        popExitTransition = {
            scaleOut(targetScale = 0.9f, animationSpec = navTween()) + fadeOut(animationSpec = navTween())
        },
    ) { backStackEntry ->
        val route = backStackEntry.toRoute<MangaDetail>()
        val context = LocalContext.current
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
                onShareClick = { mangaTitle ->
                    shareManga(context, route.mangaId, mangaTitle)
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

internal fun NavGraphBuilder.profileGraph(navController: NavHostController) {
    composable<Profile> {
        ProfileScreen(
            onReadingHistoryClick = { navController.navigate(ReadingHistory) },
            onStatisticsClick = { navController.navigate(Statistics) },
            onDownloadsClick = { navController.navigate(Downloads) },
            onEditProfileClick = { navController.navigate(EditProfile) },
            onSettingsClick = {
                navController.navigate(Setting) {
                    launchSingleTop = true
                }
            },
        )
    }
}

internal fun NavGraphBuilder.readingHistoryGraph(navController: NavHostController) {
    composable<ReadingHistory> {
        ReadingHistoryScreen(
            onBackClick = { navController.popBackStack() },
            onMangaClick = { mangaId -> navController.navigate(MangaDetail(mangaId)) },
        )
    }
}

internal fun NavGraphBuilder.editProfileGraph(navController: NavHostController) {
    composable<EditProfile> {
        EditProfileScreen(onBackClick = { navController.popBackStack() })
    }
}

internal fun NavGraphBuilder.changePasswordGraph(navController: NavHostController) {
    composable<ChangePassword> {
        ChangePasswordScreen(onBackClick = { navController.popBackStack() })
    }
}

internal fun NavGraphBuilder.downloadsGraph(navController: NavHostController) {
    composable<Downloads> {
        DownloadsScreen(onBackClick = { navController.popBackStack() })
    }
}

internal fun NavGraphBuilder.statisticsGraph(navController: NavHostController) {
    composable<Statistics> {
        StatisticsScreen(onBackClick = { navController.popBackStack() })
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
