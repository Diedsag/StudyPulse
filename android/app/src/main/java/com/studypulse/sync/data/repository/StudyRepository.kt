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
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

data class ClassOccurrence(
    val templateExternalId: String,
    val packageExternalId: String?,
    val date: String,
    val displayDate: String,
    val weekday: String,
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String,
    val subject: String,
    val lessonType: String?,
    val room: String?,
    val teacher: String?,
    val parity: String?,
    val subjectId: Long?,
    val source: String,
    val isCancelled: Boolean
)

data class DeadlineRisk(
    val taskTitle: String,
    val deadlineDate: String,
    val remainingMinutes: Int,
    val availableMinutes: Int,
    val deficitMinutes: Int
)

data class TodaySnapshot(
    val dateLabel: String,
    val greeting: String,
    val nextSlot: SlotWithTaskMaterial?,
    val todaySlots: List<SlotWithTaskMaterial>,
    val doneTodayCount: Int,
    val totalTodayCount: Int,
    val urgentTasks: List<TaskEntity>,
    val dueCardsCount: Int,
    val risks: List<DeadlineRisk>
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

    val tasks: Flow<List<TaskEntity>> = taskDao.observeActive()
    val materials: Flow<List<MaterialEntity>> = materialDao.observeActive()
    val proposedSlots: Flow<List<SlotWithTaskMaterial>> = studySlotDao.observeWithTaskMaterial("PROPOSED")
    val plannedSlots: Flow<List<SlotWithTaskMaterial>> = studySlotDao.observeWithTaskMaterial("PLANNED")

    private val zone: ZoneId get() = ZoneId.systemDefault()

    fun upcomingOccurrences(daysAhead: Int = 7): Flow<List<ClassOccurrence>> {
        return combine(
            scheduleDao.observeActive(),
            classInstanceDao.observeAll()
        ) { scheduleItems, instances ->
            buildOccurrences(scheduleItems, instances, daysAhead)
        }
    }

    suspend fun markMissedSlots(): Int = studySlotDao.markMissed(System.currentTimeMillis())

    suspend fun getTodaySnapshot(): Result<TodaySnapshot> {
        return try {
            studySlotDao.markMissed(System.currentTimeMillis())

            val now = System.currentTimeMillis()
            val today = LocalDate.now()
            val dayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
            val dayEnd = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

            val todaySlots = studySlotDao.getSlotsForDayOnce(dayStart, dayEnd)
            val nextSlot = studySlotDao.getNextSlotOnce(now)
            val doneToday = studySlotDao.countDoneForDayOnce(dayStart, dayEnd)
            val urgent = taskDao.getUrgentOnce(today.plusDays(3).toString(), 5)
            val dueCards = cardDao.countDueOnce(now)
            val risks = calculateDeadlineRisks(today)

            val hour = LocalTime.now().hour
            val greeting = when {
                hour < 6 -> "Доброй ночи"
                hour < 12 -> "Доброе утро"
                hour < 18 -> "Добрый день"
                else -> "Добрый вечер"
            }

            val dateLabel = today.format(
                DateTimeFormatter.ofPattern("d MMMM, EEEE", Locale("ru"))
            )

            Result.success(
                TodaySnapshot(
                    dateLabel = dateLabel,
                    greeting = greeting,
                    nextSlot = nextSlot,
                    todaySlots = todaySlots,
                    doneTodayCount = doneToday,
                    totalTodayCount = todaySlots.size,
                    urgentTasks = urgent,
                    dueCardsCount = dueCards,
                    risks = risks
                )
            )
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun cancelOccurrence(templateExternalId: String, date: String): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()
            database.withTransaction {
                classInstanceDao.upsert(
                    ClassInstanceEntity(
                        templateExternalId = templateExternalId,
                        date = date,
                        status = "CANCELLED",
                        updatedAt = now
                    )
                )
                val clientEventId = UUID.randomUUID().toString()
                val payloadJson = buildJsonObject {
                    put("template_external_id", templateExternalId)
                    put("date", date)
                }.toString()
                pendingEventDao.insert(
                    PendingEventEntity(
                        clientEventId = clientEventId,
                        eventType = "CLASS_CANCELLED",
                        payloadJson = payloadJson,
                        occurredAt = now,
                        createdAt = now,
                        synced = false
                    )
                )
            }
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun restoreOccurrence(templateExternalId: String, date: String): Result<Unit> {
        return try {
            database.withTransaction {
                classInstanceDao.deleteOccurrence(templateExternalId, date)
            }
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun getDueCards(packageExternalId: String?, materialExternalId: String?): Result<List<CardEntity>> {
        return try {
            val now = System.currentTimeMillis()
            val cards = when {
                !materialExternalId.isNullOrBlank() -> cardDao.getDueByMaterialOnce(materialExternalId, now)
                !packageExternalId.isNullOrBlank() -> cardDao.getDueByPackageOnce(packageExternalId, now)
                else -> cardDao.getDueAllOnce(now)
            }
            Result.success(cards)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun reviewCard(card: CardEntity, rating: Int): Result<ReviewOutcome> {
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
                        synced = false
                    )
                )
            }
            Result.success(ReviewOutcome(updatedCard.dueAt, updatedCard.intervalDays))
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun generatePlan(): Result<PlanGenerationResult> {
        return try {
            Result.success(database.withTransaction { buildPlan(clearFuturePlanned = false) })
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun replan(): Result<PlanGenerationResult> {
        return try {
            Result.success(
                database.withTransaction {
                    studySlotDao.markMissed(System.currentTimeMillis())
                    buildPlan(clearFuturePlanned = true)
                }
            )
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun acceptPlan(): Result<Int> {
        return try {
            val now = System.currentTimeMillis()
            val count = database.withTransaction {
                val proposed = studySlotDao.getAllByStatus("PROPOSED")
                val updated = studySlotDao.updateStatus("PROPOSED", "PLANNED")
                if (updated > 0) {
                    val totalMinutes = proposed.sumOf { ((it.endDateTime - it.startDateTime) / 60_000L).toInt() }
                    val clientEventId = UUID.randomUUID().toString()
                    val payloadJson = buildJsonObject {
                        put("slots_count", JsonPrimitive(updated))
                        put("total_minutes", JsonPrimitive(totalMinutes))
                    }.toString()
                    pendingEventDao.insert(
                        PendingEventEntity(
                            clientEventId = clientEventId,
                            eventType = "PLAN_ACCEPTED",
                            payloadJson = payloadJson,
                            occurredAt = now,
                            createdAt = now,
                            synced = false
                        )
                    )
                }
                updated
            }
            Result.success(count)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun completeSlot(slot: StudySlotEntity): Result<SlotCompletionResult> {
        return try {
            if (slot.status != "PLANNED") {
                return Result.failure(IllegalStateException("Слот уже не в статусе PLANNED"))
            }
            val durationMinutes = ((slot.endDateTime - slot.startDateTime) / 60_000L).toInt().coerceAtLeast(0)
            val task = taskDao.getById(slot.taskExternalId)
                ?: return Result.failure(IllegalStateException("Задание для слота не найдено"))

            val newRemaining = (task.remainingMinutes - durationMinutes).coerceAtLeast(0)
            val newStatus = if (newRemaining == 0) "DONE" else "IN_PROGRESS"
            val now = System.currentTimeMillis()

            val slotEventId = UUID.randomUUID().toString()
            val slotPayload = buildJsonObject {
                put("package_external_id", slot.packageExternalId)
                put("task_external_id", slot.taskExternalId)
                put("slot_id", JsonPrimitive(slot.id))
                put("duration_minutes", JsonPrimitive(durationMinutes))
            }.toString()

            val taskCompleted = newStatus == "DONE"
            val taskEventId = if (taskCompleted) UUID.randomUUID().toString() else null
            val taskPayload = if (taskCompleted) {
                buildJsonObject {
                    put("package_external_id", slot.packageExternalId)
                    put("task_external_id", slot.taskExternalId)
                    put("effort_minutes", JsonPrimitive(task.effortMinutes))
                }.toString()
            } else null

            database.withTransaction {
                studySlotDao.markStatus(slot.id, "DONE")
                taskDao.updateStatus(task.externalId, newStatus, newRemaining)
                pendingEventDao.insert(
                    PendingEventEntity(
                        clientEventId = slotEventId,
                        eventType = "SLOT_COMPLETED",
                        payloadJson = slotPayload,
                        occurredAt = now,
                        createdAt = now,
                        synced = false
                    )
                )
                if (taskCompleted && taskEventId != null && taskPayload != null) {
                    pendingEventDao.insert(
                        PendingEventEntity(
                            clientEventId = taskEventId,
                            eventType = "TASK_COMPLETED",
                            payloadJson = taskPayload,
                            occurredAt = now,
                            createdAt = now,
                            synced = false
                        )
                    )
                }
            }
            Result.success(SlotCompletionResult(durationMinutes, newRemaining, taskCompleted))
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    private suspend fun buildPlan(clearFuturePlanned: Boolean): PlanGenerationResult {
        val now = System.currentTimeMillis()
        val nowTime = LocalTime.now()
        val today = LocalDate.now()

        studySlotDao.deleteByStatus("PROPOSED")
        studySlotDao.deleteByStatus("MISSED")
        if (clearFuturePlanned) {
            studySlotDao.deleteFuturePlanned(now)
        }

        val dayStartBase = LocalTime.of(8, 0)
        val dayEnd = LocalTime.of(22, 0)
        val minSlotMinutes = 30
        val maxDailyLoadMinutes = 180
        val horizonDays = 3

        val scheduleItems = scheduleDao.getAllActiveOnce()
        val instances = classInstanceDao.getAllOnce()
        val originalTasks = taskDao.getActiveOnce()
        val allMaterials = materialDao.getAllActiveOnce()

        val committedSlots = studySlotDao.getCommittedOnce().filter {
            (it.status == "PLANNED" || it.status == "IN_PROGRESS") && it.startDateTime >= now
        }
        val plannedMinutesByTask = committedSlots
            .groupBy { it.taskExternalId }
            .mapValues { entry -> entry.value.sumOf { ((it.endDateTime - it.startDateTime) / 60_000L).toInt() } }

        val materialByPackage = allMaterials
            .filter { !it.packageExternalId.isNullOrBlank() }
            .groupBy { it.packageExternalId!! }
            .mapValues { entry -> entry.value.minByOrNull { it.title } }
        val materialBySubject = allMaterials
            .filter { it.subjectId != null }
            .groupBy { it.subjectId!! }
            .mapValues { entry -> entry.value.minByOrNull { it.title } }

        val cancelledKeys = instances
            .filter { it.status == "CANCELLED" }
            .map { it.templateExternalId to it.date }
            .toSet()

        val remainingByTaskId = originalTasks.associate { it.externalId to it.remainingMinutes }.toMutableMap()
        val proposedSlots = mutableListOf<StudySlotEntity>()

        for (offset in 0 until horizonDays) {
            val date = today.plusDays(offset.toLong())
            val isoDate = date.toString()
            val dayOfWeek = date.dayOfWeek.value

            val effectiveDayStart = if (offset == 0 && nowTime > dayStartBase) nowTime else dayStartBase
            if (effectiveDayStart >= dayEnd) continue

            val dayStartMillis = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val dayEndMillis = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

            val scheduleIntervals = scheduleItems
                .filter { it.dayOfWeek == dayOfWeek && (it.externalId to isoDate) !in cancelledKeys }
                .mapNotNull { item ->
                    val s = parseTimeOrNull(item.startTime) ?: return@mapNotNull null
                    val e = parseTimeOrNull(item.endTime) ?: return@mapNotNull null
                    val cs = maxOf(s, effectiveDayStart)
                    val ce = minOf(e, dayEnd)
                    if (cs < ce) TimeInterval(cs, ce) else null
                }

            val slotIntervals = committedSlots
                .filter { it.startDateTime in dayStartMillis until dayEndMillis }
                .mapNotNull { slot ->
                    val s = Instant.ofEpochMilli(slot.startDateTime).atZone(zone).toLocalTime()
                    val e = Instant.ofEpochMilli(slot.endDateTime).atZone(zone).toLocalTime()
                    val cs = maxOf(s, effectiveDayStart)
                    val ce = minOf(e, dayEnd)
                    if (cs < ce) TimeInterval(cs, ce) else null
                }

            val merged = mergeIntervals((scheduleIntervals + slotIntervals).sortedBy { it.start })
            val freeWindows = buildFreeWindows(effectiveDayStart, dayEnd, merged).toMutableList()

            var dailyLoadLeft = maxDailyLoadMinutes

            for (task in originalTasks) {
                val alreadyPlanned = plannedMinutesByTask[task.externalId] ?: 0
                var taskRemaining = (remainingByTaskId[task.externalId] ?: 0) - alreadyPlanned
                if (taskRemaining <= 0) continue

                while (taskRemaining > 0 && dailyLoadLeft > 0 && freeWindows.isNotEmpty()) {
                    val windowIndex = freeWindows.indexOfFirst { window ->
                        val d = durationMinutes(window.start, window.end)
                        d >= minSlotMinutes || (taskRemaining < minSlotMinutes && d >= taskRemaining)
                    }
                    if (windowIndex < 0) break

                    val window = freeWindows[windowIndex]
                    val windowDuration = durationMinutes(window.start, window.end)
                    val chunkMinutes = minOf(taskRemaining, windowDuration, dailyLoadLeft)
                    if (chunkMinutes <= 0) {
                        freeWindows.removeAt(windowIndex)
                        continue
                    }
                    if (chunkMinutes < minSlotMinutes && taskRemaining >= minSlotMinutes) {
                        if (dailyLoadLeft < minSlotMinutes || windowDuration < minSlotMinutes) {
                            if (windowDuration < minSlotMinutes) freeWindows.removeAt(windowIndex)
                            break
                        }
                    }

                    val startMillis = toEpochMillis(date, window.start)
                    val endMillis = toEpochMillis(date, window.start.plusMinutes(chunkMinutes.toLong()))
                    val linkedMaterial = if (!task.packageExternalId.isNullOrBlank()) {
                        materialByPackage[task.packageExternalId]
                    } else {
                        task.subjectId?.let { materialBySubject[it] }
                    }

                    proposedSlots += StudySlotEntity(
                        taskExternalId = task.externalId,
                        packageExternalId = task.packageExternalId.orEmpty(),
                        startDateTime = startMillis,
                        endDateTime = endMillis,
                        status = "PROPOSED",
                        materialExternalId = linkedMaterial?.externalId,
                        createdAt = now
                    )

                    taskRemaining -= chunkMinutes
                    dailyLoadLeft -= chunkMinutes
                    remainingByTaskId[task.externalId] = (remainingByTaskId[task.externalId] ?: 0) - chunkMinutes

                    val newStart = window.start.plusMinutes(chunkMinutes.toLong())
                    if (newStart >= window.end) freeWindows.removeAt(windowIndex)
                    else freeWindows[windowIndex] = FreeWindow(newStart, window.end)
                }
            }
        }

        studySlotDao.insertAll(proposedSlots)

        val warnings = mutableListOf<String>()
        val risks = calculateDeadlineRisks(today)
        if (originalTasks.isEmpty()) {
            warnings += "Нет активных заданий для планирования."
        }
        risks.forEach { risk ->
            warnings += "Риск просрочки «${risk.taskTitle}»: не хватает ${risk.deficitMinutes} мин до ${formatDate(risk.deadlineDate)}."
        }

        return PlanGenerationResult(proposedSlots.size, warnings)
    }

    private suspend fun calculateDeadlineRisks(today: LocalDate): List<DeadlineRisk> {
        val now = System.currentTimeMillis()
        val nowTime = LocalTime.now()
        val activeTasks = taskDao.getActiveOnce()
        if (activeTasks.isEmpty()) return emptyList()

        val scheduleItems = scheduleDao.getAllActiveOnce()
        val instances = classInstanceDao.getAllOnce()
        val cancelledKeys = instances.filter { it.status == "CANCELLED" }
            .map { it.templateExternalId to it.date }.toSet()
        val committedSlots = studySlotDao.getCommittedOnce().filter {
            (it.status == "PLANNED" || it.status == "IN_PROGRESS") && it.startDateTime >= now
        }

        val dayStartBase = LocalTime.of(8, 0)
        val dayEnd = LocalTime.of(22, 0)
        val maxDailyLoad = 180
        val risks = mutableListOf<DeadlineRisk>()

        for (task in activeTasks) {
            val deadline = try {
                LocalDate.parse(task.deadlineDate)
            } catch (_: Throwable) {
                continue
            }
            if (deadline < today) {
                risks += DeadlineRisk(task.title, task.deadlineDate, task.remainingMinutes, 0, task.remainingMinutes)
                continue
            }

            var available = 0
            var cursor = today
            while (!cursor.isAfter(deadline)) {
                val effStart = if (cursor == today && nowTime > dayStartBase) nowTime else dayStartBase
                if (effStart < dayEnd) {
                    val isoDate = cursor.toString()
                    val dayOfWeek = cursor.dayOfWeek.value
                    val dayStartMillis = cursor.atStartOfDay(zone).toInstant().toEpochMilli()
                    val dayEndMillis = cursor.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

                    val busySchedule = scheduleItems
                        .filter { it.dayOfWeek == dayOfWeek && (it.externalId to isoDate) !in cancelledKeys }
                        .sumOf { item ->
                            val s = parseTimeOrNull(item.startTime) ?: return@sumOf 0
                            val e = parseTimeOrNull(item.endTime) ?: return@sumOf 0
                            val cs = maxOf(s, effStart); val ce = minOf(e, dayEnd)
                            if (cs < ce) durationMinutes(cs, ce) else 0
                        }
                    val busySlots = committedSlots
                        .filter { it.startDateTime in dayStartMillis until dayEndMillis }
                        .sumOf { slot ->
                            val s = Instant.ofEpochMilli(slot.startDateTime).atZone(zone).toLocalTime()
                            val e = Instant.ofEpochMilli(slot.endDateTime).atZone(zone).toLocalTime()
                            val cs = maxOf(s, effStart); val ce = minOf(e, dayEnd)
                            if (cs < ce) durationMinutes(cs, ce) else 0
                        }
                    val totalWindow = durationMinutes(effStart, dayEnd)
                    val free = (totalWindow - busySchedule - busySlots).coerceAtLeast(0)
                    available += minOf(free, maxDailyLoad)
                }
                cursor = cursor.plusDays(1)
            }

            val deficit = task.remainingMinutes - available
            if (deficit > 0) {
                risks += DeadlineRisk(task.title, task.deadlineDate, task.remainingMinutes, available, deficit)
            }
        }
        return risks
    }

    private fun buildOccurrences(
        scheduleItems: List<ScheduleItemEntity>,
        instances: List<ClassInstanceEntity>,
        daysAhead: Int
    ): List<ClassOccurrence> {
        val cancelledKeys = instances.filter { it.status == "CANCELLED" }
            .map { it.templateExternalId to it.date }.toSet()
        val today = LocalDate.now()
        val shortFmt = DateTimeFormatter.ofPattern("dd.MM")
        val result = mutableListOf<ClassOccurrence>()

        for (offset in 0 until daysAhead) {
            val date = today.plusDays(offset.toLong())
            val isoDate = date.toString()
            val dayOfWeek = date.dayOfWeek.value
            val displayDate = when (offset) {
                0 -> "Сегодня"
                1 -> "Завтра"
                else -> date.format(shortFmt)
            }
            val weekday = weekdayNames[dayOfWeek].orEmpty()
            scheduleItems.filter { it.dayOfWeek == dayOfWeek }.forEach { item ->
                val isCancelled = (item.externalId to isoDate) in cancelledKeys
                result += ClassOccurrence(
                    templateExternalId = item.externalId,
                    packageExternalId = item.packageExternalId,
                    date = isoDate,
                    displayDate = displayDate,
                    weekday = weekday,
                    dayOfWeek = item.dayOfWeek,
                    startTime = item.startTime,
                    endTime = item.endTime,
                    subject = item.subject,
                    lessonType = item.lessonType,
                    room = item.room,
                    teacher = item.teacher,
                    parity = item.parity,
                    subjectId = item.subjectId,
                    source = item.source,
                    isCancelled = isCancelled
                )
            }
        }
        return result.sortedWith(compareBy({ it.date }, { it.startTime }))
    }

    private fun mergeIntervals(input: List<TimeInterval>): List<TimeInterval> {
        if (input.isEmpty()) return emptyList()
        val sorted = input.sortedBy { it.start }
        val merged = mutableListOf(sorted.first())
        for (next in sorted.drop(1)) {
            val last = merged.last()
            if (next.start <= last.end) {
                merged[merged.lastIndex] = TimeInterval(last.start, maxOf(last.end, next.end))
            } else {
                merged += next
            }
        }
        return merged
    }

    private fun buildFreeWindows(dayStart: LocalTime, dayEnd: LocalTime, occupied: List<TimeInterval>): List<FreeWindow> {
        val windows = mutableListOf<FreeWindow>()
        var cursor = dayStart
        for (interval in occupied) {
            if (cursor < interval.start) windows += FreeWindow(cursor, interval.start)
            if (interval.end > cursor) cursor = interval.end
        }
        if (cursor < dayEnd) windows += FreeWindow(cursor, dayEnd)
        return windows.filter { durationMinutes(it.start, it.end) > 0 }
    }

    private fun parseTimeOrNull(value: String): LocalTime? = try {
        LocalTime.parse(value)
    } catch (_: Throwable) {
        null
    }

    private fun durationMinutes(start: LocalTime, end: LocalTime): Int =
        Duration.between(start, end).toMinutes().toInt()

    private fun toEpochMillis(date: LocalDate, time: LocalTime): Long =
        date.atTime(time).atZone(zone).toInstant().toEpochMilli()

    private fun formatDate(raw: String): String = try {
        LocalDate.parse(raw).format(DateTimeFormatter.ofPattern("dd.MM"))
    } catch (_: Throwable) {
        raw
    }

    private data class TimeInterval(val start: LocalTime, val end: LocalTime)
    private data class FreeWindow(val start: LocalTime, val end: LocalTime)

    private val weekdayNames = mapOf(
        1 to "Понедельник", 2 to "Вторник", 3 to "Среда", 4 to "Четверг",
        5 to "Пятница", 6 to "Суббота", 7 to "Воскресенье"
    )
}