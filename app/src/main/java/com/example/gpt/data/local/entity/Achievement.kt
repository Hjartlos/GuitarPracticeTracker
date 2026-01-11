package com.example.gpt.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey val id: String,
    val unlockedAt: Long? = null,
    val progress: Int = 0
)

enum class AchievementType(
    val id: String,
    val titleRes: Int,
    val descriptionRes: Int,
    val iconName: String,
    val requirement: Int
) {
    FIRST_SESSION("first_session",
        com.example.gpt.R.string.achievement_first_session_title,
        com.example.gpt.R.string.achievement_first_session_desc,
        "play_arrow", 1),

    WEEK_STREAK("week_streak",
        com.example.gpt.R.string.achievement_week_streak_title,
        com.example.gpt.R.string.achievement_week_streak_desc,
        "local_fire_department", 7),

    MONTH_STREAK("month_streak",
        com.example.gpt.R.string.achievement_month_streak_title,
        com.example.gpt.R.string.achievement_month_streak_desc,
        "whatshot", 30),

    HOUR_TOTAL("hour_total",
        com.example.gpt.R.string.achievement_hour_total_title,
        com.example.gpt.R.string.achievement_hour_total_desc,
        "schedule", 1),

    TEN_HOURS("ten_hours",
        com.example.gpt.R.string.achievement_ten_hours_title,
        com.example.gpt.R.string.achievement_ten_hours_desc,
        "timer", 10),

    HUNDRED_HOURS("hundred_hours",
        com.example.gpt.R.string.achievement_hundred_hours_title,
        com.example.gpt.R.string.achievement_hundred_hours_desc,
        "emoji_events", 100),

    METRONOME_MASTER("metronome_master",
        com.example.gpt.R.string.achievement_metronome_master_title,
        com.example.gpt.R.string.achievement_metronome_master_desc,
        "music_note", 50),

    SPEED_DEMON("speed_demon",
        com.example.gpt.R.string.achievement_speed_demon_title,
        com.example.gpt.R.string.achievement_speed_demon_desc,
        "speed", 200),

    GOAL_GETTER("goal_getter",
        com.example.gpt.R.string.achievement_goal_getter_title,
        com.example.gpt.R.string.achievement_goal_getter_desc,
        "flag", 1),

    GOAL_CRUSHER("goal_crusher",
        com.example.gpt.R.string.achievement_goal_crusher_title,
        com.example.gpt.R.string.achievement_goal_crusher_desc,
        "military_tech", 10),

    VARIETY_PLAYER("variety_player",
        com.example.gpt.R.string.achievement_variety_player_title,
        com.example.gpt.R.string.achievement_variety_player_desc,
        "category", 5),

    EARLY_BIRD("early_bird",
        com.example.gpt.R.string.achievement_early_bird_title,
        com.example.gpt.R.string.achievement_early_bird_desc,
        "wb_sunny", 5),

    NIGHT_OWL("night_owl",
        com.example.gpt.R.string.achievement_night_owl_title,
        com.example.gpt.R.string.achievement_night_owl_desc,
        "nightlight", 5),

    PERFECT_PITCH("perfect_pitch",
        com.example.gpt.R.string.achievement_perfect_pitch_title,
        com.example.gpt.R.string.achievement_perfect_pitch_desc,
        "tune", 100)
}

