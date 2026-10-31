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
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class StudyPulseWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = (context.applicationContext as StudyPulseApplication).container
        val repository = container.studyRepository

        val todaySnapshot = repository.getTodaySnapshot()

        provideContent {
            GlanceTheme {
                WidgetContent(todaySnapshot.getOrNull())
            }
        }
    }

    @Composable
    private fun WidgetContent(snapshot: com.studypulse.sync.data.repository.TodaySnapshot?) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(16.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "StudyPulse",
                style = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.primary
                )
            )

            if (snapshot != null) {
                Text(
                    text = snapshot.dateLabel,
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = GlanceTheme.colors.onSurfaceVariant
                    )
                )

                snapshot.nextSlot?.let { slot ->
                    val time = formatTime(slot.slot.startDateTime)
                    Text(
                        text = "Ближайший слот: $time",
                        style = TextStyle(
                            fontSize = 14.sp,
                            color = GlanceTheme.colors.onSurface
                        )
                    )
                    Text(
                        text = slot.taskTitle,
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = GlanceTheme.colors.onSurfaceVariant
                        )
                    )
                } ?: run {
                    Text(
                        text = "Нет ближайших слотов",
                        style = TextStyle(
                            fontSize = 14.sp,
                            color = GlanceTheme.colors.onSurfaceVariant
                        )
                    )
                }

                if (snapshot.dueCardsCount > 0) {
                    Text(
                        text = "Карточек к повторению: ${snapshot.dueCardsCount}",
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = GlanceTheme.colors.tertiary
                        )
                    )
                }
            } else {
                Text(
                    text = "Откройте приложение",
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = GlanceTheme.colors.onSurfaceVariant
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