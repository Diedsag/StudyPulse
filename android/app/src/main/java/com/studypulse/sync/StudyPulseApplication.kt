package com.studypulse.sync

import android.app.Application
import com.studypulse.sync.di.AppContainer
import com.studypulse.sync.notifications.NotificationScheduler
import com.studypulse.sync.workers.SyncScheduler

class StudyPulseApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        NotificationScheduler.schedulePeriodicNotifications(this)
        SyncScheduler.schedulePeriodicSync(this)
    }
}