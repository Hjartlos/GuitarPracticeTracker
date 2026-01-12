package com.example.gpt.ui.onboarding

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gpt.R
import com.example.gpt.ui.practice.PracticeViewModel
import kotlinx.coroutines.launch

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
            accentColor = Color(0xFFE53935)
        ),
        OnboardingPage(
            titleRes = R.string.onboarding_practice_title,
            descriptionRes = R.string.onboarding_practice_desc,
            icon = Icons.Default.PlayArrow,
            accentColor = Color(0xFF4CAF50)
        ),
        OnboardingPage(
            titleRes = R.string.onboarding_metronome_title,
            descriptionRes = R.string.onboarding_metronome_desc,
            icon = Icons.Default.Timer,
            accentColor = Color(0xFF2196F3)
        ),
        OnboardingPage(
            titleRes = R.string.onboarding_tuner_title,
            descriptionRes = R.string.onboarding_tuner_desc,
            icon = Icons.Default.Tune,
            accentColor = Color(0xFFFF9800)
        ),
        OnboardingPage(
            titleRes = R.string.onboarding_stats_title,
            descriptionRes = R.string.onboarding_stats_desc,
            icon = Icons.Default.Insights,
            accentColor = Color(0xFF9C27B0)
        ),
        OnboardingPage(
            titleRes = R.string.audio_calibration,
            descriptionRes = R.string.onboarding_calib_desc,
            icon = Icons.Default.SettingsInputComponent,
            accentColor = Color(0xFF00BCD4),
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
            if (viewModel.isTapCalibrating.value) viewModel.cancelTapCalibration()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D0D0D),
                        Color(0xFF1A1A1A)
                    )
                )
            )
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
                            color = Color.White.copy(alpha = 0.7f)
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
                                containerColor = Color(0xFFE53935)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.get_started),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
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
                                    containerColor = Color.White.copy(alpha = 0.1f),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color.White.copy(alpha = 0.7f)
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
                                    tint = Color.White
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
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(page.descriptionRes),
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.7f),
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

    val animatedAmplitude by animateFloatAsState(targetValue = amplitude, label = "amp")

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
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(page.descriptionRes),
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                Text(
                    stringResource(R.string.input_check),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
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

                    val progress = (animatedAmplitude / 100f).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = when {
                            progress > (threshold * 2) -> Color(0xFF4CAF50)
                            progress > 0.05f -> page.accentColor
                            else -> Color.Gray
                        },
                        trackColor = Color.White.copy(alpha = 0.1f),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                val sliderThreshold = (threshold / 0.2f).coerceIn(0f, 1f)
                val sensitivityPercent = ((1f - sliderThreshold) * 100).toInt()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        stringResource(R.string.mic_sensitivity_label),
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "$sensitivityPercent%",
                        fontSize = 12.sp,
                        color = page.accentColor
                    )
                }

                Slider(
                    value = sliderThreshold,
                    onValueChange = { sliderValue ->
                        val newThreshold = (sliderValue * 0.2f).coerceAtLeast(0.001f)
                        viewModel.setInputThreshold(newThreshold)
                    },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = page.accentColor,
                        activeTrackColor = page.accentColor,
                        inactiveTrackColor = Color.White.copy(alpha=0.2f)
                    )
                )
                Text(
                    stringResource(R.string.mic_sensitivity_expl),
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    lineHeight = 14.sp
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 16.dp))

                Text(
                    stringResource(R.string.latency_label),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (isTapCalibrating) {
                    Text(
                        text = tapProgress,
                        color = page.accentColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(8) { index ->
                            val isActive = tapBeat >= (index + 1)
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(if (isActive) page.accentColor else Color.White.copy(alpha = 0.2f))
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.calibration_hold_near),
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
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
                        color = Color(0xFF4CAF50),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    stringResource(R.string.manual_calib_title) + " (${latencyOffset}ms)",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Bold
                )

                Slider(
                    value = latencyOffset.toFloat(),
                    onValueChange = { viewModel.setLatencyOffset(it.toInt()) },
                    valueRange = 0f..300f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.Gray,
                        activeTrackColor = Color.Gray,
                        inactiveTrackColor = Color.White.copy(alpha=0.2f)
                    )
                )
                Text(
                    stringResource(R.string.latency_expl),
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.5f),
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