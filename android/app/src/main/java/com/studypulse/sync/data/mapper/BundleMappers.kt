package com.studypulse.sync.data.mapper

import com.studypulse.sync.core.network.dto.BundleResponse
import com.studypulse.sync.core.network.dto.CardDto
import com.studypulse.sync.core.network.dto.MaterialDto
import com.studypulse.sync.core.network.dto.PackageDto
import com.studypulse.sync.core.network.dto.ScheduleItemDto
import com.studypulse.sync.core.network.dto.TaskDto
import com.studypulse.sync.data.local.entity.CardEntity
import com.studypulse.sync.data.local.entity.MaterialEntity
import com.studypulse.sync.data.local.entity.ScheduleItemEntity
import com.studypulse.sync.data.local.entity.StudyPackageEntity
import com.studypulse.sync.data.local.entity.TaskEntity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class BundleEntities(
    val packageEntity: StudyPackageEntity,
    val schedule: List<ScheduleItemEntity>,
    val tasks: List<TaskEntity>,
    val materials: List<MaterialEntity>,
    val cards: List<CardEntity>
)

object BundleMappers {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    fun PackageDto.toEntity(
        isDownloaded: Boolean = false,
        downloadedAt: Long? = null
    ): StudyPackageEntity {
        return StudyPackageEntity(
            externalId = externalId,
            title = title,
            subject = subject,
            description = description,
            isPublic = isPublic,
            targetDeviceUid = targetDeviceUid,
            updatedAt = updatedAt,
            downloadedAt = downloadedAt,
            isDownloaded = isDownloaded
        )
    }

    fun BundleResponse.toEntities(downloadedAt: Long): BundleEntities {
        return BundleEntities(
            packageEntity = pkg.toEntity(
                isDownloaded = true,
                downloadedAt = downloadedAt
            ),
            schedule = schedule.map { it.toEntity() },
            tasks = tasks.map { it.toEntity() },
            materials = materials.map { it.toEntity() },
            cards = cards.map { it.toEntity(downloadedAt) }
        )
    }

    private fun ScheduleItemDto.toEntity(): ScheduleItemEntity {
        return ScheduleItemEntity(
            externalId = externalId,
            packageExternalId = packageExternalId,
            subject = subject,
            lessonType = lessonType,
            dayOfWeek = dayOfWeek,
            startTime = startTime,
            endTime = endTime,
            room = room,
            teacher = teacher,
            parity = parity
        )
    }

    private fun TaskDto.toEntity(): TaskEntity {
        return TaskEntity(
            externalId = externalId,
            packageExternalId = packageExternalId,
            title = title,
            subject = subject,
            deadlineDate = deadlineDate,
            effortMinutes = effortMinutes,
            priority = priority,
            status = "PENDING",
            remainingMinutes = effortMinutes
        )
    }

    private fun MaterialDto.toEntity(): MaterialEntity {
        val keyFactsJson = if (keyFacts.isEmpty()) {
            null
        } else {
            json.encodeToString(keyFacts)
        }

        return MaterialEntity(
            externalId = externalId,
            packageExternalId = packageExternalId,
            title = title,
            sourceLabel = sourceLabel,
            summaryText = summaryText,
            keyFactsJson = keyFactsJson
        )
    }

    private fun CardDto.toEntity(now: Long): CardEntity {
        return CardEntity(
            externalId = externalId,
            packageExternalId = packageExternalId,
            materialExternalId = materialExternalId,
            front = front,
            back = back,
            hint = hint,
            position = position,
            easeFactor = 2.5,
            intervalDays = 0,
            repetitions = 0,
            lapses = 0,
            dueAt = now,
            createdAt = now,
            updatedAt = now
        )
    }
}