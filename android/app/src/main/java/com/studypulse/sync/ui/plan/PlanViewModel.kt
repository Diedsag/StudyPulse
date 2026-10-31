package com.studypulse.sync.ui.plan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studypulse.sync.StudyPulseApplication
import com.studypulse.sync.data.local.dao.SlotWithTaskMaterial
import com.studypulse.sync.data.local.entity.StudySlotEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlanUiState(
    val proposedSlots: List<SlotWithTaskMaterial> = emptyList(),
    val plannedSlots: List<SlotWithTaskMaterial> = emptyList(),
    val isGenerating: Boolean = false,
    val isAccepting: Boolean = false,
    val completingSlotId: Long? = null,
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
                    currentState.copy(proposedSlots = slots)
                }
            }
        }

        viewModelScope.launch {
            repository.plannedSlots.collect { slots ->
                _uiState.update { currentState ->
                    currentState.copy(plannedSlots = slots)
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

            if (_uiState.value.proposedSlots.isEmpty()) {
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

    fun completeSlot(slot: StudySlotEntity) {
        viewModelScope.launch {
            if (_uiState.value.completingSlotId != null) return@launch

            _uiState.update { currentState ->
                currentState.copy(
                    completingSlotId = slot.id,
                    message = null
                )
            }

            val result = repository.completeSlot(slot)

            _uiState.update { currentState ->
                result.fold(
                    onSuccess = { completion ->
                        val baseMessage =
                            "Слот выполнен: ${completion.durationMinutes} мин. " +
                                "Осталось по задаче: ${completion.taskRemainingMinutes} мин."

                        val message = if (completion.taskCompleted) {
                            "$baseMessage Задание закрыто."
                        } else {
                            baseMessage
                        }

                        currentState.copy(
                            completingSlotId = null,
                            message = message
                        )
                    },
                    onFailure = { error ->
                        currentState.copy(
                            completingSlotId = null,
                            message = error.message ?: "Не удалось отметить слот выполненным"
                        )
                    }
                )
            }
        }
    }
}