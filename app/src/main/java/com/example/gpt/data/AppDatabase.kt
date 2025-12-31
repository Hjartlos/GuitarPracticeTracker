package com.example.gpt.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [PracticeSession::class], version = 1, exportSchema = false) // <--- DODAJ exportSchema = false
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
}