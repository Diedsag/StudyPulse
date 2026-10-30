package com.studypulse.sync.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studypulse.sync.StudyPulseApplication
import com.studypulse.sync.core.datastore.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val serverUrl: String = "",
    val savedServerUrl: String = "",
    val deviceUid: String = "",
    val nickname: String = SettingsRepository.DEFAULT_NICKNAME,
    val status: ConnectionStatus = ConnectionStatus.Idle,
    val isDirty: Boolean = false
)

sealed interface ConnectionStatus {
    data object Idle : ConnectionStatus
    data object Loading : ConnectionStatus
    data class Connected(val time: String) : ConnectionStatus
    data class Error(val message: String) : ConnectionStatus
}

class SettingsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val container = (application as StudyPulseApplication).container

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = container.settingsRepository.currentSettings()

            _uiState.update { currentState ->
                currentState.copy(
                    serverUrl = settings.serverUrl,
                    savedServerUrl = settings.serverUrl,
                    deviceUid = settings.deviceUid,
                    nickname = settings.nickname,
                    isDirty = false,
                    status = ConnectionStatus.Idle
                )
            }
        }
    }

    fun onServerUrlChange(value: String) {
        _uiState.update { currentState ->
            currentState.copy(
                serverUrl = value,
                isDirty = value != currentState.savedServerUrl,
                status = ConnectionStatus.Idle
            )
        }
    }

    fun onNicknameChange(value: String) {
        _uiState.update { currentState ->
            currentState.copy(
                nickname = value,
                isDirty = true
            )
        }
    }

    fun saveSettings() {
        viewModelScope.launch {
            val state = _uiState.value

            container.settingsRepository.setServerUrl(state.serverUrl)
            container.settingsRepository.setNickname(state.nickname)

            val normalized = SettingsRepository.normalizeBaseUrl(state.serverUrl)

            _uiState.update { currentState ->
                currentState.copy(
                    serverUrl = normalized,
                    savedServerUrl = normalized,
                    nickname = state.nickname.ifBlank { SettingsRepository.DEFAULT_NICKNAME },
                    isDirty = false
                )
            }
        }
    }

    fun generateDeviceUid() {
        viewModelScope.launch {
            val deviceUid = container.settingsRepository.ensureDeviceUid()

            _uiState.update { currentState ->
                currentState.copy(deviceUid = deviceUid)
            }
        }
    }

    fun checkConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(status = ConnectionStatus.Loading) }

            try {
                val state = _uiState.value

                val rawUrl = if (state.serverUrl.isNotBlank()) {
                    state.serverUrl
                } else {
                    container.settingsRepository.currentSettings().serverUrl
                }

                val normalizedUrl = SettingsRepository.normalizeBaseUrl(rawUrl)
                val api = container.apiService(normalizedUrl)
                val health = api.getHealth()

                _uiState.update { currentState ->
                    currentState.copy(
                        status = ConnectionStatus.Connected(health.time)
                    )
                }
            } catch (e: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(
                        status = ConnectionStatus.Error(
                            message = e.message ?: "Unknown connection error"
                        )
                    )
                }
            }
        }
    }
}