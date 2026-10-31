package com.studypulse.sync.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.studypulse.sync.MainActivity
import com.studypulse.sync.StudyPulseApplication
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object ReminderChannels {
    const val SLOTS = "slots_channel"
    const val DEADLINES = "deadlines_channel"
    const val DIGEST = "digest_channel"

    fun ensure(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(SLOTS, "Учебные слоты", NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = "Напоминания о предстоящих занятиях" }
            )
            nm.createNotificationChannel(
                NotificationChannel(DEADLINES, "Дедлайны", NotificationManager.IMPORTANCE_HIGH)
                    .apply { description = "Напоминания о приближающихся дедлайнах" }
            )
            nm.createNotificationChannel(
                NotificationChannel(DIGEST, "Утренний дайджест", NotificationManager.IMPORTANCE_LOW)
                    .apply { description = "Сводка плана на день" }
            )
        }
    }
}

private fun isInQuietHours(profile: com.studypulse.sync.data.profile.StudentProfile): Boolean {
    if (!profile.quietHoursEnabled) return false
    val hour = LocalTime.now().hour
    val start = profile.quietHoursStart
    val end = profile.quietHoursEnd
    return if (start == end) false
    else if (start < end) hour in start until end
    else hour >= start || hour < end
}

private fun buildIntent(context: Context): PendingIntent {
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    return PendingIntent.getActivity(
        context, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

class SlotReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val slotId = inputData.getLong("slotId", -1L)
        if (slotId == -1L) return Result.success()

        val container = (applicationContext as StudyPulseApplication).container
        val profile = container.profileRepository.currentProfile()

        if (!profile.notificationsEnabled || !profile.slotRemindersEnabled) return Result.success()
        if (isInQuietHours(profile)) return Result.success()

        val slot = container.database.studySlotDao().getByIdOnce(slotId) ?: return Result.success()
        if (slot.status != "PLANNED") return Result.success()

        val minutesUntil = ((slot.startDateTime - System.currentTimeMillis()) / 60_000L).toInt()
        if (minutesUntil < 0) return Result.success()

        ReminderChannels.ensure(applicationContext)
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(applicationContext, ReminderChannels.SLOTS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Занятие через $minutesUntil мин")
            .setContentText("Начало в ${formatTime(slot.startDateTime)}")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(buildIntent(applicationContext))
            .setAutoCancel(true)
            .build()

        nm.notify(slotId.toInt(), notification)
        return Result.success()
    }

    private fun formatTime(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
            .toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
}

class DeadlineReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getString("taskId") ?: return Result.success()
        val hoursBefore = inputData.getInt("hoursBefore", 24)

        val container = (applicationContext as StudyPulseApplication).container
        val profile = container.profileRepository.currentProfile()

        if (!profile.notificationsEnabled || !profile.deadlineRemindersEnabled) return Result.success()
        if (isInQuietHours(profile)) return Result.success()

        val task = container.database.taskDao().getById(taskId) ?: return Result.success()
        if (task.status == "DONE") return Result.success()

        ReminderChannels.ensure(applicationContext)
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val title = if (hoursBefore >= 24) "Дедлайн завтра: ${task.title}"
        else "Дедлайн через ${hoursBefore} ч: ${task.title}"

        val notification = NotificationCompat.Builder(applicationContext, ReminderChannels.DEADLINES)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText("Предмет: ${task.subject ?: "-"}, осталось ${task.remainingMinutes} мин")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(buildIntent(applicationContext))
            .setAutoCancel(true)
            .build()

        nm.notify("${taskId}_$hoursBefore".hashCode(), notification)
        return Result.success()
    }
}

class MorningDigestWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as StudyPulseApplication).container
        val profile = container.profileRepository.currentProfile()

        if (!profile.notificationsEnabled || !profile.morningDigestEnabled) return Result.success()
        if (isInQuietHours(profile)) return Result.success()

        val now = System.currentTimeMillis()
        val dayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val dayEnd = dayStart + 24 * 60 * 60 * 1000

        val slots = container.database.studySlotDao().getSlotsForDayOnce(dayStart, dayEnd)
        if (slots.isEmpty()) return Result.success()

        ReminderChannels.ensure(applicationContext)
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val nextSlot = slots.firstOrNull { it.slot.endDateTime >= now }
        val text = if (nextSlot != null) {
            "${slots.size} слот(ов) на сегодня. Ближайший: ${nextSlot.taskTitle}"
        } else {
            "${slots.size} слот(ов) на сегодня."
        }

        val notification = NotificationCompat.Builder(applicationContext, ReminderChannels.DIGEST)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("План на сегодня")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(buildIntent(applicationContext))
            .setAutoCancel(true)
            .build()

        nm.notify("digest_${LocalDate.now()}".hashCode(), notification)
        return Result.success()
    }
}