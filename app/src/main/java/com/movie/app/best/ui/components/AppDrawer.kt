package com.movie.app.best.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.movie.app.best.BuildConfig
import com.movie.app.best.data.model.DrawerMenuItem
import com.movie.app.best.data.update.UpdateState
import com.movie.app.best.ui.screens.drawer.DrawerMenuViewModel
import com.movie.app.best.ui.screens.settings.UpdateViewModel
import com.movie.app.best.ui.theme.AppDivider
import com.movie.app.best.ui.theme.AppRed

@Composable
fun AppDrawerContent(
    onItemClick: (DrawerMenuItem) -> Unit = {},
    onUpdateClick: () -> Unit = {},
    viewModel: DrawerMenuViewModel = hiltViewModel(),
    updateViewModel: UpdateViewModel = hiltViewModel()
) {
    val items by viewModel.items.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp)
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.systemBars),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "v${BuildConfig.VERSION_NAME}",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                val availability by updateViewModel.availability.collectAsState()
                val updateResp = (availability as? UpdateState.Ready)?.data
                val updateAvailable = updateResp != null &&
                        updateResp.updateAvailable &&
                        !updateResp.forceUpdate

                if (updateAvailable) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onUpdateClick() }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(AppRed, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Update Available",
                            color = AppRed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Text(
                        text = "Running on Latest Version",
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = 12.sp
                    )
                }
            }

            HorizontalDivider(color = AppDivider, thickness = 1.dp)

            Spacer(modifier = Modifier.height(8.dp))
        }

        items(items, key = { it.id }) { item ->
            DrawerMenuRow(item = item, onClick = { onItemClick(item) })
        }
    }
}

@Composable
private fun DrawerMenuRow(
    item: DrawerMenuItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val iconRequest = remember(item.iconUrl) {
        ImageRequest.Builder(context)
            .data(item.iconUrl)
            .memoryCacheKey(item.iconUrl)
            .diskCacheKey(item.iconUrl)
            .crossfade(false)
            .build()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable { onClick() }
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (item.iconUrl.isNotBlank()) {
                SubcomposeAsyncImage(
                    model = iconRequest,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.width(24.dp))

        Text(
            text = item.label,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        if (item.isNew) {
            Spacer(modifier = Modifier.width(8.dp))
            DrawerBadge(text = "NEW", background = AppRed, foreground = Color.White)
        }
        if (item.isHot) {
            Spacer(modifier = Modifier.width(6.dp))
            DrawerBadge(text = "HOT", background = Color(0xFFFF9800), foreground = Color.Black)
        }
    }
}

@Composable
private fun DrawerBadge(
    text: String,
    background: Color,
    foreground: Color
) {
    Box(
        modifier = Modifier
            .background(background, RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = foreground,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}