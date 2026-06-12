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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.composables.icons.lucide.BookOpen
import com.composables.icons.lucide.CircleUserRound
import com.composables.icons.lucide.Compass
import com.composables.icons.lucide.Lucide
import com.example.mybookslibrary.R
import com.example.mybookslibrary.domain.model.AuthStatus
import com.example.mybookslibrary.ui.screens.onboarding.CoachMarkOverlay
import com.example.mybookslibrary.ui.screens.onboarding.CoachMarkStep
import com.example.mybookslibrary.ui.screens.onboarding.rememberCoachMarkState
import kotlin.reflect.KClass

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }
val LocalNavAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }
val LocalBottomNavPadding = compositionLocalOf<Dp> { 0.dp }
val LocalWindowWidthSizeClass = staticCompositionLocalOf { WindowWidthSizeClass.Compact }
val LocalSnackbarHostState = staticCompositionLocalOf { SnackbarHostState() }

sealed class BottomNavDestination(
    val destination: Any,
    val routeClass: KClass<out Any>,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    data object DiscoverTab :
        BottomNavDestination(Discover, Discover::class, R.string.nav_discover, Lucide.Compass)

    data object SearchTab :
        BottomNavDestination(Search, Search::class, R.string.nav_search, LucideSearchIcon)

    data object LibraryTab :
        BottomNavDestination(Library, Library::class, R.string.nav_library, Lucide.BookOpen)

    data object ProfileTab :
        BottomNavDestination(Profile, Profile::class, R.string.nav_profile, Lucide.CircleUserRound)
}

internal val bottomDestinations =
    listOf(
        BottomNavDestination.DiscoverTab,
        BottomNavDestination.SearchTab,
        BottomNavDestination.LibraryTab,
        BottomNavDestination.ProfileTab,
    )

