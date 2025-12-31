package com.example.gpt.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class PracticeSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val durationSeconds: Long,
    val exerciseType: String,
    val timestamp: Long,
    val notes: String = "",
    val tuning: String = "Standard",

    // NOWE POLA DO ANALIZY:
    val avgBpm: Int = 0,            // Wykryte tempo
    val consistencyScore: Int = 0   // Jak równo grałeś (0-100%)
)