package com.studypulse.sync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.studypulse.sync.ui.settings.SettingsScreen
import com.studypulse.sync.ui.theme.StudyPulseTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            StudyPulseTheme {
                SettingsScreen()
            }
        }
    }
}