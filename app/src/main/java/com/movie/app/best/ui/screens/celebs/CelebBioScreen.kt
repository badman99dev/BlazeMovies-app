package com.movie.app.best.ui.screens.celebs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.movie.app.best.ui.components.CastCircleFrame
import com.movie.app.best.ui.components.CastFrameYellow
import com.movie.app.best.ui.components.CompactPageHeader

@Composable
fun CelebBioScreen(
    nameId: String,
    name: String = "",
    onBackClick: () -> Unit,
    onCelebClick: (String, String) -> Unit = { _, _ -> },
    onSearchClick: () -> Unit = {},
    viewModel: CelebBioViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(nameId) {
        viewModel.configure(nameId)
    }

    val headerName = name.ifBlank { uiState.displayName }.ifBlank { "Celebrity" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        CompactPageHeader(
            title = "About $headerName",
            onBackClick = onBackClick,
            onSearchClick = onSearchClick
        )

        when {
            uiState.isLoading && uiState.displayName.isBlank() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.Red)
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 40.dp)
                ) {
                    if (uiState.photoUrl.isNotBlank() ||
                        uiState.age > 0 || uiState.birthDate.isNotBlank() ||
                        uiState.birthLocation.isNotBlank() || uiState.heightText.isNotBlank()
                    ) {
                        item { BioDetailsCard(uiState) }
                    }

                    if (uiState.biography.isNotBlank()) {
                        item { BiographySection(uiState.biography) }
                    }

                    if (uiState.relationships.isNotEmpty()) {
                        item { FamilySection(uiState.relationships, onCelebClick) }
                    }
                }
            }
        }
    }
}

@Composable
private fun BioDetailsCard(state: CelebBioUiState) {
    val aspect = if (state.photoWidth > 0 && state.photoHeight > 0)
        state.photoWidth.toFloat() / state.photoHeight.toFloat() else 2f / 3f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF1D1D1D))
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .width(140.dp)
                .aspectRatio(aspect)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF2A2A2A))
        ) {
            if (state.photoUrl.isNotBlank()) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(state.photoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = state.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            if (state.displayName.isNotBlank()) {
                Text(
                    text = state.displayName,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
            if (state.age > 0) {
                InfoRow(label = "Age", value = state.age.toString())
            }
            if (state.birthDate.isNotBlank()) {
                InfoRow(label = "DOB", value = formatBioDate(state.birthDate))
            }
            if (state.birthLocation.isNotBlank()) {
                InfoRow(label = "Location", value = state.birthLocation)
            }
            if (state.heightText.isNotBlank()) {
                InfoRow(label = "Height", value = state.heightText)
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.padding(bottom = 6.dp)) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(64.dp)
        )
        Text(
            text = value,
            color = Color(0xFFE8E8E8),
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun BiographySection(biography: String) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = "Biography",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = biography,
            color = Color(0xFFE8E8E8),
            fontSize = 14.sp,
            lineHeight = 22.sp
        )
    }
}

@Composable
private fun FamilySection(
    relationships: List<com.movie.app.best.data.model.ImdbRelationship>,
    onCelebClick: (String, String) -> Unit
) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text(
            text = "Family",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(14.dp)
        ) {
            items(relationships, key = { it.name.id + it.relationType }) { rel ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CastCircleFrame(
                        star = rel.name,
                        onClick = {
                            if (rel.name.id.isNotBlank()) {
                                onCelebClick(rel.name.id, rel.name.displayName)
                            }
                        }
                    )
                    if (rel.relationType.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = rel.relationType.replaceFirstChar { it.uppercase() },
                            color = CastFrameYellow,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private fun formatBioDate(dateStr: String): String {
    val parts = dateStr.split("-")
    if (parts.size != 3) return dateStr
    val months = listOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val y = parts[0].toIntOrNull() ?: 0
    val m = parts[1].toIntOrNull() ?: 0
    val d = parts[2].toIntOrNull() ?: 0
    return if (y > 0 && m in 1..12 && d > 0) "$d ${months[m]} $y" else dateStr
}
