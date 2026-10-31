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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studypulse.sync.data.profile.StudentProfile
import com.studypulse.sync.ui.focus.FocusTimerScreen
import com.studypulse.sync.ui.materials.MaterialsScreen
import com.studypulse.sync.ui.ocr.MockOcrScreen
import com.studypulse.sync.ui.onboarding.OnboardingScreen
import com.studypulse.sync.ui.packages.PackagesScreen
import com.studypulse.sync.ui.plan.PlanScreen
import com.studypulse.sync.ui.qa.QaScreen
import com.studypulse.sync.ui.review.ReviewRequest
import com.studypulse.sync.ui.review.ReviewScreen
import com.studypulse.sync.ui.schedule.ScheduleScreen
import com.studypulse.sync.ui.schedule.import.ScheduleImportScreen
import com.studypulse.sync.ui.settings.SettingsScreen
import com.studypulse.sync.ui.stats.StatsScreen
import com.studypulse.sync.ui.subjects.SubjectScreen
import com.studypulse.sync.ui.tasks.TasksScreen
import com.studypulse.sync.ui.theme.StudyPulseTheme
import com.studypulse.sync.ui.today.TodayScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            StudyPulseTheme {
                StudyPulseRoot()
            }
        }
    }
}

@Composable
fun StudyPulseRoot() {
    val application = LocalContext.current.applicationContext as StudyPulseApplication

    val profile by application.container.profileRepository.profile
        .collectAsStateWithLifecycle(initialValue = StudentProfile())

    LaunchedEffect(profile.onboardingCompleted, profile.autoPlanEnabled) {
        if (profile.onboardingCompleted && profile.autoPlanEnabled) {
            application.container.autoPlannerCoordinator.ensureTodayPlan()
        }
    }

    if (!profile.onboardingCompleted) {
        OnboardingScreen()
    } else {
        StudyPulseApp()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyPulseApp() {
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var reviewRequest by remember { mutableStateOf<ReviewRequest?>(null) }
    var focusSlotId by remember { mutableStateOf<Long?>(null) }
    var qaMaterialId by remember { mutableStateOf<String?>(null) }
    var showScheduleImport by remember { mutableStateOf(false) }
    var showMockOcr by remember { mutableStateOf(false) }

    val tabs = listOf(
        "Сегодня",
        "Пакеты",
        "Предметы",
        "Расписание",
        "Задания",
        "План",
        "Материалы",
        "Статистика",
        "Настройки"
    )

    fun clearOverlays() {
        reviewRequest = null
        focusSlotId = null
        qaMaterialId = null
        showScheduleImport = false
        showMockOcr = false
    }

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

            when {
                currentReview != null -> {
                    ReviewScreen(
                        request = currentReview,
                        onExit = { reviewRequest = null }
                    )
                }

                currentFocusSlotId != null -> {
                    FocusTimerScreen(
                        slotId = currentFocusSlotId,
                        onExit = { focusSlotId = null }
                    )
                }

                currentQaMaterialId != null -> {
                    QaScreen(
                        materialExternalId = currentQaMaterialId,
                        onExit = { qaMaterialId = null }
                    )
                }

                showScheduleImport -> {
                    ScheduleImportScreen(
                        onExit = { showScheduleImport = false }
                    )
                }

                showMockOcr -> {
                    MockOcrScreen(
                        onExit = { showMockOcr = false }
                    )
                }

                else -> {
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
                            0 -> TodayScreen(
                                onOpenFocus = { slotId -> focusSlotId = slotId },
                                onStartReview = { request -> reviewRequest = request },
                                onGoToPlan = { selectedTab = 5 },
                                onGoToTasks = { selectedTab = 4 }
                            )

                            1 -> PackagesScreen()

                            2 -> SubjectScreen()

                            3 -> ScheduleScreen(
                                onOpenImport = { showScheduleImport = true }
                            )

                            4 -> TasksScreen(
                                onOpenPlanner = {
                                    selectedTab = 5
                                    clearOverlays()
                                }
                            )

                            5 -> PlanScreen(
                                onStartReview = { request -> reviewRequest = request },
                                onOpenFocus = { slotId -> focusSlotId = slotId },
                                onOpenQa = { materialId -> qaMaterialId = materialId }
                            )

                            6 -> MaterialsScreen(
                                onStartReview = { request -> reviewRequest = request },
                                onAsk = { materialId -> qaMaterialId = materialId },
                                onOpenMockOcr = { showMockOcr = true }
                            )

                            7 -> StatsScreen()

                            else -> SettingsScreen()
                        }
                    }
                }
            }
        }
    }
}