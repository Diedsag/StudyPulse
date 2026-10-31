package com.studypulse.sync

import android.app.Application
import com.studypulse.sync.di.AppContainer
import com.studypulse.sync.notifications.ReminderScheduler
import com.studypulse.sync.workers.SyncScheduler

class StudyPulseApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        container.reminderScheduler.scheduleMorningDigest()
        SyncScheduler.schedulePeriodicSync(this)
    }
}