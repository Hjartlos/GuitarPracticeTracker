package com.example.gpt.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gpt.audio.ToneGenerator
import kotlin.math.abs

@Composable
fun TunerScreen(viewModel: SessionViewModel) {
    val tunerResult by viewModel.tunerResult.collectAsState()
    LaunchedEffect(Unit) { viewModel.startMonitoring() }

    var stringCount by remember { mutableStateOf(6) }
    val currentStrings = remember { mutableStateListOf<GuitarString>() }

    val tunedStrings = remember { mutableStateListOf<Int>() }

    LaunchedEffect(stringCount) {
        currentStrings.clear()
        tunedStrings.clear()
        currentStrings.addAll(TuningData.generateStandardTuning(stringCount))
    }

    var editingString by remember { mutableStateOf<GuitarString?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .shadow(8.dp, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                TunerGauge(
                    tunerResult = tunerResult,
                    modifier = Modifier.fillMaxSize().padding(top = 20.dp)
                )

                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = if(tunerResult.frequency > 20) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.3f),
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Guitar Setup", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            StringsSelector(current = stringCount) { stringCount = it }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            currentStrings.forEach { guitarString ->
                val isDetecting = abs(tunerResult.frequency - guitarString.frequency) < 4.0
                val isPerfect = isDetecting && abs(tunerResult.cents) < 5

                if (isPerfect && !tunedStrings.contains(guitarString.id)) {
                    tunedStrings.add(guitarString.id)
                }

                val isAlreadyTuned = tunedStrings.contains(guitarString.id)

                GuitarStringRowCard(
                    guitarString = guitarString,
                    isDetecting = isDetecting,
                    isPerfect = isPerfect,
                    isAlreadyTuned = isAlreadyTuned,
                    onPlay = { ToneGenerator.playTone(guitarString.frequency) },
                    onEdit = { editingString = guitarString },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (editingString != null) {
        EditStringDialog(
            guitarString = editingString!!,
            onDismiss = { editingString = null },
            onSave = { note, octave ->
                val index = currentStrings.indexOf(editingString)
                if (index != -1) {
                    val newFreq = TuningData.calculateFreq(note, octave)
                    currentStrings[index] = currentStrings[index].copy(name = note, octave = octave, frequency = newFreq)
                    tunedStrings.remove(currentStrings[index].id)
                }
                editingString = null
            }
        )
    }
}

@Composable
fun GuitarStringRowCard(
    guitarString: GuitarString,
    isDetecting: Boolean,
    isPerfect: Boolean,
    isAlreadyTuned: Boolean,
    onPlay: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val targetColor = when {
        isPerfect -> Color(0xFF00E676)
        isAlreadyTuned -> Color(0xFF1B5E20).copy(alpha = 0.6f)
        isDetecting -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val animatedBg by animateColorAsState(targetColor, label = "rowBg")
    val borderStroke = if (isDetecting) 2.dp else 0.dp
    val borderColor = if (isPerfect) Color(0xFF00E676) else MaterialTheme.colorScheme.primary
    val contentAlpha = if (isAlreadyTuned && !isDetecting) 0.6f else 1f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onPlay() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = animatedBg),
        border = if (isDetecting) androidx.compose.foundation.BorderStroke(borderStroke, borderColor) else null
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(MaterialTheme.colorScheme.background.copy(alpha=0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isAlreadyTuned) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Tuned",
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Text("${guitarString.id}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = guitarString.fullName(),
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "${String.format("%.1f", guitarString.frequency)} Hz",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                fontWeight = FontWeight.Medium
            )

            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun EditStringDialog(guitarString: GuitarString, onDismiss: () -> Unit, onSave: (String, Int) -> Unit) {
    var selectedNote by remember { mutableStateOf(guitarString.name) }
    var selectedOctave by remember { mutableStateOf(guitarString.octave) }
    val notes = TuningData.notes
    val octaves = (0..8).toList()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit String ${guitarString.id}", color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Row(modifier = Modifier.fillMaxWidth().height(200.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Note", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    LazyColumn(modifier = Modifier.width(60.dp)) {
                        items(notes) { note ->
                            TextButton(onClick = { selectedNote = note }, colors = ButtonDefaults.textButtonColors(contentColor = if(selectedNote == note) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)) { Text(note, fontWeight = if(selectedNote == note) FontWeight.Bold else FontWeight.Normal) }
                        }
                    }
                }
                Box(Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outline.copy(alpha=0.2f)))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Octave", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    LazyColumn(modifier = Modifier.width(60.dp)) {
                        items(octaves) { octave ->
                            TextButton(onClick = { selectedOctave = octave }, colors = ButtonDefaults.textButtonColors(contentColor = if(selectedOctave == octave) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)) { Text("$octave", fontWeight = if(selectedOctave == octave) FontWeight.Bold else FontWeight.Normal) }
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(selectedNote, selectedOctave) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) } },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun StringsSelector(current: Int, onSelect: (Int) -> Unit) {
    Row(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(6, 7, 8).forEach { count ->
            Box(modifier = Modifier.clip(CircleShape).background(if (current == count) MaterialTheme.colorScheme.primary else Color.Transparent).clickable { onSelect(count) }.padding(horizontal = 12.dp, vertical = 8.dp)) {
                val textColor = if (current == count) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                Text("$count", color = textColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}