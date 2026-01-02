package com.example.gpt.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun PracticeScreen(viewModel: SessionViewModel) {
    val isSessionActive by viewModel.isSessionActive.collectAsState()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()

    val exerciseType by viewModel.exerciseType.collectAsState()
    val tuning by viewModel.tuning.collectAsState()
    val notes by viewModel.notes.collectAsState()

    val isMetronomeEnabled by viewModel.isMetronomeEnabled.collectAsState()
    val metronomeBpm by viewModel.metronomeBpm.collectAsState()
    val timeSignature by viewModel.timeSignature.collectAsState()

    val amplitude by viewModel.amplitude.collectAsState()

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(top = 8.dp, bottom = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Guitar Practice Tracker",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .height(220.dp)
                .fillMaxWidth()
        ) {
            Box(modifier = Modifier.align(Alignment.Center).fillMaxWidth()) {
                if (isSessionActive) {
                    LiveWaveform(amplitude = amplitude, isActive = true)
                } else {
                    LiveWaveform(amplitude = 5f, isActive = false)
                }
            }

            Text(
                text = formatTime(elapsedSeconds),
                fontSize = 80.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (isSessionActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.align(Alignment.Center)
            )

            if (isSessionActive) {
                Text(
                    text = "● RECORDING",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    letterSpacing = 2.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = if (isSessionActive) 1.15f else 1f,
            animationSpec = infiniteRepeatable(animation = tween(800), repeatMode = RepeatMode.Reverse),
            label = "scale"
        )

        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(130.dp)) {
            if (isSessionActive) {
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .scale(pulseScale)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(MaterialTheme.colorScheme.primary.copy(alpha=0.4f), Color.Transparent)
                            ),
                            CircleShape
                        )
                )
            }

            Button(
                onClick = { viewModel.toggleSession() },
                modifier = Modifier
                    .size(90.dp)
                    .shadow(elevation = 10.dp, shape = CircleShape),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
                shape = CircleShape
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = if (isSessionActive)
                                    listOf(Color(0xFFB71C1C), Color(0xFFD32F2F))
                                else
                                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSessionActive) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (!isSessionActive) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { viewModel.toggleMetronomeEnabled(!isMetronomeEnabled) }
                    ) {
                        Checkbox(checked = isMetronomeEnabled, onCheckedChange = { viewModel.toggleMetronomeEnabled(it) })
                        Text("Metronome", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.weight(1f))
                        if (isMetronomeEnabled) {
                            Text("$metronomeBpm BPM", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (isMetronomeEnabled) {
                        Slider(
                            value = metronomeBpm.toFloat(),
                            onValueChange = { viewModel.setMetronomeBpm(it.toInt()) },
                            valueRange = 30f..300f
                        )

                        val parts = timeSignature.split("/")
                        val currentBeats = parts.getOrNull(0)?.toIntOrNull() ?: 4
                        val currentNoteValue = parts.getOrNull(1)?.toIntOrNull() ?: 4

                        Text("Time Signature", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedTextField(
                                value = currentBeats.toString(),
                                onValueChange = {
                                    val newBeats = it.filter { c -> c.isDigit() }.take(2)
                                    if(newBeats.isNotEmpty()) {
                                        viewModel.setMetronomeTimeSignature("$newBeats/$currentNoteValue")
                                    }
                                },
                                label = { Text("Beats") },
                                modifier = Modifier.width(80.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(4, 8, 16).forEach { value ->
                                    FilterChip(
                                        selected = currentNoteValue == value,
                                        onClick = { viewModel.setMetronomeTimeSignature("$currentBeats/$value") },
                                        label = { Text("1/$value") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            MetronomeVisualizer(bpm = metronomeBpm, timeSignature = timeSignature, isPlaying = true)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Exercise Type", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = exerciseType,
                onValueChange = { viewModel.updateExerciseType(it) },
                label = { Text("Enter type or select below") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            Spacer(modifier = Modifier.height(8.dp))
            val exercises = listOf("Scales", "Riffs", "Solos", "Chords", "Impro", "Theory", "Warmup")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(exercises) { type ->
                    FilterChip(
                        selected = exerciseType == type,
                        onClick = { viewModel.updateExerciseType(type) },
                        label = { Text(type) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text("Tuning", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = tuning,
                onValueChange = { viewModel.updateTuning(it) },
                label = { Text("Enter tuning or select below") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            Spacer(modifier = Modifier.height(8.dp))
            val tunings = listOf("E Standard", "Eb Standard", "Drop D", "Drop C", "Open G", "DADGAD")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tunings) { tune ->
                    FilterChip(
                        selected = tuning == tune,
                        onClick = { viewModel.updateTuning(tune) },
                        label = { Text(tune) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.tertiary,
                            selectedLabelColor = MaterialTheme.colorScheme.onTertiary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        OutlinedTextField(
            value = notes,
            onValueChange = { viewModel.updateNotes(it) },
            label = { Text("Session Notes (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun MetronomeVisualizer(bpm: Int, timeSignature: String, isPlaying: Boolean) {
    val beats = try {
        timeSignature.substringBefore("/").toInt()
    } catch (e: Exception) {
        4
    }

    val safeBeats = if (beats < 1) 4 else beats
    var currentBeat by remember { mutableIntStateOf(1) }

    LaunchedEffect(bpm, safeBeats, isPlaying) {
        if (bpm > 0 && isPlaying) {
            val delayMs = (60000.0 / bpm).toLong()
            while (true) {
                currentBeat = (currentBeat % safeBeats) + 1
                delay(delayMs)
            }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(safeBeats) { index ->
                val beatNum = index + 1
                val isActive = beatNum == currentBeat

                Box(
                    modifier = Modifier
                        .size(if(isActive) 16.dp else 12.dp)
                        .clip(CircleShape)
                        .background(
                            if(isActive) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.3f)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if(isPlaying) "Beat $currentBeat of $safeBeats ($timeSignature)" else "Ready",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun LiveWaveform(amplitude: Float, isActive: Boolean) {
    val history = remember { mutableStateListOf<Float>() }

    LaunchedEffect(amplitude, isActive) {
        if (isActive) {
            history.add(amplitude)
        } else {
            history.add((Math.sin(System.currentTimeMillis() / 200.0).toFloat() + 1) * 2f)
        }
        if (history.size > 50) history.removeAt(0)
    }

    val barColor = if(isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurface.copy(alpha=0.05f)

    Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
        val barWidth = size.width / 50f
        val maxHeight = size.height

        history.forEachIndexed { index, amp ->
            val barHeight = (amp * 1.5f).coerceIn(4f, maxHeight)
            val x = index * barWidth
            val y = (maxHeight - barHeight) / 2f

            drawRoundRect(
                color = barColor,
                topLeft = Offset(x + 2f, y),
                size = Size(barWidth - 4f, barHeight),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }
    }
}

fun formatTime(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%02d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}