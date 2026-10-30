package com.studypulse.sync

import android.app.Application
import com.studypulse.sync.di.AppContainer

class StudyPulseApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}