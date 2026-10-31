package com.studypulse.sync.data.repository

import com.studypulse.sync.core.datastore.SettingsRepository
import com.studypulse.sync.core.network.ApiService
import com.studypulse.sync.core.network.dto.SyncBatchRequest
import com.studypulse.sync.core.network.dto.SyncBatchResponse
import com.studypulse.sync.core.network.dto.SyncEventRequest
import com.studypulse.sync.data.local.AppDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import java.time.Instant

class SyncRepository(
    private val database: AppDatabase,
    private val settingsRepository: SettingsRepository,
    private val apiFactory: (String) -> ApiService
) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    val pendingCount: Flow<Int> =
        database.pendingEventDao().observeUnsyncedCount()

    suspend fun sync(): Result<SyncBatchResponse?> {
        return try {
            val settings = settingsRepository.currentSettings()
            val deviceUid = settingsRepository.ensureDeviceUid()
            val api = apiFactory(settings.serverUrl)

            val events = database.pendingEventDao().getUnsyncedOnce(limit = 100)

            if (events.isEmpty()) {
                return Result.success(null)
            }

            val requests = mutableListOf<SyncEventRequest>()
            val sentIds = mutableListOf<String>()

            for (event in events) {
                try {
                    val payload = json.parseToJsonElement(event.payloadJson)

                    requests += SyncEventRequest(
                        clientEventId = event.clientEventId,
                        eventType = event.eventType,
                        occurredAt = Instant.ofEpochMilli(event.occurredAt).toString(),
                        payload = payload
                    )

                    sentIds += event.clientEventId
                } catch (e: Throwable) {
                    database.pendingEventDao().markFailed(
                        ids = listOf(event.clientEventId),
                        error = e.message ?: "Invalid local event payload"
                    )
                }
            }

            if (requests.isEmpty()) {
                return Result.success(null)
            }

            val response = api.syncBatch(
                SyncBatchRequest(
                    deviceUid = deviceUid,
                    events = requests
                )
            )

            database.pendingEventDao().markSynced(sentIds)
            settingsRepository.setLastSyncAt(System.currentTimeMillis())

            Result.success(response)
        } catch (e: Throwable) {
            val message = e.message ?: "Sync failed"

            try {
                val unsynced = database.pendingEventDao().getUnsyncedOnce(limit = 100)
                if (unsynced.isNotEmpty()) {
                    database.pendingEventDao().markFailed(
                        ids = unsynced.map { it.clientEventId },
                        error = message
                    )
                }
            } catch (_: Throwable) {
                // ignore secondary failure
            }

            Result.failure(e)
        }
    }
}