package com.example.mybookslibrary.ui.navigation

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import com.example.mybookslibrary.ui.util.appString

@Composable
internal fun FloatingPillNavBar(
    currentDestination: NavDestination?,
    onNavigate: (BottomNavDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 24.dp, top = 12.dp, end = 24.dp, bottom = 8.dp)
            .background(Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 20.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                bottomDestinations.forEach { destination ->
                    val selected =
                        currentDestination?.hierarchy?.any {
                            it.hasRoute(destination.routeClass)
                        } == true
                    PillNavItem(
                        icon = destination.icon,
                        label = appString(destination.labelRes),
                        selected = selected,
                        onClick = { onNavigate(destination) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PillNavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit,) {
    val containerColor =
        if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        } else {
            Color.Transparent
        }

    Box(
        modifier =
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint =
            if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(24.dp),
        )
    }
}
