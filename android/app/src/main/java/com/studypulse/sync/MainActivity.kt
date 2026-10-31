package com.studypulse.sync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.studypulse.sync.ui.focus.FocusTimerScreen
import com.studypulse.sync.ui.materials.MaterialsScreen
import com.studypulse.sync.ui.packages.PackagesScreen
import com.studypulse.sync.ui.plan.PlanScreen
import com.studypulse.sync.ui.qa.QaScreen
import com.studypulse.sync.ui.review.ReviewRequest
import com.studypulse.sync.ui.review.ReviewScreen
import com.studypulse.sync.ui.schedule.ScheduleScreen
import com.studypulse.sync.ui.settings.SettingsScreen
import com.studypulse.sync.ui.stats.StatsScreen
import com.studypulse.sync.ui.tasks.TasksScreen
import com.studypulse.sync.ui.theme.StudyPulseTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            StudyPulseTheme {
                StudyPulseApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyPulseApp() {
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var reviewRequest by remember { mutableStateOf<ReviewRequest?>(null) }
    var focusSlotId by remember { mutableStateOf<Long?>(null) }
    var qaMaterialId by remember { mutableStateOf<String?>(null) }

    val tabs = listOf(
        "Пакеты",
        "Расписание",
        "Задания",
        "План",
        "Материалы",
        "Статистика",
        "Настройки"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "StudyPulse",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            val currentReview = reviewRequest
            val currentFocusSlotId = focusSlotId
            val currentQaMaterialId = qaMaterialId

            if (currentReview != null) {
                ReviewScreen(
                    request = currentReview,
                    onExit = { reviewRequest = null }
                )
            } else if (currentFocusSlotId != null) {
                FocusTimerScreen(
                    slotId = currentFocusSlotId,
                    onExit = { focusSlotId = null }
                )
            } else if (currentQaMaterialId != null) {
                QaScreen(
                    materialExternalId = currentQaMaterialId,
                    onExit = { qaMaterialId = null }
                )
            } else {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 0.dp,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            },
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    when (selectedTab) {
                        0 -> PackagesScreen()

                        1 -> ScheduleScreen()

                        2 -> TasksScreen(
                            onOpenPlanner = {
                                selectedTab = 3
                                reviewRequest = null
                                focusSlotId = null
                                qaMaterialId = null
                            }
                        )

                        3 -> PlanScreen(
                            onStartReview = { request ->
                                reviewRequest = request
                            },
                            onOpenFocus = { slotId ->
                                focusSlotId = slotId
                            }
                        )

                        4 -> MaterialsScreen(
                            onStartReview = { request ->
                                reviewRequest = request
                            },
                            onAsk = { materialId ->
                                qaMaterialId = materialId
                            }
                        )

                        5 -> StatsScreen()

                        else -> SettingsScreen()
                    }
                }
            }
        }
    }
}