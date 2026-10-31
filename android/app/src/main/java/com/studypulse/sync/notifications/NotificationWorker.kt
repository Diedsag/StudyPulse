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
import java.time.temporal.ChronoUnit

class NotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val database = (applicationContext as StudyPulseApplication).container.database
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result {
        return try {
            createNotificationChannels()

            val now = System.currentTimeMillis()
            val nowInstant = Instant.ofEpochMilli(now)

            // Уведомления о слотах через 15 минут
            val upcomingSlots = database.studySlotDao().getUpcomingSlotsOnce(
                startMillis = now,
                endMillis = now + 15 * 60 * 1000
            )

            upcomingSlots.forEach { slotWithTask ->
                val slot = slotWithTask.slot
                val slotTime = Instant.ofEpochMilli(slot.startDateTime)
                val minutesUntil = ChronoUnit.MINUTES.between(nowInstant, slotTime)

                val intent = Intent(applicationContext, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingIntent = PendingIntent.getActivity(
                    applicationContext,
                    slot.id.toInt(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notification = NotificationCompat.Builder(applicationContext, CHANNEL_SLOTS)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("Учебный слот через $minutesUntil мин")
                    .setContentText(slotWithTask.taskTitle)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build()

                notificationManager.notify(slot.id.toInt(), notification)
            }

            // Уведомления о дедлайнах через 24 часа
            val today = LocalDate.now()
            val tomorrow = today.plusDays(1).toString()
            val urgentTasks = database.taskDao().getUrgentOnce(tomorrow, 10)

            urgentTasks.forEach { task ->
                val intent = Intent(applicationContext, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingIntent = PendingIntent.getActivity(
                    applicationContext,
                    task.externalId.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notification = NotificationCompat.Builder(applicationContext, CHANNEL_DEADLINES)
                    .setSmallIcon(android.R.drawable.ic_dialog_alert)
                    .setContentTitle("Дедлайн завтра: ${task.title}")
                    .setContentText("Осталось ${task.remainingMinutes} мин")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build()

                notificationManager.notify(task.externalId.hashCode(), notification)
            }

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val slotChannel = NotificationChannel(
                CHANNEL_SLOTS,
                "Учебные слоты",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Напоминания о предстоящих учебных слотах"
            }

            val deadlineChannel = NotificationChannel(
                CHANNEL_DEADLINES,
                "Дедлайны",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Напоминания о приближающихся дедлайнах"
            }

            notificationManager.createNotificationChannel(slotChannel)
            notificationManager.createNotificationChannel(deadlineChannel)
        }
    }

    companion object {
        const val CHANNEL_SLOTS = "slots_channel"
        const val CHANNEL_DEADLINES = "deadlines_channel"
    }
}