package com.studypulse.sync.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studypulse.sync.StudyPulseApplication
import com.studypulse.sync.notifications.NotificationScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val displayName: String = "",
    val profileNotificationsEnabled: Boolean = true,
    val autoPlanEnabled: Boolean = true,
    val profileLoaded: Boolean = false,
    val serverUrl: String = "",
    val nickname: String = "",
    val deviceUid: String = "",
    val ratingOptIn: Boolean = true,
    val isDirty: Boolean = false,
    val status: ConnectionStatus = ConnectionStatus.Idle,
    val message: String? = null,
    val exportedJson: String? = null
)

sealed class ConnectionStatus {
    object Idle : ConnectionStatus()
    object Loading : ConnectionStatus()
    data class Connected(val time: String) : ConnectionStatus()
    data class Error(val message: String) : ConnectionStatus()
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as StudyPulseApplication).container
    private val settingsRepository = container.settingsRepository
    private val profileRepository = container.profileRepository
    private val planExporter = container.planExporter

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.observeSettings().collect { s ->
                _uiState.update {
                    it.copy(
                        serverUrl = s.serverUrl,
                        nickname = s.nickname,
                        deviceUid = s.deviceUid,
                        ratingOptIn = s.ratingOptIn
                    )
                }
            }
        }
        viewModelScope.launch {
            profileRepository.profile.collect { p ->
                _uiState.update {
                    it.copy(
                        displayName = p.displayName,
                        profileNotificationsEnabled = p.notificationsEnabled,
                        autoPlanEnabled = p.autoPlanEnabled,
                        profileLoaded = true
                    )
                }
            }
        }
    }

    fun toggleProfileNotifications(value: Boolean) {
        viewModelScope.launch {
            profileRepository.setNotificationsEnabled(value)
            if (value) {
                NotificationScheduler.schedulePeriodicNotifications(getApplication())
            } else {
                NotificationScheduler.cancelNotifications(getApplication())
            }
        }
    }

    fun toggleAutoPlan(value: Boolean) {
        viewModelScope.launch {
            profileRepository.setAutoPlanEnabled(value)
        }
    }

    fun onServerUrlChange(value: String) = _uiState.update { it.copy(serverUrl = value, isDirty = true) }
    fun onNicknameChange(value: String) = _uiState.update { it.copy(nickname = value, isDirty = true) }
    fun onRatingOptInChange(value: Boolean) = _uiState.update { it.copy(ratingOptIn = value, isDirty = true) }

    fun generateDeviceUid() {
        val newUid = java.util.UUID.randomUUID().toString()
        _uiState.update { it.copy(deviceUid = newUid, isDirty = true) }
    }

    fun saveSettings() {
        viewModelScope.launch {
            val s = _uiState.value
            settingsRepository.setServerUrl(s.serverUrl)
            settingsRepository.setNickname(s.nickname)
            settingsRepository.setRatingOptIn(s.ratingOptIn)
            _uiState.update { it.copy(isDirty = false, message = "Сохранено") }
        }
    }

    fun checkConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(status = ConnectionStatus.Loading) }
            try {
                val s = _uiState.value
                val api = container.apiService(s.serverUrl)
                val health = api.getHealth()
                _uiState.update { it.copy(status = ConnectionStatus.Connected(health.status)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(status = ConnectionStatus.Error(e.message ?: "Ошибка подключения")) }
            }
        }
    }

    fun exportPlan() {
        viewModelScope.launch {
            val result = planExporter.exportPlan()
            result.fold(
                onSuccess = { json -> _uiState.update { it.copy(exportedJson = json, message = "Резервная копия готова") } },
                onFailure = { error -> _uiState.update { it.copy(message = "Не удалось создать копию: ${error.message}") } }
            )
        }
    }

    fun clearExportedJson() = _uiState.update { it.copy(exportedJson = null) }
}