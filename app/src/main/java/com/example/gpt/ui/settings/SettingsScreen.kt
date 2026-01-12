package com.example.gpt.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.gpt.R
import com.example.gpt.core.notifications.NotificationHelper
import com.example.gpt.core.util.LocaleHelper
import com.example.gpt.ui.practice.PracticeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun SettingsScreen(viewModel: PracticeViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val weeklyGoal by viewModel.weeklyGoalHours.collectAsState()
    val baseFreq by viewModel.baseFrequency.collectAsState()
    val useFlats by viewModel.useFlats.collectAsState()
    val latency by viewModel.latencyOffset.collectAsState()
    val currentLangCode by viewModel.currentLanguage.collectAsState()

    val appVersion = remember {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "Unknown"
        } catch (_: Exception) {
            "1.0.0"
        }
    }

    var showGoalDialog by remember { mutableStateOf(false) }
    var showCalibrationDialog by remember { mutableStateOf(false) }
    var showTuningDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showTutorialDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    val displayLanguage = when(currentLangCode) {
        "pl" -> "Polski"
        else -> "English"
    }

    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                try {
                    val csvData = viewModel.exportToCSV()
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(csvData.toByteArray())
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, R.string.save, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(it)?.use { inputStream ->
                        val csvContent = inputStream.bufferedReader().readText()
                        val result = viewModel.importFromCSV(csvContent)
                        withContext(Dispatchers.Main) {
                            result.fold(
                                onSuccess = { count ->
                                    snackbarHostState.showSnackbar(context.getString(R.string.import_success, count))
                                },
                                onFailure = {
                                    snackbarHostState.showSnackbar(context.getString(R.string.import_error))
                                }
                            )
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        snackbarHostState.showSnackbar(context.getString(R.string.import_error))
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            SettingsSectionTitle(stringResource(R.string.practice_section))
            SettingsCard {
                SettingRow(
                    icon = Icons.Default.Flag,
                    title = stringResource(R.string.weekly_goal_label),
                    subtitle = stringResource(R.string.hours_per_week_fmt, weeklyGoal)
                ) {
                    IconButton(onClick = { showGoalDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Goal", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SettingsSectionTitle(stringResource(R.string.audio_section))
            SettingsCard {
                val notationText = if(useFlats) stringResource(R.string.flats_label) else stringResource(R.string.sharps_label)

                SettingRow(
                    icon = Icons.Default.MusicNote,
                    title = stringResource(R.string.tuning_standards),
                    subtitle = stringResource(R.string.tuning_desc_fmt, baseFreq, notationText)
                ) {
                    IconButton(onClick = { showTuningDialog = true }) {
                        Icon(Icons.Default.Tune, contentDescription = "Tune Standards", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

                SettingRow(
                    icon = Icons.Default.SettingsInputComponent,
                    title = stringResource(R.string.audio_calibration),
                    subtitle = stringResource(R.string.audio_calib_subtitle_fmt, latency)
                ) {
                    IconButton(onClick = { showCalibrationDialog = true }) {
                        Icon(Icons.Default.Equalizer, contentDescription = "Calibrate Audio", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SettingsSectionTitle(stringResource(R.string.data_section))
            SettingsCard {
                SettingRow(
                    icon = Icons.Default.Download,
                    title = stringResource(R.string.export_data),
                    subtitle = stringResource(R.string.export_desc)
                ) {
                    Row {
                        IconButton(onClick = {
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val csv = viewModel.exportToCSV()
                                    val file = File(context.cacheDir, "guitar_practice.csv")
                                    file.writeText(csv)
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/csv"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share CSV"))
                                } catch (e: Exception) { e.printStackTrace() }
                            }
                        }) { Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary) }

                        IconButton(onClick = { saveFileLauncher.launch("guitar_practice_${System.currentTimeMillis()}.csv") }) {
                            Icon(Icons.Default.Save, contentDescription = "Save", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

                SettingRow(
                    icon = Icons.Default.Upload,
                    title = stringResource(R.string.import_data),
                    subtitle = stringResource(R.string.import_desc)
                ) {
                    IconButton(onClick = { importFileLauncher.launch("text/*") }) {
                        Icon(Icons.Default.FileOpen, contentDescription = "Import", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            NotificationsSection()

            Spacer(modifier = Modifier.height(16.dp))

            SettingsSectionTitle(stringResource(R.string.app_section))
            SettingsCard {
                val isHapticEnabled by viewModel.isHapticEnabled.collectAsState()

                SettingRow(
                    icon = Icons.Default.SettingsCell,
                    title = stringResource(R.string.haptic_feedback),
                    subtitle = stringResource(R.string.haptic_feedback_desc)
                ) {
                    Switch(
                        checked = isHapticEnabled,
                        onCheckedChange = { viewModel.setHapticEnabled(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

                SettingRow(
                    icon = Icons.Default.DarkMode,
                    title = stringResource(R.string.dark_mode),
                    subtitle = if(isDarkMode) stringResource(R.string.dark_theme_desc) else stringResource(R.string.light_theme_desc)
                ) {
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { viewModel.setDarkMode(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

                SettingRow(
                    icon = Icons.Default.Language,
                    title = stringResource(R.string.language),
                    subtitle = displayLanguage
                ) {
                    IconButton(onClick = { showLanguageDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Change Language", tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

                SettingRow(
                    icon = Icons.Default.HelpOutline,
                    title = stringResource(R.string.show_tutorial),
                    subtitle = stringResource(R.string.show_tutorial_desc)
                ) {
                    IconButton(onClick = { showTutorialDialog = true }) {
                        Icon(Icons.Default.School, contentDescription = "Tutorial", tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

                SettingRow(
                    icon = Icons.Default.Info,
                    title = stringResource(R.string.about),
                    subtitle = stringResource(R.string.about_desc)
                ) {
                    IconButton(onClick = { showAboutDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "About", tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(50.dp))
        }
    }

    if (showGoalDialog) {
        GoalDialog(weeklyGoal, { showGoalDialog = false }) { viewModel.setWeeklyGoal(it) }
    }

    if (showTuningDialog) {
        TuningStandardsDialog(viewModel = viewModel, onDismiss = { showTuningDialog = false })
    }

    if (showCalibrationDialog) {
        CalibrationDialog(viewModel = viewModel, onDismiss = { showCalibrationDialog = false; viewModel.stopTestMetronome() })
    }

    if (showLanguageDialog) {
        LanguageDialog(currentLangCode, { showLanguageDialog = false }) { code ->
            viewModel.setAppLanguage(code)
            LocaleHelper.setLocale(context, code)
        }
    }

    if (showAboutDialog) {
        AboutDialog(appVersion) { showAboutDialog = false }
    }

    if (showTutorialDialog) {
        TutorialDialog { showTutorialDialog = false }
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp, top = 8.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            content()
        }
    }
}

@Composable
fun SettingRow(icon: ImageVector, title: String, subtitle: String, trailing: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    lineHeight = 14.sp
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        trailing()
    }
}

@Composable
fun TuningStandardsDialog(viewModel: PracticeViewModel, onDismiss: () -> Unit) {
    val baseFreq by viewModel.baseFrequency.collectAsState()
    val useFlats by viewModel.useFlats.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tuning_standards)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.reference_pitch_fmt), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${baseFreq} Hz", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }

                Slider(
                    value = baseFreq.toFloat(),
                    onValueChange = { viewModel.setBaseFrequency(it.toInt()) },
                    valueRange = 415f..466f,
                    steps = 50,
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                )
                Text(
                    text = stringResource(R.string.ref_pitch_expl),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.note_notation), fontWeight = FontWeight.Bold)
                        Text(
                            text = if (useFlats) stringResource(R.string.using_flats) else stringResource(R.string.using_sharps),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = useFlats,
                        onCheckedChange = { viewModel.setUseFlats(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) } },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun CalibrationDialog(viewModel: PracticeViewModel, onDismiss: () -> Unit) {
    val threshold by viewModel.inputThreshold.collectAsState()
    val margin by viewModel.rhythmMargin.collectAsState()
    val latencyOffset by viewModel.latencyOffset.collectAsState()
    val metronomeOffset by viewModel.metronomeOffset.collectAsState()
    val isTestRunning by viewModel.isTestMetronomeRunning.collectAsState()

    val isLatencyTesting by viewModel.isLatencyTesting.collectAsState()
    val latencyResult by viewModel.latencyTestResult.collectAsState()
    val isTapCalibrating by viewModel.isTapCalibrating.collectAsState()
    val tapProgress by viewModel.tapCalibrationProgress.collectAsState()
    val tapBeat by viewModel.tapCalibrationBeat.collectAsState()

    val amplitude by viewModel.amplitude.collectAsState()
    val animatedAmplitude by animateFloatAsState(targetValue = amplitude, label = "amp")

    DisposableEffect(Unit) {
        viewModel.startMonitoring()
        onDispose {
            viewModel.stopMonitoring()
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (isTapCalibrating) viewModel.cancelTapCalibration()
            onDismiss()
        },
        title = { Text(stringResource(R.string.audio_calibration)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {

                Text(stringResource(R.string.input_check), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { viewModel.toggleTestMetronome() },
                        colors = ButtonDefaults.buttonColors(containerColor = if(isTestRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(if(isTestRunning) stringResource(R.string.stop_test) else stringResource(R.string.play_click), fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    val progress = (animatedAmplitude / 100f).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = if (progress > (threshold * 2)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                val sliderThreshold = (threshold / 0.2f).coerceIn(0f, 1f)
                val sensitivityPercent = ((1f - sliderThreshold) * 100).toInt()

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.mic_sensitivity_label), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text("$sensitivityPercent%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }

                Slider(
                    value = sliderThreshold,
                    onValueChange = {
                        viewModel.setInputThreshold((it * 0.2f).coerceAtLeast(0.001f))
                    },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                )
                Text(
                    text = stringResource(R.string.mic_sensitivity_expl),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 16.dp))

                Text(stringResource(R.string.latency_label), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

                if (isTapCalibrating) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(tapProgress, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                repeat(8) { index ->
                                    val isActive = tapBeat >= index + 1
                                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha=0.2f)))
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                stringResource(R.string.calibration_hold_near),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha=0.7f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = { viewModel.runLatencyAutoCalibration() },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        enabled = !isLatencyTesting,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Text(if (isLatencyTesting) stringResource(R.string.calibrating) else stringResource(R.string.auto_calibrate_btn))
                    }
                }

                if (latencyResult != null && !isTapCalibrating) {
                    Text(latencyResult!!, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(4.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.manual_calib_title), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text("${latencyOffset}ms", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = latencyOffset.toFloat(),
                    onValueChange = { viewModel.setLatencyOffset(it.toInt()) },
                    valueRange = 0f..300f,
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                )
                Text(
                    text = stringResource(R.string.latency_expl),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 16.dp))

                Text(stringResource(R.string.metronome_sync_title), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.manual_offset), style = MaterialTheme.typography.bodyMedium)
                    Text("${metronomeOffset}ms", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = metronomeOffset.toFloat(),
                    onValueChange = { viewModel.setMetronomeOffset(it.toInt()) },
                    valueRange = 0f..200f,
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                )
                Text(
                    text = stringResource(R.string.metronome_sync_expl),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 16.dp))

                Text(stringResource(R.string.rhythm_strictness_title), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

                val strictnessPercent = ((1f - margin) * 100).toInt()

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.rhythm_strictness_fmt, strictnessPercent), style = MaterialTheme.typography.bodyMedium)
                }

                Slider(
                    value = 1f - margin,
                    onValueChange = { viewModel.setRhythmMargin(1f - it) },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                )
                Text(
                    text = stringResource(R.string.rhythm_strictness_expl),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (isTapCalibrating) viewModel.cancelTapCalibration()
                onDismiss()
            }) { Text(stringResource(R.string.done), color = MaterialTheme.colorScheme.primary) }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun GoalDialog(current: Int, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    var text by remember { mutableStateOf(current.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.set_weekly_goal_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { if (it.all { c -> c.isDigit() }) text = it },
                label = { Text(stringResource(R.string.hours_label)) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { text.toIntOrNull()?.let { onSave(it) }; onDismiss() }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun LanguageDialog(currentCode: String, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_language)) },
        text = {
            Column {
                listOf(stringResource(R.string.lang_english) to "en", stringResource(R.string.lang_polish) to "pl").forEach { (name, code) ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onSelect(code) }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = code == currentCode, onClick = null)
                        Spacer(Modifier.width(8.dp))
                        Text(name)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun AboutDialog(version: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.about_app_title)) },
        text = {
            Column {
                Text(stringResource(R.string.about_app_text), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                Text("${stringResource(R.string.version)}: $version", fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.developer) + ": Expl00")
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.audio_engine_info), style = MaterialTheme.typography.labelSmall)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun TutorialDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.tutorial_title),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(top = 8.dp)
            ) {
                TutorialSection(
                    stringResource(R.string.tutorial_practice_title),
                    stringResource(R.string.tutorial_practice_content)
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )

                TutorialSection(
                    stringResource(R.string.tutorial_rules_title),
                    stringResource(R.string.tutorial_rules_content)
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )

                TutorialSection(
                    stringResource(R.string.tutorial_metronome_title),
                    stringResource(R.string.tutorial_metronome_content)
                )

                Spacer(modifier = Modifier.height(12.dp))

                TutorialSection(
                    stringResource(R.string.tutorial_tuner_title),
                    stringResource(R.string.tutorial_tuner_content)
                )

                Spacer(modifier = Modifier.height(12.dp))

                TutorialSection(
                    stringResource(R.string.tutorial_stats_title),
                    stringResource(R.string.tutorial_stats_content)
                )

                Spacer(modifier = Modifier.height(12.dp))

                TutorialSection(
                    stringResource(R.string.tutorial_tips_title),
                    stringResource(R.string.tutorial_tips_content)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close), color = MaterialTheme.colorScheme.primary)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun NotificationsSection() {
    val context = LocalContext.current
    var reminderEnabled by remember { mutableStateOf(NotificationHelper.isReminderEnabled(context)) }
    var reminderHour by remember { mutableIntStateOf(NotificationHelper.getReminderHour(context)) }
    var reminderMinute by remember { mutableIntStateOf(NotificationHelper.getReminderMinute(context)) }
    var goalNotificationsEnabled by remember { mutableStateOf(NotificationHelper.areGoalNotificationsEnabled(context)) }
    var showTimePickerDialog by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            reminderEnabled = true
            NotificationHelper.scheduleDailyReminder(context, reminderHour, reminderMinute)
        }
    }

    LaunchedEffect(Unit) {
        NotificationHelper.createNotificationChannels(context)
    }

    SettingsSectionTitle(stringResource(R.string.notifications_section))
    SettingsCard {
        SettingRow(
            icon = Icons.Default.Notifications,
            title = stringResource(R.string.practice_reminder),
            subtitle = if (reminderEnabled) {
                stringResource(R.string.reminder_time_fmt, String.format(java.util.Locale.getDefault(), "%02d:%02d", reminderHour, reminderMinute))
            } else {
                stringResource(R.string.reminder_disabled)
            }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (reminderEnabled) {
                    IconButton(onClick = { showTimePickerDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Switch(
                    checked = reminderEnabled,
                    onCheckedChange = onCheckedChange@{ enabled ->
                        if (enabled) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                if (!NotificationHelper.hasNotificationPermission(context)) {
                                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    return@onCheckedChange
                                }
                            }
                            reminderEnabled = true
                            NotificationHelper.scheduleDailyReminder(context, reminderHour, reminderMinute)
                        } else {
                            reminderEnabled = false
                            NotificationHelper.cancelDailyReminder(context)
                        }
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

        SettingRow(
            icon = Icons.Default.EmojiEvents,
            title = stringResource(R.string.goal_notifications),
            subtitle = stringResource(R.string.goal_notifications_desc)
        ) {
            Switch(
                checked = goalNotificationsEnabled,
                onCheckedChange = { enabled ->
                    goalNotificationsEnabled = enabled
                    NotificationHelper.setGoalNotificationsEnabled(context, enabled)
                },
                colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
            )
        }
    }

    if (showTimePickerDialog) {
        TimePickerDialog(
            initialHour = reminderHour,
            initialMinute = reminderMinute,
            onDismiss = { showTimePickerDialog = false },
            onConfirm = { hour, minute ->
                reminderHour = hour
                reminderMinute = minute
                if (reminderEnabled) {
                    NotificationHelper.scheduleDailyReminder(context, hour, minute)
                }
                showTimePickerDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.set_reminder_time)) },
        text = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(state = timePickerState)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(timePickerState.hour, timePickerState.minute) }) {
                Text(stringResource(R.string.save), color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun TutorialSection(title: String, content: String) {
    Column {
        Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = content, fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}