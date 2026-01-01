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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

    LaunchedEffect(stringCount) {
        currentStrings.clear()
        currentStrings.addAll(TuningData.generateStandardTuning(stringCount))
    }

    var editingString by remember { mutableStateOf<GuitarString?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TunerGauge(tunerResult = tunerResult, modifier = Modifier.height(150.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StringsSelector(current = stringCount) { stringCount = it }
            Text("Tap to Play • Edit Icon to Set", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), fontSize = 11.sp)
        }

        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(currentStrings) { guitarString ->
                    val isDetecting = abs(tunerResult.frequency - guitarString.frequency) < 4.0
                    val isPerfect = isDetecting && abs(tunerResult.cents) < 5

                    GuitarStringRowCompact(
                        guitarString = guitarString,
                        isDetecting = isDetecting,
                        isPerfect = isPerfect,
                        onPlay = { ToneGenerator.playTone(guitarString.frequency) },
                        onEdit = { editingString = guitarString }
                    )
                }
            }
        }
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
                }
                editingString = null
            }
        )
    }
}

@Composable
fun GuitarStringRowCompact(
    guitarString: GuitarString,
    isDetecting: Boolean,
    isPerfect: Boolean,
    onPlay: () -> Unit,
    onEdit: () -> Unit
) {
    val targetColor = if (isPerfect) MaterialTheme.colorScheme.tertiary else if (isDetecting) MaterialTheme.colorScheme.primary else Color.Transparent
    val backgroundColor by animateColorAsState(targetColor, label = "bg")

    val textColor = if (isDetecting) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .clickable { onPlay() }
            .padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("${guitarString.id}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, modifier = Modifier.width(20.dp))

        val thickness = (guitarString.id * 0.6).dp
        Box(modifier = Modifier.weight(1f).padding(horizontal = 8.dp).height(thickness).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)))

        Text(guitarString.fullName(), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textColor)
        Spacer(modifier = Modifier.width(8.dp))
        Text("${String.format("%.1f", guitarString.frequency)}", fontSize = 10.sp,
            color = if (isDetecting) MaterialTheme.colorScheme.onPrimary.copy(alpha=0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(35.dp))

        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = if(isDetecting) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
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
    Row(modifier = Modifier.background(MaterialTheme.colorScheme.surface, CircleShape).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(6, 7, 8).forEach { count ->
            Box(modifier = Modifier.clip(CircleShape)
                .background(if (current == count) MaterialTheme.colorScheme.primary else Color.Transparent)
                .clickable { onSelect(count) }.padding(horizontal = 12.dp, vertical = 8.dp)) {
                val textColor = if (current == count) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                Text("$count", color = textColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}