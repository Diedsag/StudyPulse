package com.studypulse.sync.ui.plan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studypulse.sync.StudyPulseApplication
import com.studypulse.sync.data.local.dao.SlotWithTask
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlanUiState(
    val slots: List<SlotWithTask> = emptyList(),
    val isGenerating: Boolean = false,
    val isAccepting: Boolean = false,
    val message: String? = null,
    val warnings: List<String> = emptyList()
)

class PlanViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val container = (application as StudyPulseApplication).container
    private val repository = container.studyRepository

    private val _uiState = MutableStateFlow(PlanUiState())
    val uiState: StateFlow<PlanUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.proposedSlots.collect { slots ->
                _uiState.update { currentState ->
                    currentState.copy(slots = slots)
                }
            }
        }
    }

    fun generatePlan() {
        viewModelScope.launch {
            if (_uiState.value.isGenerating) return@launch

            _uiState.update { currentState ->
                currentState.copy(
                    isGenerating = true,
                    message = null,
                    warnings = emptyList()
                )
            }

            val result = repository.generatePlan()

            _uiState.update { currentState ->
                result.fold(
                    onSuccess = { planResult ->
                        currentState.copy(
                            isGenerating = false,
                            message = "Создано слотов: ${planResult.slotsCount}",
                            warnings = planResult.warnings
                        )
                    },
                    onFailure = { error ->
                        currentState.copy(
                            isGenerating = false,
                            message = error.message ?: "Не удалось построить план"
                        )
                    }
                )
            }
        }
    }

    fun acceptPlan() {
        viewModelScope.launch {
            if (_uiState.value.isAccepting) return@launch
            if (_uiState.value.slots.isEmpty()) {
                _uiState.update { currentState ->
                    currentState.copy(message = "Сначала построй план")
                }
                return@launch
            }

            _uiState.update { currentState ->
                currentState.copy(
                    isAccepting = true,
                    message = null
                )
            }

            val result = repository.acceptPlan()

            _uiState.update { currentState ->
                result.fold(
                    onSuccess = { count ->
                        currentState.copy(
                            isAccepting = false,
                            message = "Принято слотов: $count",
                            warnings = emptyList()
                        )
                    },
                    onFailure = { error ->
                        currentState.copy(
                            isAccepting = false,
                            message = error.message ?: "Не удалось принять план"
                        )
                    }
                )
            }
        }
    }
}