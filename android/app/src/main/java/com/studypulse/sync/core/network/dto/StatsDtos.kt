package com.studypulse.sync.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StatsResponse(
    @SerialName("device_uid")
    val deviceUid: String,

    @SerialName("total_xp")
    val totalXp: Int,

    @SerialName("weekly_xp")
    val weeklyXp: Int,

    @SerialName("level")
    val level: Int,

    @SerialName("streak_days")
    val streakDays: Int,

    @SerialName("rating_opt_in")
    val ratingOptIn: Boolean,

    @SerialName("weekly_period_key")
    val weeklyPeriodKey: String? = null,

    @SerialName("server_time")
    val serverTime: String? = null
)

@Serializable
data class LeaderboardEntryDto(
    @SerialName("rank")
    val rank: Int,

    @SerialName("device_uid")
    val deviceUid: String,

    @SerialName("nickname")
    val nickname: String? = null,

    @SerialName("score")
    val score: Int,

    @SerialName("level")
    val level: Int,

    @SerialName("streak_days")
    val streakDays: Int,

    @SerialName("is_current_user")
    val isCurrentUser: Boolean = false
)

@Serializable
data class LeaderboardResponse(
    @SerialName("period")
    val period: String,

    @SerialName("entries")
    val entries: List<LeaderboardEntryDto> = emptyList(),

    @SerialName("server_time")
    val serverTime: String? = null
)