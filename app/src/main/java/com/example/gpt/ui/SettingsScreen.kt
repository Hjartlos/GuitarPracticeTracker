package com.example.gpt.ui

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import java.io.File

@Composable
fun SettingsScreen(viewModel: SessionViewModel) {
    val context = LocalContext.current
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val weeklyGoal by viewModel.weeklyGoalHours.collectAsState()
    var showGoalDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text("Settings", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("Customize your experience", color = Color.Gray, fontSize = 14.sp)

        Spacer(modifier = Modifier.height(24.dp))

        SettingRow(
            icon = Icons.Default.DarkMode,
            title = "Dark Mode",
            subtitle = "Metal-themed dark interface"
        ) {
            Switch(
                checked = isDarkMode,
                onCheckedChange = { viewModel.setDarkMode(it) },
                colors = SwitchDefaults.colors(
                    checkedTrackColor = Color(0xFFE53935),
                    checkedThumbColor = Color.White
                )
            )
        }

        HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))

        SettingRow(
            icon = Icons.Default.Flag,
            title = "Weekly Goal",
            subtitle = "$weeklyGoal hours per week"
        ) {
            IconButton(onClick = { showGoalDialog = true }) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFFE53935))
            }
        }

        HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))

        SettingRow(
            icon = Icons.Default.Download,
            title = "Export Data",
            subtitle = "Download practice history as CSV"
        ) {
            IconButton(onClick = {
                val csv = viewModel.exportToCSV(context)
                val file = File(context.cacheDir, "guitar_practice_${System.currentTimeMillis()}.csv")
                file.writeText(csv)

                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Export CSV"))
            }) {
                Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFFE53935))
            }
        }
    }

    if (showGoalDialog) {
        var newGoal by remember { mutableStateOf(weeklyGoal.toString()) }

        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = { Text("Set Weekly Goal", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = newGoal,
                    onValueChange = { newGoal = it.filter { c -> c.isDigit() } },
                    label = { Text("Hours per week", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFE53935)
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    newGoal.toIntOrNull()?.let { viewModel.setWeeklyGoal(it) }
                    showGoalDialog = false
                }) {
                    Text("Save", color = Color(0xFFE53935))
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoalDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E1E1E)
        )
    }
}

@Composable
fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(subtitle, color = Color.Gray, fontSize = 12.sp)
            }
        }
        trailing()
    }
}
