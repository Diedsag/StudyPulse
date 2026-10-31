package com.studypulse.sync.data.export

import android.annotation.SuppressLint
import com.studypulse.sync.data.local.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ExportSubject(
    val id: Long,
    val name: String,
    val colorArgb: Int,
    val teacher: String?,
    val source: String
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ExportScheduleItem(
    val externalId: String,
    val subject: String,
    val lessonType: String?,
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String,
    val room: String?,
    val teacher: String?,
    val parity: String?,
    val source: String
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ExportTask(
    val externalId: String,
    val title: String,
    val subject: String?,
    val deadlineDate: String,
    val effortMinutes: Int,
    val remainingMinutes: Int,
    val priority: Int,
    val status: String,
    val source: String
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ExportMaterial(
    val externalId: String,
    val title: String,
    val sourceLabel: String?,
    val summaryText: String,
    val source: String
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ExportData(
    val version: Int = 1,
    val exportedAt: String,
    val subjects: List<ExportSubject>,
    val scheduleTemplates: List<ExportScheduleItem>,
    val tasks: List<ExportTask>,
    val materials: List<ExportMaterial>
)

class PlanExporter(
    private val database: AppDatabase
) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    suspend fun exportPlan(): Result<String> {
        return try {
            val subjectsList = database.subjectDao().observeAll().first()
            val scheduleTemplatesList = database.scheduleDao().getAllActiveOnce()
            val tasksList = database.taskDao().getActiveOnce()
            val materialsList = database.materialDao().getAllActiveOnce()

            val subjects = subjectsList.map { s ->
                ExportSubject(
                    id = s.id,
                    name = s.name,
                    colorArgb = s.colorArgb,
                    teacher = s.teacher,
                    source = s.source
                )
            }

            val scheduleTemplates = scheduleTemplatesList.map { s ->
                ExportScheduleItem(
                    externalId = s.externalId,
                    subject = s.subject,
                    lessonType = s.lessonType,
                    dayOfWeek = s.dayOfWeek,
                    startTime = s.startTime,
                    endTime = s.endTime,
                    room = s.room,
                    teacher = s.teacher,
                    parity = s.parity,
                    source = s.source
                )
            }

            val tasks = tasksList.map { t ->
                ExportTask(
                    externalId = t.externalId,
                    title = t.title,
                    subject = t.subject,
                    deadlineDate = t.deadlineDate,
                    effortMinutes = t.effortMinutes,
                    remainingMinutes = t.remainingMinutes,
                    priority = t.priority,
                    status = t.status,
                    source = t.source
                )
            }

            val materials = materialsList.map { m ->
                ExportMaterial(
                    externalId = m.externalId,
                    title = m.title,
                    sourceLabel = m.sourceLabel,
                    summaryText = m.summaryText,
                    source = m.source
                )
            }

            val exportData = ExportData(
                exportedAt = Instant.now().toString(),
                subjects = subjects,
                scheduleTemplates = scheduleTemplates,
                tasks = tasks,
                materials = materials
            )

            // ИСПРАВЛЕНИЕ: Явное указание сериализатора устраняет необходимость в @OptIn
            val jsonString = json.encodeToString(ExportData.serializer(), exportData)
            Result.success(jsonString)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}