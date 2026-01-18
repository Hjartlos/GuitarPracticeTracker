package com.example.gpt.ui.onboarding

import androidx.compose.ui.BiasAlignment
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.gpt.R
import com.example.gpt.ui.practice.PracticeViewModel
import kotlinx.coroutines.launch
import kotlin.math.sqrt

data class OnboardingPage(
    val titleRes: Int,
    val descriptionRes: Int,
    val icon: ImageVector,
    val accentColor: Color,
    val isCalibrationPage: Boolean = false
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    viewModel: PracticeViewModel,
    onFinished: () -> Unit
) {
    val pages = listOf(
        OnboardingPage(
            titleRes = R.string.onboarding_welcome_title,
            descriptionRes = R.string.onboarding_welcome_desc,
            icon = Icons.Default.MusicNote,
            accentColor = MaterialTheme.colorScheme.primary
        ),
        OnboardingPage(
            titleRes = R.string.onboarding_practice_title,
            descriptionRes = R.string.onboarding_practice_desc,
            icon = Icons.Default.PlayArrow,
            accentColor = MaterialTheme.colorScheme.tertiary
        ),
        OnboardingPage(
            titleRes = R.string.onboarding_metronome_title,
            descriptionRes = R.string.onboarding_metronome_desc,
            icon = Icons.Default.Timer,
            accentColor = MaterialTheme.colorScheme.secondary
        ),
        OnboardingPage(
            titleRes = R.string.onboarding_tuner_title,
            descriptionRes = R.string.onboarding_tuner_desc,
            icon = Icons.Default.Tune,
            accentColor = MaterialTheme.colorScheme.tertiary
        ),
        OnboardingPage(
            titleRes = R.string.onboarding_stats_title,
            descriptionRes = R.string.onboarding_stats_desc,
            icon = Icons.Default.Insights,
            accentColor = MaterialTheme.colorScheme.primary
        ),
        OnboardingPage(
            titleRes = R.string.audio_calibration,
            descriptionRes = R.string.onboarding_calib_desc,
            icon = Icons.Default.SettingsInputComponent,
            accentColor = MaterialTheme.colorScheme.secondary,
            isCalibrationPage = true
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    val isCalibrationPageActive = pagerState.currentPage == pages.lastIndex
    LaunchedEffect(isCalibrationPageActive) {
        if (isCalibrationPageActive) {
            viewModel.startMonitoring()
        } else {
            viewModel.stopMonitoring()
            viewModel.stopTestMetronome()
            viewModel.stopAudioMonitoring()
            if (viewModel.isTapCalibrating.value) viewModel.cancelTapCalibration()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                AnimatedVisibility(
                    visible = pagerState.currentPage != pages.lastIndex,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    TextButton(onClick = onFinished) {
                        Text(
                            text = stringResource(R.string.skip),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                if (pagerState.currentPage == pages.lastIndex) {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = false
            ) { pageIndex ->
                val page = pages[pageIndex]
                if (page.isCalibrationPage) {
                    OnboardingCalibrationContent(
                        page = page,
                        viewModel = viewModel
                    )
                } else {
                    OnboardingPageContent(
                        page = page,
                        pageIndex = pageIndex
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pages.forEachIndexed { index, page ->
                        PageIndicator(
                            isSelected = pagerState.currentPage == index,
                            color = page.accentColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (pagerState.currentPage == pages.lastIndex) {
                        Button(
                            onClick = onFinished,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.get_started),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (pagerState.currentPage > 0) {
                                FloatingActionButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                        }
                                    },
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Spacer(modifier = Modifier.width(32.dp))
                            }
                            FloatingActionButton(
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                },
                                containerColor = pages[pagerState.currentPage].accentColor,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Next",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    pageIndex: Int
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pageAnim")
    val iconScale = infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Box(
            modifier = Modifier
                .size(160.dp * iconScale.value),
            contentAlignment = Alignment.Center
        ) {

            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            page.accentColor.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    ),
                    radius = size.minDimension / 2
                )
            }

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                page.accentColor.copy(alpha = 0.2f),
                                page.accentColor.copy(alpha = 0.05f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = page.accentColor
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = stringResource(page.titleRes),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(page.descriptionRes),
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}

@Composable
private fun OnboardingCalibrationContent(
    page: OnboardingPage,
    viewModel: PracticeViewModel
) {
    val threshold by viewModel.inputThreshold.collectAsState()
    val amplitude by viewModel.amplitude.collectAsState()
    val isLatencyTesting by viewModel.isLatencyTesting.collectAsState()
    val latencyResult by viewModel.latencyTestResult.collectAsState()
    val latencyOffset by viewModel.latencyOffset.collectAsState()
    val isTestRunning by viewModel.isTestMetronomeRunning.collectAsState()
    val isTapCalibrating by viewModel.isTapCalibrating.collectAsState()
    val tapBeat by viewModel.tapCalibrationBeat.collectAsState()
    val tapProgress by viewModel.tapCalibrationProgress.collectAsState()
    val isMonitoring by viewModel.isMonitoringEnabled.collectAsState()
    val metronomeOffset by viewModel.metronomeOffset.collectAsState()
    val isHapticEnabled by viewModel.isHapticEnabled.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startMonitoring()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.startMonitoring()
    }

    val animatedAmplitude by animateFloatAsState(
        targetValue = amplitude,
        label = "amp",
        animationSpec = tween(50, easing = LinearEasing)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(page.titleRes),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(page.descriptionRes),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                Text(
                    stringResource(R.string.input_check),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { viewModel.toggleTestMetronome() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if(isTestRunning) MaterialTheme.colorScheme.error else page.accentColor
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = if(isTestRunning) stringResource(R.string.stop_test) else stringResource(R.string.play_click),
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = { viewModel.toggleAudioMonitoring() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if(isMonitoring) MaterialTheme.colorScheme.error else page.accentColor.copy(alpha=0.2f),
                            contentColor = if(isMonitoring) Color.White else page.accentColor
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            imageVector = if(isMonitoring) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = "Monitor",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if(isMonitoring) stringResource(R.string.monitoring_stop) else stringResource(R.string.monitoring_listen),
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .background(Color.Black, RoundedCornerShape(4.dp))
                        .padding(2.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    val progress = (animatedAmplitude / 100f).coerceIn(0f, 1f)
                    val thresholdVisualPos = (sqrt(threshold) * 3f).coerceIn(0f, 1f)
                    val isGateOpen = progress > thresholdVisualPos
                    val thresholdBias = (thresholdVisualPos * 2f) - 1f

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (isGateOpen) Color(0xFF00C853)
                                else Color(0xFFD50000)
                            )
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(2.dp)
                            .align(BiasAlignment(thresholdBias, 0f))
                            .background(Color.White)
                    )

                    Text(
                        text = if (isGateOpen) stringResource(R.string.gate_open) else stringResource(R.string.gate_closed),
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 6.dp)
                    )
                }

                if (isMonitoring) {
                    Text(
                        text = stringResource(R.string.feedback_warning),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                val sliderPosition = 1f - (sqrt(threshold) * 3f).coerceIn(0f, 1f)
                val sensitivityPercent = if (sliderPosition > 0.96f) 100 else (sliderPosition * 100).toInt()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        stringResource(R.string.mic_sensitivity_label),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "$sensitivityPercent%",
                        fontSize = 12.sp,
                        color = page.accentColor
                    )
                }

                Slider(
                    value = sliderPosition,
                    onValueChange = { sliderValue ->
                        val invertedPos = 1f - sliderValue
                        val linear = (invertedPos / 3f)
                        val newThreshold = (linear * linear).coerceAtLeast(0.0001f)
                        viewModel.setInputThreshold(newThreshold)
                    },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = page.accentColor,
                        activeTrackColor = page.accentColor,
                        inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.2f)
                    )
                )
                Text(
                    stringResource(R.string.mic_sensitivity_expl),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 14.sp
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 16.dp))

                Text(
                    stringResource(R.string.latency_label),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (isTapCalibrating) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = tapProgress,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                repeat(5) { index ->
                                    val isActive = tapBeat >= (index + 1)
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(if (isActive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f))
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.calibration_hold_near),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = { viewModel.runLatencyAutoCalibration() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLatencyTesting,
                        colors = ButtonDefaults.buttonColors(containerColor = page.accentColor.copy(alpha=0.2f), contentColor = page.accentColor),
                        border = BorderStroke(1.dp, page.accentColor.copy(alpha=0.5f))
                    ) {
                        Text(if (isLatencyTesting) stringResource(R.string.calibrating) else stringResource(R.string.auto_calibrate_btn))
                    }
                }

                if (latencyResult != null && !isTapCalibrating) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = latencyResult!!,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    stringResource(R.string.manual_calib_title) + " (${latencyOffset}ms)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )

                Slider(
                    value = latencyOffset.toFloat(),
                    onValueChange = { viewModel.setLatencyOffset(it.toInt()) },
                    valueRange = 0f..300f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        activeTrackColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.2f)
                    )
                )
                Text(
                    stringResource(R.string.latency_expl),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 14.sp
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 16.dp))

                Text(
                    stringResource(R.string.metronome_sync_title),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.haptic_feedback),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Switch(
                        checked = isHapticEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.setHapticEnabled(enabled)
                            if (enabled) {
                                viewModel.triggerTestVibration()
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = page.accentColor,
                            checkedTrackColor = page.accentColor.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.scale(0.8f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.manual_offset),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${metronomeOffset}ms",
                        fontSize = 12.sp,
                        color = page.accentColor
                    )
                }

                Slider(
                    value = metronomeOffset.toFloat(),
                    onValueChange = {
                        viewModel.setMetronomeOffset(it.toInt())
                        viewModel.startSyncTest()
                    },
                    onValueChangeFinished = {
                        viewModel.stopSyncTest()
                    },
                    valueRange = 0f..400f,
                    colors = SliderDefaults.colors(
                        thumbColor = page.accentColor,
                        activeTrackColor = page.accentColor,
                        inactiveTrackColor = Color.White.copy(alpha=0.2f)
                    )
                )

                Text(
                    stringResource(R.string.metronome_sync_expl),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
private fun PageIndicator(
    isSelected: Boolean,
    color: Color
) {
    val width by animateDpAsState(
        targetValue = if (isSelected) 32.dp else 8.dp,
        animationSpec = tween(300),
        label = "indicatorWidth"
    )

    val indicatorColor by animateColorAsState(
        targetValue = if (isSelected) color else Color.White.copy(alpha = 0.3f),
        animationSpec = tween(300),
        label = "indicatorColor"
    )

    Box(
        modifier = Modifier
            .height(8.dp)
            .width(width)
            .clip(CircleShape)
            .background(indicatorColor)
    )
}