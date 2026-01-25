package com.example.gpt.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Delete
import androidx.room.Query
import com.example.gpt.data.local.entity.PracticeSession
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert
    suspend fun insertSession(session: PracticeSession)

    @Update
    suspend fun updateSession(session: PracticeSession)

    @Delete
    suspend fun deleteSession(session: PracticeSession)

    @Query("SELECT * FROM sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<PracticeSession>>

    @Query("SELECT * FROM sessions WHERE timestamp >= :startTime")
    fun getSessionsFromDate(startTime: Long): Flow<List<PracticeSession>>

    @Query("SELECT EXISTS(SELECT 1 FROM sessions WHERE timestamp = :timestamp AND durationSeconds = :duration AND exerciseType = :exerciseType)")
    suspend fun sessionExists(timestamp: Long, duration: Long, exerciseType: String): Boolean

    @Query("DELETE FROM sessions")
    suspend fun clearAll()
}