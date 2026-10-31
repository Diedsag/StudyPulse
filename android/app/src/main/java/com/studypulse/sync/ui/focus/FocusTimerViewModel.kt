package com.studypulse.sync.ui.focus

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studypulse.sync.StudyPulseApplication
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FocusUiState(
    val isLoading: Boolean = false,
    val taskTitle: String = "",
    val totalSeconds: Int = 0,
    val remainingSeconds: Int = 0,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false,
    val isCompleting: Boolean = false,
    val message: String? = null
)

class FocusViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val container = (application as StudyPulseApplication).container
    private val assistantRepository = container.assistantRepository

    private var slotId: Long? = null
    private var tickerJob: Job? = null

    private val _uiState = MutableStateFlow(FocusUiState())
    val uiState: StateFlow<FocusUiState> = _uiState.asStateFlow()

    fun start(id: Long) {
        if (slotId == id && _uiState.value.totalSeconds > 0) return

        slotId = id
        tickerJob?.cancel()

        _uiState.update {
            FocusUiState(isLoading = true)
        }

        viewModelScope.launch {
            val info = assistantRepository.getSlotFocusInfo(id)

            if (info == null) {
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        message = "Слот не найден"
                    )
                }
                return@launch
            }

            val durationMillis = info.slot.endDateTime - info.slot.startDateTime
            val durationMinutes = ((durationMillis / 60_000L).toInt()).coerceAtLeast(1)
            val totalSeconds = durationMinutes * 60

            _uiState.update { currentState ->
                currentState.copy(
                    isLoading = false,
                    taskTitle = info.taskTitle,
                    totalSeconds = totalSeconds,
                    remainingSeconds = totalSeconds,
                    isRunning = false,
                    isFinished = false,
                    message = null
                )
            }
        }
    }

    fun toggle() {
        val state = _uiState.value
        if (state.totalSeconds <= 0 || state.isFinished) return

        if (state.isRunning) {
            tickerJob?.cancel()
            _uiState.update { currentState ->
                currentState.copy(isRunning = false)
            }
        } else {
            _uiState.update { currentState ->
                currentState.copy(isRunning = true)
            }
            startTicker()
        }
    }

    fun reset() {
        tickerJob?.cancel()

        _uiState.update { currentState ->
            currentState.copy(
                remainingSeconds = currentState.totalSeconds,
                isRunning = false,
                isFinished = false,
                message = null
            )
        }
    }

    fun completeSlot() {
        val currentSlotId = slotId ?: return

        if (_uiState.value.isCompleting) return

        _uiState.update { currentState ->
            currentState.copy(
                isCompleting = true,
                message = null
            )
        }

        viewModelScope.launch {
            val result = assistantRepository.completeSlotById(currentSlotId)

            _uiState.update { currentState ->
                result.fold(
                    onSuccess = { completion ->
                        tickerJob?.cancel()

                        val base = "Слот отмечен выполненным. Осталось по задаче: ${completion.taskRemainingMinutes} мин."
                        val message = if (completion.taskCompleted) {
                            "$base Задание закрыто."
                        } else {
                            base
                        }

                        currentState.copy(
                            isCompleting = false,
                            isRunning = false,
                            isFinished = true,
                            message = message
                        )
                    },
                    onFailure = { error ->
                        currentState.copy(
                            isCompleting = false,
                            message = error.message ?: "Не удалось отметить слот выполненным"
                        )
                    }
                )
            }
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()

        tickerJob = viewModelScope.launch {
            while (true) {
                val current = _uiState.value

                if (!current.isRunning || current.remainingSeconds <= 0) break

                delay(1000L)

                val newRemaining = current.remainingSeconds - 1

                _uiState.update { currentState ->
                    currentState.copy(
                        remainingSeconds = newRemaining,
                        isRunning = newRemaining > 0,
                        isFinished = newRemaining == 0,
                        message = if (newRemaining == 0) "Время вышло" else currentState.message
                    )
                }
            }
        }
    }

    override fun onCleared() {
        tickerJob?.cancel()
        super.onCleared()
    }
}