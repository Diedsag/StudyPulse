package com.studypulse.sync.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceRegisterRequest(
    @SerialName("device_uid")
    val deviceUid: String,

    @SerialName("nickname")
    val nickname: String,

    @SerialName("rating_opt_in")
    val ratingOptIn: Boolean
)

@Serializable
data class DeviceRegisterResponse(
    @SerialName("device_uid")
    val deviceUid: String,

    @SerialName("nickname")
    val nickname: String? = null,

    @SerialName("created")
    val created: Boolean = false,

    @SerialName("total_xp")
    val totalXp: Int = 0,

    @SerialName("weekly_xp")
    val weeklyXp: Int = 0,

    @SerialName("level")
    val level: Int = 1,

    @SerialName("streak_days")
    val streakDays: Int = 0,

    @SerialName("server_time")
    val serverTime: String? = null
)

@Serializable
data class PackageDto(
    @SerialName("external_id")
    val externalId: String,

    @SerialName("title")
    val title: String,

    @SerialName("subject")
    val subject: String? = null,

    @SerialName("description")
    val description: String? = null,

    @SerialName("is_public")
    val isPublic: Boolean = true,

    @SerialName("target_device_uid")
    val targetDeviceUid: String? = null,

    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class PackageListResponse(
    @SerialName("packages")
    val packages: List<PackageDto> = emptyList(),

    @SerialName("server_time")
    val serverTime: String? = null
)

@Serializable
data class ScheduleItemDto(
    @SerialName("external_id")
    val externalId: String,

    @SerialName("package_external_id")
    val packageExternalId: String,

    @SerialName("subject")
    val subject: String,

    @SerialName("lesson_type")
    val lessonType: String? = null,

    @SerialName("day_of_week")
    val dayOfWeek: Int,

    @SerialName("start_time")
    val startTime: String,

    @SerialName("end_time")
    val endTime: String,

    @SerialName("room")
    val room: String? = null,

    @SerialName("teacher")
    val teacher: String? = null,

    @SerialName("parity")
    val parity: String? = null
)

@Serializable
data class TaskDto(
    @SerialName("external_id")
    val externalId: String,

    @SerialName("package_external_id")
    val packageExternalId: String,

    @SerialName("title")
    val title: String,

    @SerialName("subject")
    val subject: String? = null,

    @SerialName("deadline_date")
    val deadlineDate: String,

    @SerialName("effort_minutes")
    val effortMinutes: Int,

    @SerialName("priority")
    val priority: Int
)

@Serializable
data class MaterialDto(
    @SerialName("external_id")
    val externalId: String,

    @SerialName("package_external_id")
    val packageExternalId: String,

    @SerialName("title")
    val title: String,

    @SerialName("source_label")
    val sourceLabel: String? = null,

    @SerialName("summary_text")
    val summaryText: String,

    @SerialName("key_facts")
    val keyFacts: List<String> = emptyList()
)

@Serializable
data class CardDto(
    @SerialName("external_id")
    val externalId: String,

    @SerialName("package_external_id")
    val packageExternalId: String,

    @SerialName("material_external_id")
    val materialExternalId: String? = null,

    @SerialName("front")
    val front: String,

    @SerialName("back")
    val back: String,

    @SerialName("hint")
    val hint: String? = null,

    @SerialName("position")
    val position: Int = 0
)

@Serializable
data class BundleResponse(
    @SerialName("package")
    val pkg: PackageDto,

    @SerialName("schedule")
    val schedule: List<ScheduleItemDto> = emptyList(),

    @SerialName("tasks")
    val tasks: List<TaskDto> = emptyList(),

    @SerialName("materials")
    val materials: List<MaterialDto> = emptyList(),

    @SerialName("cards")
    val cards: List<CardDto> = emptyList(),

    @SerialName("server_time")
    val serverTime: String? = null
)