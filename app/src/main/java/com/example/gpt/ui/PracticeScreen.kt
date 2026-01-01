package com.example.gpt.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.height(200.dp).fillMaxWidth()) {
            if (isSessionActive) {
                LiveWaveform(amplitude = amplitude, isActive = true)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formatTime(elapsedSeconds),
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSessionActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    style = MaterialTheme.typography.displayLarge
                )
                if (isSessionActive) {
                    Text("RECORDING", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = if (isSessionActive) 1.2f else 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )

        Box(contentAlignment = Alignment.Center) {
            if (isSessionActive) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(pulseScale)
                        .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape)
                )
            }

            Button(
                onClick = { viewModel.toggleSession() },
                modifier = Modifier.size(120.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSessionActive) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary
                ),
                shape = CircleShape,
                elevation = ButtonDefaults.buttonElevation(8.dp)
            ) {
                Icon(
                    imageVector = if (isSessionActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = if(isSessionActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        if (!isSessionActive) {
            Text("SESSION SETUP", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, letterSpacing = 2.sp)
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccessTime, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Metronome", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Switch(
                            checked = isMetronomeEnabled,
                            onCheckedChange = { viewModel.toggleMetronomeEnabled(it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                        )
                    }
                    if (isMetronomeEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("$metronomeBpm BPM", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("Sig: $timeSignature", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Slider(
                            value = metronomeBpm.toFloat(),
                            onValueChange = { viewModel.setMetronomeBpm(it.toInt()) },
                            valueRange = 40f..220f,
                            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                        )
                        OutlinedTextField(
                            value = timeSignature,
                            onValueChange = { viewModel.setMetronomeTimeSignature(it) },
                            label = { Text("Time Signature") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            ExerciseTypeSelector(exerciseType) { viewModel.updateExerciseType(it) }
            Spacer(modifier = Modifier.height(12.dp))
            TuningSelector(tuning) { viewModel.updateTuning(it) }
            Spacer(modifier = Modifier.height(24.dp))
        }

        OutlinedTextField(
            value = notes,
            onValueChange = { viewModel.updateNotes(it) },
            label = { Text("Session Notes") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        )

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun LiveWaveform(amplitude: Float, isActive: Boolean) {
    val history = remember { mutableStateListOf<Float>() }

    LaunchedEffect(amplitude) {
        if (isActive) {
            history.add(amplitude)
            if (history.size > 40) history.removeAt(0)
        }
    }

    val barColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)

    Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
        val barWidth = size.width / 40f
        val maxHeight = size.height

        history.forEachIndexed { index, amp ->
            val barHeight = (amp * 2f).coerceIn(0f, maxHeight)
            val x = index * barWidth
            val y = (maxHeight - barHeight) / 2f

            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barWidth * 0.8f, barHeight),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseTypeSelector(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val types = listOf("Scales", "Riffs", "Solos", "Theory", "Chords", "Improvisation")
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected, onValueChange = { onSelect(it) }, readOnly = false, label = { Text("Exercise Type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface, focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            types.forEach { type -> DropdownMenuItem(text = { Text(type, color = MaterialTheme.colorScheme.onSurface) }, onClick = { onSelect(type); expanded = false }) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TuningSelector(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val tunings = listOf("E Standard", "Drop D", "Drop C", "Open G", "DADGAD")
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected, onValueChange = { onSelect(it) }, readOnly = false, label = { Text("Tuning") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface, focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            tunings.forEach { tuning -> DropdownMenuItem(text = { Text(tuning, color = MaterialTheme.colorScheme.onSurface) }, onClick = { onSelect(tuning); expanded = false }) }
        }
    }
}

fun formatTime(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%02d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}