package com.example.gpt.ui

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gpt.theme.GPTTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import dagger.hilt.android.AndroidEntryPoint
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: SessionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            GPTTheme(darkTheme = isDarkMode) {
                MainScreen(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(viewModel: SessionViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    val micPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    LaunchedEffect(Unit) {
        if (!micPermission.status.isGranted) {
            micPermission.launchPermissionRequest()
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF1A1A1A)) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.PlayArrow, "Practice", tint = Color.White) },
                    label = { Text("Practice", color = Color.White) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Insights, "Stats", tint = Color.White) },
                    label = { Text("Stats", color = Color.White) }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Settings, "Settings", tint = Color.White) },
                    label = { Text("Settings", color = Color.White) }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Brush.verticalGradient(listOf(Color(0xFF0D0D0D), Color(0xFF1A1A1A))))
        ) {
            when (selectedTab) {
                0 -> PracticeScreen(viewModel)
                1 -> StatisticsScreen(viewModel)
                2 -> SettingsScreen(viewModel)
            }
        }
    }
}

@Composable
fun PracticeScreen(viewModel: SessionViewModel) {
    val isSessionActive by viewModel.isSessionActive.collectAsState()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()
    val exerciseType by viewModel.exerciseType.collectAsState()
    val tuning by viewModel.tuning.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val tunerData by viewModel.tunerResult.collectAsState()
    val weeklyProgress by viewModel.weeklyProgress.collectAsState()
    val weeklyGoal by viewModel.weeklyGoalHours.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        WeeklyGoalCard(progress = weeklyProgress, goalHours = weeklyGoal)
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = formatTime(elapsedSeconds),
            fontSize = 64.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSessionActive) Color(0xFFE53935) else Color.Gray
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { viewModel.toggleSession() },
            modifier = Modifier.size(120.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isSessionActive) Color(0xFFB71C1C) else Color(0xFFE53935)
            ),
            shape = RoundedCornerShape(60.dp)
        ) {
            Icon(
                imageVector = if (isSessionActive) Icons.Default.Close else Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (isSessionActive && tunerData != null) {
            TunerGauge(tunerResult = tunerData!!)
        } else {
            Text("Press Start to activate tuner", color = Color.Gray, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        ExerciseTypeSelector(exerciseType) { viewModel.updateExerciseType(it) }
        Spacer(modifier = Modifier.height(12.dp))
        TuningSelector(tuning) { viewModel.updateTuning(it) }
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = notes,
            onValueChange = { viewModel.updateNotes(it) },
            label = { Text("Notes", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFFE53935),
                unfocusedBorderColor = Color.DarkGray
            )
        )
    }
}

@Composable
fun WeeklyGoalCard(progress: Float, goalHours: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF252525))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Weekly Goal", color = Color.White, fontWeight = FontWeight.Bold)
                Text("${(progress * goalHours).toInt()}h / ${goalHours}h", color = Color(0xFFE53935))
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = Color(0xFFE53935),
                trackColor = Color(0xFF3A3A3A)
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
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text("Exercise Type", color = Color.Gray) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFFE53935),
                unfocusedBorderColor = Color.DarkGray
            )
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            types.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type, color = Color.White) },
                    onClick = {
                        onSelect(type)
                        expanded = false
                    }
                )
            }
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
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text("Tuning", color = Color.Gray) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFFE53935),
                unfocusedBorderColor = Color.DarkGray
            )
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            tunings.forEach { tuning ->
                DropdownMenuItem(
                    text = { Text(tuning, color = Color.White) },
                    onClick = {
                        onSelect(tuning)
                        expanded = false
                    }
                )
            }
        }
    }
}

fun formatTime(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%02d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
