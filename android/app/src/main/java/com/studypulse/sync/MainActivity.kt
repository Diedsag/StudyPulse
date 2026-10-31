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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.studypulse.sync.ui.packages.PackagesScreen
import com.studypulse.sync.ui.plan.PlanScreen
import com.studypulse.sync.ui.schedule.ScheduleScreen
import com.studypulse.sync.ui.settings.SettingsScreen
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = topBarTitle(selectedTab))
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Text("📦") },
                    label = { Text("Packages") }
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Text("📅") },
                    label = { Text("Schedule") }
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Text("📝") },
                    label = { Text("Tasks") }
                )

                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Text("🧠") },
                    label = { Text("Plan") }
                )

                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
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
            when (selectedTab) {
                0 -> PackagesScreen()
                1 -> ScheduleScreen()
                2 -> TasksScreen(
                    onOpenPlanner = { selectedTab = 3 }
                )
                3 -> PlanScreen()
                else -> SettingsScreen()
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
        else -> "StudyPulse Settings"
    }
}