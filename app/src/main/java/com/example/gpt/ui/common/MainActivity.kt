package com.example.gpt.ui.common

import android.Manifest
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.gpt.R
import com.example.gpt.ui.onboarding.OnboardingScreen
import com.example.gpt.ui.practice.PracticeScreen
import com.example.gpt.ui.practice.PracticeViewModel
import com.example.gpt.ui.settings.SettingsScreen
import com.example.gpt.ui.splash.SplashScreen
import com.example.gpt.ui.statistics.StatisticsScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gpt.ui.tuner.TunerScreen
import com.example.gpt.theme.GPTTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import dagger.hilt.android.AndroidEntryPoint

private const val PREF_ONBOARDING_COMPLETED = "onboarding_completed"
private const val PREF_SPLASH_SHOWN_SESSION = "splash_shown_session"

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val viewModel: PracticeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val splashShown = savedInstanceState?.getBoolean(PREF_SPLASH_SHOWN_SESSION, false) ?: false

        lifecycle.addObserver(viewModel)

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            GPTTheme(darkTheme = isDarkMode) {
                AppContent(viewModel, splashAlreadyShown = splashShown)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(PREF_SPLASH_SHOWN_SESSION, true)
    }
}

enum class AppScreen {
    SPLASH,
    ONBOARDING,
    MAIN
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AppContent(viewModel: PracticeViewModel, splashAlreadyShown: Boolean = false) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE) }

    val micPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    LaunchedEffect(Unit) {
        if (!micPermission.status.isGranted) {
            micPermission.launchPermissionRequest()
        }
    }

    val onboardingCompleted = remember {
        prefs.getBoolean(PREF_ONBOARDING_COMPLETED, false)
    }

    var currentScreen by rememberSaveable {
        val initialScreen = when {
            splashAlreadyShown -> if (onboardingCompleted) AppScreen.MAIN else AppScreen.ONBOARDING
            else -> AppScreen.SPLASH
        }
        mutableStateOf(initialScreen)
    }

    when (currentScreen) {
        AppScreen.SPLASH -> {
            SplashScreen(
                onSplashFinished = {
                    currentScreen = if (onboardingCompleted) {
                        AppScreen.MAIN
                    } else {
                        AppScreen.ONBOARDING
                    }
                }
            )
        }

        AppScreen.ONBOARDING -> {
            OnboardingScreen(
                viewModel = viewModel,
                onFinished = {
                    prefs.edit().putBoolean("onboarding_completed", true).apply()
                    currentScreen = AppScreen.MAIN
                }
            )
        }

        AppScreen.MAIN -> {
            MainScreen(viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: PracticeViewModel) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val newlyUnlockedAchievement by viewModel.newlyUnlockedAchievement.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_practice)) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.MusicNote, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_tuner)) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.Insights, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_stats)) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_settings)) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        ) { padding ->
            GuitarPatternBackground(
                isDarkMode = isDarkMode,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                AnimatedContent(
                    targetState = selectedTab,
                    label = "TabTransition",
                    transitionSpec = {
                        fadeIn(animationSpec = tween(150)) togetherWith
                                fadeOut(animationSpec = tween(100))
                    }
                ) { targetTab ->
                    when (targetTab) {
                        0 -> PracticeScreen(viewModel)
                        1 -> TunerScreen(viewModel)
                        2 -> StatisticsScreen(viewModel)
                        3 -> SettingsScreen(viewModel)
                    }
                }
            }
        }

        newlyUnlockedAchievement?.let { achievementType ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 48.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                com.example.gpt.ui.achievements.AchievementUnlockedToast(
                    achievementType = achievementType,
                    onDismiss = { viewModel.dismissAchievementToast() }
                )
            }
        }
    }
}

@Composable
fun GuitarPatternBackground(
    isDarkMode: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val patternColor = MaterialTheme.colorScheme.onBackground
    val gradientColors = listOf(
        MaterialTheme.colorScheme.background,
        MaterialTheme.colorScheme.surface
    )

    val density = LocalDensity.current
    val pickPath = remember(density) {
        val size = with(density) { 40.dp.toPx() }
        val scale = size / 53f
        val centerX = size / 2
        val centerY = size / 2

        Path().apply {
            moveTo(centerX, centerY + 26.5f * scale)
            cubicTo(centerX + 5f * scale, centerY + 26.5f * scale, centerX + 10f * scale, centerY + 21.5f * scale, centerX + 14f * scale, centerY + 13.5f * scale)
            cubicTo(centerX + 19f * scale, centerY + 2.5f * scale, centerX + 23f * scale, centerY - 8.5f * scale, centerX + 23f * scale, centerY - 13.5f * scale)
            cubicTo(centerX + 23f * scale, centerY - 18.5f * scale, centerX + 20f * scale, centerY - 21.5f * scale, centerX + 15f * scale, centerY - 23.5f * scale)
            cubicTo(centerX + 8f * scale, centerY - 25.5f * scale, centerX - 8f * scale, centerY - 25.5f * scale, centerX - 15f * scale, centerY - 23.5f * scale)
            cubicTo(centerX - 20f * scale, centerY - 21.5f * scale, centerX - 23f * scale, centerY - 18.5f * scale, centerX - 23f * scale, centerY - 13.5f * scale)
            cubicTo(centerX - 23f * scale, centerY - 8.5f * scale, centerX - 19f * scale, centerY + 2.5f * scale, centerX - 14f * scale, centerY + 13.5f * scale)
            cubicTo(centerX - 10f * scale, centerY + 21.5f * scale, centerX - 5f * scale, centerY + 26.5f * scale, centerX, centerY + 26.5f * scale)
            close()
        }
    }

    Box(
        modifier = modifier.background(Brush.verticalGradient(gradientColors))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val spacingX = 70.dp.toPx()
            val spacingY = 80.dp.toPx()
            val verticalDensity = 0.7f
            val effectiveRowHeight = spacingY * verticalDensity
            val rows = (size.height / effectiveRowHeight).toInt() + 2
            val cols = (size.width / spacingX).toInt() + 2
            val pickSize = 36.dp.toPx()

            val patternAlpha = if (isDarkMode) 0.015f else 0.05f

            for (row in 0..rows) {
                for (col in 0..cols) {
                    val offsetX = col * spacingX + if (row % 2 == 1) spacingX / 2 else 0f
                    val offsetY = row * effectiveRowHeight
                    val rotationPattern = (row + col) % 4
                    val rotation = when(rotationPattern) {
                        0 -> 180f
                        1 -> 15f
                        2 -> 0f
                        else -> -15f
                    }

                    withTransform({
                        translate(left = offsetX - (spacingX/2), top = offsetY - (spacingY/2))
                        rotate(degrees = rotation, pivot = Offset(pickSize/2, pickSize/2))
                    }) {
                        drawPath(
                            path = pickPath,
                            color = patternColor.copy(alpha = patternAlpha),
                            style = Stroke(width = 1.5f)
                        )
                    }
                }
            }
        }
        content()
    }
}