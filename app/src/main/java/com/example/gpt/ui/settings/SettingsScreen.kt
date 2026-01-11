package com.example.gpt.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import java.io.File

@Composable
fun SettingsScreen(viewModel: PracticeViewModel) {
    val context = LocalContext.current
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

    var importResultMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val displayLanguage = when(currentLangCode) {
        "pl" -> "Polski"
        else -> "English"
    }

    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let {
            try {
                val csvData = viewModel.exportToCSV()
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(csvData.toByteArray())
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { inputStream ->
                    val csvContent = inputStream.bufferedReader().readText()
                    val result = viewModel.importFromCSV(csvContent)
                    result.fold(
                        onSuccess = { count ->
                            importResultMessage = context.getString(R.string.import_success, count)
                        },
                        onFailure = {
                            importResultMessage = context.getString(R.string.import_error)
                        }
                    )
                }
            } catch (e: Exception) {
                importResultMessage = context.getString(R.string.import_error)
            }
        }
    }

    LaunchedEffect(importResultMessage) {
        importResultMessage?.let {
            snackbarHostState.showSnackbar(it)
            importResultMessage = null
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
            Text(
                stringResource(R.string.practice_section),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingRow(icon = Icons.Default.Flag, title = stringResource(R.string.weekly_goal_label), subtitle = stringResource(R.string.hours_per_week_fmt, weeklyGoal)) {
                        IconButton(onClick = { showGoalDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Goal", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                stringResource(R.string.audio_section),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
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

                    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

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
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                stringResource(R.string.data_section),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingRow(icon = Icons.Default.Download, title = stringResource(R.string.export_data), subtitle = stringResource(R.string.export_desc)) {
                        Row {
                            IconButton(onClick = {
                                try {
                                    val csv = viewModel.exportToCSV()
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

                    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

                    SettingRow(icon = Icons.Default.Upload, title = stringResource(R.string.import_data), subtitle = stringResource(R.string.import_desc)) {
                        IconButton(onClick = { importFileLauncher.launch("text/*") }) {
                            Icon(Icons.Default.FileOpen, contentDescription = "Import", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            NotificationsSection()

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                stringResource(R.string.app_section),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                val isHapticEnabled by viewModel.isHapticEnabled.collectAsState()

                Column(modifier = Modifier.padding(16.dp)) {

                    SettingRow(icon = Icons.Default.SettingsCell, title = stringResource(R.string.haptic_feedback), subtitle = stringResource(R.string.haptic_feedback_desc)) {
                        Switch(
                            checked = isHapticEnabled,
                            onCheckedChange = { viewModel.setHapticEnabled(it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary, checkedThumbColor = MaterialTheme.colorScheme.onPrimary)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

                    SettingRow(icon = Icons.Default.DarkMode, title = stringResource(R.string.dark_mode), subtitle = if(isDarkMode) stringResource(R.string.dark_theme_desc) else stringResource(R.string.light_theme_desc)) {
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { viewModel.setDarkMode(it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary, checkedThumbColor = MaterialTheme.colorScheme.onPrimary)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

                    SettingRow(
                        icon = Icons.Default.Language,
                        title = stringResource(R.string.language),
                        subtitle = displayLanguage
                    ) {
                        IconButton(onClick = { showLanguageDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Change Language", tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

                    SettingRow(
                        icon = Icons.Default.HelpOutline,
                        title = stringResource(R.string.show_tutorial),
                        subtitle = stringResource(R.string.show_tutorial_desc)
                    ) {
                        IconButton(onClick = { showTutorialDialog = true }) {
                            Icon(Icons.Default.School, contentDescription = "Tutorial", tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

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
            }

            Spacer(modifier = Modifier.height(50.dp))
        }
    }


    if (showGoalDialog) {
        var newGoal by remember { mutableStateOf(weeklyGoal.toString()) }
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = { Text(stringResource(R.string.set_weekly_goal_title), color = MaterialTheme.colorScheme.onSurface) },
            text = {
                OutlinedTextField(
                    value = newGoal,
                    onValueChange = { input ->
                        if (input.isEmpty()) { newGoal = ""; return@OutlinedTextField }
                        val filtered = input.filter { it.isDigit() }
                        if (filtered.toIntOrNull() in 1..30) { newGoal = filtered }
                    },
                    label = { Text(stringResource(R.string.hours_label)) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface, focusedBorderColor = MaterialTheme.colorScheme.primary)
                )
            },
            confirmButton = { TextButton(onClick = { newGoal.toIntOrNull()?.let { viewModel.setWeeklyGoal(it) }; showGoalDialog = false }) { Text(stringResource(R.string.save), color = MaterialTheme.colorScheme.primary) } },
            dismissButton = { TextButton(onClick = { showGoalDialog = false }) { Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.6f)) } },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showTuningDialog) {
        TuningStandardsDialog(viewModel = viewModel, onDismiss = { showTuningDialog = false })
    }

    if (showCalibrationDialog) {
        CalibrationDialog(viewModel = viewModel, onDismiss = { showCalibrationDialog = false; viewModel.stopTestMetronome() })
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.select_language), color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column {

                    val englishName = stringResource(R.string.lang_english)
                    val polishName = stringResource(R.string.lang_polish)
                    val languages = listOf(englishName to "en", polishName to "pl")

                    languages.forEach { (langName, langCode) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (currentLangCode != langCode) {
                                        showLanguageDialog = false
                                        viewModel.setAppLanguage(langCode)
                                        LocaleHelper.setLocale(context, langCode)
                                    } else {
                                        showLanguageDialog = false
                                    }
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isSelected = (langCode == currentLangCode)
                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(langName, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text(stringResource(R.string.cancel)) }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text(stringResource(R.string.about_app_title), color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column {
                    Text(stringResource(R.string.about_app_text), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("${stringResource(R.string.developer)}: Expl00", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("${stringResource(R.string.version)}: $appVersion", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.audio_engine_info), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.5f))
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text(stringResource(R.string.close), color = MaterialTheme.colorScheme.primary) }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showTutorialDialog) {
        AlertDialog(
            onDismissRequest = { showTutorialDialog = false },
            title = { Text(stringResource(R.string.tutorial_title), color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 8.dp)
                ) {
                    TutorialSection(
                        title = stringResource(R.string.tutorial_practice_title),
                        content = stringResource(R.string.tutorial_practice_content)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    TutorialSection(
                        title = stringResource(R.string.tutorial_metronome_title),
                        content = stringResource(R.string.tutorial_metronome_content)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    TutorialSection(
                        title = stringResource(R.string.tutorial_tuner_title),
                        content = stringResource(R.string.tutorial_tuner_content)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    TutorialSection(
                        title = stringResource(R.string.tutorial_stats_title),
                        content = stringResource(R.string.tutorial_stats_content)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    TutorialSection(
                        title = stringResource(R.string.tutorial_tips_title),
                        content = stringResource(R.string.tutorial_tips_content)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showTutorialDialog = false }) {
                    Text(stringResource(R.string.close), color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
fun TuningStandardsDialog(viewModel: PracticeViewModel, onDismiss: () -> Unit) {
    val baseFreq by viewModel.baseFrequency.collectAsState()
    val useFlats by viewModel.useFlats.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tuning_standards), color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {

                Text(stringResource(R.string.reference_pitch_fmt), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${baseFreq} Hz", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }

                Slider(
                    value = baseFreq.toFloat(),
                    onValueChange = { viewModel.setBaseFrequency(it.toInt()) },
                    valueRange = 415f..466f,
                    steps = 50,
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("415Hz", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("466Hz", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.1f))
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(stringResource(R.string.note_notation), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            text = if (useFlats) stringResource(R.string.using_flats) else stringResource(R.string.using_sharps),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = useFlats,
                        onCheckedChange = { viewModel.setUseFlats(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary, checkedThumbColor = MaterialTheme.colorScheme.onPrimary)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.done), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun CalibrationDialog(viewModel: PracticeViewModel, onDismiss: () -> Unit) {
    val threshold by viewModel.inputThreshold.collectAsState()
    val margin by viewModel.rhythmMargin.collectAsState()
    val latencyOffset by viewModel.latencyOffset.collectAsState()
    val isTestRunning by viewModel.isTestMetronomeRunning.collectAsState()

    val isLatencyTesting by viewModel.isLatencyTesting.collectAsState()
    val latencyResult by viewModel.latencyTestResult.collectAsState()

    val isTapCalibrating by viewModel.isTapCalibrating.collectAsState()
    val tapBeat by viewModel.tapCalibrationBeat.collectAsState()
    val tapProgress by viewModel.tapCalibrationProgress.collectAsState()

    val amplitude by viewModel.amplitude.collectAsState()
    val animatedAmplitude by animateFloatAsState(targetValue = amplitude, label = "amp")

    LaunchedEffect(Unit) { viewModel.startMonitoring() }

    AlertDialog(
        onDismissRequest = {
            if (isTapCalibrating) viewModel.cancelTapCalibration()
            onDismiss()
        },
        title = { Text(stringResource(R.string.audio_calibration), color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {

                Text(stringResource(R.string.input_check), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                Text(stringResource(R.string.input_check_desc), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { viewModel.toggleTestMetronome() },
                        colors = ButtonDefaults.buttonColors(containerColor = if(isTestRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(if(isTestRunning) stringResource(R.string.stop_test) else stringResource(R.string.play_click), fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    val progress = (animatedAmplitude / 60f).coerceIn(0f, 1f)
                    Column(modifier = Modifier.weight(1f)) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)),
                            color = when {
                                progress > 0.5f -> MaterialTheme.colorScheme.primary
                                progress > 0.1f -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                val sliderThreshold = ((0.5f - threshold) / 0.45f).coerceIn(0f, 1f)
                val sensitivityPercent = (sliderThreshold * 100).toInt()
                Text(stringResource(R.string.mic_sensitivity_fmt, sensitivityPercent), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)

                Slider(
                    value = sliderThreshold,
                    onValueChange = { sliderValue ->
                        val newThreshold = 0.5f - (sliderValue * 0.45f)
                        viewModel.setInputThreshold(newThreshold)
                    },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.low_noise), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.high_sensitive), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(16.dp))

                Text(stringResource(R.string.latency_comp_fmt, latencyOffset), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)

                if (isTapCalibrating) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = tapProgress,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(8) { index ->
                                    val beatNumber = index + 1
                                    val isActive = tapBeat >= beatNumber
                                    val isCurrent = tapBeat == beatNumber

                                    Box(
                                        modifier = Modifier
                                            .size(if (isCurrent) 24.dp else 20.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isCurrent -> MaterialTheme.colorScheme.primary
                                                    isActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                                }
                                            )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { viewModel.registerCalibrationTap() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Text(
                                    text = stringResource(R.string.tap_here),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    Button(
                        onClick = { viewModel.startTapCalibration() },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        enabled = !isLatencyTesting,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Text(if (isLatencyTesting) stringResource(R.string.calibrating) else stringResource(R.string.tap_calibrate_btn))
                    }
                }

                if (latencyResult != null && !isTapCalibrating) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(latencyResult!!, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(stringResource(R.string.manual_offset), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = latencyOffset.toFloat(),
                    onValueChange = { viewModel.setLatencyOffset(it.toInt()) },
                    valueRange = 0f..300f,
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(16.dp))

                val sliderMargin = 1f - ((margin - 0.1f) / 0.4f)
                Text(stringResource(R.string.rhythm_strictness_fmt, (sliderMargin * 100).toInt()), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                Text(stringResource(R.string.rhythm_strictness_desc), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))

                Slider(
                    value = sliderMargin.coerceIn(0f, 1f),
                    onValueChange = { viewModel.setRhythmMargin(0.5f - (it * 0.4f)) },
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.strictness_normal), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.strictness_strict), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (isTapCalibrating) viewModel.cancelTapCalibration()
                onDismiss()
            }) {
                Text(stringResource(R.string.done), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
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
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    maxLines = 2,
                    lineHeight = 14.sp
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        trailing()
    }
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

    Text(
        stringResource(R.string.notifications_section),
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
    )
    Spacer(modifier = Modifier.height(8.dp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

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
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

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
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
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
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(state = timePickerState)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(timePickerState.hour, timePickerState.minute) }) {
                Text(stringResource(R.string.save), color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun TutorialSection(
    title: String,
    content: String
) {
    Column {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = content,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

