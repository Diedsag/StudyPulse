package com.studypulse.sync.data.repository

import androidx.room.withTransaction
import com.studypulse.sync.data.local.AppDatabase
import com.studypulse.sync.data.local.dao.SlotWithTaskMaterial
import com.studypulse.sync.data.local.entity.CardEntity
import com.studypulse.sync.data.local.entity.ClassInstanceEntity
import com.studypulse.sync.data.local.entity.MaterialEntity
import com.studypulse.sync.data.local.entity.PendingEventEntity
import com.studypulse.sync.data.local.entity.ReviewLogEntity
import com.studypulse.sync.data.local.entity.ScheduleItemEntity
import com.studypulse.sync.data.local.entity.StudySlotEntity
import com.studypulse.sync.data.local.entity.TaskEntity
import com.studypulse.sync.data.srs.SpacedRepetitionScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

data class ClassOccurrence(
    val templateExternalId: String,
    val packageExternalId: String,

    val date: String,
    val displayDate: String,
    val weekday: String,

    val startTime: String,
    val endTime: String,

    val subject: String,
    val lessonType: String?,
    val room: String?,
    val teacher: String?,

    val isCancelled: Boolean
)

data class PlanGenerationResult(
    val slotsCount: Int,
    val warnings: List<String>
)

data class ReviewOutcome(
    val nextDueAt: Long,
    val intervalDays: Int
)

data class SlotCompletionResult(
    val durationMinutes: Int,
    val taskRemainingMinutes: Int,
    val taskCompleted: Boolean
)

