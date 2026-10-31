package com.studypulse.sync.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.studypulse.sync.MainActivity
import com.studypulse.sync.StudyPulseApplication
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class StudyPulseWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = (context.applicationContext as StudyPulseApplication).container

        val profile = container.profileRepository.currentProfile()
        val now = System.currentTimeMillis()

        val nextSlot = container.database.studySlotDao().getNextSlotOnce(now)
        val dueCards = container.database.cardDao().countDueOnce(now)

        val tomorrow = LocalDate.now().plusDays(1).toString()
        val urgentCount = container.database.taskDao().getUrgentOnce(tomorrow, 10).size

        provideContent {
            GlanceTheme {
                WidgetContent(
                    displayName = profile.displayName,
                    taskTitle = nextSlot?.taskTitle,
                    startEpochMillis = nextSlot?.slot?.startDateTime,
                    dueCards = dueCards,
                    urgentCount = urgentCount
                )
            }
        }
    }

    @Composable
    private fun WidgetContent(
        displayName: String,
        taskTitle: String?,
        startEpochMillis: Long?,
        dueCards: Int,
        urgentCount: Int
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(14.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "StudyPulse",
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.primary
                )
            )

            if (displayName.isNotBlank()) {
                Text(
                    text = "Привет, $displayName",
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = GlanceTheme.colors.onSurface
                    )
                )
            }

            Text(
                text = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM")),
                style = TextStyle(
                    fontSize = 12.sp,
                    color = GlanceTheme.colors.onSurface
                )
            )

            if (taskTitle != null && startEpochMillis != null) {
                Text(
                    text = "Ближайшее: $taskTitle",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = GlanceTheme.colors.onSurface
                    )
                )

                Text(
                    text = formatTime(startEpochMillis),
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = GlanceTheme.colors.primary
                    )
                )
            } else {
                Text(
                    text = "Нет ближайших слотов",
                    style = TextStyle(
                        fontSize = 13.sp,
                        color = GlanceTheme.colors.onSurface
                    )
                )
            }

            if (dueCards > 0) {
                Text(
                    text = "Карточек: $dueCards",
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = GlanceTheme.colors.onSurface
                    )
                )
            }

            if (urgentCount > 0) {
                Text(
                    text = "Срочных заданий: $urgentCount",
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = GlanceTheme.colors.primary
                    )
                )
            }
        }
    }

    private fun formatTime(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
            .toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
}

class StudyPulseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StudyPulseWidget()
}