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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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
        in 90..100 -> Color(0xFF00C853)
        in 80..89 -> Color(0xFFFFD600)
        in 1..79 -> Color(0xFFD50000)
        else -> Color(0xFFD50000)
    }
}

@Composable
fun SessionItemExpandable(session: PracticeSession, viewModel: PracticeViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

    val activeSessionId by viewModel.activePlayerSessionId.collectAsState()
    val isGlobalPlaying by viewModel.isPlayerPlaying.collectAsState()
    val isActive = activeSessionId == session.id
    val isPlaying = isActive && isGlobalPlaying

    val analysisResults by viewModel.detailedAnalysisResults.collectAsState()
    val isAnalyzingMap by viewModel.isAnalyzingSession.collectAsState()

    val hitData = analysisResults[session.id]
    val isAnalyzing = isAnalyzingMap[session.id] == true

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("${session.exerciseType} (${session.tuning})", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)

                    if (session.avgBpm > 0) {
                        val accText = if (session.consistencyScore == 0) stringResource(R.string.no_notes_detected) else "Acc: ${session.consistencyScore}%"
                        val textColor = if (session.consistencyScore == 0) MaterialTheme.colorScheme.error else getAccuracyColor(session.consistencyScore)
                        Text("${session.avgBpm} BPM • $accText", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Text(stringResource(R.string.free_play), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontStyle = FontStyle.Italic)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${session.durationSeconds / 60} min", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text(dateFormat.format(Date(session.timestamp)), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            bpm = session.avgBpm
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
                                        contentDescription = "Play/Pause",
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
    bpm: Int
) {
    var showDetails by remember { mutableStateOf(false) }

    val greenColor = Color(0xFF00C853)
    val redColor = Color(0xFFD50000)
    val yellowColor = Color(0xFFFFD600)

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
                    value = "${analysisResult.hitsOnBeat}/${analysisResult.detailedHits.size}",
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
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            LegendItem(color = greenColor, label = stringResource(R.string.legend_on_beat))
            LegendItem(color = yellowColor, label = stringResource(R.string.legend_close))
            LegendItem(color = redColor, label = stringResource(R.string.legend_off_beat))
        }

        Spacer(modifier = Modifier.height(8.dp))

        ScrollableRhythmTimeline(
            hits = analysisResult.detailedHits,
            bpm = bpm,
            totalDuration = analysisResult.sessionDurationSeconds
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
    val totalHits = result.detailedHits.size
    val onBeat = result.hitsOnBeat
    val ghosts = result.detailedHits.count { it.isGhostNote }
    val early = result.detailedHits.count { !it.isGhostNote && !it.isOnBeat && it.deviationMs < 0 }
    val late = result.detailedHits.count { !it.isGhostNote && !it.isOnBeat && it.deviationMs > 0 }

    val onBeatPct = if(totalHits > 0) (onBeat * 100 / totalHits) else 0
    val ghostsPct = if(totalHits > 0) (ghosts * 100 / totalHits) else 0
    val earlyPct = if(totalHits > 0) (early * 100 / totalHits) else 0
    val latePct = if(totalHits > 0) (late * 100 / totalHits) else 0

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

            StatSummaryRow(stringResource(R.string.legend_on_beat), onBeat, onBeatPct, Color(0xFF00C853))
            StatSummaryRow("Early", early, earlyPct, Color(0xFFFFD600))
            StatSummaryRow("Late", late, latePct, Color(0xFFFFD600))
            StatSummaryRow(stringResource(R.string.stats_ghost), ghosts, ghostsPct, Color.Gray)

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.1f))
            StatSummaryRow("TOTAL", totalHits, 100, MaterialTheme.colorScheme.onSurface, isBold = true)
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
    totalDuration: Double
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    val pixelsPerSecond = 60.dp
    val totalWidth = with(density) { (totalDuration * pixelsPerSecond.toPx()).toInt() }

    val greenColor = Color(0xFF00C853)
    val redColor = Color(0xFFD50000)
    val yellowColor = Color(0xFFFFD600)
    val beatLineColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()

    val beatInterval = 60.0 / bpm

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
                        .width(with(density) { totalWidth.toDp() }.coerceAtLeast(300.dp))
                        .fillMaxHeight()
                        .padding(vertical = 8.dp)
                ) {
                    val height = size.height
                    val width = size.width
                    val pxPerSecond = if (totalDuration > 0) width / totalDuration.toFloat() else 60f
                    var beatTime = 0.0
                    var beatNumber = 0
                    while (beatTime <= totalDuration) {
                        val x = (beatTime * pxPerSecond).toFloat()
                        drawLine(
                            color = if (beatNumber % 4 == 0) beatLineColor.copy(alpha = 0.6f) else beatLineColor,
                            start = Offset(x, 0f),
                            end = Offset(x, height),
                            strokeWidth = if (beatNumber % 4 == 0) 2f else 1f,
                            pathEffect = if (beatNumber % 4 != 0) PathEffect.dashPathEffect(floatArrayOf(4f, 4f)) else null
                        )
                        if (beatNumber % 4 == 0) {
                            drawContext.canvas.nativeCanvas.drawText(
                                "${beatNumber + 1}",
                                x + 4f,
                                height - 5f,
                                android.graphics.Paint().apply {
                                    color = textColor
                                    textSize = 24f
                                    alpha = 255
                                }
                            )
                        }

                        beatTime += beatInterval
                        beatNumber++
                    }
                    var timeMarker = 0.0
                    while (timeMarker <= totalDuration) {
                        val x = (timeMarker * pxPerSecond).toFloat()

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

                        timeMarker += 5.0
                    }
                    hits.forEach { hit ->
                        val x = (hit.timeSeconds * pxPerSecond).toFloat()
                        val absDeviation = abs(hit.deviationMs)

                        val hitColor = when {
                            hit.isGhostNote -> Color.Gray
                            hit.isOnBeat -> greenColor
                            absDeviation < 80 -> yellowColor
                            else -> redColor
                        }

                        val normalizedDeviation = (absDeviation.coerceAtMost(100) / 100f)
                        val barHeight = height * (0.3f + 0.4f * (1f - normalizedDeviation))
                        val barWidth = 4f

                        drawRoundRect(
                            color = hitColor,
                            topLeft = Offset(x - barWidth / 2, (height - barHeight) / 2),
                            size = Size(barWidth, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
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

                val statusColor = when {
                    hit.isGhostNote -> ghostColor
                    hit.isOnBeat -> greenColor
                    absDeviation < 80 -> yellowColor
                    else -> redColor
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
                        text = if(hit.isGhostNote) stringResource(R.string.stats_ghost) else "${if (hit.deviationMs > 0) "+" else ""}${hit.deviationMs}ms",
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