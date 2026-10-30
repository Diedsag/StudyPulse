package com.studypulse.sync.di

import android.content.Context
import com.studypulse.sync.core.datastore.SettingsRepository
import com.studypulse.sync.core.datastore.settingsDataStore
import com.studypulse.sync.core.network.ApiClient
import com.studypulse.sync.core.network.ApiService
import com.studypulse.sync.data.local.AppDatabase
import com.studypulse.sync.data.repository.PackageRepository
import com.studypulse.sync.data.repository.PackageRepositoryImpl

class AppContainer(context: Context) {

    val settingsRepository: SettingsRepository =
        SettingsRepository(context.settingsDataStore)

    val database: AppDatabase =
        AppDatabase.getInstance(context)

    private val apiFactory: (String) -> ApiService = { baseUrl ->
        ApiClient.create(baseUrl)
    }

    val packageRepository: PackageRepository =
        PackageRepositoryImpl(
            database = database,
            settingsRepository = settingsRepository,
            apiFactory = apiFactory
        )

    fun apiService(baseUrl: String): ApiService =
        apiFactory(baseUrl)
}