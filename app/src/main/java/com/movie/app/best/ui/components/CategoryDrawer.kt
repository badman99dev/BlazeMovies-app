package com.movie.app.best.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.SubcomposeAsyncImage
import com.movie.app.best.BuildConfig
import com.movie.app.best.data.model.Category
import com.movie.app.best.data.repository.PrefetchCache
import com.movie.app.best.ui.screens.categories.CategoriesViewModel
import com.movie.app.best.ui.theme.AppBg
import com.movie.app.best.ui.theme.CardDark
import com.movie.app.best.ui.theme.AppDivider
import com.movie.app.best.ui.theme.AppRed
import com.movie.app.best.ui.theme.SecondaryText
import androidx.compose.foundation.shape.CircleShape

@Composable
fun CategoryDrawerContent(
    onCategoryClick: (slug: String, name: String) -> Unit,
    onAllCategoriesClick: () -> Unit = {},
    onUpdateClick: () -> Unit = {},
    viewModel: CategoriesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp)
            .background(AppBg)
    ) {
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

            val updateResp = PrefetchCache.updateResponse
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onAllCategoriesClick() }
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Category,
                contentDescription = null,
                tint = AppRed,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = "All Categories",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        HorizontalDivider(color = AppDivider, thickness = 0.5.dp)

        Spacer(modifier = Modifier.height(4.dp))

        if (uiState.isLoading && uiState.categories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AppRed, modifier = Modifier.size(28.dp))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(uiState.categories, key = { it.slug }) { category ->
                    DrawerCategoryItem(
                        category = category,
                        onClick = { onCategoryClick(category.slug, category.categoryName) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerCategoryItem(
    category: Category,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (category.bannerUrl.isNotBlank()) {
            SubcomposeAsyncImage(
                model = category.bannerUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(6.dp)),
                loading = {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier
                        .size(40.dp)
                        .background(CardDark)) {
                        Icon(
                            imageVector = if (category.categoryName.contains("series", ignoreCase = true) ||
                                category.categoryName.contains("tv", ignoreCase = true))
                                Icons.Default.Tv else Icons.Default.Movie,
                            contentDescription = null,
                            tint = SecondaryText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                error = {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier
                        .size(40.dp)
                        .background(CardDark)) {
                        Icon(
                            imageVector = Icons.Default.BrokenImage,
                            contentDescription = null,
                            tint = SecondaryText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            )
        } else {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(CardDark),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (category.categoryName.contains("series", ignoreCase = true) ||
                        category.categoryName.contains("tv", ignoreCase = true))
                        Icons.Default.Tv else Icons.Default.Movie,
                    contentDescription = null,
                    tint = SecondaryText,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = category.categoryName,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Text(
                text = "${category.count} items",
                color = SecondaryText,
                fontSize = 11.sp
            )
        }
    }
}
