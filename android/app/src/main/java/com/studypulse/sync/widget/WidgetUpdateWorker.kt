package com.studypulse.sync.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class WidgetUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val manager = GlanceAppWidgetManager(applicationContext)
            val widget = StudyPulseWidget()

            manager.getGlanceIds(StudyPulseWidget::class.java).forEach { glanceId ->
                widget.update(applicationContext, glanceId)
            }

            Result.success()
        } catch (_: Throwable) {
            Result.success()
        }
    }
}