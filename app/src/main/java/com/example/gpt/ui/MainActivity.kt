package com.example.gpt.ui

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    var selectedTab by remember { mutableIntStateOf(0) }
    val micPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    LaunchedEffect(Unit) {
        if (!micPermission.status.isGranted) micPermission.launchPermissionRequest()
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Icon(Icons.Default.PlayArrow, "Practice") }, label = { Text("Practice") })
                NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Icon(Icons.Default.MusicNote, "Tuner") }, label = { Text("Tuner") })
                NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 }, icon = { Icon(Icons.Default.Insights, "Stats") }, label = { Text("Stats") })
                NavigationBarItem(selected = selectedTab == 3, onClick = { selectedTab = 3 }, icon = { Icon(Icons.Default.Settings, "Settings") }, label = { Text("Settings") })
            }
        }
    ) { padding ->
        val backgroundModifier = if (isDarkMode) {
            Modifier.background(Brush.verticalGradient(listOf(Color(0xFF0D0D0D), Color(0xFF1E1E1E))))
        } else {
            Modifier.background(MaterialTheme.colorScheme.background)
        }

        Box(modifier = Modifier.fillMaxSize().padding(padding).then(backgroundModifier)) {
            when (selectedTab) {
                0 -> PracticeScreen(viewModel)
                1 -> TunerScreen(viewModel)
                2 -> StatisticsScreen(viewModel)
                3 -> SettingsScreen(viewModel)
            }
        }
    }
}