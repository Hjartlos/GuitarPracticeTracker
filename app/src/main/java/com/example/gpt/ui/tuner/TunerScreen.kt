package com.example.gpt.ui.tuner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gpt.R
import com.example.gpt.core.audio.ToneGenerator
import com.example.gpt.ui.practice.PracticeViewModel
import java.util.Locale
import kotlin.math.abs

@Composable
fun TunerScreen(viewModel: PracticeViewModel) {
    val tunerResult by viewModel.tunerResult.collectAsState()
    val sensitivity by viewModel.inputThreshold.collectAsState()
    val baseFrequency by viewModel.baseFrequency.collectAsState()
    val useFlats by viewModel.useFlats.collectAsState()

    val isTonePlaying by ToneGenerator.isPlaying.collectAsState()
    val isDark = isSystemInDarkTheme()

    DisposableEffect(Unit) {
        viewModel.startMonitoring()

        onDispose {
            ToneGenerator.stopCurrentTone()
            viewModel.stopMonitoring()
        }
    }

    var stringCount by remember { mutableIntStateOf(6) }
    val currentStrings = remember { mutableStateListOf<GuitarString>() }
    val tunedStrings = remember { mutableStateListOf<Int>() }

    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(stringCount, baseFrequency) {
        currentStrings.clear()
        tunedStrings.clear()
        currentStrings.addAll(TuningData.generateStandardTuning(stringCount, baseFrequency.toFloat()))
    }

    var editingString by remember { mutableStateOf<GuitarString?>(null) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item(span = { GridItemSpan(2) }) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .shadow(8.dp, RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(
                        containerColor = if(isDark) MaterialTheme.colorScheme.surface.copy(alpha=0.9f)
                        else MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        TunerGauge(
                            tunerResult = tunerResult,
                            useFlats = useFlats,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 8.dp, bottom = 4.dp, start = 4.dp, end = 4.dp)
                        )

                        IconButton(
                            onClick = { showSettings = !showSettings },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = if(showSettings) Icons.Default.Close else Icons.Default.Settings,
                                contentDescription = stringResource(R.string.settings_title),
                                tint = if(showSettings) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.6f)
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = showSettings,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if(isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            val sensitivityPercent = ((0.5f - sensitivity) / 0.45f * 100).toInt().coerceIn(0, 100)
                            Text(stringResource(R.string.mic_sensitivity_fmt, sensitivityPercent), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Slider(
                                value = (0.5f - sensitivity) / 0.45f,
                                onValueChange = { sliderValue ->
                                    val newThreshold = 0.5f - (sliderValue * 0.45f)
                                    viewModel.setInputThreshold(newThreshold)
                                },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary)
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(stringResource(R.string.reference_pitch_fmt), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("${baseFrequency} Hz", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = baseFrequency.toFloat(),
                                onValueChange = { viewModel.setBaseFrequency(it.toInt()) },
                                valueRange = 415f..466f,
                                steps = 50,
                                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.string_setup), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    StringsSelector(current = stringCount) { stringCount = it }
                }

                Text(
                    text = stringResource(R.string.tap_to_play_tone),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )
            }
        }

        items(currentStrings) { guitarString ->
            val isDetecting = abs(tunerResult.frequency - guitarString.frequency) < 5.0
            val isPerfect = isDetecting && abs(tunerResult.cents) < 5 && tunerResult.isLocked

            if (isPerfect && !tunedStrings.contains(guitarString.id)) {
                tunedStrings.add(guitarString.id)
            }

            val isAlreadyTuned = tunedStrings.contains(guitarString.id)

            val displayString = guitarString.copy(
                name = if (useFlats) TuningData.convertSharpToFlat(guitarString.name) else guitarString.name
            )

            CompactStringCard(
                guitarString = displayString,
                isDetecting = isDetecting,
                isPerfect = isPerfect,
                isAlreadyTuned = isAlreadyTuned,
                isTonePlaying = isTonePlaying,
                onPlay = {
                    if (!isTonePlaying) {
                        ToneGenerator.playGuitarTone(guitarString.frequency)
                    }
                },
                onEdit = { editingString = guitarString }
            )
        }
    }

    if (editingString != null) {
        EditStringDialog(
            guitarString = editingString!!,
            useFlats = useFlats,
            onDismiss = { editingString = null },
            onSave = { note, octave ->
                val index = currentStrings.indexOf(editingString)
                if (index != -1) {
                    val newFreq = TuningData.calculateFreq(note, octave, baseFrequency.toFloat())
                    currentStrings[index] = currentStrings[index].copy(name = note, octave = octave, frequency = newFreq)
                    tunedStrings.remove(currentStrings[index].id)
                }
                editingString = null
            }
        )
    }
}

@Composable
fun CompactStringCard(
    guitarString: GuitarString,
    isDetecting: Boolean,
    isPerfect: Boolean,
    isAlreadyTuned: Boolean,
    isTonePlaying: Boolean,
    onPlay: () -> Unit,
    onEdit: () -> Unit
) {
    val targetColor = when {
        isPerfect -> Color(0xFF00E676)
        isAlreadyTuned -> Color(0xFF1B5E20).copy(alpha = 0.6f)
        isDetecting -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val animatedBg by animateColorAsState(targetColor, label = "cardBg")
    val borderColor = if (isPerfect) Color(0xFF00E676) else MaterialTheme.colorScheme.primary
    val borderStroke = if (isDetecting) 2.dp else 0.dp

    val stringColor = when {
        isPerfect -> Color.White
        isDetecting -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    }

    val stringThickness = when(guitarString.id) {
        1 -> 1.5f
        2 -> 2f
        3 -> 2.5f
        4 -> 3.5f
        5 -> 4f
        6 -> 4.5f
        7 -> 5f
        else -> 3f
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .clickable(enabled = !isTonePlaying) { onPlay() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = animatedBg),
        border = if (isDetecting) BorderStroke(borderStroke, borderColor) else null
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
            ) {
                val width = size.width
                val height = size.height
                val centerY = height / 2

                drawLine(
                    color = stringColor.copy(alpha = 0.3f),
                    start = Offset(0f, centerY),
                    end = Offset(width, centerY),
                    strokeWidth = stringThickness,
                    cap = StrokeCap.Round
                )

                if (isDetecting && !isPerfect) {
                    val waveAmplitude = 4f
                    val waveFrequency = 8f
                    var x = 0f
                    while (x < width) {
                        val y1 = centerY + waveAmplitude * kotlin.math.sin((x / width) * waveFrequency * Math.PI).toFloat()
                        val y2 = centerY + waveAmplitude * kotlin.math.sin(((x + 2) / width) * waveFrequency * Math.PI).toFloat()
                        drawLine(
                            color = stringColor.copy(alpha = 0.25f),
                            start = Offset(x, y1),
                            end = Offset(x + 2, y2),
                            strokeWidth = stringThickness * 0.6f
                        )
                        x += 2f
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.string_n, guitarString.id),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if(isPerfect) Color.White.copy(alpha=0.9f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = guitarString.fullName(),
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${String.format(Locale.US, "%.1f", guitarString.frequency)} Hz",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }

            if (isAlreadyTuned || isPerfect) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = if(isPerfect) Color.White else Color(0xFF00E676),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .size(16.dp)
                )
            }

            IconButton(
                onClick = onEdit,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.edit),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun EditStringDialog(
    guitarString: GuitarString,
    useFlats: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit
) {
    var selectedNote by remember { mutableStateOf(if (useFlats) TuningData.convertSharpToFlat(guitarString.name) else guitarString.name) }
    var selectedOctave by remember { mutableIntStateOf(guitarString.octave) }

    val octaves = TuningData.validOctaves
    val notes = TuningData.notes.map { if (useFlats) TuningData.convertSharpToFlat(it) else it }

    val noteSharpForValidation = TuningData.convertFlatToSharp(selectedNote)
    val isCurrentValid = TuningData.isValidTuning(noteSharpForValidation, selectedOctave)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_string_title, guitarString.id), color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column {
                Row(modifier = Modifier.fillMaxWidth().height(200.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.note), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        LazyColumn(modifier = Modifier.width(60.dp)) {
                            items(notes) { note ->
                                val noteSharp = TuningData.convertFlatToSharp(note)
                                val isNoteValid = TuningData.isValidTuning(noteSharp, selectedOctave)

                                TextButton(
                                    onClick = { if (isNoteValid) selectedNote = note },
                                    enabled = isNoteValid,
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = when {
                                            selectedNote == note -> MaterialTheme.colorScheme.primary
                                            isNoteValid -> MaterialTheme.colorScheme.onSurfaceVariant
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                        },
                                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Text(note, fontWeight = if(selectedNote == note) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }
                    Box(Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outline.copy(alpha=0.2f)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.octave), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        LazyColumn(modifier = Modifier.width(60.dp)) {
                            items(octaves) { octave ->
                                val currentNoteSharp = TuningData.convertFlatToSharp(selectedNote)

                                TextButton(
                                    onClick = {
                                        selectedOctave = octave
                                        if (!TuningData.isValidTuning(currentNoteSharp, octave)) {
                                            val validNotes = TuningData.getValidNotesForOctave(octave)
                                            if (validNotes.isNotEmpty()) {
                                                selectedNote = if (useFlats) TuningData.convertSharpToFlat(validNotes.first()) else validNotes.first()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = if(selectedOctave == octave) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) {
                                    Text("$octave", fontWeight = if(selectedOctave == octave) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }
                }

                if (!isCurrentValid) {
                    Text(
                        text = stringResource(R.string.tuning_out_of_range),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedNote, selectedOctave) },
                enabled = isCurrentValid,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant) } },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun StringsSelector(current: Int, onSelect: (Int) -> Unit) {
    Row(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(6, 7).forEach { count ->
            Box(modifier = Modifier.clip(CircleShape).background(if (current == count) MaterialTheme.colorScheme.primary else Color.Transparent).clickable { onSelect(count) }.padding(horizontal = 12.dp, vertical = 8.dp)) {
                val textColor = if (current == count) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                Text("$count", color = textColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}