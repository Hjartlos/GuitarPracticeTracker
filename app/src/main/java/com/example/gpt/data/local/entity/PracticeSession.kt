package com.example.gpt.data.local.entity

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
    val avgBpm: Int = 0,
    val consistencyScore: Int = 0,
    val timeSignature: String = "4/4",
    val audioPath: String? = null
)