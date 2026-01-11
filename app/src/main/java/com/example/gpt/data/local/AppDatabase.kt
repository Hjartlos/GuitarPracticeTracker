package com.example.gpt.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.gpt.data.local.dao.AchievementDao
import com.example.gpt.data.local.dao.SessionDao
import com.example.gpt.data.local.entity.Achievement
import com.example.gpt.data.local.entity.PracticeSession

@Database(
    entities = [PracticeSession::class, Achievement::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun achievementDao(): AchievementDao
}