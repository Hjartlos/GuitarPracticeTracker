package com.example.gpt.ui.achievements

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.gpt.R
import com.example.gpt.data.local.entity.Achievement
import com.example.gpt.data.local.entity.AchievementType
import com.example.gpt.ui.practice.PracticeViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AchievementsScreen(viewModel: PracticeViewModel) {
    val achievements by viewModel.allAchievements.collectAsState()
    val unlockedCount by viewModel.unlockedAchievementsCount.collectAsState()
    val totalCount by viewModel.totalAchievementsCount.collectAsState()

    AchievementsSection(
        achievements = achievements,
        unlockedCount = unlockedCount,
        totalCount = totalCount,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    )
}

@Composable
fun AchievementsSection(
    achievements: List<Achievement>,
    unlockedCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    var selectedAchievement by remember { mutableStateOf<AchievementType?>(null) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val canScrollForward by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItemIndex < totalItems - 1
        }
    }

    val canScrollBackward by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.achievements_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "$unlockedCount / $totalCount",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { if (totalCount > 0) unlockedCount.toFloat() / totalCount else 0f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        val chunkedAchievements = remember { AchievementType.values().toList().chunked(2) }

        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(chunkedAchievements) { columnItems ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    columnItems.forEach { type ->
                        val achievement = achievements.find { it.id == type.id }
                        val isUnlocked = achievement?.unlockedAt != null
                        val progress = achievement?.progress ?: 0

                        AchievementBadge(
                            type = type,
                            isUnlocked = isUnlocked,
                            progress = progress,
                            onClick = { selectedAchievement = type }
                        )
                    }
                }
            }
        }

        if (chunkedAchievements.size > 4) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            listState.animateScrollBy(-300f)
                        }
                    },
                    enabled = canScrollBackward
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Scroll Left",
                        tint = if (canScrollBackward) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                }

                Spacer(modifier = Modifier.width(32.dp))

                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            listState.animateScrollBy(300f)
                        }
                    },
                    enabled = canScrollForward
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Scroll Right",
                        tint = if (canScrollForward) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                }
            }
        }
    }

    selectedAchievement?.let { type ->
        val achievement = achievements.find { it.id == type.id }
        AchievementDetailDialog(
            type = type,
            achievement = achievement,
            onDismiss = { selectedAchievement = null }
        )
    }
}

@Composable
private fun AchievementBadge(
    type: AchievementType,
    isUnlocked: Boolean,
    progress: Int,
    onClick: () -> Unit
) {
    val icon = getIconForAchievementType(type)
    val color = if (isUnlocked) getColorForAchievement(type) else Color.Gray

    val scale by animateFloatAsState(
        targetValue = if (isUnlocked) 1f else 0.95f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "badgeScale"
    )

    Card(
        modifier = Modifier
            .width(85.dp)
            .height(110.dp)
            .scale(scale)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if(isUnlocked) 2.dp else 0.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(28.dp)
                        .alpha(if (isUnlocked) 1f else 0.4f),
                    tint = color
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(type.titleRes),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isUnlocked) 1f else 0.6f),
                    textAlign = TextAlign.Center,
                    lineHeight = 12.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!isUnlocked) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(2.dp)
                        .size(18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(color = Color.Black.copy(alpha = 0.6f))

                        if (progress > 0) {
                            drawArc(
                                color = Color(0xFFFF5252),
                                startAngle = -90f,
                                sweepAngle = 360f * (progress / 100f),
                                useCenter = false,
                                style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }

                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(9.dp),
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun AchievementDetailDialog(
    type: AchievementType,
    achievement: Achievement?,
    onDismiss: () -> Unit
) {
    val isUnlocked = achievement?.unlockedAt != null
    val color = if (isUnlocked) getColorForAchievement(type) else Color.Gray
    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isUnlocked) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(color.copy(alpha = 0.4f), Color.Transparent)
                                )
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(
                                color = color.copy(alpha = if (isUnlocked) 0.2f else 0.1f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getIconForAchievementType(type),
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .alpha(if (isUnlocked) 1f else 0.5f),
                            tint = color
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(type.titleRes),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(type.descriptionRes),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (isUnlocked) {
                    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    val date = Date(achievement?.unlockedAt ?: 0)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.unlocked_on, dateFormat.format(date)),
                            fontSize = 12.sp,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    val progress = achievement?.progress ?: 0
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Postęp", style = MaterialTheme.typography.labelSmall)
                            Text("$progress%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { progress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = color,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.close))
                }
            }
        }
    }
}

@Composable
fun AchievementUnlockedToast(
    achievementType: AchievementType,
    onDismiss: () -> Unit
) {
    val color = getColorForAchievement(achievementType)

    LaunchedEffect(achievementType) {
        kotlinx.coroutines.delay(4000)
        onDismiss()
    }

    val slideIn = remember {
        slideInVertically(initialOffsetY = { -it }, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn()
    }
    val slideOut = remember {
        slideOutVertically(targetOffsetY = { -it }, animationSpec = tween(300)) + fadeOut()
    }

    AnimatedVisibility(
        visible = true,
        enter = slideIn,
        exit = slideOut
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(color.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getIconForAchievementType(achievementType),
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🏆 " + stringResource(R.string.achievement_unlocked),
                        fontSize = 12.sp,
                        color = color,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(achievementType.titleRes),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun getIconForAchievementType(type: AchievementType): ImageVector {
    return when (type.name) {
        "FIRST_SESSION" -> Icons.Default.Start
        "WEEK_STREAK" -> Icons.Default.DateRange
        "MONTH_STREAK" -> Icons.Default.CalendarMonth
        "HOUR_TOTAL" -> Icons.Default.AccessTime
        "TEN_HOURS" -> Icons.Default.Timer
        "HUNDRED_HOURS" -> Icons.Default.Whatshot
        "METRONOME_MASTER" -> Icons.Default.MusicNote
        "SPEED_DEMON" -> Icons.Default.Speed
        "GOAL_GETTER" -> Icons.Default.Flag
        "GOAL_CRUSHER" -> Icons.Default.EmojiEvents
        "VARIETY_PLAYER" -> Icons.Default.Category
        "EARLY_BIRD" -> Icons.Default.WbSunny
        "NIGHT_OWL" -> Icons.Default.Nightlight
        "PERFECT_PITCH" -> Icons.Default.Tune
        else -> Icons.Default.Star
    }
}

private fun getColorForAchievement(type: AchievementType): Color {
    return when (type.name) {
        "FIRST_SESSION" -> Color(0xFF4CAF50)
        "WEEK_STREAK" -> Color(0xFFFF9800)
        "MONTH_STREAK" -> Color(0xFFE53935)
        "HOUR_TOTAL" -> Color(0xFF2196F3)
        "TEN_HOURS" -> Color(0xFF9C27B0)
        "HUNDRED_HOURS" -> Color(0xFFFFD700)
        "METRONOME_MASTER" -> Color(0xFF00BCD4)
        "SPEED_DEMON" -> Color(0xFFE91E63)
        "GOAL_GETTER" -> Color(0xFF8BC34A)
        "GOAL_CRUSHER" -> Color(0xFFFF5722)
        "VARIETY_PLAYER" -> Color(0xFF673AB7)
        "EARLY_BIRD" -> Color(0xFFFFEB3B)
        "NIGHT_OWL" -> Color(0xFF3F51B5)
        "PERFECT_PITCH" -> Color(0xFF009688)
        else -> Color.Gray
    }
}