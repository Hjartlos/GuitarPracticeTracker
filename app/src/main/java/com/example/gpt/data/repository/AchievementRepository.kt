package com.example.gpt.data.repository

import com.example.gpt.data.local.dao.AchievementDao
import com.example.gpt.data.local.dao.SessionDao
import com.example.gpt.data.local.entity.Achievement
import com.example.gpt.data.local.entity.AchievementType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AchievementRepository @Inject constructor(
    private val achievementDao: AchievementDao,
    private val sessionDao: SessionDao
) {
    val allAchievements: Flow<List<Achievement>> = achievementDao.getAllAchievements()
    val unlockedAchievements: Flow<List<Achievement>> = achievementDao.getUnlockedAchievements()
    val unlockedCount: Flow<Int> = achievementDao.getUnlockedCount()
    val totalCount: Flow<Int> = achievementDao.getTotalCount()

    suspend fun initializeAchievements() {
        val existing = achievementDao.getAllAchievements().first()
        if (existing.isEmpty()) {
            val initialAchievements = AchievementType.entries.map { type ->
                Achievement(id = type.id, unlockedAt = null, progress = 0)
            }
            achievementDao.insertAll(initialAchievements)
        }
    }

    suspend fun checkAchievementsAfterSession(
        sessionDurationMinutes: Long,
        bpm: Int,
        exerciseType: String,
        isMetronomeSession: Boolean
    ): List<AchievementType> {
        val newlyUnlocked = mutableListOf<AchievementType>()
        val sessions = sessionDao.getAllSessions().first()
        val totalMinutes = sessions.sumOf { it.durationSeconds } / 60
        val totalHours = totalMinutes / 60

        if (sessions.size == 1) {
            if (unlockIfLocked(AchievementType.FIRST_SESSION)) {
                newlyUnlocked.add(AchievementType.FIRST_SESSION)
            }
        }

        if (totalHours >= 1) {
            if (unlockIfLocked(AchievementType.HOUR_TOTAL)) {
                newlyUnlocked.add(AchievementType.HOUR_TOTAL)
            }
        }
        if (totalHours >= 10) {
            if (unlockIfLocked(AchievementType.TEN_HOURS)) {
                newlyUnlocked.add(AchievementType.TEN_HOURS)
            }
        }
        if (totalHours >= 100) {
            if (unlockIfLocked(AchievementType.HUNDRED_HOURS)) {
                newlyUnlocked.add(AchievementType.HUNDRED_HOURS)
            }
        }

        if (bpm >= 200 && isMetronomeSession) {
            if (unlockIfLocked(AchievementType.SPEED_DEMON)) {
                newlyUnlocked.add(AchievementType.SPEED_DEMON)
            }
        }

        val metronomerSessions = sessions.count { it.avgBpm > 0 }
        updateProgress(AchievementType.METRONOME_MASTER, (metronomerSessions * 100) / 50)
        if (metronomerSessions >= 50) {
            if (unlockIfLocked(AchievementType.METRONOME_MASTER)) {
                newlyUnlocked.add(AchievementType.METRONOME_MASTER)
            }
        }

        val exerciseTypes = sessions.map { it.exerciseType }.distinct().size
        updateProgress(AchievementType.VARIETY_PLAYER, (exerciseTypes * 100) / 5)
        if (exerciseTypes >= 5) {
            if (unlockIfLocked(AchievementType.VARIETY_PLAYER)) {
                newlyUnlocked.add(AchievementType.VARIETY_PLAYER)
            }
        }

        val streakDays = calculateStreak(sessions.map { it.timestamp })
        updateProgress(AchievementType.WEEK_STREAK, (streakDays * 100) / 7)
        updateProgress(AchievementType.MONTH_STREAK, (streakDays * 100) / 30)

        if (streakDays >= 7) {
            if (unlockIfLocked(AchievementType.WEEK_STREAK)) {
                newlyUnlocked.add(AchievementType.WEEK_STREAK)
            }
        }
        if (streakDays >= 30) {
            if (unlockIfLocked(AchievementType.MONTH_STREAK)) {
                newlyUnlocked.add(AchievementType.MONTH_STREAK)
            }
        }

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (hour in 5..8) {
            val earlyBirdSessions = countSessionsInTimeRange(sessions.map { it.timestamp }, 5, 8)
            updateProgress(AchievementType.EARLY_BIRD, (earlyBirdSessions * 100) / 5)
            if (earlyBirdSessions >= 5) {
                if (unlockIfLocked(AchievementType.EARLY_BIRD)) {
                    newlyUnlocked.add(AchievementType.EARLY_BIRD)
                }
            }
        }
        if (hour in 22..23 || hour in 0..4) {
            val nightOwlSessions = countSessionsInTimeRange(sessions.map { it.timestamp }, 22, 4)
            updateProgress(AchievementType.NIGHT_OWL, (nightOwlSessions * 100) / 5)
            if (nightOwlSessions >= 5) {
                if (unlockIfLocked(AchievementType.NIGHT_OWL)) {
                    newlyUnlocked.add(AchievementType.NIGHT_OWL)
                }
            }
        }

        return newlyUnlocked
    }

    suspend fun checkGoalAchievements(): List<AchievementType> {
        val newlyUnlocked = mutableListOf<AchievementType>()

        val achievement = achievementDao.getAchievementById(AchievementType.GOAL_GETTER.id)
        val currentProgress = achievement?.progress ?: 0

        if (currentProgress == 0) {
            achievementDao.updateProgress(AchievementType.GOAL_GETTER.id, 100)
            if (unlockIfLocked(AchievementType.GOAL_GETTER)) {
                newlyUnlocked.add(AchievementType.GOAL_GETTER)
            }
        }

        return newlyUnlocked
    }

    suspend fun checkTunerAchievements(perfectTunes: Int): List<AchievementType> {
        val newlyUnlocked = mutableListOf<AchievementType>()

        updateProgress(AchievementType.PERFECT_PITCH, perfectTunes)
        if (perfectTunes >= 100) {
            if (unlockIfLocked(AchievementType.PERFECT_PITCH)) {
                newlyUnlocked.add(AchievementType.PERFECT_PITCH)
            }
        }

        return newlyUnlocked
    }

    private suspend fun unlockIfLocked(type: AchievementType): Boolean {
        val achievement = achievementDao.getAchievementById(type.id)
        if (achievement?.unlockedAt == null) {
            achievementDao.unlockAchievement(type.id, System.currentTimeMillis())
            return true
        }
        return false
    }

    private suspend fun updateProgress(type: AchievementType, progress: Int) {
        val clampedProgress = progress.coerceIn(0, 100)
        achievementDao.updateProgress(type.id, clampedProgress)
    }

    private fun calculateStreak(timestamps: List<Long>): Int {
        if (timestamps.isEmpty()) return 0

        val calendar = Calendar.getInstance()
        val daySet = timestamps.map { timestamp ->
            calendar.timeInMillis = timestamp
            "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.DAY_OF_YEAR)}"
        }.toSet()

        var streak = 0
        calendar.timeInMillis = System.currentTimeMillis()

        while (true) {
            val dayKey = "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.DAY_OF_YEAR)}"
            if (daySet.contains(dayKey)) {
                streak++
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }

        return streak
    }

    fun observeCurrentStreak(): Flow<Int> {
        return sessionDao.getAllSessions().map { sessions ->
            val timestamps = sessions.map { it.timestamp }
            calculateStreak(timestamps)
        }
    }

    private fun countSessionsInTimeRange(timestamps: List<Long>, startHour: Int, endHour: Int): Int {
        val calendar = Calendar.getInstance()
        return timestamps.count { timestamp ->
            calendar.timeInMillis = timestamp
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            if (startHour <= endHour) {
                hour in startHour..endHour
            } else {
                hour >= startHour || hour <= endHour
            }
        }
    }
}

