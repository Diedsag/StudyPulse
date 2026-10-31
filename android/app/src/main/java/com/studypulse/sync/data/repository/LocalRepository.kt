package com.studypulse.sync.data.repository

import com.studypulse.sync.data.local.AppDatabase
import com.studypulse.sync.data.local.entity.MaterialEntity
import com.studypulse.sync.data.local.entity.ScheduleItemEntity
import com.studypulse.sync.data.local.entity.SubjectEntity
import com.studypulse.sync.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class LocalRepository(
    private val database: AppDatabase
) {

    private val subjectDao = database.subjectDao()
    private val scheduleDao = database.scheduleDao()
    private val taskDao = database.taskDao()
    private val materialDao = database.materialDao()

    val subjects: Flow<List<SubjectEntity>> = subjectDao.observeAll()

    suspend fun createSubject(name: String, colorArgb: Int, teacher: String?): Result<SubjectEntity> {
        return try {
            val trimmedName = name.trim()
            if (trimmedName.isBlank()) return Result.failure(IllegalArgumentException("Название предмета не может быть пустым"))
            val now = System.currentTimeMillis()
            val entity = SubjectEntity(
                name = trimmedName, colorArgb = colorArgb,
                teacher = teacher?.trim()?.takeIf { it.isNotBlank() },
                source = "LOCAL", packageExternalId = null, createdAt = now, updatedAt = now
            )
            val id = subjectDao.insert(entity)
            Result.success(entity.copy(id = id))
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun updateSubject(id: Long, name: String, colorArgb: Int, teacher: String?): Result<SubjectEntity> {
        return try {
            if (id <= 0L) return Result.failure(IllegalArgumentException("Некорректный идентификатор предмета"))
            val existing = subjectDao.getById(id) ?: return Result.failure(IllegalStateException("Предмет не найден"))
            val trimmedName = name.trim()
            if (trimmedName.isBlank()) return Result.failure(IllegalArgumentException("Название предмета не может быть пустым"))
            val updated = existing.copy(
                name = trimmedName, colorArgb = colorArgb,
                teacher = teacher?.trim()?.takeIf { it.isNotBlank() }, updatedAt = System.currentTimeMillis()
            )
            subjectDao.update(updated)
            Result.success(updated)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun deleteSubject(id: Long): Result<Unit> {
        return try {
            if (id <= 0L) return Result.failure(IllegalArgumentException("Некорректный идентификатор предмета"))
            subjectDao.deleteById(id)
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun createScheduleTemplate(
        subjectId: Long?, subjectName: String, lessonType: String, dayOfWeek: Int,
        startTime: String, endTime: String, room: String?, teacher: String?, parity: String
    ): Result<ScheduleItemEntity> {
        return try {
            validateScheduleFields(dayOfWeek, startTime, endTime)
            val entity = ScheduleItemEntity(
                externalId = UUID.randomUUID().toString(), packageExternalId = null,
                subject = normalizeSubjectName(subjectName),
                lessonType = lessonType.trim().takeIf { it.isNotBlank() },
                dayOfWeek = dayOfWeek, startTime = startTime.trim(), endTime = endTime.trim(),
                room = room?.trim()?.takeIf { it.isNotBlank() }, teacher = teacher?.trim()?.takeIf { it.isNotBlank() },
                parity = parity.trim().takeIf { it.isNotBlank() } ?: "EVERY", subjectId = subjectId, source = "LOCAL"
            )
            scheduleDao.insertAll(listOf(entity))
            Result.success(entity)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun updateScheduleTemplate(
        externalId: String, subjectId: Long?, subjectName: String, lessonType: String, dayOfWeek: Int,
        startTime: String, endTime: String, room: String?, teacher: String?, parity: String
    ): Result<ScheduleItemEntity> {
        return try {
            if (externalId.isBlank()) return Result.failure(IllegalArgumentException("Некорректный идентификатор занятия"))
            validateScheduleFields(dayOfWeek, startTime, endTime)
            val entity = ScheduleItemEntity(
                externalId = externalId, packageExternalId = null,
                subject = normalizeSubjectName(subjectName),
                lessonType = lessonType.trim().takeIf { it.isNotBlank() },
                dayOfWeek = dayOfWeek, startTime = startTime.trim(), endTime = endTime.trim(),
                room = room?.trim()?.takeIf { it.isNotBlank() }, teacher = teacher?.trim()?.takeIf { it.isNotBlank() },
                parity = parity.trim().takeIf { it.isNotBlank() } ?: "EVERY", subjectId = subjectId, source = "LOCAL"
            )
            scheduleDao.insertAll(listOf(entity))
            Result.success(entity)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun restoreScheduleTemplate(entity: ScheduleItemEntity) {
        scheduleDao.insertAll(listOf(entity))
    }

    suspend fun deleteScheduleTemplate(externalId: String): Result<Unit> {
        return try {
            if (externalId.isBlank()) return Result.failure(IllegalArgumentException("Некорректный идентификатор занятия"))
            scheduleDao.deleteById(externalId)
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun getScheduleTemplate(externalId: String): ScheduleItemEntity? {
        return scheduleDao.getAllActiveOnce().firstOrNull { it.externalId == externalId }
    }

    suspend fun createTask(
        subjectId: Long?, subjectName: String?, title: String, deadlineDate: String,
        effortMinutes: Int, priority: Int, description: String?
    ): Result<TaskEntity> {
        return try {
            val trimmedTitle = title.trim()
            if (trimmedTitle.isBlank()) return Result.failure(IllegalArgumentException("Название задания не может быть пустым"))
            if (effortMinutes <= 0) return Result.failure(IllegalArgumentException("Трудоёмкость должна быть больше нуля"))
            LocalDate.parse(deadlineDate.trim())
            val now = System.currentTimeMillis()
            val entity = TaskEntity(
                externalId = UUID.randomUUID().toString(), packageExternalId = null, title = trimmedTitle,
                subject = subjectName?.trim()?.takeIf { it.isNotBlank() }, deadlineDate = deadlineDate.trim(),
                effortMinutes = effortMinutes, priority = priority.coerceIn(1, 3), status = "PENDING",
                remainingMinutes = effortMinutes, subjectId = subjectId, source = "LOCAL",
                description = description?.trim()?.takeIf { it.isNotBlank() }, createdAt = now, updatedAt = now
            )
            taskDao.insertAll(listOf(entity))
            Result.success(entity)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun updateTask(
        externalId: String, subjectId: Long?, subjectName: String?, title: String, deadlineDate: String,
        effortMinutes: Int, remainingMinutes: Int, priority: Int, status: String, description: String?
    ): Result<TaskEntity> {
        return try {
            if (externalId.isBlank()) return Result.failure(IllegalArgumentException("Некорректный идентификатор задания"))
            val trimmedTitle = title.trim()
            if (trimmedTitle.isBlank()) return Result.failure(IllegalArgumentException("Название задания не может быть пустым"))
            if (effortMinutes <= 0) return Result.failure(IllegalArgumentException("Трудоёмкость должна быть больше нуля"))
            LocalDate.parse(deadlineDate.trim())
            val existing = taskDao.getById(externalId) ?: return Result.failure(IllegalStateException("Задание не найдено"))
            val normalizedStatus = if (status.isBlank()) existing.status else status
            val normalizedRemaining = if (normalizedStatus == "DONE") 0 else remainingMinutes.coerceIn(0, effortMinutes)
            val entity = existing.copy(
                title = trimmedTitle, subject = subjectName?.trim()?.takeIf { it.isNotBlank() },
                deadlineDate = deadlineDate.trim(), effortMinutes = effortMinutes, priority = priority.coerceIn(1, 3),
                status = normalizedStatus, remainingMinutes = normalizedRemaining, subjectId = subjectId,
                source = "LOCAL", description = description?.trim()?.takeIf { it.isNotBlank() },
                updatedAt = System.currentTimeMillis()
            )
            taskDao.update(entity)
            Result.success(entity)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun markTaskDone(externalId: String): Result<Unit> {
        return try {
            if (externalId.isBlank()) return Result.failure(IllegalArgumentException("Некорректный идентификатор задания"))
            taskDao.updateStatus(externalId, "DONE", 0)
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun restoreTask(entity: TaskEntity) {
        taskDao.insertAll(listOf(entity))
    }

    suspend fun deleteTask(externalId: String): Result<Unit> {
        return try {
            if (externalId.isBlank()) return Result.failure(IllegalArgumentException("Некорректный идентификатор задания"))
            taskDao.deleteById(externalId)
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun createMaterial(
        subjectId: Long?, subjectName: String?, title: String, text: String, sourceLabel: String?
    ): Result<MaterialEntity> {
        return try {
            val trimmedTitle = title.trim()
            val trimmedText = text.trim()
            if (trimmedTitle.isBlank()) return Result.failure(IllegalArgumentException("Название материала не может быть пустым"))
            if (trimmedText.isBlank()) return Result.failure(IllegalArgumentException("Текст материала не может быть пустым"))
            val now = System.currentTimeMillis()
            val entity = MaterialEntity(
                externalId = UUID.randomUUID().toString(), packageExternalId = null, title = trimmedTitle,
                sourceLabel = sourceLabel?.trim()?.takeIf { it.isNotBlank() }, summaryText = trimmedText,
                keyFactsJson = null, subjectId = subjectId, source = "LOCAL", rawText = trimmedText,
                createdAt = now, updatedAt = now
            )
            materialDao.insertAll(listOf(entity))
            Result.success(entity)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun updateMaterial(
        externalId: String, subjectId: Long?, subjectName: String?, title: String, text: String, sourceLabel: String?
    ): Result<MaterialEntity> {
        return try {
            if (externalId.isBlank()) return Result.failure(IllegalArgumentException("Некорректный идентификатор материала"))
            val trimmedTitle = title.trim()
            val trimmedText = text.trim()
            if (trimmedTitle.isBlank()) return Result.failure(IllegalArgumentException("Название материала не может быть пустым"))
            if (trimmedText.isBlank()) return Result.failure(IllegalArgumentException("Текст материала не может быть пустым"))
            val existing = materialDao.getAllActiveOnce().firstOrNull { it.externalId == externalId }
                ?: return Result.failure(IllegalStateException("Материал не найден"))
            val entity = existing.copy(
                title = trimmedTitle, sourceLabel = sourceLabel?.trim()?.takeIf { it.isNotBlank() },
                summaryText = trimmedText, subjectId = subjectId, source = "LOCAL", rawText = trimmedText,
                updatedAt = System.currentTimeMillis()
            )
            materialDao.update(entity)
            Result.success(entity)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun deleteMaterial(externalId: String): Result<Unit> {
        return try {
            if (externalId.isBlank()) return Result.failure(IllegalArgumentException("Некорректный идентификатор материала"))
            materialDao.deleteById(externalId)
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    private fun normalizeSubjectName(value: String): String {
        val trimmed = value.trim()
        return if (trimmed.isBlank()) "Без предмета" else trimmed
    }

    private fun validateScheduleFields(dayOfWeek: Int, startTime: String, endTime: String) {
        if (dayOfWeek !in 1..7) throw IllegalArgumentException("День недели должен быть от 1 до 7")
        val start = LocalTime.parse(startTime.trim())
        val end = LocalTime.parse(endTime.trim())
        if (end <= start) throw IllegalArgumentException("Время окончания должно быть позже времени начала")
    }
}