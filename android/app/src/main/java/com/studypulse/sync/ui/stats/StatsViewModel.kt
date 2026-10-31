package com.studypulse.sync.ui.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studypulse.sync.StudyPulseApplication
import com.studypulse.sync.core.network.dto.LeaderboardEntryDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StatsUiState(
    val period: String = "week",
    val isLoading: Boolean = false,
    val hasCache: Boolean = false,
    val totalXp: Int = 0,
    val weeklyXp: Int = 0,
    val level: Int = 1,
    val streakDays: Int = 0,
    val ratingOptIn: Boolean = true,
    val weeklyPeriodKey: String? = null,
    val fetchedAt: Long? = null,
    val leaderboard: List<LeaderboardEntryDto> = emptyList(),
    val errorMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val container = (application as StudyPulseApplication).container
    private val repository = container.statsRepository
    private val selectedPeriod = MutableStateFlow("week")

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    private var isRefreshingAll = false

    init {
        viewModelScope.launch {
            selectedPeriod
                .flatMapLatest { period ->
                    repository.observeSnapshot(period)
                }
                .collect { snapshot ->
                    _uiState.update { currentState ->
                        if (snapshot == null) {
                            currentState.copy(
                                hasCache = false,
                                leaderboard = emptyList(),
                                totalXp = 0,
                                weeklyXp = 0,
                                level = 1,
                                streakDays = 0,
                                ratingOptIn = true,
                                weeklyPeriodKey = null,
                                fetchedAt = null
                            )
                        } else {
                            currentState.copy(
                                period = snapshot.period,
                                hasCache = true,
                                totalXp = snapshot.totalXp,
                                weeklyXp = snapshot.weeklyXp,
                                level = snapshot.level,
                                streakDays = snapshot.streakDays,
                                ratingOptIn = snapshot.ratingOptIn,
                                weeklyPeriodKey = snapshot.weeklyPeriodKey,
                                fetchedAt = snapshot.fetchedAt,
                                leaderboard = snapshot.leaderboard,
                                errorMessage = currentState.errorMessage
                            )
                        }
                    }
                }
        }
    }

    fun selectPeriod(period: String) {
        if (selectedPeriod.value == period) return

        selectedPeriod.value = period

        _uiState.update { currentState ->
            currentState.copy(
                period = period,
                errorMessage = null
            )
        }

        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            if (_uiState.value.isLoading) return@launch

            _uiState.update { currentState ->
                currentState.copy(
                    isLoading = true,
                    errorMessage = null
                )
            }

            val result = repository.refresh(selectedPeriod.value)

            _uiState.update { currentState ->
                result.fold(
                    onSuccess = {
                        currentState.copy(isLoading = false)
                    },
                    onFailure = { error ->
                        currentState.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Не удалось загрузить статистику"
                        )
                    }
                )
            }
        }
    }

    fun refreshAllPeriods() {
        viewModelScope.launch {
            if (isRefreshingAll || _uiState.value.isLoading) return@launch

            isRefreshingAll = true

            _uiState.update { currentState ->
                currentState.copy(
                    isLoading = true,
                    errorMessage = null
                )
            }

            try {
                val current = selectedPeriod.value
                val other = if (current == "week") "all" else "week"
                val errors = mutableListOf<String>()

                for (p in listOf(current, other).distinct()) {
                    repository.refresh(p).exceptionOrNull()?.let { error ->
                        errors += error.message ?: "Не удалось обновить период $p"
                    }
                }

                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        errorMessage = if (errors.isEmpty()) {
                            null
                        } else {
                            errors.distinct().joinToString("; ")
                        }
                    )
                }
            } catch (e: Throwable) {
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Не удалось обновить статистику"
                    )
                }
            } finally {
                isRefreshingAll = false
            }
        }
    }
}