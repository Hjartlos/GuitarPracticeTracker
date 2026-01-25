package com.example.gpt.ui.statistics

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gpt.R
import com.example.gpt.core.audio.AnalysisResult
import com.example.gpt.core.audio.RhythmHit
import com.example.gpt.data.local.entity.PracticeSession
import com.example.gpt.ui.achievements.AchievementsSection
import com.example.gpt.ui.practice.PracticeViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.floor

import kotlin.math.ceil
import kotlin.math.max

@Composable
fun StatisticsScreen(viewModel: PracticeViewModel) {
    val allSessions by viewModel.allSessions.collectAsState()
    val weeklyProgress by viewModel.weeklyProgress.collectAsState()
    val weeklyGoal by viewModel.weeklyGoalHours.collectAsState()

    val achievements by viewModel.allAchievements.collectAsState()
    val unlockedCount by viewModel.unlockedAchievementsCount.collectAsState()
    val totalCount by viewModel.totalAchievementsCount.collectAsState()

    var selectedExerciseFilter by remember { mutableStateOf<String?>(null) }
    var filterMetronomeOnly by remember { mutableStateOf(false) }

    var showInHours by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopAudioPlayback()
        }
    }

    val exerciseTypes = remember(allSessions) { allSessions.map { it.exerciseType }.distinct() }

    val filteredSessions = remember(allSessions, selectedExerciseFilter, filterMetronomeOnly) {
        allSessions.filter { session ->
            val typeMatch = selectedExerciseFilter == null || session.exerciseType == selectedExerciseFilter
            val metronomeMatch = !filterMetronomeOnly || session.avgBpm > 0
            typeMatch && metronomeMatch
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    AchievementsSection(
                        achievements = achievements,
                        unlockedCount = unlockedCount,
                        totalCount = totalCount
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.scroll_to_see_more),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            WeeklyGoalCard(progress = weeklyProgress, goalHours = weeklyGoal)

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                stringResource(R.string.filters),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = filterMetronomeOnly,
                        onClick = { filterMetronomeOnly = !filterMetronomeOnly },
                        label = { Text(stringResource(R.string.filter_metronome_only)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
                items(exerciseTypes) { type ->
                    FilterChip(
                        selected = selectedExerciseFilter == type,
                        onClick = {
                            selectedExerciseFilter = if (selectedExerciseFilter == type) null else type
                        },
                        label = { Text(type) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        stringResource(R.string.daily_activity),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        stringResource(R.string.current_week_subtitle),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                UnitSelector(isHours = showInHours) { showInHours = it }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val dailyDataMinutes =
                remember(filteredSessions) { calculateDailyStats(filteredSessions) }
            val chartData = remember(dailyDataMinutes, showInHours) {
                if (showInHours) {
                    dailyDataMinutes.map { it.first to (it.second / 60f) }
                } else {
                    dailyDataMinutes
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                BarChart(chartData, weeklyGoal, showInHours)
            }
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                stringResource(R.string.time_by_exercise),
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))

            ExerciseBreakdown(filteredSessions)

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                stringResource(R.string.history_title),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(filteredSessions) { session ->
            SessionItemExpandable(session = session, viewModel = viewModel)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
@Composable
fun UnitSelector(isHours: Boolean, onUnitChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        UnitOption(text = stringResource(R.string.unit_min), isSelected = !isHours) { onUnitChange(false) }
        UnitOption(text = stringResource(R.string.unit_hrs), isSelected = isHours) { onUnitChange(true) }
    }
}

@Composable
fun UnitOption(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

fun getAccuracyColor(score: Int): Color {
    return when (score) {
        in 96..100 -> Color(0xFF1B5E20)
        in 76..95 -> Color(0xFF4CAF50)
        in 51..75 -> Color(0xFFFFD600)
        else -> Color(0xFFD50000)
    }
}

@Composable
fun SessionItemExpandable(session: PracticeSession, viewModel: PracticeViewModel) {
    var expanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

    val activeSessionId by viewModel.activePlayerSessionId.collectAsState()
    val isGlobalPlaying by viewModel.isPlayerPlaying.collectAsState()
    val isActive = activeSessionId == session.id
    val isPlaying = isActive && isGlobalPlaying

    val analysisResults by viewModel.detailedAnalysisResults.collectAsState()
    val isAnalyzingMap by viewModel.isAnalyzingSession.collectAsState()

    val hitData = analysisResults[session.id]
    val isAnalyzing = isAnalyzingMap[session.id] == true

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_session_title)) },
            text = { Text(stringResource(R.string.delete_session_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSession(session)
                        showDeleteDialog = false
                    }
                ) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurface)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("${session.exerciseType} (${session.tuning})", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))

                    if (session.avgBpm > 0) {
                        val realScore = if (hitData != null) {
                            calculateConsistencyScore(hitData.detailedHits)
                        } else {
                            session.consistencyScore
                        }
                        LaunchedEffect(realScore, hitData) {
                            if (hitData != null && realScore != session.consistencyScore) {
                                viewModel.updateSessionAccuracy(session, realScore)
                            }
                        }

                        val accText = if (realScore == 0 && session.consistencyScore == 0)
                            stringResource(R.string.no_notes_detected)
                        else
                            stringResource(R.string.accuracy_label, realScore)

                        val textColor = if (realScore == 0) MaterialTheme.colorScheme.error else getAccuracyColor(realScore)
                        val marginPercent = (session.rhythmMargin * 100).toInt()

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${session.avgBpm} BPM",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = " • ",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = accText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            Text(
                                text = " • " + stringResource(R.string.margin_label, marginPercent),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(stringResource(R.string.free_play), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontStyle = FontStyle.Italic)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${session.durationSeconds / 60} min", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text(dateFormat.format(Date(session.timestamp)), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.delete),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.1f))
                Spacer(modifier = Modifier.height(12.dp))

                if (session.avgBpm > 0 && session.consistencyScore > 0) {
                    if (hitData != null) {
                        FullRhythmAnalysisView(
                            analysisResult = hitData,
                            bpm = session.avgBpm,
                            margin = session.rhythmMargin
                        )
                    } else if (isAnalyzing) {
                        Box(modifier = Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.analyzeSessionRhythm(session) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.full_analysis_btn), fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                } else if (session.avgBpm > 0 && session.consistencyScore == 0) {
                    Text(stringResource(R.string.no_notes_detected), fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (session.notes.isNotEmpty()) {
                    Text(stringResource(R.string.notes_label), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(session.notes, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (session.audioPath != null) {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.recording_label), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { viewModel.toggleHistoryPlayback(session) },
                                    modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = stringResource(R.string.play_pause),
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                if (isActive) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        val progress by viewModel.playbackProgress.collectAsState()
                                        Slider(
                                            value = progress,
                                            onValueChange = { viewModel.seekAudio(it) },
                                            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary),
                                            modifier = Modifier.height(20.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                            listOf(0.5f, 0.75f, 1.0f).forEach { speed ->
                                                val currentSpeed by viewModel.playbackSpeed.collectAsState()
                                                val isSelected = currentSpeed == speed
                                                Text(
                                                    text = "${speed}x",
                                                    modifier = Modifier
                                                        .clickable { viewModel.setPlaybackSpeed(speed) }
                                                        .background(if(isSelected) MaterialTheme.colorScheme.onSurface.copy(alpha=0.1f) else Color.Transparent, RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Text(stringResource(R.string.tap_to_listen), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                } else {
                    Text(stringResource(R.string.no_audio_available), fontSize = 12.sp, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun WeeklyGoalCard(progress: Float, goalHours: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.weekly_goal_label), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.weekly_progress_fmt, (progress * goalHours).toInt(), goalHours), color = MaterialTheme.colorScheme.primary)
            }
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
        }
    }
}

@Composable
fun BarChart(data: List<Pair<String, Float>>, weeklyGoalHours: Int, isHours: Boolean) {
    val dailyGoal = if (isHours) {
        if (weeklyGoalHours > 0) weeklyGoalHours.toFloat() / 7f else 0.33f
    } else {
        if (weeklyGoalHours > 0) (weeklyGoalHours * 60f) / 7f else 20f
    }

    val maxDataValue = data.maxOfOrNull { it.second } ?: 0f

    val yAxisMax: Float
    val stepSize: Float

    if (isHours) {
        val rawMax = max(maxDataValue, dailyGoal)
        stepSize = when {
            rawMax <= 5f -> 0.5f
            rawMax <= 10f -> 1f
            else -> 2f
        }
        yAxisMax = (ceil(rawMax / stepSize) * stepSize)
    } else {
        val rawMax = max(maxDataValue, dailyGoal).coerceAtLeast(10f)
        stepSize = when {
            rawMax <= 30f -> 5f
            rawMax <= 180f -> 15f
            rawMax <= 360f -> 30f
            else -> 60f
        }
        yAxisMax = (ceil(rawMax / stepSize) * stepSize)
    }

    val steps = (yAxisMax / stepSize).toInt()

    val barColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)

    val density = LocalDensity.current
    val textPaint = remember(density) {
        android.graphics.Paint().apply {
            color = labelColor
            textSize = with(density) { 10.sp.toPx() }
            textAlign = android.graphics.Paint.Align.RIGHT
            isAntiAlias = true
        }
    }
    val labelPaint = remember(density) {
        android.graphics.Paint().apply {
            color = labelColor
            textSize = with(density) { 11.sp.toPx() }
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
    }

    Canvas(modifier = Modifier.fillMaxSize().padding(start = 32.dp, end = 16.dp, top = 20.dp, bottom = 30.dp)) {
        val width = size.width
        val height = size.height
        val barWidth = width / (data.size * 2f)
        val stepX = width / data.size

        for (i in 0..steps) {
            val yVal = i * stepSize
            val yPos = height - (height * (yVal / yAxisMax))

            drawLine(
                color = gridColor,
                start = Offset(0f, yPos),
                end = Offset(width, yPos),
                strokeWidth = 3f
            )

            val labelText = if (isHours) {
                if (yVal % 1.0f == 0.0f) "${yVal.toInt()}" else String.format(Locale.US, "%.1f", yVal)
            } else {
                "${yVal.toInt()}"
            }

            drawContext.canvas.nativeCanvas.drawText(
                labelText,
                -15f,
                yPos + 10f,
                textPaint
            )
        }

        data.forEachIndexed { index, (dayName, value) ->
            val x = index * stepX + (stepX / 2) - (barWidth / 2)
            val barHeight = (value / yAxisMax) * height
            drawRect(color = barColor, topLeft = Offset(x, height - barHeight), size = Size(barWidth, barHeight))

            drawContext.canvas.nativeCanvas.drawText(
                dayName,
                x + barWidth/2,
                height + 40f,
                labelPaint
            )
        }
    }
}

@Composable
fun ExerciseBreakdown(sessions: List<PracticeSession>) {
    val stats = remember(sessions) {
        sessions.groupBy { it.exerciseType }.mapValues { (_, list) -> list.sumOf { it.durationSeconds } / 60f }.toList().sortedByDescending { it.second }
    }
    val totalMinutes = stats.sumOf { it.second.toDouble() }.toFloat().coerceAtLeast(1f)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (stats.isEmpty()) { Text(stringResource(R.string.no_data_filters), color = MaterialTheme.colorScheme.onSurfaceVariant) } else {
                stats.forEach { (type, minutes) ->
                    val percent = minutes / totalMinutes
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(type, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("${minutes.toInt()} min", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))) {
                            Box(modifier = Modifier.fillMaxWidth(percent).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
                        }
                    }
                }
            }
        }
    }
}

fun calculateDailyStats(sessions: List<PracticeSession>): List<Pair<String, Float>> {
    val days = mutableListOf<Pair<String, Float>>()
    val dateFormat = SimpleDateFormat("E", Locale.getDefault())
    val calendar = Calendar.getInstance()

    var daysToSubtract = calendar.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY
    if (daysToSubtract < 0) daysToSubtract += 7
    calendar.add(Calendar.DAY_OF_YEAR, -daysToSubtract)

    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)

    val weekStart = calendar.timeInMillis

    for (i in 0..6) {
        val currentDay = Calendar.getInstance()
        currentDay.timeInMillis = weekStart
        currentDay.add(Calendar.DAY_OF_YEAR, i)

        val dayStart = currentDay.timeInMillis
        currentDay.set(Calendar.HOUR_OF_DAY, 23)
        currentDay.set(Calendar.MINUTE, 59)
        val dayEnd = currentDay.timeInMillis

        val daySessions = sessions.filter { it.timestamp in dayStart..dayEnd }
        val totalMinutes = daySessions.sumOf { it.durationSeconds } / 60f

        days.add(dateFormat.format(Date(dayStart)) to totalMinutes)
    }
    return days
}

@Composable
fun FullRhythmAnalysisView(
    analysisResult: AnalysisResult,
    bpm: Int,
    margin: Float
) {
    var showDetails by remember { mutableStateOf(false) }

    val greenColor = Color(0xFF00C853)
    val yellowColor = Color(0xFFFFD600)
    val redColor = Color(0xFFD50000)

    val totalHits = analysisResult.detailedHits.size
    val ghostHits = analysisResult.detailedHits.count { it.noteType == "Ghost" }
    val meaningfulHits = (totalHits - ghostHits).coerceAtLeast(1)

    val validHits = analysisResult.detailedHits.count {
        it.noteType == "Perfect" || it.noteType == "Good"
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.rhythm_alignment),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatBadge(
                    label = stringResource(R.string.stats_hits),
                    value = "$validHits/$meaningfulHits",
                    color = greenColor
                )
                StatBadge(
                    label = stringResource(R.string.stats_duration),
                    value = formatDuration(analysisResult.sessionDurationSeconds),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            LegendItem(color = greenColor, label = stringResource(R.string.legend_perfect))
            LegendItem(color = yellowColor, label = stringResource(R.string.legend_good))
            LegendItem(color = redColor, label = stringResource(R.string.legend_miss))
        }

        Spacer(modifier = Modifier.height(8.dp))

        ScrollableRhythmTimeline(
            hits = analysisResult.detailedHits,
            bpm = bpm,
            totalDuration = analysisResult.sessionDurationSeconds,
            margin = margin
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDetails = !showDetails }
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.show_details),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
            Icon(
                imageVector = if (showDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }

        if (showDetails) {
            Spacer(modifier = Modifier.height(8.dp))
            AnalysisSummaryTable(analysisResult)
        }
    }
}

@Composable
fun AnalysisSummaryTable(result: AnalysisResult) {
    val totalSignals = result.detailedHits.size

    val perfectHits = result.detailedHits.count { it.noteType == "Perfect" }
    val goodHits = result.detailedHits.count { it.noteType == "Good" }
    val missHits = result.detailedHits.count { it.noteType == "Miss" }
    val ghostHits = result.detailedHits.count { it.noteType == "Ghost" }

    val meaningfulHits = (perfectHits + goodHits + missHits).coerceAtLeast(1)

    val perfectPct = (perfectHits.toDouble() / meaningfulHits * 100.0).roundToInt()
    val goodPct = (goodHits.toDouble() / meaningfulHits * 100.0).roundToInt()

    val missPct = 100 - perfectPct - goodPct

    val colorPerfect = Color(0xFF00C853)
    val colorGood = Color(0xFFFFD600)
    val colorMiss = Color(0xFFD50000)
    val colorGhost = Color(0xFF3D5AFE)

    val ghostPct = if (totalSignals > 0) (ghostHits.toDouble() / totalSignals * 100.0).roundToInt() else 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.table_status), fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.stats_hits), fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(0.5f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                Text("%", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(0.5f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.1f))

            StatSummaryRow(stringResource(R.string.legend_perfect), perfectHits, perfectPct, colorPerfect)
            StatSummaryRow(stringResource(R.string.legend_good), goodHits, goodPct, colorGood)
            StatSummaryRow(stringResource(R.string.legend_miss), missHits, missPct, colorMiss)

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.1f))

            StatSummaryRow(stringResource(R.string.legend_ghost), ghostHits, ghostPct, colorGhost)

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.1f))

            StatSummaryRow(stringResource(R.string.stats_total), meaningfulHits, 100, MaterialTheme.colorScheme.onSurface, isBold = true)
        }
    }
}

