package com.example.mybookslibrary.ui.navigation

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.mybookslibrary.ui.util.appString
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import com.example.mybookslibrary.R
import com.example.mybookslibrary.ui.screens.DiscoverScreen
import com.example.mybookslibrary.ui.screens.LibraryScreen
import com.example.mybookslibrary.ui.screens.SearchScreen
import com.example.mybookslibrary.ui.screens.SettingScreen
import com.example.mybookslibrary.ui.screens.reader.ReaderScreen
import com.example.mybookslibrary.ui.screens.detail.MangaDetailScreen
import com.example.mybookslibrary.ui.screens.MangaReviewScreen

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }
val LocalNavAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

sealed class BottomNavDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector
) {
    data object Discover : BottomNavDestination("discover", R.string.nav_discover, Icons.Filled.Home)
    data object Search : BottomNavDestination("search", R.string.nav_search, Icons.Filled.Search)
    data object Library : BottomNavDestination("library", R.string.nav_library, Icons.Filled.Favorite)
    data object Setting : BottomNavDestination("setting", R.string.nav_setting, Icons.Filled.Person)
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
        mangaId: String, chapterId: String, chapterTitle: String, startPageIndex: Int
    ) = "$route/${Uri.encode(mangaId)}/${Uri.encode(chapterId)}/${Uri.encode(chapterTitle)}/$startPageIndex"

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
        mangaId: String, title: String, coverArt: String?, description: String, tags: List<String>
    ): String {
        val safeDesc = description.take(NAV_ARG_DESC_MAX_LENGTH)
        val safeTags = tags.take(3).joinToString(",")
        return "$route/${Uri.encode(mangaId)}/${Uri.encode(title)}/${Uri.encode(coverArt ?: "")}/${Uri.encode(safeDesc)}/${Uri.encode(safeTags)}"
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
fun MainNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = currentDestination?.hierarchy?.none { dest ->
        dest.route?.startsWith(ReaderDestination.route) == true ||
            dest.route?.startsWith(MangaDetailDestination.route) == true ||
            dest.route?.startsWith(MangaReviewDestination.route) == true
    } ?: true

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                FloatingPillNavBar(
                    destinations = bottomDestinations,
                    currentDestination = currentDestination,
                    onNavigate = { destination ->
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        SharedTransitionLayout {
            CompositionLocalProvider(LocalSharedTransitionScope provides this@SharedTransitionLayout) {
                NavHost(
                    navController = navController,
                    startDestination = BottomNavDestination.Discover.route,
                    modifier = Modifier.padding(top = innerPadding.calculateTopPadding(), bottom = 0.dp),
                    enterTransition = { fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)) },
                    exitTransition = { fadeOut(animationSpec = tween(300, easing = FastOutSlowInEasing)) },
                    popEnterTransition = { fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)) },
                    popExitTransition = { fadeOut(animationSpec = tween(300, easing = FastOutSlowInEasing)) }
                ) {
            composable(
                route = BottomNavDestination.Discover.route,
                enterTransition = { fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)) + scaleIn(initialScale = 0.95f, animationSpec = tween(300, easing = FastOutSlowInEasing)) },
                exitTransition = { fadeOut(animationSpec = tween(300, easing = FastOutSlowInEasing)) }
            ) {
                CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this@composable) {
                    DiscoverScreen(
                        onMangaClick = { manga ->
                            navController.navigate(
                                MangaDetailDestination.createRoute(
                                    mangaId = manga.id, title = manga.title,
                                    coverArt = manga.coverArt, description = manga.description, tags = manga.tags
                                )
                            )
                        },
                        onSearchClick = {
                            navController.navigate(BottomNavDestination.Search.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true; restoreState = true
                            }
                        },
                        onLibraryClick = {
                            navController.navigate(BottomNavDestination.Library.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true; restoreState = true
                            }
                        },
                        onProfileClick = {
                            navController.navigate(BottomNavDestination.Setting.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true; restoreState = true
                            }
                        }
                    )
                }
            }
            composable(
                route = BottomNavDestination.Search.route,
                enterTransition = { fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)) + scaleIn(initialScale = 0.95f, animationSpec = tween(300, easing = FastOutSlowInEasing)) },
                exitTransition = { fadeOut(animationSpec = tween(300, easing = FastOutSlowInEasing)) }
            ) {
                CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this@composable) {
                    SearchScreen(
                        onMangaClick = { manga ->
                            navController.navigate(
                                MangaDetailDestination.createRoute(
                                    mangaId = manga.id, title = manga.title,
                                    coverArt = manga.coverArt, description = manga.description, tags = manga.tags
                                )
                            )
                        }
                    )
                }
            }
            composable(
                route = BottomNavDestination.Library.route,
                enterTransition = { fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)) + scaleIn(initialScale = 0.95f, animationSpec = tween(300, easing = FastOutSlowInEasing)) },
                exitTransition = { fadeOut(animationSpec = tween(300, easing = FastOutSlowInEasing)) }
            ) {
                CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this@composable) {
                    LibraryScreen(
                        onOpenDetail = { mangaId, title, coverUrl ->
                            navController.navigate(
                                MangaDetailDestination.createRoute(mangaId, title, coverUrl, "", emptyList())
                            )
                        }
                    )
                }
            }
            composable(
                route = BottomNavDestination.Setting.route,
                enterTransition = { fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)) + scaleIn(initialScale = 0.95f, animationSpec = tween(300, easing = FastOutSlowInEasing)) },
                exitTransition = { fadeOut(animationSpec = tween(300, easing = FastOutSlowInEasing)) }
            ) { SettingScreen() }
            composable(
                route = MangaDetailDestination.routePattern,
                arguments = listOf(
                    navArgument(MangaDetailDestination.mangaIdArgumentName) { type = NavType.StringType },
                    navArgument(MangaDetailDestination.titleArgumentName) { type = NavType.StringType },
                    navArgument(MangaDetailDestination.coverArtArgumentName) { type = NavType.StringType },
                    navArgument(MangaDetailDestination.descriptionArgumentName) { type = NavType.StringType },
                    navArgument(MangaDetailDestination.tagsArgumentName) { type = NavType.StringType }
                ),
                enterTransition = {
                    scaleIn(initialScale = 0.9f, animationSpec = tween(300, easing = FastOutSlowInEasing)) +
                            fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing))
                },
                popExitTransition = {
                    scaleOut(targetScale = 0.9f, animationSpec = tween(300, easing = FastOutSlowInEasing)) +
                            fadeOut(animationSpec = tween(300, easing = FastOutSlowInEasing))
                }
            ) { backStackEntry ->
                val args = backStackEntry.arguments ?: return@composable
                val tagsString = args.getString(MangaDetailDestination.tagsArgumentName) ?: ""
                val tags = tagsString.split(",").filter { it.isNotBlank() }
                CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this@composable) {
                    MangaDetailScreen(
                        mangaId = args.getString(MangaDetailDestination.mangaIdArgumentName) ?: "",
                        title = args.getString(MangaDetailDestination.titleArgumentName) ?: "",
                        coverArt = args.getString(MangaDetailDestination.coverArtArgumentName) ?: "",
                        description = args.getString(MangaDetailDestination.descriptionArgumentName) ?: "",
                        tags = tags,
                        onBackClick = { navController.popBackStack() },
                        onReadChapter = { mangaId, chapterId, chapterTitle ->
                            navController.navigate(
                                ReaderDestination.createRoute(mangaId, chapterId, chapterTitle, 0)
                            )
                        },
                        onReviewClick = { mangaId ->
                            navController.navigate(MangaReviewDestination.createRoute(mangaId))
                        }
                    )
                }
            }
            composable(
                route = MangaReviewDestination.routePattern,
                arguments = listOf(
                    navArgument(MangaReviewDestination.mangaIdArgumentName) { type = NavType.StringType }
                )
            ) {
                MangaReviewScreen(onBackClick = { navController.popBackStack() })
            }
            composable(
                route = ReaderDestination.routePattern,
                arguments = listOf(
                    navArgument(ReaderDestination.mangaIdArgumentName) { type = NavType.StringType },
                    navArgument(ReaderDestination.chapterIdArgumentName) { type = NavType.StringType },
                    navArgument(ReaderDestination.chapterTitleArgumentName) { type = NavType.StringType },
                    navArgument(ReaderDestination.startPageIndexArgumentName) { type = NavType.IntType }
                ),
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing))
                },
                popExitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(300, easing = FastOutSlowInEasing))
                }
            ) { ReaderScreen(onBackClick = { navController.popBackStack() }) }
        }
            }
        }
    }
}

@Composable
private fun FloatingPillNavBar(
    destinations: List<BottomNavDestination>,
    currentDestination: NavDestination?,
    onNavigate: (BottomNavDestination) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 32.dp, vertical = 16.dp)
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                destinations.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                    PillNavItem(
                        icon = destination.icon,
                        label = appString(destination.labelRes),
                        selected = selected,
                        onClick = { onNavigate(destination) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PillNavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
    }
}
