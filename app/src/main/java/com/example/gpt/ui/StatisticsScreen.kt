package com.example.gpt.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gpt.data.PracticeSession
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(viewModel: SessionViewModel) {
    val allSessions by viewModel.allSessions.collectAsState()
    val weeklyProgress by viewModel.weeklyProgress.collectAsState()
    val weeklyGoal by viewModel.weeklyGoalHours.collectAsState()

    var selectedExerciseFilter by remember { mutableStateOf<String?>(null) }
    var filterMetronomeOnly by remember { mutableStateOf(false) }

    val exerciseTypes = remember(allSessions) { allSessions.map { it.exerciseType }.distinct() }

    val filteredSessions = remember(allSessions, selectedExerciseFilter, filterMetronomeOnly) {
        allSessions.filter { session ->
            val typeMatch = selectedExerciseFilter == null || session.exerciseType == selectedExerciseFilter
            val metronomeMatch = !filterMetronomeOnly || session.avgBpm > 0
            typeMatch && metronomeMatch
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text("Statistics", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(16.dp))

            WeeklyGoalCard(progress = weeklyProgress, goalHours = weeklyGoal)
            Spacer(modifier = Modifier.height(24.dp))

            Text("Filters", fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha=0.7f), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = filterMetronomeOnly,
                        onClick = { filterMetronomeOnly = !filterMetronomeOnly },
                        label = { Text("Metronome Only") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = MaterialTheme.colorScheme.onPrimary)
                    )
                }
                items(exerciseTypes) { type ->
                    FilterChip(
                        selected = selectedExerciseFilter == type,
                        onClick = { selectedExerciseFilter = if (selectedExerciseFilter == type) null else type },
                        label = { Text(type) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = MaterialTheme.colorScheme.onPrimary)
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            Text("Last 7 Days (Minutes)", fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha=0.7f))
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth().height(250.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                val dailyData = remember(filteredSessions) { calculateDailyStats(filteredSessions) }
                BarChart(dailyData)
            }
            Spacer(modifier = Modifier.height(24.dp))

            Text("Time by Exercise", fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha=0.7f))
            Spacer(modifier = Modifier.height(8.dp))
            ExerciseBreakdown(filteredSessions)
            Spacer(modifier = Modifier.height(24.dp))

            Text("History", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(filteredSessions) { session ->
            SessionItemExpandable(session = session, viewModel = viewModel)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun SessionItemExpandable(
    session: PracticeSession,
    viewModel: SessionViewModel
) {
    var expanded by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

    val playingSessionId by viewModel.currentlyPlayingSessionId.collectAsState()
    val isPlaying = playingSessionId == session.id

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${session.exerciseType} (${session.tuning})",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp
                    )

                    if (session.avgBpm > 0) {
                        Text(
                            text = "${session.avgBpm} BPM • Acc: ${session.consistencyScore}%",
                            color = if(session.consistencyScore > 80) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    } else {
                        Text("Free Play", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontStyle = FontStyle.Italic)
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${session.durationSeconds / 60} min",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = dateFormat.format(Date(session.timestamp)),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.1f))
                Spacer(modifier = Modifier.height(12.dp))

                if (session.notes.isNotEmpty()) {
                    Text("Notes:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(session.notes, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (session.audioPath != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Nagłówek playera
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Session Recording", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { viewModel.toggleAudioPlayback(session) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        contentDescription = "Play/Stop",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                if (isPlaying) {
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
                                    Text("Tap to listen", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                } else {
                    Text("No Audio Available", fontSize = 12.sp, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.5f))
                }
            }
        }
    }
}

@Composable
fun WeeklyGoalCard(progress: Float, goalHours: Int) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Weekly Goal", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                Text("${(progress * goalHours).toInt()}h / ${goalHours}h", color = MaterialTheme.colorScheme.primary)
            }
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
        }
    }
}

@Composable
fun BarChart(data: List<Pair<String, Float>>) {
    val maxMinutes = data.maxOfOrNull { it.second }?.coerceAtLeast(10f) ?: 10f
    val chartMax = ((maxMinutes / 10).toInt() + 1) * 10f
    val barColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)

    Canvas(modifier = Modifier.fillMaxSize().padding(start = 40.dp, end = 16.dp, top = 20.dp, bottom = 30.dp)) {
        val width = size.width
        val height = size.height
        val barWidth = width / (data.size * 2f)
        val stepX = width / data.size
        val steps = 4
        for (i in 0..steps) {
            val yVal = chartMax * (i.toFloat() / steps)
            val yPos = height - (height * (i.toFloat() / steps))
            drawLine(color = gridColor, start = Offset(0f, yPos), end = Offset(width, yPos))
            drawContext.canvas.nativeCanvas.drawText("${yVal.toInt()}", -15f, yPos + 10f, android.graphics.Paint().apply { color = labelColor; textSize = 32f; textAlign = android.graphics.Paint.Align.RIGHT })
        }
        data.forEachIndexed { index, (dayName, minutes) ->
            val x = index * stepX + (stepX / 2) - (barWidth / 2)
            val barHeight = (minutes / chartMax) * height
            drawRect(color = barColor, topLeft = Offset(x, height - barHeight), size = Size(barWidth, barHeight))
            drawContext.canvas.nativeCanvas.drawText(dayName, x + barWidth/2, height + 40f, android.graphics.Paint().apply { color = labelColor; textSize = 34f; textAlign = android.graphics.Paint.Align.CENTER })
        }
    }
}

@Composable
fun ExerciseBreakdown(sessions: List<PracticeSession>) {
    val stats = remember(sessions) {
        sessions.groupBy { it.exerciseType }.mapValues { (_, list) -> list.sumOf { it.durationSeconds } / 60f }.toList().sortedByDescending { it.second }
    }
    val totalMinutes = stats.sumOf { it.second.toDouble() }.toFloat().coerceAtLeast(1f)
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (stats.isEmpty()) { Text("No data matching filters.", color = MaterialTheme.colorScheme.onSurfaceVariant) } else {
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
    for (i in 6 downTo 0) {
        val checkCal = Calendar.getInstance()
        checkCal.add(Calendar.DAY_OF_YEAR, -i)
        val dayStart = checkCal.apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0) }.timeInMillis
        val dayEnd = checkCal.apply { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59) }.timeInMillis
        val daySessions = sessions.filter { it.timestamp in dayStart..dayEnd }
        val totalMinutes = daySessions.sumOf { it.durationSeconds } / 60f
        days.add(dateFormat.format(checkCal.time) to totalMinutes)
    }
    return days
}