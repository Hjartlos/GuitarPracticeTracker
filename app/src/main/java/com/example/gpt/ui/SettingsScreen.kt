package com.example.gpt.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import java.io.File

@Composable
fun SettingsScreen(viewModel: SessionViewModel) {
    val context = LocalContext.current
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val weeklyGoal by viewModel.weeklyGoalHours.collectAsState()

    var showGoalDialog by remember { mutableStateOf(false) }
    var showCalibrationDialog by remember { mutableStateOf(false) }

    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let {
            try {
                val csvData = viewModel.exportToCSV(context)
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(csvData.toByteArray())
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Settings", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Text("Customize your experience", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), fontSize = 14.sp)

        Spacer(modifier = Modifier.height(24.dp))

        SettingRow(icon = Icons.Default.DarkMode, title = "Dark Mode", subtitle = if(isDarkMode) "Dark theme" else "Light theme") {
            Switch(
                checked = isDarkMode,
                onCheckedChange = { viewModel.setDarkMode(it) },
                colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary, checkedThumbColor = MaterialTheme.colorScheme.onPrimary)
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

        SettingRow(icon = Icons.Default.Flag, title = "Weekly Goal", subtitle = "$weeklyGoal hours per week") {
            IconButton(onClick = { showGoalDialog = true }) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Goal", tint = MaterialTheme.colorScheme.primary)
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

        SettingRow(icon = Icons.Default.Tune, title = "Audio Calibration", subtitle = "Sensitivity, Latency & Rhythm") {
            IconButton(onClick = { showCalibrationDialog = true }) {
                Icon(Icons.Default.SettingsInputComponent, contentDescription = "Calibrate", tint = MaterialTheme.colorScheme.primary)
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

        SettingRow(icon = Icons.Default.Download, title = "Export Data", subtitle = "Save or Share CSV") {
            Row {
                IconButton(onClick = {
                    try {
                        val csv = viewModel.exportToCSV(context)
                        val file = File(context.cacheDir, "guitar_practice.csv")
                        file.writeText(csv)
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        val intent = Intent(Intent.ACTION_SEND).apply { type = "text/csv"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                        context.startActivity(Intent.createChooser(intent, "Share CSV"))
                    } catch (e: Exception) { e.printStackTrace() }
                }) { Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary) }

                IconButton(onClick = { saveFileLauncher.launch("guitar_practice_${System.currentTimeMillis()}.csv") }) {
                    Icon(Icons.Default.Save, contentDescription = "Save", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(modifier = Modifier.height(50.dp))
    }

    if (showGoalDialog) {
        var newGoal by remember { mutableStateOf(weeklyGoal.toString()) }
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = { Text("Set Weekly Goal", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                OutlinedTextField(
                    value = newGoal,
                    onValueChange = { newGoal = it.filter { c -> c.isDigit() } },
                    label = { Text("Hours") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface, focusedBorderColor = MaterialTheme.colorScheme.primary)
                )
            },
            confirmButton = { TextButton(onClick = { newGoal.toIntOrNull()?.let { viewModel.setWeeklyGoal(it) }; showGoalDialog = false }) { Text("Save", color = MaterialTheme.colorScheme.primary) } },
            dismissButton = { TextButton(onClick = { showGoalDialog = false }) { Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.6f)) } },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showCalibrationDialog) {
        CalibrationDialog(viewModel = viewModel, onDismiss = { showCalibrationDialog = false; viewModel.stopTestMetronome() })
    }
}

@Composable
fun CalibrationDialog(viewModel: SessionViewModel, onDismiss: () -> Unit) {
    val threshold by viewModel.inputThreshold.collectAsState()
    val margin by viewModel.rhythmMargin.collectAsState()
    val latencyOffset by viewModel.latencyOffset.collectAsState()
    val isTestRunning by viewModel.isTestMetronomeRunning.collectAsState()

    val amplitude by viewModel.amplitude.collectAsState()
    val animatedAmplitude by animateFloatAsState(targetValue = amplitude, label = "amp")

    LaunchedEffect(Unit) { viewModel.startMonitoring() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Audio Calibration", color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {

                Text("Input Check", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { viewModel.toggleTestMetronome() },
                        colors = ButtonDefaults.buttonColors(containerColor = if(isTestRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(if(isTestRunning) "Stop Test" else "Play Click", fontSize = 10.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    val progress = (animatedAmplitude / 60f).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)),
                        color = if(progress > 0.1f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                val sliderThreshold = 1f - ((threshold - 0.05f) / 0.45f)
                Text("Sensitivity (${(sliderThreshold * 100).toInt()}%)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)

                Slider(
                    value = sliderThreshold.coerceIn(0f, 1f),
                    onValueChange = { viewModel.setInputThreshold(0.5f - (it * 0.45f)) },
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Low (Anti-noise)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("High (Sensitive)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(16.dp))

                Text("Latency Compensation (${latencyOffset}ms)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                Text("Increase if recording feels late compared to metronome.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = latencyOffset.toFloat(),
                    onValueChange = { viewModel.setLatencyOffset(it.toInt()) },
                    valueRange = 0f..300f,
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.height(16.dp))

                val sliderMargin = 1f - ((margin - 0.1f) / 0.4f)
                Text("Rhythm Strictness (${(sliderMargin * 100).toInt()}%)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)

                Slider(
                    value = sliderMargin.coerceIn(0f, 1f),
                    onValueChange = { viewModel.setRhythmMargin(0.5f - (it * 0.4f)) },
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Normal", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Strict", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun SettingRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, trailing: @Composable () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(subtitle, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), fontSize = 12.sp)
            }
        }
        trailing()
    }
}