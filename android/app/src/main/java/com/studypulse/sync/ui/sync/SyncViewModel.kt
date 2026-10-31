package com.studypulse.sync.ui.sync

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studypulse.sync.StudyPulseApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SyncUiState(
    val isSyncing: Boolean = false,
    val pendingCount: Int = 0,
    val lastSyncAt: Long? = null,
    val message: String? = null,
    val totalXp: Int = 0,
    val weeklyXp: Int = 0,
    val level: Int = 1,
    val streakDays: Int = 0
)

class SyncViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val container = (application as StudyPulseApplication).container
    private val syncRepository = container.syncRepository

    private val _uiState = MutableStateFlow(SyncUiState())
    val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            syncRepository.pendingCount.collect { count ->
                _uiState.update { currentState ->
                    currentState.copy(pendingCount = count)
                }
            }
        }

        viewModelScope.launch {
            container.settingsRepository.settings.collect { settings ->
                _uiState.update { currentState ->
                    currentState.copy(lastSyncAt = settings.lastSyncAt)
                }
            }
        }
    }

    fun sync() {
        viewModelScope.launch {
            if (_uiState.value.isSyncing) return@launch

            _uiState.update { currentState ->
                currentState.copy(
                    isSyncing = true,
                    message = null
                )
            }

            val result = syncRepository.sync()

            _uiState.update { currentState ->
                result.fold(
                    onSuccess = { response ->
                        if (response == null) {
                            currentState.copy(
                                isSyncing = false,
                                message = "Нет событий для синхронизации."
                            )
                        } else {
                            currentState.copy(
                                isSyncing = false,
                                message =
                                    "Принято: ${response.accepted}, " +
                                        "дубликатов: ${response.duplicated}, " +
                                        "отклонено: ${response.rejected}, " +
                                        "XP: +${response.xpAwardedTotal}",
                                totalXp = response.totalXp,
                                weeklyXp = response.weeklyXp,
                                level = response.level,
                                streakDays = response.streakDays
                            )
                        }
                    },
                    onFailure = { error ->
                        currentState.copy(
                            isSyncing = false,
                            message = error.message ?: "Не удалось синхронизировать"
                        )
                    }
                )
            }
        }
    }
}