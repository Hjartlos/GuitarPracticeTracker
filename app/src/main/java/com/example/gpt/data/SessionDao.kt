package com.example.gpt.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert
    suspend fun insertSession(session: PracticeSession)

    // Pobiera całą historię (najnowsze na górze)
    @Query("SELECT * FROM sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<PracticeSession>>

    // Pobiera sumę minut z ostatnich X dni (do statystyk)
    @Query("SELECT * FROM sessions WHERE timestamp >= :startTime")
    fun getSessionsFromDate(startTime: Long): Flow<List<PracticeSession>>

    @Query("DELETE FROM sessions")
    suspend fun clearAll()
}