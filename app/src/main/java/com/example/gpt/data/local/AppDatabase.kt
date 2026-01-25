package com.example.gpt.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.gpt.data.local.dao.AchievementDao
import com.example.gpt.data.local.dao.SessionDao
import com.example.gpt.data.local.entity.Achievement
import com.example.gpt.data.local.entity.PracticeSession

@Database(
    entities = [PracticeSession::class, Achievement::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun achievementDao(): AchievementDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sessions ADD COLUMN rhythmMargin REAL NOT NULL DEFAULT 0.3")
            }
        }
    }
}