class StudyRepository(
    private val database: AppDatabase
) {

    private val scheduleDao = database.scheduleDao()
    private val taskDao = database.taskDao()
    private val classInstanceDao = database.classInstanceDao()
    private val studySlotDao = database.studySlotDao()
    private val materialDao = database.materialDao()
    private val cardDao = database.cardDao()
    private val reviewLogDao = database.reviewLogDao()
    private val pendingEventDao = database.pendingEventDao()

    val tasks: Flow<List<TaskEntity>> =
        taskDao.observeDownloaded()

    val materials: Flow<List<MaterialEntity>> =
        materialDao.observeDownloaded()

    val proposedSlots: Flow<List<SlotWithTaskMaterial>> =
        studySlotDao.observeWithTaskMaterial("PROPOSED")

    val plannedSlots: Flow<List<SlotWithTaskMaterial>> =
        studySlotDao.observeWithTaskMaterial("PLANNED")

    fun upcomingOccurrences(daysAhead: Int = 7): Flow<List<ClassOccurrence>> {
        return combine(
            scheduleDao.observeDownloaded(),
            classInstanceDao.observeAll()
        ) { scheduleItems, instances ->
            buildOccurrences(
                scheduleItems = scheduleItems,
                instances = instances,
                daysAhead = daysAhead
            )
        }
    }

    suspend fun cancelOccurrence(
        templateExternalId: String,
        date: String
    ) {
        classInstanceDao.upsert(
            ClassInstanceEntity(
                templateExternalId = templateExternalId,
                date = date,
                status = "CANCELLED",
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun getDueCards(
        packageExternalId: String?,
        materialExternalId: String?
    ): Result<List<CardEntity>> {
        return try {
            val now = System.currentTimeMillis()

            val cards = when {
                !materialExternalId.isNullOrBlank() ->
                    cardDao.getDueByMaterialOnce(materialExternalId, now)

                !packageExternalId.isNullOrBlank() ->
                    cardDao.getDueByPackageOnce(packageExternalId, now)

                else ->
                    cardDao.getDueAllOnce(now)
            }

            Result.success(cards)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun reviewCard(
        card: CardEntity,
        rating: Int
    ): Result<ReviewOutcome> {
        return try {
            val now = System.currentTimeMillis()
            val updatedCard = SpacedRepetitionScheduler.review(card, rating, now)
            val clientEventId = UUID.randomUUID().toString()

            val payloadJson = buildJsonObject {
                put("package_external_id", card.packageExternalId)
                put("card_external_id", card.externalId)
                put("rating", JsonPrimitive(rating))
                put("previous_interval_days", JsonPrimitive(card.intervalDays))
                put("new_interval_days", JsonPrimitive(updatedCard.intervalDays))
            }.toString()

            database.withTransaction {
                cardDao.update(updatedCard)

                reviewLogDao.insert(
                    ReviewLogEntity(
                        clientEventId = clientEventId,
                        cardExternalId = card.externalId,
                        packageExternalId = card.packageExternalId,
                        rating = rating,
                        reviewedAt = now,
                        previousIntervalDays = card.intervalDays,
                        newIntervalDays = updatedCard.intervalDays,
                        previousEaseFactor = card.easeFactor,
                        newEaseFactor = updatedCard.easeFactor,
                        synced = false
                    )
                )

                pendingEventDao.insert(
                    PendingEventEntity(
                        clientEventId = clientEventId,
                        eventType = "CARD_REVIEWED",
                        payloadJson = payloadJson,
                        occurredAt = now,
                        createdAt = now,
                        retryCount = 0,
                        lastError = null,
                        synced = false
                    )
                )
            }

            Result.success(
                ReviewOutcome(
                    nextDueAt = updatedCard.dueAt,
                    intervalDays = updatedCard.intervalDays
                )
            )
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun generatePlan(): Result<PlanGenerationResult> {
        return try {
            val result = database.withTransaction {
                generatePlanInternal()
            }

            Result.success(result)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun acceptPlan(): Result<Int> {
        return try {
            val count = database.withTransaction {
                studySlotDao.updateStatus(
                    oldStatus = "PROPOSED",
                    newStatus = "PLANNED"
                )
            }

            Result.success(count)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun completeSlot(
        slot: StudySlotEntity
    ): Result<SlotCompletionResult> {
        return try {
            if (slot.status != "PLANNED") {
                return Result.failure(
                    IllegalStateException("Слот уже не находится в статусе PLANNED")
                )
            }

            val durationMinutes = (
                (slot.endDateTime - slot.startDateTime) / 60_000L
                ).toInt().coerceAtLeast(0)

            val task = taskDao.getById(slot.taskExternalId)
                ?: return Result.failure(
                    IllegalStateException("Задание для слота не найдено")
                )

            val newRemainingMinutes =
                (task.remainingMinutes - durationMinutes).coerceAtLeast(0)

            val newTaskStatus = if (newRemainingMinutes == 0) {
                "DONE"
            } else {
                "IN_PROGRESS"
            }

            val now = System.currentTimeMillis()

            val slotEventId = UUID.randomUUID().toString()
            val slotPayloadJson = buildJsonObject {
                put("package_external_id", slot.packageExternalId)
                put("task_external_id", slot.taskExternalId)
                put("slot_id", JsonPrimitive(slot.id))
                put("duration_minutes", JsonPrimitive(durationMinutes))
            }.toString()

            val taskCompleted = newTaskStatus == "DONE"
            val taskEventId = if (taskCompleted) UUID.randomUUID().toString() else null
            val taskPayloadJson = if (taskCompleted) {
                buildJsonObject {
                    put("package_external_id", slot.packageExternalId)
                    put("task_external_id", slot.taskExternalId)
                    put("effort_minutes", JsonPrimitive(task.effortMinutes))
                }.toString()
            } else {
                null
            }

            database.withTransaction {
                studySlotDao.markStatus(
                    id = slot.id,
                    status = "DONE"
                )

                taskDao.updateStatus(
                    externalId = task.externalId,
                    status = newTaskStatus,
                    remainingMinutes = newRemainingMinutes
                )

                pendingEventDao.insert(
                    PendingEventEntity(
                        clientEventId = slotEventId,
                        eventType = "SLOT_COMPLETED",
                        payloadJson = slotPayloadJson,
                        occurredAt = now,
                        createdAt = now,
                        retryCount = 0,
                        lastError = null,
                        synced = false
                    )
                )

                if (taskCompleted && taskEventId != null && taskPayloadJson != null) {
                    pendingEventDao.insert(
                        PendingEventEntity(
                            clientEventId = taskEventId,
                            eventType = "TASK_COMPLETED",
                            payloadJson = taskPayloadJson,
                            occurredAt = now,
                            createdAt = now,
                            retryCount = 0,
                            lastError = null,
                            synced = false
                        )
                    )
                }
            }

            Result.success(
                SlotCompletionResult(
                    durationMinutes = durationMinutes,
                    taskRemainingMinutes = newRemainingMinutes,
                    taskCompleted = taskCompleted
                )
            )
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    private suspend fun generatePlanInternal(): PlanGenerationResult {
        studySlotDao.deleteByStatus("PROPOSED")

        val now = System.currentTimeMillis()
        val today = LocalDate.now()

        val dayStart = LocalTime.of(8, 0)
        val dayEnd = LocalTime.of(22, 0)

        val minSlotMinutes = 30
        val maxDailyLoadMinutes = 180
        val horizonDays = 3

        val scheduleItems = scheduleDao.getAllDownloadedOnce()
        val instances = classInstanceDao.getAllOnce()
        val originalTasks = taskDao.getActiveOnce()

        val materialByPackage = materialDao.getAllDownloadedOnce()
            .groupBy { it.packageExternalId }
            .mapValues { entry ->
                entry.value.minByOrNull { material -> material.title }
            }

        val cancelledKeys = instances
            .filter { it.status == "CANCELLED" }
            .map { it.templateExternalId to it.date }
            .toSet()

        val remainingByTaskId = originalTasks
            .associate { it.externalId to it.remainingMinutes }
            .toMutableMap()

        val proposedSlots = mutableListOf<StudySlotEntity>()

        for (offset in 0 until horizonDays) {
            val date = today.plusDays(offset.toLong())
            val isoDate = date.toString()
            val dayOfWeek = date.dayOfWeek.value

            val occupiedIntervals = scheduleItems
                .filter { item ->
                    item.dayOfWeek == dayOfWeek &&
                        (item.externalId to isoDate) !in cancelledKeys
                }
                .mapNotNull { item ->
                    val start = parseTimeOrNull(item.startTime) ?: return@mapNotNull null
                    val end = parseTimeOrNull(item.endTime) ?: return@mapNotNull null

                    val clippedStart = maxOf(start, dayStart)
                    val clippedEnd = minOf(end, dayEnd)

                    if (clippedStart < clippedEnd) {
                        TimeInterval(clippedStart, clippedEnd)
                    } else {
                        null
                    }
                }
                .sortedBy { it.start }

            val mergedOccupied = mergeIntervals(occupiedIntervals)
            val freeWindows = buildFreeWindows(
                dayStart = dayStart,
                dayEnd = dayEnd,
                occupied = mergedOccupied
            ).toMutableList()

            var dailyLoadLeft = maxDailyLoadMinutes

            for (task in originalTasks) {
                var taskRemaining = remainingByTaskId[task.externalId] ?: 0

                if (taskRemaining <= 0) continue

                while (
                    taskRemaining > 0 &&
                    dailyLoadLeft > 0 &&
                    freeWindows.isNotEmpty()
                ) {
                    val windowIndex = freeWindows.indexOfFirst { window ->
                        val windowDuration = durationMinutes(window.start, window.end)

                        windowDuration >= minSlotMinutes ||
                            (taskRemaining < minSlotMinutes && windowDuration >= taskRemaining)
                    }

                    if (windowIndex < 0) break

                    val window = freeWindows[windowIndex]
                    val windowDuration = durationMinutes(window.start, window.end)

                    val chunkMinutes = minOf(
                        taskRemaining,
                        windowDuration,
                        dailyLoadLeft
                    )

                    if (chunkMinutes <= 0) {
                        freeWindows.removeAt(windowIndex)
                        continue
                    }

                    if (chunkMinutes < minSlotMinutes && taskRemaining >= minSlotMinutes) {
                        if (dailyLoadLeft < minSlotMinutes) break

                        if (windowDuration < minSlotMinutes) {
                            freeWindows.removeAt(windowIndex)
                            continue
                        }

                        break
                    }

                    val startMillis = toEpochMillis(date, window.start)
                    val endMillis = toEpochMillis(
                        date,
                        window.start.plusMinutes(chunkMinutes.toLong())
                    )

                    proposedSlots += StudySlotEntity(
                        taskExternalId = task.externalId,
                        packageExternalId = task.packageExternalId,
                        startDateTime = startMillis,
                        endDateTime = endMillis,
                        status = "PROPOSED",
                        materialExternalId = materialByPackage[task.packageExternalId]?.externalId,
                        createdAt = now
                    )

                    taskRemaining -= chunkMinutes
                    dailyLoadLeft -= chunkMinutes
                    remainingByTaskId[task.externalId] = taskRemaining

                    val newWindowStart = window.start.plusMinutes(chunkMinutes.toLong())

                    if (newWindowStart >= window.end) {
                        freeWindows.removeAt(windowIndex)
                    } else {
                        freeWindows[windowIndex] = FreeWindow(newWindowStart, window.end)
                    }
                }

                remainingByTaskId[task.externalId] = taskRemaining
            }
        }

        studySlotDao.insertAll(proposedSlots)

        val warnings = mutableListOf<String>()

        if (originalTasks.isEmpty()) {
            warnings += "Нет активных заданий для планирования."
        } else {
            for (task in originalTasks) {
                val allocatedMinutes = proposedSlots
                    .filter { it.taskExternalId == task.externalId }
                    .sumOf { slot ->
                        ((slot.endDateTime - slot.startDateTime) / 60_000L).toInt()
                    }

                val shortage = task.remainingMinutes - allocatedMinutes

                if (shortage > 0) {
                    warnings += "Не хватает ${shortage} мин для: ${task.title}"
                }
            }
        }

        return PlanGenerationResult(
            slotsCount = proposedSlots.size,
            warnings = warnings
        )
    }

    private fun buildOccurrences(
        scheduleItems: List<ScheduleItemEntity>,
        instances: List<ClassInstanceEntity>,
        daysAhead: Int
    ): List<ClassOccurrence> {
        val cancelledKeys = instances
            .filter { it.status == "CANCELLED" }
            .map { it.templateExternalId to it.date }
            .toSet()

        val today = LocalDate.now()
        val shortDateFormatter = DateTimeFormatter.ofPattern("dd.MM")
        val result = mutableListOf<ClassOccurrence>()

        for (offset in 0 until daysAhead) {
            val date = today.plusDays(offset.toLong())
            val isoDate = date.toString()
            val dayOfWeek = date.dayOfWeek.value

            val displayDate = when (offset) {
                0 -> "Сегодня"
                1 -> "Завтра"
                else -> date.format(shortDateFormatter)
            }

            val weekday = weekdayNames[dayOfWeek].orEmpty()

            val itemsForDay = scheduleItems.filter { it.dayOfWeek == dayOfWeek }

            for (item in itemsForDay) {
                val isCancelled = (item.externalId to isoDate) in cancelledKeys

                result += ClassOccurrence(
                    templateExternalId = item.externalId,
                    packageExternalId = item.packageExternalId,
                    date = isoDate,
                    displayDate = displayDate,
                    weekday = weekday,
                    startTime = item.startTime,
                    endTime = item.endTime,
                    subject = item.subject,
                    lessonType = item.lessonType,
                    room = item.room,
                    teacher = item.teacher,
                    isCancelled = isCancelled
                )
            }
        }

        return result.sortedWith(
            compareBy({ it.date }, { it.startTime })
        )
    }

    private fun mergeIntervals(input: List<TimeInterval>): List<TimeInterval> {
        if (input.isEmpty()) return emptyList()

        val sorted = input.sortedBy { it.start }
        val merged = mutableListOf(sorted.first())

        for (next in sorted.drop(1)) {
            val last = merged.last()

            if (next.start <= last.end) {
                merged[merged.lastIndex] = TimeInterval(
                    start = last.start,
                    end = maxOf(last.end, next.end)
                )
            } else {
                merged += next
            }
        }

        return merged
    }

    private fun buildFreeWindows(
        dayStart: LocalTime,
        dayEnd: LocalTime,
        occupied: List<TimeInterval>
    ): List<FreeWindow> {
        val windows = mutableListOf<FreeWindow>()
        var cursor = dayStart

        for (interval in occupied) {
            if (cursor < interval.start) {
                windows += FreeWindow(cursor, interval.start)
            }

            if (interval.end > cursor) {
                cursor = interval.end
            }
        }

        if (cursor < dayEnd) {
            windows += FreeWindow(cursor, dayEnd)
        }

        return windows.filter { durationMinutes(it.start, it.end) > 0 }
    }

    private fun parseTimeOrNull(value: String): LocalTime? {
        return try {
            LocalTime.parse(value)
        } catch (_: Throwable) {
            null
        }
    }

    private fun durationMinutes(start: LocalTime, end: LocalTime): Int {
        return Duration.between(start, end).toMinutes().toInt()
    }

    private fun toEpochMillis(date: LocalDate, time: LocalTime): Long {
        return date
            .atTime(time)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    private data class TimeInterval(
        val start: LocalTime,
        val end: LocalTime
    )

    private data class FreeWindow(
        val start: LocalTime,
        val end: LocalTime
    )

    private val weekdayNames = mapOf(
        1 to "Понедельник",
        2 to "Вторник",
        3 to "Среда",
        4 to "Четверг",
        5 to "Пятница",
        6 to "Суббота",
        7 to "Воскресенье"
    )
}