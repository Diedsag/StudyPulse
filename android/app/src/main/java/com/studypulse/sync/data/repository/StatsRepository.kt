package com.studypulse.sync.data.repository

import com.studypulse.sync.core.datastore.SettingsRepository
import com.studypulse.sync.core.network.ApiService
import com.studypulse.sync.core.network.dto.DeviceRegisterRequest
import com.studypulse.sync.core.network.dto.LeaderboardEntryDto
import com.studypulse.sync.data.local.AppDatabase
import com.studypulse.sync.data.local.entity.StatsSnapshotEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class StatsSnapshot(
    val period: String,
    val totalXp: Int,
    val weeklyXp: Int,
    val level: Int,
    val streakDays: Int,
    val ratingOptIn: Boolean,
    val weeklyPeriodKey: String?,
    val fetchedAt: Long,
    val leaderboard: List<LeaderboardEntryDto>
)

class StatsRepository(
    private val database: AppDatabase,
    private val settingsRepository: SettingsRepository,
    private val apiFactory: (String) -> ApiService
) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    private val dao = database.statsSnapshotDao()

    fun observeSnapshot(period: String): Flow<StatsSnapshot?> {
        return dao.observeByPeriod(period)
            .map { items ->
                items.firstOrNull()?.toDomain()
            }
    }

    suspend fun refresh(period: String): Result<StatsSnapshot> {
        return try {
            val settings = settingsRepository.currentSettings()
            val deviceUid = settingsRepository.ensureDeviceUid()
            val api = apiFactory(settings.serverUrl)

            api.registerDevice(
                DeviceRegisterRequest(
                    deviceUid = deviceUid,
                    nickname = settings.nickname,
                    ratingOptIn = settings.ratingOptIn
                )
            )

            val stats = api.getStats(deviceUid)
            val leaderboard = api.getLeaderboard(
                deviceUid = deviceUid,
                period = period,
                limit = 20
            )

            val now = System.currentTimeMillis()

            val entity = StatsSnapshotEntity(
                period = period,
                deviceUid = deviceUid,
                totalXp = stats.totalXp,
                weeklyXp = stats.weeklyXp,
                level = stats.level,
                streakDays = stats.streakDays,
                ratingOptIn = stats.ratingOptIn,
                weeklyPeriodKey = stats.weeklyPeriodKey,
                fetchedAt = now,
                leaderboardJson = json.encodeToString(leaderboard.entries)
            )

            dao.upsert(entity)

            Result.success(entity.toDomain())
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    private fun StatsSnapshotEntity.toDomain(): StatsSnapshot {
        val leaderboard = try {
            json.decodeFromString<List<LeaderboardEntryDto>>(leaderboardJson)
        } catch (_: Throwable) {
            emptyList()
        }

        return StatsSnapshot(
            period = period,
            totalXp = totalXp,
            weeklyXp = weeklyXp,
            level = level,
            streakDays = streakDays,
            ratingOptIn = ratingOptIn,
            weeklyPeriodKey = weeklyPeriodKey,
            fetchedAt = fetchedAt,
            leaderboard = leaderboard
        )
    }
}