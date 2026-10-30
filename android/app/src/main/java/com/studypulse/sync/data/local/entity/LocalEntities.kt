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
    indices = [Index("packageExternalId"), Index("deadlineDate")]
)
data class TaskEntity(
    @PrimaryKey
    val externalId: String,

    val packageExternalId: String,

    val title: String,
    val subject: String?,

    val deadlineDate: String,
    val effortMinutes: Int,
    val priority: Int
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