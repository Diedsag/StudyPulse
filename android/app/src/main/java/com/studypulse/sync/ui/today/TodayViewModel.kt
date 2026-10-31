package com.studypulse.sync.ui.today

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studypulse.sync.StudyPulseApplication
import com.studypulse.sync.data.local.dao.SlotWithTaskMaterial
import com.studypulse.sync.data.local.entity.TaskEntity
import com.studypulse.sync.data.repository.DeadlineRisk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TodayUiState(
    val isLoading: Boolean = true,
    val dateLabel: String = "",
    val greeting: String = "",
    val nextSlot: SlotWithTaskMaterial? = null,
    val todaySlots: List<SlotWithTaskMaterial> = emptyList(),
    val doneTodayCount: Int = 0,
    val totalTodayCount: Int = 0,
    val urgentTasks: List<TaskEntity> = emptyList(),
    val dueCardsCount: Int = 0,
    val risks: List<DeadlineRisk> = emptyList(),
    val errorMessage: String? = null
)

class TodayViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as StudyPulseApplication).container
    private val repository = container.studyRepository

    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.getTodaySnapshot()
            _uiState.update { current ->
                result.fold(
                    onSuccess = { snap ->
                        current.copy(
                            isLoading = false,
                            dateLabel = snap.dateLabel,
                            greeting = snap.greeting,
                            nextSlot = snap.nextSlot,
                            todaySlots = snap.todaySlots,
                            doneTodayCount = snap.doneTodayCount,
                            totalTodayCount = snap.totalTodayCount,
                            urgentTasks = snap.urgentTasks,
                            dueCardsCount = snap.dueCardsCount,
                            risks = snap.risks
                        )
                    },
                    onFailure = { error ->
                        current.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Не удалось загрузить данные"
                        )
                    }
                )
            }
        }
    }
}