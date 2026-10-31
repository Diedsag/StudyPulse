package com.studypulse.sync.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.studypulse.sync.StudyPulseApplication
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class ReminderScheduler(
    private val context: Context
) {

    private val wm = WorkManager.getInstance(context)
    private val container = (context.applicationContext as StudyPulseApplication).container

    suspend fun scheduleForToday() {
        val profile = container.profileRepository.currentProfile()
        if (!profile.notificationsEnabled || !profile.slotRemindersEnabled) return

        val now = System.currentTimeMillis()
        val dayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val dayEnd = dayStart + 24 * 60 * 60 * 1000

        val slots = container.database.studySlotDao().getSlotsForDayOnce(dayStart, dayEnd)

        slots.forEach { swt ->
            val slot = swt.slot
            if (slot.status != "PLANNED") return@forEach
            val triggerAt = slot.startDateTime - 15 * 60 * 1000
            val delay = triggerAt - now
            if (delay <= 0) return@forEach

            val request: OneTimeWorkRequest = OneTimeWorkRequestBuilder<SlotReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf("slotId" to slot.id))
                .build()

            wm.enqueueUniqueWork(
                "slot-reminder-${slot.id}",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }

    suspend fun scheduleDeadlineReminders() {
        val profile = container.profileRepository.currentProfile()
        if (!profile.notificationsEnabled || !profile.deadlineRemindersEnabled) return

        val now = System.currentTimeMillis()
        val tasks = container.database.taskDao().getActiveOnce()

        tasks.forEach { task ->
            val deadlineMillis = parseDeadlineToEndOfDay(task.deadlineDate) ?: return@forEach

            listOf(24, 3).forEach { hoursBefore ->
                val triggerAt = deadlineMillis - hoursBefore * 60 * 60 * 1000
                val delay = triggerAt - now
                if (delay <= 0) return@forEach

                val request: OneTimeWorkRequest = OneTimeWorkRequestBuilder<DeadlineReminderWorker>()
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .setInputData(workDataOf(
                        "taskId" to task.externalId,
                        "hoursBefore" to hoursBefore
                    ))
                    .build()

                wm.enqueueUniqueWork(
                    "deadline-reminder-${task.externalId}-$hoursBefore",
                    ExistingWorkPolicy.REPLACE,
                    request
                )
            }
        }
    }

    fun scheduleMorningDigest() {
        val profile = container.profileRepository.currentProfile()
        if (!profile.notificationsEnabled || !profile.morningDigestEnabled) {
            wm.cancelUniqueWork(MORNING_DIGEST_WORK)
            return
        }

        val now = LocalDateTime.now()
        val target = now.withHour(7).withMinute(0).withSecond(0).withNano(0)
        val trigger = if (now >= target) target.plusDays(1) else target
        val delay = java.time.Duration.between(now, trigger).seconds

        val request = PeriodicWorkRequestBuilder<MorningDigestWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delay, TimeUnit.SECONDS)
            .build()

        wm.enqueueUniquePeriodicWork(
            MORNING_DIGEST_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelAll() {
        wm.cancelUniqueWork(MORNING_DIGEST_WORK)
    }

    private fun parseDeadlineToEndOfDay(raw: String): Long? = try {
        LocalDate.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE)
            .atTime(23, 59)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    } catch (_: Throwable) {
        null
    }

    companion object {
        private const val MORNING_DIGEST_WORK = "study_pulse_morning_digest"
    }
}