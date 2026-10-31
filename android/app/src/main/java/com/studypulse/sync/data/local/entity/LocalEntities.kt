package com.studypulse.sync.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "packages",
    indices = [Index("isDownloaded")]
)
data class StudyPackageEntity(
    @PrimaryKey
    val externalId: String,

    val title: String,
    val subject: String?,
    val description: String?,

    val isPublic: Boolean,
    val targetDeviceUid: String?,

    val updatedAt: String?,

    val downloadedAt: Long?,
    val isDownloaded: Boolean
)

@Entity(
    tableName = "schedule_items",
    indices = [Index("packageExternalId"), Index("dayOfWeek")]
)
data class ScheduleItemEntity(
    @PrimaryKey
    val externalId: String,

    val packageExternalId: String,

    val subject: String,
    val lessonType: String?,

    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String,

    val room: String?,
    val teacher: String?,
    val parity: String?
)

@Entity(
    tableName = "tasks",
    indices = [
        Index("packageExternalId"),
        Index("deadlineDate"),
        Index("status")
    ]
)
data class TaskEntity(
    @PrimaryKey
    val externalId: String,

    val packageExternalId: String,

    val title: String,
    val subject: String?,

    val deadlineDate: String,
    val effortMinutes: Int,
    val priority: Int,

    val status: String,
    val remainingMinutes: Int
)

@Entity(
    tableName = "materials",
    indices = [Index("packageExternalId")]
)
data class MaterialEntity(
    @PrimaryKey
    val externalId: String,

    val packageExternalId: String,

    val title: String,
    val sourceLabel: String?,

    val summaryText: String,
    val keyFactsJson: String?
)

@Entity(
    tableName = "cards",
    indices = [Index("packageExternalId"), Index("dueAt")]
)
data class CardEntity(
    @PrimaryKey
    val externalId: String,

    val packageExternalId: String,
    val materialExternalId: String?,

    val front: String,
    val back: String,
    val hint: String?,
    val position: Int,

    val easeFactor: Double,
    val intervalDays: Int,
    val repetitions: Int,
    val lapses: Int,

    val dueAt: Long,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "class_instances",
    primaryKeys = ["templateExternalId", "date"],
    indices = [Index("date")]
)
data class ClassInstanceEntity(
    val templateExternalId: String,
    val date: String,

    val status: String,

    val overriddenStartTime: String? = null,
    val overriddenEndTime: String? = null,

    val updatedAt: Long
)

@Entity(
    tableName = "study_slots",
    indices = [
        Index("taskExternalId"),
        Index("status"),
        Index("startDateTime")
    ]
)
data class StudySlotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val taskExternalId: String,
    val packageExternalId: String,

    val startDateTime: Long,
    val endDateTime: Long,

    val status: String,

    val materialExternalId: String? = null,
    val createdAt: Long
)

@Entity(
    tableName = "review_logs",
    indices = [
        Index("cardExternalId"),
        Index("reviewedAt"),
        Index("synced")
    ]
)
data class ReviewLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val clientEventId: String,
    val cardExternalId: String,
    val packageExternalId: String,

    val rating: Int,
    val reviewedAt: Long,

    val previousIntervalDays: Int,
    val newIntervalDays: Int,
    val previousEaseFactor: Double,
    val newEaseFactor: Double,

    val synced: Boolean = false
)

@Entity(
    tableName = "pending_events",
    indices = [
        Index("synced"),
        Index("eventType")
    ]
)
data class PendingEventEntity(
    @PrimaryKey
    val clientEventId: String,

    val eventType: String,
    val payloadJson: String,

    val occurredAt: Long,
    val createdAt: Long,

    val retryCount: Int = 0,
    val lastError: String? = null,
    val synced: Boolean = false
)