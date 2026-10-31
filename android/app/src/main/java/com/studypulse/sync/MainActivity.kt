package com.studypulse.sync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.studypulse.sync.ui.materials.MaterialsScreen
import com.studypulse.sync.ui.packages.PackagesScreen
import com.studypulse.sync.ui.plan.PlanScreen
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (reviewRequest != null) {
                            "StudyPulse Review"
                        } else {
                            topBarTitle(selectedTab)
                        }
                    )
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0 && reviewRequest == null,
                    onClick = {
                        selectedTab = 0
                        reviewRequest = null
                    },
                    icon = { Text("📦") },
                    label = { Text("Packages") }
                )

                NavigationBarItem(
                    selected = selectedTab == 1 && reviewRequest == null,
                    onClick = {
                        selectedTab = 1
                        reviewRequest = null
                    },
                    icon = { Text("📅") },
                    label = { Text("Schedule") }
                )

                NavigationBarItem(
                    selected = selectedTab == 2 && reviewRequest == null,
                    onClick = {
                        selectedTab = 2
                        reviewRequest = null
                    },
                    icon = { Text("📝") },
                    label = { Text("Tasks") }
                )

                NavigationBarItem(
                    selected = selectedTab == 3 && reviewRequest == null,
                    onClick = {
                        selectedTab = 3
                        reviewRequest = null
                    },
                    icon = { Text("🧠") },
                    label = { Text("Plan") }
                )

                NavigationBarItem(
                    selected = selectedTab == 4 && reviewRequest == null,
                    onClick = {
                        selectedTab = 4
                        reviewRequest = null
                    },
                    icon = { Text("📚") },
                    label = { Text("Materials") }
                )

                NavigationBarItem(
                    selected = selectedTab == 5 && reviewRequest == null,
                    onClick = {
                        selectedTab = 5
                        reviewRequest = null
                    },
                    icon = { Text("📊") },
                    label = { Text("Stats") }
                )

                NavigationBarItem(
                    selected = selectedTab == 6 && reviewRequest == null,
                    onClick = {
                        selectedTab = 6
                        reviewRequest = null
                    },
                    icon = { Text("⚙️") },
                    label = { Text("Settings") }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            val currentReview = reviewRequest

            if (currentReview != null) {
                ReviewScreen(
                    request = currentReview,
                    onExit = { reviewRequest = null }
                )
            } else {
                when (selectedTab) {
                    0 -> PackagesScreen()

                    1 -> ScheduleScreen()

                    2 -> TasksScreen(
                        onOpenPlanner = {
                            selectedTab = 3
                            reviewRequest = null
                        }
                    )

                    3 -> PlanScreen(
                        onStartReview = { request ->
                            reviewRequest = request
                        }
                    )

                    4 -> MaterialsScreen(
                        onStartReview = { request ->
                            reviewRequest = request
                        }
                    )

                    5 -> StatsScreen()

                    else -> SettingsScreen()
                }
            }
        }
    }
}

private fun topBarTitle(tab: Int): String {
    return when (tab) {
        0 -> "StudyPulse Packages"
        1 -> "StudyPulse Schedule"
        2 -> "StudyPulse Tasks"
        3 -> "StudyPulse Plan"
        4 -> "StudyPulse Materials"
        5 -> "StudyPulse Stats"
        else -> "StudyPulse Settings"
    }
}