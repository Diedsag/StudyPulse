package com.studypulse.sync.data.repository

import androidx.room.withTransaction
import com.studypulse.sync.core.datastore.SettingsRepository
import com.studypulse.sync.core.network.ApiService
import com.studypulse.sync.core.network.dto.DeviceRegisterRequest
import com.studypulse.sync.data.local.AppDatabase
import com.studypulse.sync.data.local.entity.PendingEventEntity
import com.studypulse.sync.data.local.entity.StudyPackageEntity
import com.studypulse.sync.data.mapper.BundleMappers.toEntities
import com.studypulse.sync.data.mapper.BundleMappers.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID

interface PackageRepository {

    val packages: Flow<List<StudyPackageEntity>>

    suspend fun refresh(): Result<Unit>

    suspend fun downloadPackage(externalId: String): Result<Unit>
}

class PackageRepositoryImpl(
    private val database: AppDatabase,
    private val settingsRepository: SettingsRepository,
    private val apiFactory: (String) -> ApiService
) : PackageRepository {

    override val packages: Flow<List<StudyPackageEntity>> =
        database.packageDao().observeAll()

    override suspend fun refresh(): Result<Unit> {
        return try {
            val (deviceUid, api) = prepareApiAndDevice()

            val response = api.getPackages(deviceUid)
            val existingPackages = database.packageDao().getAllOnce()
                .associateBy { it.externalId }

            val entities = response.packages.map { dto ->
                val old = existingPackages[dto.externalId]

                dto.toEntity(
                    isDownloaded = old?.isDownloaded ?: false,
                    downloadedAt = old?.downloadedAt
                )
            }

            database.packageDao().upsertAll(entities)

            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    override suspend fun downloadPackage(externalId: String): Result<Unit> {
        return try {
            val (deviceUid, api) = prepareApiAndDevice()

            val bundle = api.getPackageBundle(
                externalId = externalId,
                deviceUid = deviceUid
            )

            val now = System.currentTimeMillis()
            val entities = bundle.toEntities(now)

            database.withTransaction {
                database.scheduleDao().deleteByPackage(externalId)
                database.taskDao().deleteByPackage(externalId)
                database.materialDao().deleteByPackage(externalId)
                database.cardDao().deleteByPackage(externalId)

                database.packageDao().upsertAll(listOf(entities.packageEntity))

                database.scheduleDao().insertAll(entities.schedule)
                database.taskDao().insertAll(entities.tasks)
                database.materialDao().insertAll(entities.materials)
                database.cardDao().insertAll(entities.cards)

                val clientEventId = UUID.randomUUID().toString()
                val payloadJson = buildJsonObject {
                    put("package_external_id", externalId)
                }.toString()

                database.pendingEventDao().insert(
                    PendingEventEntity(
                        clientEventId = clientEventId,
                        eventType = "PACKAGE_DOWNLOADED",
                        payloadJson = payloadJson,
                        occurredAt = now,
                        createdAt = now,
                        retryCount = 0,
                        lastError = null,
                        synced = false
                    )
                )
            }

            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    private suspend fun prepareApiAndDevice(): Pair<String, ApiService> {
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

        return deviceUid to api
    }
}