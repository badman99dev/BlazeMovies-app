package com.movie.app.best.ui.screens.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.movie.app.best.R
import com.movie.app.best.data.model.AppUser
import com.movie.app.best.ui.components.CompactPageHeader
import com.movie.app.best.ui.components.PageHeaderIconButton
import com.movie.app.best.ui.screens.auth.AuthViewModel
import com.movie.app.best.ui.theme.AppRed

@Composable
fun ProfileScreen(
    onLoginClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onNotificationClick: () -> Unit = {},
    onBookmarksClick: () -> Unit = {},
    authViewModel: AuthViewModel = hiltViewModel(),
    firebaseRepository: com.movie.app.best.data.repository.FirebaseRepository = hiltViewModel<com.movie.app.best.ui.screens.profile.ProfileFirebaseViewModel>().firebaseRepository
) {
    val authState by authViewModel.uiState.collectAsState()
    var userTier by remember { mutableStateOf("normal_user") }

    LaunchedEffect(Unit) {
        authViewModel.refreshAuthState()
        authViewModel.autoCheckVerification()
        try {
            val profile = firebaseRepository.getOrCreateUserProfile()
            if (profile != null) userTier = profile.tier
        } catch (_: Exception) {}
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
    ) {
        CompactPageHeader(
            title = "Profile",
            actions = {
                PageHeaderIconButton(onClick = onNotificationClick) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(22.dp)
                    )
                }
                PageHeaderIconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        )

        if (authState.isLoggedIn && authState.user != null) {
            val user = authState.user!!
            LoggedInView(
                user = user,
                userTier = userTier,
                needsVerification = authState.needsVerification,
                isLoggingOut = authState.isLoggingOut,
                onLogoutClick = { authViewModel.logout() },
                onBookmarksClick = onBookmarksClick
            )
        } else {
            LoggedOutView(onLoginClick = onLoginClick)
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun LoggedInView(
    user: AppUser,
    userTier: String,
    needsVerification: Boolean,
    isLoggingOut: Boolean,
    onLogoutClick: () -> Unit,
    onBookmarksClick: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            AppRed.copy(alpha = 0.12f),
                            Color.Black
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box {
                    if (!user.avatarUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = user.avatarUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF333333))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(AppRed.copy(alpha = 0.5f), AppRed.copy(alpha = 0.2f)),
                                        start = Offset.Zero,
                                        end = Offset(64.dp.value, 64.dp.value)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (user.firstName?.take(1) ?: user.username.take(1)).uppercase(),
                                color = Color.White,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (user.isVerified == 1) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 2.dp, y = (-2).dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_verified_badge),
                                contentDescription = "Verified",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = listOfNotNull(user.firstName, user.lastName).joinToString(" ").ifBlank { user.username },
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = user.email,
                    color = Color.White.copy(alpha = 0.35f),
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ProfileStat(value = if (user.isVerified == 1) "Verified" else "Unverified", label = "Status")
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(32.dp)
                            .background(Color.White.copy(alpha = 0.08f))
                    )
                    val roleDisplay = when (userTier) {
                        "vip_user" -> "VIP 💎"
                        "moderator" -> "Moderator 🛡️"
                        else -> "User"
                    }
                    ProfileStat(value = roleDisplay, label = "Role")
                }
            }
        }

        if (needsVerification) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFFF6B00).copy(alpha = 0.08f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚠ Verify your email to unlock all features",
                    color = Color(0xFFFF9800).copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = Color(0xFFFF9800).copy(alpha = 0.5f),
                    strokeWidth = 1.5.dp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { onBookmarksClick() }
                .background(Color.White.copy(alpha = 0.03f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Bookmark,
                contentDescription = null,
                tint = AppRed.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "My Bookmarks",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "→",
                color = Color.White.copy(alpha = 0.2f),
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .then(
                    if (!isLoggingOut) Modifier.clickable { onLogoutClick() }
                    else Modifier
                )
                .background(Color(0xFFFF5252).copy(alpha = 0.1f))
                .border(
                    width = 0.5.dp,
                    color = Color(0xFFFF5252).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(50)
                )
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.ExitToApp,
                contentDescription = null,
                tint = if (isLoggingOut) Color(0xFFFF5252).copy(alpha = 0.4f) else Color(0xFFFF5252),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isLoggingOut) "Signing out..." else "Sign Out",
                color = if (isLoggingOut) Color(0xFFFF5252).copy(alpha = 0.4f) else Color(0xFFFF5252),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (isLoggingOut) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = Color(0xFFFF5252),
                    strokeWidth = 1.5.dp
                )
            }
        }
    }
}

@Composable
private fun ProfileStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.3f),
            fontSize = 10.sp
        )
    }
}

@Composable
private fun LoggedOutView(onLoginClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(AppRed.copy(alpha = 0.15f), Color.Transparent),
                        radius = 72.dp.value
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "👤",
                fontSize = 32.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Sign in to BlazeMovies",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Bookmark, comment & more",
            color = Color.White.copy(alpha = 0.35f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onLoginClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppRed)
        ) {
            Text(
                text = "Sign In",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.02f))
                .padding(14.dp)
        ) {
            FeatureRow("🔒", "Secure login via Google Firebase")
            Spacer(modifier = Modifier.height(6.dp))
            FeatureRow("🔖", "Bookmark your favorites")
            Spacer(modifier = Modifier.height(6.dp))
            FeatureRow("💬", "Comment on movies & series")
            Spacer(modifier = Modifier.height(6.dp))
            FeatureRow("📥", "Request new content")
        }
    }
}

@Composable
private fun FeatureRow(emoji: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = emoji, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
    }
}



