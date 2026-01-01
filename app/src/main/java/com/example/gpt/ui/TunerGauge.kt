package com.example.gpt.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gpt.audio.TunerResult
import kotlin.math.cos
import kotlin.math.sin

private const val GAUGE_START_ANGLE = 180f
private const val GAUGE_SWEEP_ANGLE = 180f
private const val NEEDLE_CENTER_ANGLE = 270f
private const val CENTS_TO_ANGLE_FACTOR = 1.4f
private const val NEEDLE_STROKE_WIDTH = 10f
private const val ARC_STROKE_WIDTH = 20f
private const val CENTER_DOT_RADIUS = 15f
private const val GAUGE_RADIUS_OFFSET = 40f

@Composable
fun TunerGauge(tunerResult: TunerResult, modifier: Modifier = Modifier) {
    val cents = tunerResult.cents.coerceIn(-50, 50)
    val isInTune = cents in -5..5

    val animatedCents by animateFloatAsState(targetValue = cents.toFloat(), label = "CentsAnimation")
    val animatedNeedleColor by animateColorAsState(targetValue = if (isInTune) Color.Green else Color.Red, label = "NeedleColorAnimation")
    val arcColor = Color.DarkGray

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth(0.8f).height(200.dp)) {
            val w = size.width
            val h = size.height
            val centerX = w / 2
            val centerY = h

            drawArc(color = arcColor, startAngle = GAUGE_START_ANGLE, sweepAngle = GAUGE_SWEEP_ANGLE, useCenter = false, style = Stroke(width = ARC_STROKE_WIDTH, cap = StrokeCap.Round))

            val angle = NEEDLE_CENTER_ANGLE + (animatedCents * CENTS_TO_ANGLE_FACTOR)
            val angleRad = Math.toRadians(angle.toDouble())
            val radius = w / 2 - GAUGE_RADIUS_OFFSET
            val endX = centerX + radius * cos(angleRad).toFloat()
            val endY = centerY + radius * sin(angleRad).toFloat()

            drawLine(color = animatedNeedleColor, start = Offset(centerX, centerY), end = Offset(endX, endY), strokeWidth = NEEDLE_STROKE_WIDTH, cap = StrokeCap.Round)
            drawCircle(color = Color.White, radius = CENTER_DOT_RADIUS, center = Offset(centerX, centerY))
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.BottomCenter)) {
            Text(
                text = "${tunerResult.note}${tunerResult.octave ?: ""}",
                fontSize = 50.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            if (tunerResult.isLocked) {
                Text(
                    text = if (tunerResult.cents in -5..5) "Perfect" else if (tunerResult.cents < 0) "Flat (b)" else "Sharp (#)",
                    color = if (tunerResult.cents in -5..5) Color.Green else Color.Red,
                    fontSize = 14.sp
                )
            }
        }
    }
}