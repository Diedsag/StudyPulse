package com.studypulse.sync

import android.app.Application
import com.studypulse.sync.di.AppContainer
import com.studypulse.sync.workers.SyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class StudyPulseApplication : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        appScope.launch {
            runCatching {
                container.reminderScheduler.scheduleMorningDigest()
            }
        }

        SyncScheduler.schedulePeriodicSync(this)
    }
}