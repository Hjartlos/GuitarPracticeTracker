package com.example.gpt.ui.practice

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gpt.R
import com.example.gpt.core.audio.BeatType
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.sqrt

@Composable
fun PracticeScreen(viewModel: PracticeViewModel) {
    val weeklyProgress by viewModel.weeklyProgress.collectAsState()
    val weeklyGoal by viewModel.weeklyGoalHours.collectAsState()
    val currentStreak by viewModel.streakDays.collectAsState()

    val isSessionActive by viewModel.isSessionActive.collectAsState()
    val isCountingIn by viewModel.isCountingIn.collectAsState()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()

    val exerciseType by viewModel.exerciseType.collectAsState()
    val tuning by viewModel.tuning.collectAsState()
    val notes by viewModel.notes.collectAsState()

    val exerciseTypeError by viewModel.exerciseTypeError.collectAsState()
    val tuningError by viewModel.tuningError.collectAsState()

    val isMetronomeEnabled by viewModel.isMetronomeEnabled.collectAsState()

    val isRhythmAnalysisEnabled by viewModel.isRhythmAnalysisEnabled.collectAsState()

    val metronomeBpm by viewModel.metronomeBpm.collectAsState()
    val timeSignature by viewModel.timeSignature.collectAsState()

    val beatPattern by viewModel.beatPattern.collectAsState()
    val currentPlayingBeat by viewModel.currentPlayingBeat.collectAsState()
    val isMetronomePlaying by viewModel.isMetronomePlaying.collectAsState()

    val amplitude by viewModel.amplitude.collectAsState()

    val scrollState = rememberScrollState()
    val isDark = isSystemInDarkTheme()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DashboardHeader(
            weeklyProgress = weeklyProgress,
            weeklyGoalHours = weeklyGoal,
            streakDays = currentStreak
        )

        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 24.dp)
                ) {
                    if (isCountingIn) {
                        Text(
                            text = stringResource(R.string.get_ready),
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.displayMedium
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSessionActive) {
                                Box(modifier = Modifier.align(Alignment.CenterStart).padding(start = 24.dp)) {
                                    RecordingDot()
                                }
                            }

                            Text(
                                text = formatTime(elapsedSeconds),
                                fontSize = 64.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (isSessionActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                style = MaterialTheme.typography.displayLarge
                            )
                        }
                    }

                    if (isMetronomeEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.rhythm_pattern),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.6f),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        InteractiveMetronomeDisplay(
                            beatPattern = beatPattern,
                            currentPlayingBeat = if (isSessionActive || isCountingIn) currentPlayingBeat else -1,
                            bpm = metronomeBpm,
                            onBeatClick = { index -> viewModel.toggleBeatType(index) }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                            LegendItem(color = Color(0xFFD32F2F), label = stringResource(R.string.accent))
                            Spacer(modifier = Modifier.width(12.dp))
                            LegendItem(color = Color(0xFF8B0000), label = stringResource(R.string.beat))
                            Spacer(modifier = Modifier.width(12.dp))
                            LegendItem(color = Color.Gray.copy(alpha=0.5f), label = stringResource(R.string.mute))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

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

            Spacer(modifier = Modifier.height(32.dp))

            if (!isSessionActive) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.metronome),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Switch(
                                checked = isMetronomeEnabled,
                                onCheckedChange = { viewModel.toggleMetronomeEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        if (isMetronomeEnabled) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Analytics,
                                        contentDescription = null,
                                        tint = if (isRhythmAnalysisEnabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = stringResource(R.string.enable_rhythm_analysis),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (isRhythmAnalysisEnabled)
                                                stringResource(R.string.rhythm_analysis_desc_on)
                                            else
                                                stringResource(R.string.rhythm_analysis_desc_off),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Switch(
                                    checked = isRhythmAnalysisEnabled,
                                    onCheckedChange = { viewModel.toggleRhythmAnalysis(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = MaterialTheme.colorScheme.secondary,
                                        checkedThumbColor = MaterialTheme.colorScheme.onSecondary
                                    ),
                                    modifier = Modifier.scale(0.9f)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            val parts = timeSignature.split("/")
                            val currentBeats = parts.getOrNull(0)?.toIntOrNull() ?: 4
                            val currentNoteValue = parts.getOrNull(1)?.toIntOrNull() ?: 4

                            val maxSliderValue = if (currentNoteValue == 8) 150f else 300f

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("$metronomeBpm BPM", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }

                            Slider(
                                value = metronomeBpm.toFloat(),
                                onValueChange = { viewModel.setMetronomeBpm(it.toInt()) },
                                valueRange = 30f..maxSliderValue,
                                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                            )

                            var beatsInput by remember(currentBeats) { mutableStateOf(currentBeats.toString()) }

                            LaunchedEffect(currentBeats) {
                                if (beatsInput.toIntOrNull() != currentBeats) {
                                    beatsInput = currentBeats.toString()
                                }
                            }

                            Text(stringResource(R.string.time_sig), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                OutlinedTextField(
                                    value = beatsInput,
                                    onValueChange = { input ->
                                        if (input.isEmpty()) {
                                            beatsInput = ""
                                            return@OutlinedTextField
                                        }
                                        if (input.all { it.isDigit() }) {
                                            val number = input.toIntOrNull()
                                            if (number != null && number in 1..16) {
                                                beatsInput = input
                                                viewModel.setMetronomeTimeSignature("$number/$currentNoteValue")
                                            }
                                        }
                                    },
                                    label = { Text(stringResource(R.string.beats)) },
                                    modifier = Modifier.width(90.dp),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(4, 8).forEach { value ->
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
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(stringResource(R.string.exercise_type), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = exerciseType,
                    onValueChange = { viewModel.updateExerciseType(it) },
                    label = { Text(if(exerciseTypeError) stringResource(R.string.required_field) else stringResource(R.string.enter_type_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = exerciseTypeError,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                Spacer(modifier = Modifier.height(8.dp))

                val exercises = listOf(
                    "Scales" to R.string.ex_scales,
                    "Riffs" to R.string.ex_riffs,
                    "Solos" to R.string.ex_solos,
                    "Chords" to R.string.ex_chords,
                    "Impro" to R.string.ex_impro,
                    "Theory" to R.string.ex_theory,
                    "Warmup" to R.string.ex_warmup
                )

                val firstExerciseName = stringResource(R.string.ex_scales)
                LaunchedEffect(exerciseType) {
                    if (exerciseType.isEmpty()) {
                        viewModel.updateExerciseType(firstExerciseName)
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(exercises) { (_, resId) ->
                        val localizedName = stringResource(resId)

                        FilterChip(
                            selected = exerciseType == localizedName,
                            onClick = {
                                viewModel.updateExerciseType(localizedName)
                            },
                            label = { Text(localizedName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(stringResource(R.string.tuning_label), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = tuning,
                    onValueChange = { viewModel.updateTuning(it) },
                    label = { Text(if(tuningError) stringResource(R.string.required_field) else stringResource(R.string.enter_tuning_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = tuningError,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                Spacer(modifier = Modifier.height(8.dp))
                val tunings = listOf("E Standard", "Eb Standard", "Drop D", "Drop C", "Open G", "Open C")
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
                label = { Text(stringResource(R.string.session_notes_label)) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun DashboardHeader(
    weeklyProgress: Float,
    weeklyGoalHours: Int,
    streakDays: Int = 3
) {
    val currentHours = (weeklyProgress * weeklyGoalHours)
    val percentage = (weeklyProgress * 100).toInt()
    val isDark = isSystemInDarkTheme()
    val animatedProgress by animateFloatAsState(
        targetValue = weeklyProgress,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "progress"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.dashboard_weekly_goal).uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.dashboard_streak_fmt, streakDays),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = if(isDark) 0.08f else 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    Color(0xFFFF5252)
                                )
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = String.format(java.util.Locale.US, "%.1fh / %dh", currentHours, weeklyGoalHours),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun RecordingDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "recDot")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(Color.Red.copy(alpha = alpha))
    )
}

@Composable
fun InteractiveMetronomeDisplay(
    beatPattern: List<BeatType>,
    currentPlayingBeat: Int,
    bpm: Int,
    onBeatClick: (Int) -> Unit
) {
    val safeAnimDuration = if (bpm > 0) min(100, (60000 / bpm) / 2) else 100
    val beatCount = beatPattern.size

    if (beatCount == 0) return

    val sweepAngle = 360f / beatCount

    val gapAngle = if (beatCount < 8) 7f else 8f
    val effectiveSweepAngle = (sweepAngle - gapAngle).coerceAtLeast(5f)

    val circleSize = 180.dp

    val strokeWidth = 24f

    val animatedColors = beatPattern.mapIndexed { index, type ->
        val isPlayingNow = (index + 1) == currentPlayingBeat
        val baseColor = when(type) {
            BeatType.ACCENT -> Color(0xFFD32F2F)
            BeatType.NORMAL -> Color(0xFF8B0000)
            BeatType.MUTE -> Color.Gray.copy(alpha=0.3f)
        }
        animateColorAsState(
            targetValue = if(isPlayingNow) Color.White else baseColor,
            animationSpec = tween(durationMillis = safeAnimDuration),
            label = "beatColor$index"
        ).value
    }

    val animatedStrokeWidths = beatPattern.mapIndexed { index, _ ->
        val isPlayingNow = (index + 1) == currentPlayingBeat
        animateFloatAsState(
            targetValue = if(isPlayingNow) strokeWidth * 1.4f else strokeWidth,
            animationSpec = tween(durationMillis = safeAnimDuration),
            label = "strokeWidth$index"
        ).value
    }

    Box(
        modifier = Modifier
            .size(circleSize)
            .pointerInput(beatCount) {
                detectTapGestures { offset ->
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    val tapX = offset.x - centerX
                    val tapY = offset.y - centerY

                    val distanceFromCenter = sqrt(tapX * tapX + tapY * tapY)
                    val maxRadius = min(size.width, size.height) / 2f
                    val innerRadius = maxRadius * 0.35f

                    if (distanceFromCenter < innerRadius) {
                        return@detectTapGestures
                    }

                    var angle = Math.toDegrees(atan2(tapX.toDouble(), -tapY.toDouble())).toFloat()
                    if (angle < 0) angle += 360f

                    val clickedIndex = (angle / sweepAngle).toInt().coerceIn(0, beatCount - 1)
                    onBeatClick(clickedIndex)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val ringStrokeWidth = strokeWidth
            val radius = (size.minDimension - ringStrokeWidth) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            beatPattern.forEachIndexed { index, _ ->
                val startAngle = -90f + (index * sweepAngle) + (gapAngle / 2f)

                drawArc(
                    color = animatedColors[index],
                    startAngle = startAngle,
                    sweepAngle = effectiveSweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(
                        width = animatedStrokeWidths[index],
                        cap = StrokeCap.Round
                    )
                )
            }
        }

        if (currentPlayingBeat > 0) {
            Text(
                text = currentPlayingBeat.toString(),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = if(isSystemInDarkTheme()) Color.White else MaterialTheme.colorScheme.onSurface
            )
        } else {
            Icon(
                imageVector = Icons.Default.TouchApp,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

fun formatTime(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%02d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}