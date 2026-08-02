package com.movie.app.best.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.movie.app.best.ui.theme.AppRed

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home      : BottomNavItem(Screen.Home.route,      "Home",      Icons.Filled.Home,                Icons.Outlined.Home)
    object Zee5      : BottomNavItem(Screen.Zee5.route,      "ZEE5",      Icons.Filled.PlayCircle,          Icons.Outlined.PlayCircle)
    object MyList    : BottomNavItem(Screen.Library.route,   "My List",   Icons.Filled.BookmarkAdded,       Icons.Outlined.BookmarkAdd)
    object Downloads : BottomNavItem(Screen.Downloads.route, "Downloads", Icons.Filled.Download,            Icons.Outlined.Download)
    object Profile   : BottomNavItem(Screen.Profile.route,   "Profile",   Icons.Filled.Person,              Icons.Outlined.Person)
}

@Composable
fun BottomNavigationBar(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Zee5,
        BottomNavItem.MyList,
        BottomNavItem.Downloads,
        BottomNavItem.Profile
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isHiddenScreen = currentRoute?.startsWith("movie/") == true
            || currentRoute?.startsWith("series/") == true
            || currentRoute?.startsWith("videoPlayer") == true
            || currentRoute?.startsWith("category/") == true
            || currentRoute?.startsWith("zee5_detail/") == true
            || currentRoute?.startsWith("zee5_collection/") == true
            || currentRoute?.startsWith("zee5_watch/") == true
            || currentRoute == "login"

    if (isHiddenScreen) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0A0A0A))
            .navigationBarsPadding()
    ) {
        // Top hairline
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(Color.White.copy(alpha = 0.07f))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 0.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                NavItem(
                    item = item,
                    selected = currentRoute == item.route,
                    onClick = {
                        if (currentRoute != item.route) {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
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
    // Smooth animated values — tween only, no spring/bounce
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.18f else 1f,
        animationSpec = tween(200),
        label = "iconScale"
    )
    val iconTint by animateColorAsState(
        targetValue = if (selected) AppRed else Color(0xFF777777),
        animationSpec = tween(220),
        label = "iconTint"
    )
    val labelTint by animateColorAsState(
        targetValue = if (selected) AppRed else Color(0xFF666666),
        animationSpec = tween(220),
        label = "labelTint"
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(250),
        label = "glowAlpha"
    )
    val pillAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(200),
        label = "pillAlpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .widthIn(min = 56.dp)
    ) {
        // Icon area with glow behind
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(36.dp)
        ) {
            // Radial glow — animates in smoothly
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                AppRed.copy(alpha = 0.28f * glowAlpha),
                                AppRed.copy(alpha = 0.10f * glowAlpha),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            )

            // Icon — only icon scales, nothing else moves
            Icon(
                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                contentDescription = item.title,
                tint = iconTint,
                modifier = Modifier
                    .size(22.dp)
                    .scale(iconScale)
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        // Label — stays in place, only color animates
        Text(
            text = item.title,
            color = labelTint,
            fontSize = 9.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Selected indicator dot at bottom
        Box(
            modifier = Modifier
                .size(width = 16.dp, height = 2.5.dp)
                .background(
                    AppRed.copy(alpha = pillAlpha),
                    RoundedCornerShape(50)
                )
        )
    }
}
