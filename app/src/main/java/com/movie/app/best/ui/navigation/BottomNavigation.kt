package com.movie.app.best.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.movie.app.best.ui.theme.AppRed

fun NavController.navigateToBottomTab(route: String) {
    val startDestId = graph.findStartDestination().id
    
    // If we're on a secondary screen (like Categories) and navigating to Home (startDestination),
    // pop the backstack back to the startDestination so the root tab shows immediately.
    if (route == Screen.Home.route) {
        val popped = popBackStack(startDestId, false)
        if (!popped && currentDestination?.route != route) {
            navigate(route) {
                popUpTo(startDestId) {
                    inclusive = false
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
        return
    }

    // For other tabs (Zee5, Downloads, Library), ensure we pop back to startDestination
    // then switch to the tab with state preservation.
    navigate(route) {
        popUpTo(startDestId) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home      : BottomNavItem(Screen.Home.route,      "Home",      Icons.Filled.Home,                Icons.Outlined.Home)
    object Sports    : BottomNavItem(Screen.Sports.route,    "Sports",    Icons.Filled.SportsSoccer,        Icons.Outlined.SportsSoccer)
    object Zee5      : BottomNavItem(Screen.Zee5.route,      "ZEE5",      Icons.Filled.PlayCircle,          Icons.Outlined.PlayCircle)
    object Downloads : BottomNavItem(Screen.Downloads.route, "Downloads", Icons.Filled.Download,            Icons.Outlined.Download)
    object Library   : BottomNavItem(Screen.Library.route,   "Library",   Icons.Filled.Person,              Icons.Outlined.Person)
}

@Composable
fun BottomNavigationBar(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Sports,
        BottomNavItem.Zee5,
        BottomNavItem.Downloads,
        BottomNavItem.Library
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0C0C0C))
            .navigationBarsPadding()
    ) {
        // Subtle top separator hairline
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(Color.White.copy(alpha = 0.08f))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                NavItem(
                    item = item,
                    selected = currentRoute == item.route,
                    onClick = {
                        if (currentRoute != item.route) {
                            navController.navigateToBottomTab(item.route)
                        } else if (item.route == Screen.Home.route) {
                            // If user is already on Home or near home, ensure stack is clean
                            navController.popBackStack(Screen.Home.route, false)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun NavItem(
    item: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    // Snappy, subtle micro-interactions
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = tween(150),
        label = "iconScale"
    )
    val iconTint by animateColorAsState(
        targetValue = if (selected) AppRed else Color(0xFF888888),
        animationSpec = tween(160),
        label = "iconTint"
    )
    val labelTint by animateColorAsState(
        targetValue = if (selected) AppRed else Color(0xFF888888),
        animationSpec = tween(160),
        label = "labelTint"
    )
    val indicatorAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(160),
        label = "indicatorAlpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .widthIn(min = 52.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                contentDescription = item.title,
                tint = iconTint,
                modifier = Modifier
                    .size(22.dp)
                    .scale(iconScale)
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = item.title,
            color = labelTint,
            fontSize = 9.5.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(3.dp))

        // Sleek indicator pill at bottom (fades in cleanly without jumping)
        Box(
            modifier = Modifier
                .size(width = 14.dp, height = 2.5.dp)
                .background(
                    AppRed.copy(alpha = indicatorAlpha),
                    RoundedCornerShape(50)
                )
        )
    }
}
