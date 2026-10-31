package com.studypulse.sync.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studypulse.sync.StudyPulseApplication
import com.studypulse.sync.data.backup.BackupInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val displayName: String = "",
    val profileNotificationsEnabled: Boolean = true,
    val autoPlanEnabled: Boolean = true,
    val slotRemindersEnabled: Boolean = true,
    val deadlineRemindersEnabled: Boolean = true,
    val morningDigestEnabled: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: Int = 23,
    val quietHoursEnd: Int = 7,
    val profileLoaded: Boolean = false,
    val serverUrl: String = "",
    val nickname: String = "",
    val deviceUid: String = "",
    val ratingOptIn: Boolean = true,
    val isDirty: Boolean = false,
    val status: ConnectionStatus = ConnectionStatus.Idle,
    val message: String? = null,
    val backups: List<BackupInfo> = emptyList(),
    val isCreatingBackup: Boolean = false,
    val isRestoringBackup: Boolean = false
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
    private val backupManager = container.backupManager
    private val reminderScheduler = container.reminderScheduler

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadBackups()

        viewModelScope.launch {
            try {
                val s = settingsRepository.currentSettings()
                _uiState.update {
                    it.copy(
                        serverUrl = s.serverUrl,
                        nickname = s.nickname,
                        deviceUid = s.deviceUid,
                        ratingOptIn = s.ratingOptIn
                    )
                }
            } catch (_: Throwable) { }
        }

        viewModelScope.launch {
            profileRepository.profile.collect { p ->
                _uiState.update {
                    it.copy(
                        displayName = p.displayName,
                        profileNotificationsEnabled = p.notificationsEnabled,
                        autoPlanEnabled = p.autoPlanEnabled,
                        slotRemindersEnabled = p.slotRemindersEnabled,
                        deadlineRemindersEnabled = p.deadlineRemindersEnabled,
                        morningDigestEnabled = p.morningDigestEnabled,
                        quietHoursEnabled = p.quietHoursEnabled,
                        quietHoursStart = p.quietHoursStart,
                        quietHoursEnd = p.quietHoursEnd,
                        profileLoaded = true
                    )
                }
            }
        }
    }

    fun loadBackups() {
        viewModelScope.launch {
            val backups = backupManager.listBackups()
            _uiState.update { it.copy(backups = backups) }
        }
    }

    fun createBackup() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingBackup = true, message = null) }
            val result = backupManager.createBackup()
            _uiState.update { currentState ->
                result.fold(
                    onSuccess = { currentState.copy(isCreatingBackup = false, message = "Резервная копия создана") },
                    onFailure = { error -> currentState.copy(isCreatingBackup = false, message = "Не удалось создать копию: ${error.message}") }
                )
            }
            loadBackups()
        }
    }

    fun restoreBackup(backupPath: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRestoringBackup = true, message = null) }
            val result = backupManager.restoreBackup(backupPath)
            _uiState.update { currentState ->
                result.fold(
                    onSuccess = { currentState.copy(isRestoringBackup = false, message = "Данные восстановлены. Перезапустите приложение.") },
                    onFailure = { error -> currentState.copy(isRestoringBackup = false, message = "Не удалось восстановить: ${error.message}") }
                )
            }
        }
    }

    fun deleteBackup(backupPath: String) {
        viewModelScope.launch {
            val result = backupManager.deleteBackup(backupPath)
            result.fold(
                onSuccess = { loadBackups() },
                onFailure = { error -> _uiState.update { it.copy(message = "Не удалось удалить: ${error.message}") } }
            )
        }
    }

    fun toggleProfileNotifications(value: Boolean) {
        viewModelScope.launch {
            profileRepository.setNotificationsEnabled(value)
            if (value) {
                reminderScheduler.scheduleMorningDigest()
                reminderScheduler.scheduleForToday()
                reminderScheduler.scheduleDeadlineReminders()
            } else {
                reminderScheduler.cancelAll()
            }
        }
    }

    fun toggleAutoPlan(value: Boolean) {
        viewModelScope.launch { profileRepository.setAutoPlanEnabled(value) }
    }

    fun toggleSlotReminders(value: Boolean) {
        viewModelScope.launch {
            profileRepository.setSlotRemindersEnabled(value)
            if (value) reminderScheduler.scheduleForToday()
        }
    }

    fun toggleDeadlineReminders(value: Boolean) {
        viewModelScope.launch {
            profileRepository.setDeadlineRemindersEnabled(value)
            if (value) reminderScheduler.scheduleDeadlineReminders()
        }
    }

    fun toggleMorningDigest(value: Boolean) {
        viewModelScope.launch {
            profileRepository.setMorningDigestEnabled(value)
            reminderScheduler.scheduleMorningDigest()
        }
    }

    fun toggleQuietHours(value: Boolean) {
        viewModelScope.launch {
            val s = _uiState.value
            profileRepository.setQuietHours(value, s.quietHoursStart, s.quietHoursEnd)
        }
    }

    fun incrementQuietStart() {
        viewModelScope.launch {
            val s = _uiState.value
            profileRepository.setQuietHours(s.quietHoursEnabled, (s.quietHoursStart + 1) % 24, s.quietHoursEnd)
        }
    }

    fun decrementQuietStart() {
        viewModelScope.launch {
            val s = _uiState.value
            profileRepository.setQuietHours(s.quietHoursEnabled, (s.quietHoursStart + 23) % 24, s.quietHoursEnd)
        }
    }

    fun incrementQuietEnd() {
        viewModelScope.launch {
            val s = _uiState.value
            profileRepository.setQuietHours(s.quietHoursEnabled, s.quietHoursStart, (s.quietHoursEnd + 1) % 24)
        }
    }

    fun decrementQuietEnd() {
        viewModelScope.launch {
            val s = _uiState.value
            profileRepository.setQuietHours(s.quietHoursEnabled, s.quietHoursStart, (s.quietHoursEnd + 23) % 24)
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

    fun clearMessage() = _uiState.update { it.copy(message = null) }
}