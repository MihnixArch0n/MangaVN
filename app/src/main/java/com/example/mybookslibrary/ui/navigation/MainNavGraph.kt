@file:Suppress("ktlint:standard:property-naming")

package com.example.mybookslibrary.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.mybookslibrary.R
import kotlin.reflect.KClass

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }
val LocalNavAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }
val LocalBottomNavPadding = compositionLocalOf<Dp> { 0.dp }

sealed class BottomNavDestination(
    val destination: Any,
    val routeClass: KClass<out Any>,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    data object DiscoverTab :
        BottomNavDestination(Discover, Discover::class, R.string.nav_discover, Icons.Filled.Home)

    data object SearchTab :
        BottomNavDestination(Search, Search::class, R.string.nav_search, Icons.Filled.Search)

    data object LibraryTab :
        BottomNavDestination(Library, Library::class, R.string.nav_library, Icons.Filled.Favorite)

    data object SettingTab :
        BottomNavDestination(Setting, Setting::class, R.string.nav_setting, Icons.Filled.Person)
}

internal val bottomDestinations =
    listOf(
        BottomNavDestination.DiscoverTab,
        BottomNavDestination.SearchTab,
        BottomNavDestination.LibraryTab,
        BottomNavDestination.SettingTab,
    )

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MainNavHost(loggedInUserId: String?) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    LaunchedEffect(loggedInUserId) {
        if (loggedInUserId == null) {
            if (currentDestination != null &&
                !currentDestination.hasRoute<Login>() &&
                !currentDestination.hasRoute<Register>()
            ) {
                navController.navigate(Login) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    val showBottomBar =
        currentDestination?.hierarchy?.none { dest ->
            dest.hasRoute<Login>() ||
                dest.hasRoute<Register>() ||
                dest.hasRoute<Reader>() ||
                dest.hasRoute<MangaDetail>() ||
                dest.hasRoute<MangaReview>()
        } ?: true
    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                FloatingPillNavBar(
                    currentDestination = currentDestination,
                    onNavigate = { destination ->
                        navController.navigate(destination.destination) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    modifier = Modifier.testTag(FLOATING_PILL_NAV_TAG),
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .consumeWindowInsets(innerPadding)
                    .testTag(MAIN_NAV_CONTENT_TAG),
        ) {
            SharedTransitionLayout {
                CompositionLocalProvider(
                    LocalSharedTransitionScope provides this@SharedTransitionLayout,
                    LocalBottomNavPadding provides innerPadding.calculateBottomPadding(),
                ) {
                    NavHost(
                        navController = navController,
                        startDestination =
                        if (loggedInUserId == null) {
                            Login
                        } else {
                            Discover
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
}

internal const val MAIN_NAV_CONTENT_TAG = "main-nav-content"
internal const val FLOATING_PILL_NAV_TAG = "floating-pill-nav"
