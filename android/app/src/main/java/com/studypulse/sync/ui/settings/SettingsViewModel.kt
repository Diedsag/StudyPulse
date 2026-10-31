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
    val serverUrl: String = "",
    val nickname: String = "",
    val deviceUid: String = "",
    val ratingOptIn: Boolean = true,
    val notificationsEnabled: Boolean = true,
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
    private val planExporter = container.planExporter

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = settingsRepository.currentSettings()
            _uiState.update {
                it.copy(
                    serverUrl = settings.serverUrl,
                    nickname = settings.nickname,
                    deviceUid = settings.deviceUid,
                    ratingOptIn = settings.ratingOptIn,
                    notificationsEnabled = settings.notificationsEnabled
                )
            }
        }
    }

    fun onServerUrlChange(value: String) {
        _uiState.update { it.copy(serverUrl = value, isDirty = true) }
    }

    fun onNicknameChange(value: String) {
        _uiState.update { it.copy(nickname = value, isDirty = true) }
    }

    fun onRatingOptInChange(value: Boolean) {
        _uiState.update { it.copy(ratingOptIn = value, isDirty = true) }
    }

    fun onNotificationsChange(value: Boolean) {
        _uiState.update { it.copy(notificationsEnabled = value, isDirty = true) }
        if (value) {
            NotificationScheduler.schedulePeriodicNotifications(getApplication())
        } else {
            NotificationScheduler.cancelNotifications(getApplication())
        }
    }

    fun generateDeviceUid() {
        val newUid = java.util.UUID.randomUUID().toString()
        _uiState.update { it.copy(deviceUid = newUid, isDirty = true) }
    }

    fun saveSettings() {
        viewModelScope.launch {
            val state = _uiState.value
            settingsRepository.setServerUrl(state.serverUrl)
            settingsRepository.setNickname(state.nickname)
            settingsRepository.setRatingOptIn(state.ratingOptIn)
            settingsRepository.setNotificationsEnabled(state.notificationsEnabled)
            _uiState.update { it.copy(isDirty = false, message = "Настройки сохранены") }
        }
    }

    fun checkConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(status = ConnectionStatus.Loading) }
            try {
                val state = _uiState.value
                val api = container.apiService(state.serverUrl)
                val health = api.getHealth()
                _uiState.update {
                    it.copy(status = ConnectionStatus.Connected(health.status))
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(status = ConnectionStatus.Error(e.message ?: "Ошибка подключения"))
                }
            }
        }
    }

    fun exportPlan() {
        viewModelScope.launch {
            val result = planExporter.exportPlan()
            result.fold(
                onSuccess = { json ->
                    _uiState.update { it.copy(exportedJson = json, message = "План экспортирован") }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(message = "Ошибка экспорта: ${error.message}") }
                }
            )
        }
    }

    fun clearExportedJson() {
        _uiState.update { it.copy(exportedJson = null) }
    }
}