@Suppress("CyclomaticComplexMethod", "ComplexCondition", "LongMethod")
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MainNavHost(
    authStatus: AuthStatus,
    incomingMangaId: String? = null,
    onboardingWelcomeDone: Boolean = true,
    onWelcomeDone: () -> Unit = {},
    inAppTourDone: Boolean = true,
    onTourDone: () -> Unit = {},
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val coachMarkState = rememberCoachMarkState()
    val snackbarHostState = remember { SnackbarHostState() }
    val isReturningUser = remember { onboardingWelcomeDone }
    val showTour = authStatus != AuthStatus.LOGGED_OUT && !inAppTourDone

    var navBarVisible by remember { mutableStateOf(true) }
    val navBarScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 1f) {
                    navBarVisible = true
                } else if (available.y < -1f) {
                    navBarVisible = false
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(authStatus) {
        if (authStatus == AuthStatus.LOGGED_OUT) {
            if (currentDestination != null &&
                !currentDestination.hasRoute<Login>() &&
                !currentDestination.hasRoute<Register>() &&
                !currentDestination.hasRoute<Onboarding>()
            ) {
                navController.navigate(Login) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    // Navigate to manga detail when app is opened via share sheet (ACTION_SEND from browser).
    LaunchedEffect(incomingMangaId) {
        if (!incomingMangaId.isNullOrBlank() && authStatus != AuthStatus.LOGGED_OUT) {
            navController.navigate(MangaDetail(incomingMangaId)) {
                launchSingleTop = true
            }
        }
    }

    val showNav =
        currentDestination?.hierarchy?.none { dest ->
            dest.hasRoute<Login>() ||
                dest.hasRoute<Register>() ||
                dest.hasRoute<Onboarding>() ||
                dest.hasRoute<Reader>() ||
                dest.hasRoute<MangaDetail>() ||
                dest.hasRoute<MangaReview>()
        } ?: false

    // Rail CHỈ cho tablet (sw ≥ 600dp); phone landscape vẫn dùng bottom bar
    val useRail = showNav && com.example.mybookslibrary.ui.util.isTablet()

    // KHÔNG dùng saveState/restoreState: secondary screen (Setting, History…) nằm trong
    // saved stack sẽ bị restore lại khi bấm tab → navbar nhìn như không phản ứng.
    val navBarCallback: (BottomNavDestination) -> Unit = { destination ->
        navController.navigate(destination.destination) {
            popUpTo(navController.graph.findStartDestination().id)
            launchSingleTop = true
        }
    }

    @Composable
    fun NavContent(modifier: Modifier = Modifier) {
        SharedTransitionLayout {
            CompositionLocalProvider(
                LocalSharedTransitionScope provides this@SharedTransitionLayout,
                LocalBottomNavPadding provides if (useRail) 0.dp else 80.dp,
                LocalSnackbarHostState provides snackbarHostState,
            ) {
                NavHost(
                    navController = navController,
                    startDestination =
                    when {
                        authStatus == AuthStatus.LOGGED_OUT && !onboardingWelcomeDone -> Onboarding
                        authStatus == AuthStatus.LOGGED_OUT -> Login
                        else -> Discover
                    },
                    modifier = modifier,
                    enterTransition = {
                        fadeIn(tween(250)) + scaleIn(
                            initialScale = 0.94f,
                            animationSpec = tween(250, easing = FastOutSlowInEasing),
                        )
                    },
                    exitTransition = { fadeOut(tween(200)) },
                    popEnterTransition = {
                        fadeIn(tween(250)) + scaleIn(
                            initialScale = 0.94f,
                            animationSpec = tween(250, easing = FastOutSlowInEasing),
                        )
                    },
                    popExitTransition = {
                        fadeOut(tween(200)) + scaleOut(
                            targetScale = 0.94f,
                            animationSpec = tween(200, easing = FastOutSlowInEasing),
                        )
                    },
                ) {
                    onboardingGraph(navController, onWelcomeDone)
                    authGraph(navController, isReturningUser = isReturningUser)
                    mainTabsGraph(navController)
                    mangaDetailGraph(navController)
                    reviewGraph(navController)
                    profileGraph(navController)
                    editProfileGraph(navController)
                    changePasswordGraph(navController)
                    readingHistoryGraph(navController)
                    statisticsGraph(navController)
                    downloadsGraph(navController)
                    readerGraph(navController)
                }
            }
        }
    }

    if (useRail) {
        Row(modifier = Modifier.fillMaxSize().testTag(MAIN_NAV_CONTENT_TAG)) {
            FloatingPillNavBar(
                currentDestination = currentDestination,
                onNavigate = navBarCallback,
                modifier = Modifier.testTag(FLOATING_PILL_NAV_TAG),
                coachMarkState = if (showTour) coachMarkState else null,
            )
            NavContent(modifier = Modifier.weight(1f))
        }
    } else {
        Scaffold(
            bottomBar = {
                if (showNav) {
                    FloatingPillNavBar(
                        currentDestination = currentDestination,
                        onNavigate = navBarCallback,
                        modifier = Modifier.testTag(FLOATING_PILL_NAV_TAG),
                        // Tour cần đo vị trí tab → ép navbar hiện khi tour đang chạy
                        isVisible = navBarVisible || showTour,
                        coachMarkState = if (showTour) coachMarkState else null,
                    )
                }
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { innerPadding ->
            Box(
                modifier =
                    Modifier
                        .consumeWindowInsets(innerPadding)
                        .nestedScroll(navBarScrollConnection)
                        .testTag(MAIN_NAV_CONTENT_TAG),
            ) {
                NavContent()
            }
        }
    }

    CoachMarkOverlay(
        visible = showTour,
        state = coachMarkState,
        steps =
            listOf(
                CoachMarkStep("tab_discover", R.string.tour_step1_title, R.string.tour_step1_body),
                CoachMarkStep("tab_search", R.string.tour_step2_title, R.string.tour_step2_body),
                CoachMarkStep("tab_library", R.string.tour_step3_title, R.string.tour_step3_body),
                CoachMarkStep("tab_settings", R.string.tour_step4_title, R.string.tour_step4_body),
            ),
        onDismiss = onTourDone,
    )
}

internal const val MAIN_NAV_CONTENT_TAG = "main-nav-content"
internal const val FLOATING_PILL_NAV_TAG = "floating-pill-nav"
