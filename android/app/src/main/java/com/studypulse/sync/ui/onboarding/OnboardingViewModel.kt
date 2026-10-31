package com.studypulse.sync.ui.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studypulse.sync.StudyPulseApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val step: Int = 0,
    val displayName: String = "",
    val studyStartHour: Int = 9,
    val studyEndHour: Int = 21,
    val dailyMinutes: Int = 120,
    val notificationsEnabled: Boolean = true,
    val autoPlanEnabled: Boolean = true,
    val isSaving: Boolean = false,
    val message: String? = null
)

class OnboardingViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val profileRepository =
        (application as StudyPulseApplication).container.profileRepository

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    val totalSteps: Int = 4

    fun next() {
        val currentStep = _uiState.value.step

        if (currentStep < totalSteps - 1) {
            _uiState.update { currentState ->
                currentState.copy(
                    step = currentStep + 1,
                    message = null
                )
            }
        } else {
            finish()
        }
    }

    fun back() {
        _uiState.update { currentState ->
            if (currentState.step > 0) {
                currentState.copy(
                    step = currentState.step - 1,
                    message = null
                )
            } else {
                currentState
            }
        }
    }

    fun onDisplayNameChange(value: String) {
        _uiState.update { currentState ->
            currentState.copy(displayName = value)
        }
    }

    fun incrementStartHour() {
        _uiState.update { currentState ->
            val maxAllowed = currentState.studyEndHour - 1
            if (currentState.studyStartHour < maxAllowed && currentState.studyStartHour < 22) {
                currentState.copy(studyStartHour = currentState.studyStartHour + 1)
            } else {
                currentState
            }
        }
    }

    fun decrementStartHour() {
        _uiState.update { currentState ->
            if (currentState.studyStartHour > 6) {
                currentState.copy(studyStartHour = currentState.studyStartHour - 1)
            } else {
                currentState
            }
        }
    }

    fun incrementEndHour() {
        _uiState.update { currentState ->
            if (currentState.studyEndHour < 23) {
                currentState.copy(studyEndHour = currentState.studyEndHour + 1)
            } else {
                currentState
            }
        }
    }

    fun decrementEndHour() {
        _uiState.update { currentState ->
            val minAllowed = currentState.studyStartHour + 1
            if (currentState.studyEndHour > minAllowed) {
                currentState.copy(studyEndHour = currentState.studyEndHour - 1)
            } else {
                currentState
            }
        }
    }

    fun incrementDailyMinutes() {
        _uiState.update { currentState ->
            currentState.copy(
                dailyMinutes = (currentState.dailyMinutes + 30).coerceAtMost(360)
            )
        }
    }

    fun decrementDailyMinutes() {
        _uiState.update { currentState ->
            currentState.copy(
                dailyMinutes = (currentState.dailyMinutes - 30).coerceAtLeast(30)
            )
        }
    }

    fun toggleNotifications(value: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(notificationsEnabled = value)
        }
    }

    fun toggleAutoPlan(value: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(autoPlanEnabled = value)
        }
    }

    fun finish() {
        viewModelScope.launch {
            _uiState.update { currentState ->
                currentState.copy(isSaving = true, message = null)
            }

            try {
                val state = _uiState.value

                profileRepository.saveOnboarding(
                    displayName = state.displayName,
                    studyStartHour = state.studyStartHour,
                    studyEndHour = state.studyEndHour,
                    defaultDailyMinutes = state.dailyMinutes,
                    notificationsEnabled = state.notificationsEnabled,
                    autoPlanEnabled = state.autoPlanEnabled
                )

                _uiState.update { currentState ->
                    currentState.copy(
                        isSaving = false,
                        message = "Профиль сохранён"
                    )
                }
            } catch (e: Throwable) {
                _uiState.update { currentState ->
                    currentState.copy(
                        isSaving = false,
                        message = e.message ?: "Не удалось сохранить профиль"
                    )
                }
            }
        }
    }
}