package com.example.mybookslibrary.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.mybookslibrary.ui.screens.DiscoverScreen
import com.example.mybookslibrary.ui.screens.LibraryScreen
import com.example.mybookslibrary.ui.screens.MangaDetailScreen
import com.example.mybookslibrary.ui.screens.SearchScreen
import com.example.mybookslibrary.ui.screens.SettingScreen
import com.example.mybookslibrary.ui.screens.reader.ReaderScreen

sealed class BottomNavDestination(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    data object Discover : BottomNavDestination(
        route = "discover",
        label = "Discover",
        icon = Icons.Filled.Favorite
    )

    data object Search : BottomNavDestination(
        route = "search",
        label = "Search",
        icon = Icons.Filled.Search
    )

    data object Library : BottomNavDestination(
        route = "library",
        label = "Library",
        icon = Icons.Filled.Favorite
    )

    data object Setting : BottomNavDestination(
        route = "setting",
        label = "Setting",
        icon = Icons.Filled.Person
    )
}

private val bottomDestinations = listOf(
    BottomNavDestination.Discover,
    BottomNavDestination.Search,
    BottomNavDestination.Library,
    BottomNavDestination.Setting
)

object ReaderDestination {
    const val route = "reader"
    private const val mangaIdArg = "mangaId"
    private const val chapterIdArg = "chapterId"
    private const val chapterTitleArg = "chapterTitle"
    private const val startPageIndexArg = "startPageIndex"
    const val routePattern = "$route/{$mangaIdArg}/{$chapterIdArg}/{$chapterTitleArg}/{$startPageIndexArg}"

    fun createRoute(
        mangaId: String,
        chapterId: String,
        chapterTitle: String,
        startPageIndex: Int
    ): String {
        return "$route/${Uri.encode(mangaId)}/${Uri.encode(chapterId)}/${Uri.encode(chapterTitle)}/$startPageIndex"
    }

    const val mangaIdArgumentName = mangaIdArg
    const val chapterIdArgumentName = chapterIdArg
    const val chapterTitleArgumentName = chapterTitleArg
    const val startPageIndexArgumentName = startPageIndexArg
}

object MangaDetailDestination {
    const val route = "mangaDetail"
    private const val mangaIdArg = "mangaId"
    const val routePattern = "$route/{$mangaIdArg}"

    fun createRoute(mangaId: String): String = "$route/${Uri.encode(mangaId)}"

    const val mangaIdArgumentName = mangaIdArg
}

@Composable
fun MainNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = currentDestination?.hierarchy?.any {
        it.route?.startsWith(ReaderDestination.route) == true ||
            it.route?.startsWith(MangaDetailDestination.route) == true
    } != true

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomBar(
                    destinations = bottomDestinations,
                    currentDestination = currentDestination,
                    onNavigate = { destination ->
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavDestination.Discover.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavDestination.Discover.route) {
                DiscoverScreen()
            }
            composable(BottomNavDestination.Search.route) {
                SearchScreen()
            }
            composable(BottomNavDestination.Library.route) {
                LibraryScreen(
                    onOpenMangaDetail = { mangaId ->
                        navController.navigate(MangaDetailDestination.createRoute(mangaId))
                    }
                )
            }
            composable(BottomNavDestination.Setting.route) {
                SettingScreen()
            }
            composable(
                route = MangaDetailDestination.routePattern,
                arguments = listOf(
                    navArgument(MangaDetailDestination.mangaIdArgumentName) {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                MangaDetailScreen(
                    mangaId = backStackEntry
                        .arguments
                        ?.getString(MangaDetailDestination.mangaIdArgumentName)
                        .orEmpty()
                )
            }
            composable(
                route = ReaderDestination.routePattern,
                arguments = listOf(
                    navArgument(ReaderDestination.mangaIdArgumentName) {
                        type = NavType.StringType
                    },
                    navArgument(ReaderDestination.chapterIdArgumentName) {
                        type = NavType.StringType
                    },
                    navArgument(ReaderDestination.chapterTitleArgumentName) {
                        type = NavType.StringType
                    },
                    navArgument(ReaderDestination.startPageIndexArgumentName) {
                        type = NavType.IntType
                    }
                )
            ) {
                ReaderScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun BottomBar(
    destinations: List<BottomNavDestination>,
    currentDestination: NavDestination?,
    onNavigate: (BottomNavDestination) -> Unit
) {
    NavigationBar {
        destinations.forEach { destination ->
            val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(destination) },
                icon = { Icon(imageVector = destination.icon, contentDescription = destination.label) },
                label = { Text(destination.label) }
            )
        }
    }
}

