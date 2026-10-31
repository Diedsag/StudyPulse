package com.studypulse.sync.ui.schedule

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studypulse.sync.StudyPulseApplication
import com.studypulse.sync.data.repository.ClassOccurrence
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScheduleUiState(
    val occurrences: List<ClassOccurrence> = emptyList(),
    val cancellingKey: String? = null,
    val errorMessage: String? = null
)

class ScheduleViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val container = (application as StudyPulseApplication).container
    private val repository = container.studyRepository

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.upcomingOccurrences(daysAhead = 7).collect { occurrences ->
                _uiState.update { currentState ->
                    currentState.copy(occurrences = occurrences)
                }
            }
        }
    }

    fun cancelOccurrence(occurrence: ClassOccurrence) {
        val key = occurrenceKey(occurrence)

        viewModelScope.launch {
            if (_uiState.value.cancellingKey != null) return@launch

            _uiState.update { currentState ->
                currentState.copy(
                    cancellingKey = key,
                    errorMessage = null
                )
            }

            try {
                repository.cancelOccurrence(
                    templateExternalId = occurrence.templateExternalId,
                    date = occurrence.date
                )
            } catch (e: Throwable) {
                _uiState.update { currentState ->
                    currentState.copy(
                        errorMessage = e.message ?: "Не удалось отменить занятие"
                    )
                }
            } finally {
                _uiState.update { currentState ->
                    currentState.copy(cancellingKey = null)
                }
            }
        }
    }

    private fun occurrenceKey(occurrence: ClassOccurrence): String {
        return "${occurrence.templateExternalId}|${occurrence.date}|${occurrence.startTime}"
    }
}