@Composable
fun StatSummaryRow(label: String, count: Int, percent: Int, color: Color, isBold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = color,
            fontWeight = if(isBold) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$count",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            modifier = Modifier.weight(0.5f)
        )
        Text(
            text = "$percent%",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            modifier = Modifier.weight(0.5f)
        )
    }
}

@Composable
fun StatBadge(label: String, value: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Text(
            text = "$label: $value",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ScrollableRhythmTimeline(
    hits: List<RhythmHit>,
    bpm: Int,
    totalDuration: Double,
    margin: Float
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    val pixelsPerSecondDp = 80.dp
    val pxPerSecond = with(density) { pixelsPerSecondDp.toPx() }
    val visualPaddingPx = with(density) { 10.dp.toPx() }

    val beatInterval = 60.0 / bpm
    val toleranceMs = 35.0 + (80.0 * margin)
    val toleranceSeconds = toleranceMs / 1000.0
    val toleranceWidthPx = (toleranceSeconds * pxPerSecond).toFloat()

    val toleranceBeatPositions = buildSet<Double> {
        val baseBeats = ceil(totalDuration / beatInterval).toInt()
        for (i in 0..baseBeats) add(i.toDouble())

        hits.forEach { hit ->
            if (!hit.isGhostNote) {
                add(hit.targetTimeSeconds / beatInterval)
            }
        }
    }

    val minBeatPos = toleranceBeatPositions.minOrNull() ?: 0.0
    val maxBeatPos = toleranceBeatPositions.maxOrNull() ?: 0.0

    val startTime =
        (minBeatPos * beatInterval) - toleranceSeconds

    val endTime =
        max(
            (maxBeatPos * beatInterval) + toleranceSeconds,
            totalDuration
        )

    val contentWidthPx =
        ((endTime - startTime) * pxPerSecond).toFloat()

    val totalWidthPx =
        visualPaddingPx + contentWidthPx + visualPaddingPx

    val totalWidthDp = with(density) { totalWidthPx.toDp() }

    val greenColor = Color(0xFF00C853)
    val yellowColor = Color(0xFFFFD600)
    val redColor = Color(0xFFD50000)
    val beatLineColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    val toleranceColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(8.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(scrollState)
                    .fillMaxHeight()
            ) {
                Canvas(
                    modifier = Modifier
                        .width(totalWidthDp.coerceAtLeast(300.dp))
                        .fillMaxHeight()
                        .padding(vertical = 8.dp)
                ) {
                    val height = size.height

                    fun getX(time: Double): Float =
                        (visualPaddingPx +
                                ((time - startTime) * pxPerSecond)).toFloat()

                    toleranceBeatPositions.forEach { beatPos ->
                        val time = beatPos * beatInterval
                        val x = getX(time)

                        drawRect(
                            color = toleranceColor,
                            topLeft = Offset(x - toleranceWidthPx, 0f),
                            size = Size(toleranceWidthPx * 2, height)
                        )
                    }

                    val firstBeatIndex =
                        floor(startTime / beatInterval).toInt()

                    val lastBeatIndex =
                        ceil(endTime / beatInterval).toInt()

                    for (i in firstBeatIndex..lastBeatIndex) {
                        val time = i * beatInterval
                        val x = getX(time)
                        val isMeasure = i % 4 == 0

                        drawLine(
                            color = if (isMeasure)
                                beatLineColor.copy(alpha = 0.6f)
                            else beatLineColor,
                            start = Offset(x, 0f),
                            end = Offset(x, height),
                            strokeWidth = if (isMeasure) 2f else 1f,
                            pathEffect = if (!isMeasure)
                                PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                            else null
                        )

                        if (isMeasure && time >= 0) {
                            drawContext.canvas.nativeCanvas.drawText(
                                "${i + 1}",
                                x + 4f,
                                height - 5f,
                                android.graphics.Paint().apply {
                                    color = textColor
                                    textSize = 24f
                                    alpha = 255
                                }
                            )
                        }
                    }

                    val firstMarker = floor(startTime / 5.0).toInt()
                    val lastMarker = ceil(endTime / 5.0).toInt()

                    for (i in firstMarker..lastMarker) {
                        val timeMarker = i * 5.0
                        if (timeMarker >= 0) {
                            val x = getX(timeMarker)
                            drawContext.canvas.nativeCanvas.drawText(
                                formatTimeShort(timeMarker),
                                x + 2f,
                                20f,
                                android.graphics.Paint().apply {
                                    color = textColor
                                    textSize = 22f
                                    alpha = 255
                                }
                            )
                        }
                    }

                    hits.forEach { hit ->
                        val x = getX(hit.timeSeconds)
                        val absDeviation = abs(hit.deviationMs)

                        val hitColor = when (hit.noteType) {
                            "Perfect" -> greenColor
                            "Good" -> yellowColor
                            "Miss" -> redColor
                            else -> Color.Gray
                        }

                        val normalizedDeviation =
                            (absDeviation.coerceAtMost(100) / 100f)

                        val barHeight =
                            height * (0.3f + 0.4f * (1f - normalizedDeviation))

                        val barWidth = 4f

                        drawRoundRect(
                            color = hitColor,
                            topLeft = Offset(
                                x - barWidth / 2,
                                (height - barHeight) / 2
                            ),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(2f, 2f)
                        )
                    }
                }
            }
        }

        Text(
            text = stringResource(R.string.scroll_to_see_more),
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}


@Composable
fun HitDetailsTable(hits: List<RhythmHit>) {
    val greenColor = Color(0xFF00C853)
    val redColor = Color(0xFFD50000)
    val yellowColor = Color(0xFFFFD600)
    val ghostColor = Color.Gray

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.table_time), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.table_beat), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.table_deviation), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.table_status), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(4.dp))

            val displayHits = hits.take(20)
            displayHits.forEach { hit ->
                val absDeviation = abs(hit.deviationMs)

                val statusColor = when (hit.noteType) {
                    "Ghost" -> ghostColor
                    "Miss" -> redColor
                    "Perfect" -> greenColor
                    "Good" -> yellowColor
                    else -> ghostColor
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        formatTimeShort(hit.timeSeconds),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "#${hit.beatNumber + 1}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = if(hit.isGhostNote) stringResource(R.string.legend_ghost) else "${if (hit.deviationMs > 0) "+" else ""}${hit.deviationMs}ms",
                        fontSize = 11.sp,
                        color = statusColor,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(statusColor, CircleShape)
                        )
                    }
                }
            }

            if (hits.size > 20) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.and_more_hits, hits.size - 20),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }
}

fun calculateConsistencyScore(hits: List<RhythmHit>): Int {
    val perfectHits = hits.count { it.noteType == "Perfect" }
    val goodHits = hits.count { it.noteType == "Good" }
    val missHits = hits.count { it.noteType == "Miss" }

    val meaningfulHits = (perfectHits + goodHits + missHits).coerceAtLeast(1)

    val perfectPct = (perfectHits.toDouble() / meaningfulHits * 100.0).roundToInt()
    val goodPct = (goodHits.toDouble() / meaningfulHits * 100.0).roundToInt()

    return perfectPct + goodPct
}

fun formatDuration(seconds: Double): String {
    val mins = (seconds / 60).toInt()
    val secs = (seconds % 60).toInt()
    return "${mins}:${secs.toString().padStart(2, '0')}"
}

fun formatTimeShort(seconds: Double): String {
    val mins = (seconds / 60).toInt()
    val secs = (seconds % 60).toInt()
    return "${mins}:${secs.toString().padStart(2, '0')}"
}