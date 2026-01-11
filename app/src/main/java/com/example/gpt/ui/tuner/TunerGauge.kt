package com.example.gpt.ui.tuner

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gpt.R
import com.example.gpt.core.audio.TunerResult
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private const val GAUGE_START_ANGLE = 180f
private const val GAUGE_SWEEP_ANGLE = 180f
private const val NEEDLE_CENTER_ANGLE = 270f
private const val CENTS_TO_ANGLE_FACTOR = 1.8f
private const val NEEDLE_STROKE_WIDTH = 8f
private const val ARC_STROKE_WIDTH = 25f
private const val CENTER_DOT_RADIUS = 12f

@Composable
fun TunerGauge(
    tunerResult: TunerResult,
    useFlats: Boolean,
    modifier: Modifier = Modifier
) {
    val cents = tunerResult.cents.coerceIn(-50, 50)
    val isInTune = cents in -5..5
    val isDark = isSystemInDarkTheme()
    val contentColor = MaterialTheme.colorScheme.onSurface
    val tickColor = MaterialTheme.colorScheme.onSurfaceVariant

    val animatedCents by animateFloatAsState(targetValue = cents.toFloat(), label = "CentsAnimation")

    val targetNeedleColor = when {
        isInTune -> Color(0xFF00C853)
        kotlin.math.abs(cents) < 15 -> Color(0xFFFFD600)
        else -> Color(0xFFD50000)
    }

    val animatedNeedleColor by animateColorAsState(targetValue = targetNeedleColor, label = "NeedleColorAnimation")

    val arcBrush = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFFD50000),
            Color(0xFFFFD600),
            Color(0xFF00C853),
            Color(0xFFFFD600),
            Color(0xFFD50000)
        )
    )

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth(0.85f).height(200.dp)) {
            val w = size.width
            val h = size.height

            val radius = min(w / 2, h) - 10f

            val centerX = w / 2
            val centerY = h * 0.85f

            drawArc(
                brush = arcBrush,
                startAngle = GAUGE_START_ANGLE,
                sweepAngle = GAUGE_SWEEP_ANGLE,
                useCenter = false,
                topLeft = Offset(centerX - radius, centerY - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = ARC_STROKE_WIDTH, cap = StrokeCap.Round)
            )

            for (i in -50..50 step 10) {
                val isMainTick = i == 0 || i == -50 || i == 50
                val tickAngle = NEEDLE_CENTER_ANGLE + (i * CENTS_TO_ANGLE_FACTOR)
                val tickRad = Math.toRadians(tickAngle.toDouble())

                val startOffset = if (isMainTick) 35f else 20f

                val innerRadius = radius - startOffset - (ARC_STROKE_WIDTH / 2)
                val outerRadius = radius - (ARC_STROKE_WIDTH / 2) + 5f

                val startX = centerX + innerRadius * cos(tickRad).toFloat()
                val startY = centerY + innerRadius * sin(tickRad).toFloat()

                val endX = centerX + outerRadius * cos(tickRad).toFloat()
                val endY = centerY + outerRadius * sin(tickRad).toFloat()
                drawLine(
                    color = tickColor.copy(alpha = if (isMainTick) 0.8f else 0.4f),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = if (isMainTick) 3f else 2f
                )

                if (isMainTick) {
                    val textRadius = innerRadius - 25f
                    val textX = centerX + textRadius * cos(tickRad).toFloat()
                    val textY = centerY + textRadius * sin(tickRad).toFloat()

                    drawContext.canvas.nativeCanvas.drawText(
                        if (i > 0) "+$i" else "$i",
                        textX,
                        textY + 10f,
                        android.graphics.Paint().apply {
                            color = tickColor.toArgb()
                            alpha = 180
                            textSize = 28f
                            textAlign = android.graphics.Paint.Align.CENTER
                            isFakeBoldText = true
                        }
                    )
                }
            }

            val angle = NEEDLE_CENTER_ANGLE + (animatedCents * CENTS_TO_ANGLE_FACTOR)
            val angleRad = Math.toRadians(angle.toDouble())
            val needleLen = radius - 10f
            val endX = centerX + needleLen * cos(angleRad).toFloat()
            val endY = centerY + needleLen * sin(angleRad).toFloat()

            drawLine(
                color = animatedNeedleColor,
                start = Offset(centerX, centerY),
                end = Offset(endX, endY),
                strokeWidth = NEEDLE_STROKE_WIDTH,
                cap = StrokeCap.Round
            )

            drawCircle(color = contentColor, radius = CENTER_DOT_RADIUS, center = Offset(centerX, centerY))
            drawCircle(color = animatedNeedleColor, radius = CENTER_DOT_RADIUS * 0.6f, center = Offset(centerX, centerY))
        }

        val infiniteTransition = rememberInfiniteTransition(label = "inTunePulse")
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.8f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.BottomCenter)) {
            val displayNote = if (useFlats) TuningData.convertSharpToFlat(tunerResult.note) else tunerResult.note

            if (tunerResult.isLocked && tunerResult.frequency > 0) {
                Text(
                    text = String.format(java.util.Locale.US, "%.1f Hz", tunerResult.frequency),
                    fontSize = 14.sp,
                    color = if (isInTune) animatedNeedleColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = "$displayNote${tunerResult.octave ?: ""}",
                fontSize = 64.sp,
                color = if (isInTune) animatedNeedleColor else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Black
            )
            if (tunerResult.isLocked) {
                val statusText = if (isInTune) stringResource(R.string.tuner_perfect)
                else if (tunerResult.cents < 0) stringResource(R.string.tuner_too_low)
                else stringResource(R.string.tuner_too_high)
                val centsDisplay = if (!isInTune) {
                    val sign = if (tunerResult.cents > 0) "+" else ""
                    val centsWord = if (kotlin.math.abs(tunerResult.cents) == 1) "cent" else "cents"
                    " ($sign${tunerResult.cents} $centsWord)"
                } else ""

                Text(
                    text = statusText + centsDisplay,
                    color = if (isInTune) animatedNeedleColor.copy(alpha = pulseAlpha) else animatedNeedleColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    letterSpacing = 2.sp
                )
            } else {
                Text(
                    text = stringResource(R.string.tuner_listening),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.3f),
                    fontSize = 12.sp
                )
            }
        }
    }
}