package com.studypulse.sync.di

import android.content.Context
import com.studypulse.sync.core.datastore.SettingsRepository
import com.studypulse.sync.core.datastore.settingsDataStore
import com.studypulse.sync.core.network.ApiClient
import com.studypulse.sync.core.network.ApiService

class AppContainer(context: Context) {

    val settingsRepository: SettingsRepository =
        SettingsRepository(context.settingsDataStore)

    fun apiService(baseUrl: String): ApiService =
        ApiClient.create(baseUrl)
}