package com.studypulse.sync.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stats_snapshots")
data class StatsSnapshotEntity(
    @PrimaryKey
    val period: String,

    val deviceUid: String,

    val totalXp: Int,
    val weeklyXp: Int,
    val level: Int,
    val streakDays: Int,

    val ratingOptIn: Boolean,
    val weeklyPeriodKey: String?,

    val fetchedAt: Long,
    val leaderboardJson: String
)