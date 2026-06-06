@file:Suppress("ktlint:standard:property-naming")

package com.example.mybookslibrary.ui.navigation

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.mybookslibrary.R

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }
val LocalNavAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

sealed class BottomNavDestination(val route: String, @param:StringRes val labelRes: Int, val icon: ImageVector,) {
    data object Discover : BottomNavDestination("discover", R.string.nav_discover, Icons.Filled.Home)

    data object Search : BottomNavDestination("search", R.string.nav_search, Icons.Filled.Search)

    data object Library : BottomNavDestination("library", R.string.nav_library, Icons.Filled.Favorite)

    data object Setting : BottomNavDestination("setting", R.string.nav_setting, Icons.Filled.Person)
}

internal val bottomDestinations =
    listOf(
        BottomNavDestination.Discover,
        BottomNavDestination.Search,
        BottomNavDestination.Library,
        BottomNavDestination.Setting,
    )

object AuthDestination {
    const val Login = "login"
    const val Register = "register"
}

object ReaderDestination {
    const val route = "reader"
    private const val mangaIdArg = "mangaId"
    private const val chapterIdArg = "chapterId"
    private const val chapterTitleArg = "chapterTitle"
    private const val startPageIndexArg = "startPageIndex"
    const val routePattern = "$route/{$mangaIdArg}/{$chapterIdArg}/{$chapterTitleArg}/{$startPageIndexArg}"

    fun createRoute(mangaId: String, chapterId: String, chapterTitle: String, startPageIndex: Int,) =
        "$route/${Uri.encode(mangaId)}/${Uri.encode(chapterId)}/${Uri.encode(chapterTitle)}/$startPageIndex"

    const val mangaIdArgumentName = mangaIdArg
    const val chapterIdArgumentName = chapterIdArg
    const val chapterTitleArgumentName = chapterTitleArg
    const val startPageIndexArgumentName = startPageIndexArg
}

object MangaDetailDestination {
    private const val NAV_ARG_DESC_MAX_LENGTH = 600
    const val route = "manga_detail"
    private const val mangaIdArg = "mangaId"
    private const val titleArg = "title"
    private const val coverArtArg = "coverArt"
    private const val descriptionArg = "description"
    private const val tagsArg = "tags"
    const val routePattern = "$route/{$mangaIdArg}/{$titleArg}/{$coverArtArg}/{$descriptionArg}/{$tagsArg}"

    fun createRoute(
        mangaId: String,
        title: String,
        coverArt: String?,
        description: String,
        tags: List<String>,
    ): String {
        val safeDesc = description.take(NAV_ARG_DESC_MAX_LENGTH)
        val safeTags = tags.take(3).joinToString(",")
        return "$route/${Uri.encode(mangaId)}/${Uri.encode(title)}/${Uri.encode(coverArt ?: "")}/" +
            "${Uri.encode(safeDesc)}/${Uri.encode(safeTags)}"
    }

    const val mangaIdArgumentName = mangaIdArg
    const val titleArgumentName = titleArg
    const val coverArtArgumentName = coverArtArg
    const val descriptionArgumentName = descriptionArg
    const val tagsArgumentName = tagsArg
}

object MangaReviewDestination {
    const val route = "manga_review"
    private const val mangaIdArg = "mangaId"
    const val routePattern = "$route/{$mangaIdArg}"

    fun createRoute(mangaId: String) = "$route/${Uri.encode(mangaId)}"

    const val mangaIdArgumentName = mangaIdArg
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MainNavHost(loggedInUserId: String?) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    LaunchedEffect(loggedInUserId) {
        if (loggedInUserId == null) {
            val current = currentDestination?.route
            if (current != null &&
                current != AuthDestination.Login &&
                current != AuthDestination.Register
            ) {
                navController.navigate(AuthDestination.Login) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    val showBottomBar =
        currentDestination?.hierarchy?.none { dest ->
            dest.route == AuthDestination.Login ||
                dest.route == AuthDestination.Register ||
                dest.route?.startsWith(ReaderDestination.route) == true ||
                dest.route?.startsWith(MangaDetailDestination.route) == true ||
                dest.route?.startsWith(MangaReviewDestination.route) == true
        } ?: true
    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                FloatingPillNavBar(
                    currentDestination = currentDestination,
                    onNavigate = { destination ->
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) {
        SharedTransitionLayout {
            CompositionLocalProvider(LocalSharedTransitionScope provides this@SharedTransitionLayout) {
                NavHost(
                    navController = navController,
                    startDestination =
                    if (loggedInUserId == null) {
                        AuthDestination.Login
                    } else {
                        BottomNavDestination.Discover.route
                    },
                    enterTransition = { fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)) },
                    exitTransition = { fadeOut(animationSpec = tween(300, easing = FastOutSlowInEasing)) },
                    popEnterTransition = { fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)) },
                    popExitTransition = { fadeOut(animationSpec = tween(300, easing = FastOutSlowInEasing)) },
                ) {
                    authGraph(navController)
                    mainTabsGraph(navController)
                    mangaDetailGraph(navController)
                    reviewGraph(navController)
                    readerGraph(navController)
                }
            }
        }
    }
}
