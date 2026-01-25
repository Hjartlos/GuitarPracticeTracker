package com.example.gpt.data.repository

import com.example.gpt.data.local.dao.AchievementDao
import com.example.gpt.data.local.dao.SessionDao
import com.example.gpt.data.local.entity.Achievement
import com.example.gpt.data.local.entity.AchievementType
import com.example.gpt.data.local.entity.PracticeSession
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

    private val MIN_SESSION_TIME_FOR_STATS = 300L
    private val MIN_METRONOME_TIME = 60L
    private val MIN_DAILY_TIME_FOR_STREAK = 600L

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
        isMetronomeSession: Boolean,
        consistencyScore: Int
    ): List<AchievementType> {
        val newlyUnlocked = mutableListOf<AchievementType>()
        val allSessions = sessionDao.getAllSessions().first()

        val totalMinutes = allSessions.sumOf { it.durationSeconds } / 60
        val totalHours = totalMinutes / 60

        if (allSessions.isNotEmpty()) {
            if (unlockIfLocked(AchievementType.FIRST_SESSION)) {
                newlyUnlocked.add(AchievementType.FIRST_SESSION)
            }
        }

        if (totalHours >= 1) if (unlockIfLocked(AchievementType.HOUR_TOTAL)) newlyUnlocked.add(AchievementType.HOUR_TOTAL)
        if (totalHours >= 10) if (unlockIfLocked(AchievementType.TEN_HOURS)) newlyUnlocked.add(AchievementType.TEN_HOURS)
        if (totalHours >= 100) if (unlockIfLocked(AchievementType.HUNDRED_HOURS)) newlyUnlocked.add(AchievementType.HUNDRED_HOURS)

        if (bpm >= 200 && isMetronomeSession && consistencyScore > 50) {
            if (unlockIfLocked(AchievementType.SPEED_DEMON)) {
                newlyUnlocked.add(AchievementType.SPEED_DEMON)
            }
        }

        val validMetronomeSessions = allSessions.count {
            it.avgBpm > 0 && it.durationSeconds >= MIN_METRONOME_TIME && it.consistencyScore > 0
        }
        updateProgress(AchievementType.METRONOME_MASTER, (validMetronomeSessions * 100) / 50)
        if (validMetronomeSessions >= 50) {
            if (unlockIfLocked(AchievementType.METRONOME_MASTER)) newlyUnlocked.add(AchievementType.METRONOME_MASTER)
        }

        val distinctTypes = allSessions
            .filter { it.durationSeconds >= MIN_SESSION_TIME_FOR_STATS && (it.consistencyScore > 0 || it.avgBpm == 0) }
            .map { it.exerciseType }
            .distinct()
            .size

        updateProgress(AchievementType.VARIETY_PLAYER, (distinctTypes * 100) / 5)
        if (distinctTypes >= 5) {
            if (unlockIfLocked(AchievementType.VARIETY_PLAYER)) newlyUnlocked.add(AchievementType.VARIETY_PLAYER)
        }

        val streakDays = calculateStreak(allSessions)
        updateProgress(AchievementType.WEEK_STREAK, (streakDays * 100) / 7)
        updateProgress(AchievementType.MONTH_STREAK, (streakDays * 100) / 30)

        if (streakDays >= 7) if (unlockIfLocked(AchievementType.WEEK_STREAK)) newlyUnlocked.add(AchievementType.WEEK_STREAK)
        if (streakDays >= 30) if (unlockIfLocked(AchievementType.MONTH_STREAK)) newlyUnlocked.add(AchievementType.MONTH_STREAK)

        val validTimeSessions = allSessions.filter {
            it.durationSeconds >= MIN_SESSION_TIME_FOR_STATS && (it.consistencyScore > 0 || it.avgBpm == 0)
        }

        val earlyBirdDays = countDistinctDaysInTimeRange(validTimeSessions, 5, 8)
        updateProgress(AchievementType.EARLY_BIRD, (earlyBirdDays * 100) / 5)
        if (earlyBirdDays >= 5) {
            if (unlockIfLocked(AchievementType.EARLY_BIRD)) newlyUnlocked.add(AchievementType.EARLY_BIRD)
        }

        val nightOwlDays = countDistinctDaysInTimeRange(validTimeSessions, 22, 4)
        updateProgress(AchievementType.NIGHT_OWL, (nightOwlDays * 100) / 5)
        if (nightOwlDays >= 5) {
            if (unlockIfLocked(AchievementType.NIGHT_OWL)) newlyUnlocked.add(AchievementType.NIGHT_OWL)
        }

        return newlyUnlocked
    }

    suspend fun checkGoalAchievements(isWeeklyGoalMet: Boolean): List<AchievementType> {
        val newlyUnlocked = mutableListOf<AchievementType>()

        if (isWeeklyGoalMet) {
            updateProgress(AchievementType.GOAL_GETTER, 100)
            if (unlockIfLocked(AchievementType.GOAL_GETTER)) {
                newlyUnlocked.add(AchievementType.GOAL_GETTER)
            }

            val currentCrusher = achievementDao.getAchievementById(AchievementType.GOAL_CRUSHER.id)?.progress ?: 0
            if (currentCrusher < 100) {
                updateProgress(AchievementType.GOAL_CRUSHER, currentCrusher + 10)
            }
            if (currentCrusher + 10 >= 100) {
                if (unlockIfLocked(AchievementType.GOAL_CRUSHER)) {
                    newlyUnlocked.add(AchievementType.GOAL_CRUSHER)
                }
            }
        }
        return newlyUnlocked
    }

    suspend fun checkTunerAchievements(perfectTunes: Int): List<AchievementType> {
        val newlyUnlocked = mutableListOf<AchievementType>()
        updateProgress(AchievementType.PERFECT_PITCH, perfectTunes)
        if (perfectTunes >= 100) {
            if (unlockIfLocked(AchievementType.PERFECT_PITCH)) newlyUnlocked.add(AchievementType.PERFECT_PITCH)
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
        val clamped = progress.coerceIn(0, 100)
        val current = achievementDao.getAchievementById(type.id)?.progress ?: 0
        if (clamped > current) {
            achievementDao.updateProgress(type.id, clamped)
        }
    }

    private fun calculateStreak(sessions: List<PracticeSession>): Int {
        if (sessions.isEmpty()) return 0

        val calendar = Calendar.getInstance()

        val sessionsByDay = sessions.groupBy { session ->
            calendar.timeInMillis = session.timestamp
            "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.DAY_OF_YEAR)}"
        }

        val validDayKeys = sessionsByDay.filter { (_, daySessions) ->
            val validDuration = daySessions
                .filter { it.consistencyScore > 0 || it.avgBpm == 0 }
                .sumOf { it.durationSeconds }

            validDuration >= MIN_DAILY_TIME_FOR_STREAK
        }.keys

        var streak = 0
        calendar.timeInMillis = System.currentTimeMillis()

        while (true) {
            val dayKey = "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.DAY_OF_YEAR)}"
            if (validDayKeys.contains(dayKey)) {
                streak++
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                if (streak == 0) {
                    calendar.add(Calendar.DAY_OF_YEAR, -1)
                    val yesterdayKey = "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.DAY_OF_YEAR)}"
                    if (validDayKeys.contains(yesterdayKey)) {
                        streak++
                        calendar.add(Calendar.DAY_OF_YEAR, -1)
                        continue
                    } else {
                        break
                    }
                } else {
                    break
                }
            }
        }
        return streak
    }

    suspend fun getAchievementProgress(type: AchievementType): Int {
        return achievementDao.getAchievementById(type.id)?.progress ?: 0
    }

    fun observeCurrentStreak(): Flow<Int> {
        return sessionDao.getAllSessions().map { calculateStreak(it) }
    }

    private fun countDistinctDaysInTimeRange(sessions: List<PracticeSession>, startHour: Int, endHour: Int): Int {
        val calendar = Calendar.getInstance()

        val filteredSessions = sessions.filter { session ->
            calendar.timeInMillis = session.timestamp
            val hour = calendar.get(Calendar.HOUR_OF_DAY)

            if (startHour <= endHour) {
                hour in startHour..endHour
            } else {
                hour >= startHour || hour <= endHour
            }
        }
        val distinctDays = filteredSessions.map { session ->
            calendar.timeInMillis = session.timestamp
            "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.DAY_OF_YEAR)}"
        }.distinct()

        return distinctDays.size
    }
}