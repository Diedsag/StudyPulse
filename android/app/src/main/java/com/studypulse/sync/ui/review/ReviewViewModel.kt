package com.studypulse.sync.ui.review

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studypulse.sync.StudyPulseApplication
import com.studypulse.sync.data.local.entity.CardEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class ReviewUiState(
    val isLoading: Boolean = false,
    val isReviewing: Boolean = false,
    val cards: List<CardEntity> = emptyList(),
    val index: Int = 0,
    val showAnswer: Boolean = false,
    val finished: Boolean = false,
    val reviewedCount: Int = 0,
    val message: String? = null,
    val nextDueLabel: String? = null
)

class ReviewViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val container = (application as StudyPulseApplication).container
    private val repository = container.studyRepository

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    fun start(materialExternalId: String?) {
        viewModelScope.launch {
            _uiState.update { currentState ->
                currentState.copy(
                    isLoading = true,
                    isReviewing = false,
                    cards = emptyList(),
                    index = 0,
                    showAnswer = false,
                    finished = false,
                    reviewedCount = 0,
                    message = null,
                    nextDueLabel = null
                )
            }

            val result = repository.getDueCards(materialExternalId)

            _uiState.update { currentState ->
                result.fold(
                    onSuccess = { cards ->
                        if (cards.isEmpty()) {
                            currentState.copy(
                                isLoading = false,
                                finished = true,
                                message = "Нет карточек, готовых к повторению."
                            )
                        } else {
                            currentState.copy(
                                isLoading = false,
                                cards = cards,
                                index = 0,
                                showAnswer = false,
                                finished = false,
                                reviewedCount = 0,
                                message = null,
                                nextDueLabel = null
                            )
                        }
                    },
                    onFailure = { error ->
                        currentState.copy(
                            isLoading = false,
                            finished = true,
                            message = error.message ?: "Не удалось загрузить карточки"
                        )
                    }
                )
            }
        }
    }

    fun revealAnswer() {
        _uiState.update { currentState ->
            currentState.copy(showAnswer = true)
        }
    }

    fun rate(rating: Int) {
        val state = _uiState.value
        val card = state.cards.getOrNull(state.index) ?: return

        if (state.isReviewing) return

        _uiState.update { currentState ->
            currentState.copy(
                isReviewing = true,
                message = null
            )
        }

        viewModelScope.launch {
            val result = repository.reviewCard(card, rating)

            _uiState.update { currentState ->
                result.fold(
                    onSuccess = { outcome ->
                        val nextIndex = currentState.index + 1
                        val finished = nextIndex >= currentState.cards.size

                        currentState.copy(
                            isReviewing = false,
                            index = nextIndex,
                            showAnswer = false,
                            finished = finished,
                            reviewedCount = currentState.reviewedCount + 1,
                            nextDueLabel = formatDue(outcome.nextDueAt),
                            message = if (finished) "Повторение завершено" else null
                        )
                    },
                    onFailure = { error ->
                        currentState.copy(
                            isReviewing = false,
                            message = error.message ?: "Не удалось сохранить повторение"
                        )
                    }
                )
            }
        }
    }

    private fun formatDue(dueAt: Long): String {
        val now = System.currentTimeMillis()
        val diff = dueAt - now

        if (diff <= 0) return "сейчас"

        if (diff < 60L * 60L * 1000L) {
            val minutes = ((diff + 59L * 1000L) / (60L * 1000L)).toInt()
            return "через $minutes мин"
        }

        val zonedDateTime = Instant
            .ofEpochMilli(dueAt)
            .atZone(ZoneId.systemDefault())

        val date = zonedDateTime.toLocalDate()
        val time = zonedDateTime.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))

        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)

        return when (date) {
            today -> "сегодня $time"
            tomorrow -> "завтра $time"
            else -> date.format(DateTimeFormatter.ofPattern("dd.MM")) + " $time"
        }
    }
}