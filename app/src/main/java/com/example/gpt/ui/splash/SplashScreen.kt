package com.example.gpt.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Brush
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "metronomeNeedle")

    val angle by infiniteTransition.animateFloat(
        initialValue = -25f,
        targetValue = 25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "needleAngle"
    )

    LaunchedEffect(true) {
        delay(2500)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF121212), Color(0xFF2C2C2C))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.offset(y = (-50).dp)
        ) {
            Canvas(modifier = Modifier.size(300.dp)) {
                val scale = size.width / 108f

                withTransform({
                    scale(scale, scale, pivot = Offset.Zero)
                }) {
                    translate(top = 3f) {

                        val redOutlinePath = Path().apply {
                            moveTo(54f, 82f)
                            cubicTo(60f, 82f, 66f, 76f, 70f, 68f)
                            cubicTo(76f, 55f, 80f, 44f, 80f, 39f)
                            cubicTo(80f, 33f, 76f, 29f, 69f, 27f)
                            cubicTo(61f, 25f, 54f, 24f, 54f, 24f)
                            cubicTo(54f, 24f, 47f, 25f, 39f, 27f)
                            cubicTo(32f, 29f, 28f, 33f, 28f, 39f)
                            cubicTo(28f, 44f, 32f, 55f, 38f, 68f)
                            cubicTo(42f, 76f, 48f, 82f, 54f, 82f)
                            close()
                        }
                        drawPath(
                            path = redOutlinePath,
                            color = Color(0xFFE53935),
                            style = Stroke(width = 1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )

                        val whiteOutlinePath = Path().apply {
                            moveTo(54f, 80.5f)
                            cubicTo(59.5f, 80.5f, 65f, 75f, 69f, 67.5f)
                            cubicTo(74.5f, 55f, 78.5f, 44f, 78.5f, 39.5f)
                            cubicTo(78.5f, 34f, 75f, 30.5f, 68.5f, 28.5f)
                            cubicTo(61f, 26.5f, 54f, 25.5f, 54f, 25.5f)
                            cubicTo(54f, 25.5f, 47f, 26.5f, 39.5f, 28.5f)
                            cubicTo(33f, 30.5f, 29.5f, 34f, 29.5f, 39.5f)
                            cubicTo(29.5f, 44f, 33.5f, 55f, 39f, 67.5f)
                            cubicTo(43f, 75f, 48.5f, 80.5f, 54f, 80.5f)
                            close()
                        }
                        drawPath(
                            path = whiteOutlinePath,
                            color = Color.White,
                            style = Stroke(width = 1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )

                        val trianglePath = Path().apply {
                            moveTo(54f, 35f)
                            lineTo(66f, 65f)
                            lineTo(42f, 65f)
                            close()
                        }
                        drawPath(path = trianglePath, color = Color(0xFFE53935))

                        val highlightPath = Path().apply {
                            moveTo(54f, 40f)
                            lineTo(62f, 61f)
                            lineTo(46f, 61f)
                            close()
                        }
                        drawPath(path = highlightPath, color = Color.White.copy(alpha = 0.3f))

                        val pivotX = 54f
                        val pivotY = 58f

                        rotate(degrees = angle, pivot = Offset(pivotX, pivotY)) {

                            val stickLength = 15.23f

                            drawLine(
                                color = Color.White,
                                start = Offset(pivotX, pivotY),
                                end = Offset(pivotX, pivotY - stickLength),
                                strokeWidth = 2.5f,
                                cap = StrokeCap.Round
                            )

                            drawCircle(
                                color = Color.White,
                                radius = 3.5f,
                                center = Offset(pivotX, pivotY - stickLength)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "GUITAR",
                fontSize = 38.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 6.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "PRACTICE",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.8f),
                letterSpacing = 8.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "TRACKER",
                fontSize = 24.sp,
                fontWeight = FontWeight.Light,
                color = Color(0xFFE53935),
                letterSpacing = 10.sp
            )
        }
    }
}