package com.example.gpt.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gpt.audio.TunerResult
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TunerGauge(
    tunerResult: TunerResult,
    modifier: Modifier = Modifier
) {
    val cents = tunerResult.cents.coerceIn(-50, 50) // Ograniczamy zakres
    val isInTune = cents in -5..5

    // Kolory inspirowane slajdami (Czerwony/Czarny)
    val needleColor = if (isInTune) Color.Green else Color.Red
    val arcColor = Color.DarkGray

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Canvas(modifier = Modifier
            .fillMaxWidth(0.8f)
            .height(200.dp)
        ) {
            val w = size.width
            val h = size.height
            val strokeWidth = 20f

            // 1. Rysowanie łuku (tło)
            drawArc(
                color = arcColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // 2. Obliczanie pozycji wskazówki
            // Kąt: -50 centów = 180 stopni, +50 centów = 360 (0) stopni. Środek = 270.
            // Mapujemy cents (-50..50) na kąt (200..340 stopni) żeby nie dotykało ziemi
            val angle = 270f + (cents * 1.4f) // 1.4 to mnożnik rozpiętości
            val angleRad = Math.toRadians(angle.toDouble())

            val radius = w / 2 - 40f
            val centerX = w / 2
            val centerY = h // Dół canvasu

            val endX = centerX + radius * cos(angleRad).toFloat()
            val endY = centerY + radius * sin(angleRad).toFloat()

            // 3. Rysowanie wskazówki
            drawLine(
                color = needleColor,
                start = Offset(centerX, centerY),
                end = Offset(endX, endY),
                strokeWidth = 10f,
                cap = StrokeCap.Round
            )

            // 4. Kropka u podstawy
            drawCircle(color = Color.White, radius = 15f, center = Offset(centerX, centerY))
        }

        // Tekst Nuty w środku
        Text(
            text = tunerResult.note,
            fontSize = 60.sp,
            color = Color.White,
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}