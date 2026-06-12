package com.example.mybookslibrary.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import com.example.mybookslibrary.ui.screens.onboarding.CoachMarkState
import com.example.mybookslibrary.ui.theme.Alphas
import com.example.mybookslibrary.ui.theme.Dimens
import com.example.mybookslibrary.ui.util.appString
import com.example.mybookslibrary.ui.util.rememberAppHaptic

@Composable
internal fun FloatingPillNavBar(
    currentDestination: NavDestination?,
    onNavigate: (BottomNavDestination) -> Unit,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
    coachMarkState: CoachMarkState? = null,
) {
    if (!com.example.mybookslibrary.ui.util.isTablet()) {
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
        ) {
            PillBottomBar(currentDestination, onNavigate, modifier, coachMarkState)
        }
    } else {
        RailSideBar(currentDestination, onNavigate, modifier, coachMarkState)
    }
}

@Composable
private fun PillBottomBar(
    currentDestination: NavDestination?,
    onNavigate: (BottomNavDestination) -> Unit,
    modifier: Modifier = Modifier,
    coachMarkState: CoachMarkState? = null,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Dimens.SpacingXl, vertical = Dimens.SpacingSm),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        MaterialTheme.shapes.extraLarge,
                    )
                    .padding(horizontal = Dimens.SpacingSm, vertical = Dimens.SpacingSm),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            bottomDestinations.forEach { destination ->
                val selected =
                    currentDestination?.hierarchy?.any {
                        it.hasRoute(destination.routeClass)
                    } == true
                val coachKey = navDestinationCoachKey(destination)
                PillNavItem(
                    icon = destination.icon,
                    label = appString(destination.labelRes),
                    selected = selected,
                    onClick = { onNavigate(destination) },
                    modifier =
                        if (coachMarkState != null && coachKey != null) {
                            Modifier.onGloballyPositioned { coachMarkState.registerTarget(coachKey, it) }
                        } else {
                            Modifier
                        },
                )
            }
        }
    }
}

@Composable
private fun RailSideBar(
    currentDestination: NavDestination?,
    onNavigate: (BottomNavDestination) -> Unit,
    modifier: Modifier = Modifier,
    coachMarkState: CoachMarkState? = null,
) {
    Column(
        modifier =
            modifier
                .width(72.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(vertical = Dimens.SpacingLg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingXs, Alignment.Top),
    ) {
        bottomDestinations.forEach { destination ->
            val selected =
                currentDestination?.hierarchy?.any {
                    it.hasRoute(destination.routeClass)
                } == true
            val coachKey = navDestinationCoachKey(destination)
            RailNavItem(
                icon = destination.icon,
                label = appString(destination.labelRes),
                selected = selected,
                onClick = { onNavigate(destination) },
                modifier =
                    if (coachMarkState != null && coachKey != null) {
                        Modifier.onGloballyPositioned { coachMarkState.registerTarget(coachKey, it) }
                    } else {
                        Modifier
                    },
            )
        }
    }
}

private fun navDestinationCoachKey(destination: BottomNavDestination): String? =
    when (destination) {
        BottomNavDestination.DiscoverTab -> "tab_discover"
        BottomNavDestination.SearchTab -> "tab_search"
        BottomNavDestination.LibraryTab -> "tab_library"
        BottomNavDestination.ProfileTab -> "tab_settings"
    }

@Composable
private fun PillNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = rememberAppHaptic()
    val bgColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = Alphas.ContainerSelected) else Color.Transparent,
        label = "pillBg",
    )
    val tintColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "pillTint",
    )
    val horizontalPad by animateDpAsState(
        targetValue = if (selected) 16.dp else 12.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "pillPad",
    )

    Row(
        modifier =
            modifier
                .clip(CircleShape)
                .background(bgColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Tab,
                    onClick = {
                        haptic.confirm()
                        onClick()
                    },
                )
                .padding(horizontal = horizontalPad, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = label, tint = tintColor, modifier = Modifier.size(22.dp))
        AnimatedVisibility(
            visible = selected,
            enter = slideInHorizontally { -it / 2 } + fadeIn(),
            exit = slideOutHorizontally { -it / 2 } + fadeOut(),
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = tintColor,
                modifier = Modifier.padding(start = Dimens.SpacingSm),
            )
        }
    }
}

@Composable
private fun RailNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = rememberAppHaptic()
    val bgColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = Alphas.ContainerSelected) else Color.Transparent,
        label = "railBg",
    )
    val tintColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "railTint",
    )

    Column(
        modifier =
            modifier
                .clip(CircleShape)
                .background(bgColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Tab,
                    onClick = {
                        haptic.confirm()
                        onClick()
                    },
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = label, tint = tintColor, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = tintColor)
    }
}
