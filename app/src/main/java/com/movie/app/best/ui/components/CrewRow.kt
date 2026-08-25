package com.movie.app.best.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.movie.app.best.data.model.CrewPerson

val CrewDirectorPink = Color(0xFFE91E63)

@Composable
fun CollapsibleCrewRow(
    crew: List<CrewPerson>,
    onNameClick: (String, String) -> Unit = { _, _ -> }
) {
    if (crew.isEmpty()) return

    var expanded by remember { mutableStateOf(false) }
    val arrowRotation by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = tween(300),
        label = "arrow_rot"
    )

    Spacer(modifier = Modifier.height(14.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier
                .size(22.dp)
                .rotate(arrowRotation)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "Cast & Crew",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "  (${crew.size})",
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal
        )
    }

    AnimatedVisibility(
        visible = expanded,
        enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(tween(300)),
        exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(tween(200))
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(crew, key = { if (it.id.isNotBlank()) "crew_${it.id}" else "plain_${it.displayName}" }) { person ->
                CrewCircleFrame(
                    person = person,
                    onClick = {
                        if (person.isClickable) onNameClick(person.id, person.displayName)
                    }
                )
            }
        }
    }
}

@Composable
fun CrewCircleFrame(
    person: CrewPerson,
    onClick: () -> Unit = {}
) {
    val borderColor: Color? = when {
        !person.isClickable -> null
        person.category == CrewPerson.CAT_DIRECTOR -> CrewDirectorPink
        else -> CastFrameYellow
    }
    val labelColor = borderColor ?: Color.White.copy(alpha = 0.4f)

    Column(
        modifier = Modifier.width(72.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(0xFF1A1A1A))
                .then(
                    if (borderColor != null) {
                        Modifier
                            .border(2.5.dp, borderColor, CircleShape)
                            .clickable { onClick() }
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (person.photoUrl.isNotBlank()) {
                AsyncImage(
                    model = person.photoUrl,
                    contentDescription = person.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(if (person.isClickable) 50.dp else 56.dp)
                        .clip(CircleShape)
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = person.displayName,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        if (person.category.isNotBlank()) {
            Text(
                text = person.category,
                color = labelColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (person.characters.isNotBlank() && person.category != CrewPerson.CAT_DIRECTOR) {
            Text(
                text = person.characters,
                color = Color.White.copy(alpha = 0.35f),
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
