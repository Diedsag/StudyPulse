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
    val isReplanning: Boolean = false,
    val isAccepting: Boolean = false,
    val completingSlotId: Long? = null,
    val message: String? = null,
    val warnings: List<String> = emptyList()
)

class PlanViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as StudyPulseApplication).container
    private val repository = container.studyRepository

    private val _uiState = MutableStateFlow(PlanUiState())
    val uiState: StateFlow<PlanUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.markMissedSlots()
        }
        viewModelScope.launch {
            repository.proposedSlots.collect { slots ->
                _uiState.update { it.copy(proposedSlots = slots) }
            }
        }
        viewModelScope.launch {
            repository.plannedSlots.collect { slots ->
                _uiState.update { it.copy(plannedSlots = slots) }
            }
        }
    }

    fun generatePlan() = runPlanGeneration(replan = false)

    fun replan() = runPlanGeneration(replan = true)

    private fun runPlanGeneration(replan: Boolean) {
        viewModelScope.launch {
            val busy = _uiState.value.isGenerating || _uiState.value.isReplanning || _uiState.value.isAccepting
            if (busy) return@launch

            _uiState.update {
                if (replan) it.copy(isReplanning = true, message = null, warnings = emptyList())
                else it.copy(isGenerating = true, message = null, warnings = emptyList())
            }

            val result = if (replan) repository.replan() else repository.generatePlan()

            _uiState.update { current ->
                result.fold(
                    onSuccess = { planResult ->
                        current.copy(
                            isGenerating = false,
                            isReplanning = false,
                            message = "Создано слотов: ${planResult.slotsCount}",
                            warnings = planResult.warnings
                        )
                    },
                    onFailure = { error ->
                        current.copy(
                            isGenerating = false,
                            isReplanning = false,
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
                _uiState.update { it.copy(message = "Сначала построй план") }
                return@launch
            }
            _uiState.update { it.copy(isAccepting = true, message = null) }
            val result = repository.acceptPlan()
            _uiState.update { current ->
                result.fold(
                    onSuccess = { count ->
                        current.copy(isAccepting = false, message = "Принято слотов: $count", warnings = emptyList())
                    },
                    onFailure = { error ->
                        current.copy(isAccepting = false, message = error.message ?: "Не удалось принять план")
                    }
                )
            }
        }
    }

    fun completeSlot(slot: StudySlotEntity) {
        viewModelScope.launch {
            if (_uiState.value.completingSlotId != null) return@launch
            _uiState.update { it.copy(completingSlotId = slot.id, message = null) }
            val result = repository.completeSlot(slot)
            _uiState.update { current ->
                result.fold(
                    onSuccess = { completion ->
                        val base = "Слот выполнен: ${completion.durationMinutes} мин. " +
                            "Осталось по задаче: ${completion.taskRemainingMinutes} мин."
                        current.copy(
                            completingSlotId = null,
                            message = if (completion.taskCompleted) "$base Задание закрыто." else base
                        )
                    },
                    onFailure = { error ->
                        current.copy(
                            completingSlotId = null,
                            message = error.message ?: "Не удалось отметить слот выполненным"
                        )
                    }
                )
            }
        }
    }
}