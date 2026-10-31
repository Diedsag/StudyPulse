package com.studypulse.sync.data.repository

import com.studypulse.sync.data.local.AppDatabase
import com.studypulse.sync.data.local.entity.ClassInstanceEntity
import com.studypulse.sync.data.local.entity.ScheduleItemEntity
import com.studypulse.sync.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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

class StudyRepository(
    private val database: AppDatabase
) {

    private val scheduleDao = database.scheduleDao()
    private val taskDao = database.taskDao()
    private val classInstanceDao = database.classInstanceDao()

    val tasks: Flow<List<TaskEntity>> =
        taskDao.observeDownloaded()

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