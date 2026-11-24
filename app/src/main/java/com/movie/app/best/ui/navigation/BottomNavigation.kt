package com.movie.app.best.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.movie.app.best.data.settings.VisualSettings
import com.movie.app.best.ui.theme.AppRed
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

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
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Sports,
        BottomNavItem.Downloads,
        BottomNavItem.Library
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val glassOn = !VisualSettings.navGlassDisabled && hazeState != null
    val glassShape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (glassOn) {
                    Modifier
                        .clip(glassShape)
                        .hazeEffect(
                            state = hazeState,
                            style = HazeStyle(
                                backgroundColor = Color(0xFF0C0C0C),
                                tint = HazeTint(Color(0xFF0C0C0C).copy(alpha = 0.55f)),
                                blurRadius = 24.dp,
                                noiseFactor = 0.05f
                            )
                        )
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.08f), Color.Transparent)
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.20f), Color.White.copy(alpha = 0.04f))
                            ),
                            shape = glassShape
                        )
                } else {
                    Modifier
                        .clip(glassShape)
                        .background(Color(0xFF0C0C0C))
                }
            )
            .navigationBarsPadding()
    ) {
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

        // Blaze Red top hairline — brand signature, shown in both glass & flat modes
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            AppRed.copy(alpha = 0.45f),
                            AppRed.copy(alpha = 0.45f),
                            Color.Transparent
                        )
                    )
                )
        )
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
        targetValue = if (selected) 1.06f else 1f,
        animationSpec = tween(150),
        label = "iconScale"
    )
    // Selected content is pure white so it stays crisp on the red bubble; inactive stays muted grey.
    val contentTint by animateColorAsState(
        targetValue = if (selected) Color.White else Color(0xFF888888),
        animationSpec = tween(160),
        label = "contentTint"
    )
    val bubbleAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(180),
        label = "bubbleAlpha"
    )
    val bubbleScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.88f,
        animationSpec = tween(180),
        label = "bubbleScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .widthIn(min = 60.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        // Active bubble — translucent Blaze Red glass with a thin red/white border
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    alpha = bubbleAlpha
                    scaleX = bubbleScale
                    scaleY = bubbleScale
                }
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFFDC2626).copy(alpha = 0.32f),
                            Color(0xFFDC2626).copy(alpha = 0.20f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.35f),
                            Color(0xFFDC2626).copy(alpha = 0.55f)
                        )
                    ),
                    shape = RoundedCornerShape(18.dp)
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                contentDescription = item.title,
                tint = contentTint,
                modifier = Modifier
                    .size(22.dp)
                    .scale(iconScale)
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = item.title,
                color = contentTint,
                fontSize = 9.5.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1
            )
        }
    }
}
