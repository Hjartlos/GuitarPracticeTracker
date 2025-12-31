package com.example.gpt.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gpt.data.PracticeSession
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StatisticsScreen(viewModel: SessionViewModel) {
    val sessions by viewModel.allSessions.collectAsState()

    // Obliczamy sumę godzin (do celu tygodniowego)
    val totalSeconds = sessions.sumOf { it.durationSeconds }
    val totalHours = totalSeconds / 3600f

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Nagłówek
        Text("Statistics", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("Your Progress This Week", color = Color.Gray, fontSize = 14.sp)

        Spacer(modifier = Modifier.height(20.dp))

        // 1. WYKRES
        Card(
            modifier = Modifier.fillMaxWidth().height(180.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            if (sessions.isNotEmpty()) {
                SessionGraph(sessions)
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No data to display", color = Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Session History", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))

        // 2. LISTA HISTORII Z BPM
        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sessions) { session ->
                SessionItem(session)
            }
        }
    }
}

@Composable
fun SessionItem(session: PracticeSession) {
    val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
    val dateStr = dateFormat.format(Date(session.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF252525))
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEWA STRONA: Typ i Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.exerciseType,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 16.sp
                )
                if (session.notes.isNotEmpty()) {
                    Text(
                        text = session.notes,
                        color = Color.Gray,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Wyświetlanie wyników analizy (jeśli są dostępne)
                if (session.avgBpm > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Badge(containerColor = Color(0xFFE53935)) {
                            Text("${session.avgBpm} BPM", color = Color.White, fontSize = 10.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Accuracy: ${session.consistencyScore}%",
                            color = if(session.consistencyScore > 80) Color.Green else Color.LightGray,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // PRAWA STRONA: Czas i Data
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${session.durationSeconds / 60} min",
                    color = Color(0xFFE53935), // Czerwony akcent
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = dateStr,
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )
            }
        }
    }
}

// ... (Funkcja SessionGraph zostaje taka sama jak wcześniej, lub wklej ją z poprzedniej odpowiedzi) ...
@Composable
fun SessionGraph(sessions: List<PracticeSession>) {
    val dataPoints = sessions.take(7).reversed().map { it.durationSeconds.toFloat() }
    val maxVal = dataPoints.maxOrNull()?.coerceAtLeast(1f) ?: 1f

    Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        val width = size.width
        val height = size.height
        val stepX = width / (dataPoints.size - 1).coerceAtLeast(1)

        val path = Path()

        dataPoints.forEachIndexed { index, value ->
            val x = index * stepX
            val y = height - (value / maxVal * height)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = Color(0xFFE53935),
            style = Stroke(width = 5f)
        )

        // Gradient pod wykresem
        val fillPath = Path().apply {
            addPath(path)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFE53935).copy(alpha = 0.3f), Color.Transparent)
            )
        )
    }
}