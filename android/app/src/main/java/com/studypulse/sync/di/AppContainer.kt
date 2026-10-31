package com.studypulse.sync.di

import android.content.Context
import com.studypulse.sync.core.datastore.SettingsRepository
import com.studypulse.sync.core.datastore.settingsDataStore
import com.studypulse.sync.core.network.ApiClient
import com.studypulse.sync.core.network.ApiService
import com.studypulse.sync.data.backup.BackupManager
import com.studypulse.sync.data.export.PlanExporter
import com.studypulse.sync.data.local.AppDatabase
import com.studypulse.sync.data.profile.ProfileRepository
import com.studypulse.sync.data.profile.profileDataStore
import com.studypulse.sync.data.qa.QaService
import com.studypulse.sync.data.repository.AssistantRepository
import com.studypulse.sync.data.repository.LocalRepository
import com.studypulse.sync.data.repository.PackageRepository
import com.studypulse.sync.data.repository.PackageRepositoryImpl
import com.studypulse.sync.data.repository.StatsRepository
import com.studypulse.sync.data.repository.StudyRepository
import com.studypulse.sync.data.repository.SyncRepository
import com.studypulse.sync.domain.planning.AutoPlannerCoordinator

class AppContainer(context: Context) {

    val settingsRepository: SettingsRepository =
        SettingsRepository(context.settingsDataStore)

    val profileRepository: ProfileRepository =
        ProfileRepository(context.profileDataStore)

    val database: AppDatabase =
        AppDatabase.getInstance(context)

    private val apiFactory: (String) -> ApiService = { baseUrl ->
        ApiClient.create(baseUrl)
    }

    val qaService: QaService = QaService(database)

    val planExporter: PlanExporter = PlanExporter(database)

    val backupManager: BackupManager =
        BackupManager(context, planExporter)

    val packageRepository: PackageRepository =
        PackageRepositoryImpl(
            database = database,
            settingsRepository = settingsRepository,
            apiFactory = apiFactory
        )

    val studyRepository: StudyRepository =
        StudyRepository(database)

    val localRepository: LocalRepository =
        LocalRepository(database, qaService)

    val syncRepository: SyncRepository =
        SyncRepository(
            database = database,
            settingsRepository = settingsRepository,
            apiFactory = apiFactory
        )

    val statsRepository: StatsRepository =
        StatsRepository(
            database = database,
            settingsRepository = settingsRepository,
            apiFactory = apiFactory
        )

    val assistantRepository: AssistantRepository =
        AssistantRepository(
            database = database,
            studyRepository = studyRepository
        )

    val autoPlannerCoordinator: AutoPlannerCoordinator =
        AutoPlannerCoordinator(
            studyRepository = studyRepository,
            profileRepository = profileRepository
        )

    fun apiService(baseUrl: String): ApiService =
        apiFactory(baseUrl)
}