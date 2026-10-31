package com.studypulse.sync.ui.qa

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studypulse.sync.StudyPulseApplication
import com.studypulse.sync.data.local.entity.MaterialEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QaMessage(
    val isUser: Boolean,
    val text: String
)

data class QaUiState(
    val isLoading: Boolean = false,
    val materialFound: Boolean = false,
    val materialTitle: String = "",
    val messages: List<QaMessage> = emptyList(),
    val input: String = "",
    val isSending: Boolean = false
)

class QaViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val container = (application as StudyPulseApplication).container
    private val assistantRepository = container.assistantRepository

    private var material: MaterialEntity? = null

    private val _uiState = MutableStateFlow(QaUiState())
    val uiState: StateFlow<QaUiState> = _uiState.asStateFlow()

    fun start(materialExternalId: String) {
        viewModelScope.launch {
            _uiState.update { currentState ->
                currentState.copy(isLoading = true)
            }

            val loadedMaterial = assistantRepository.getMaterial(materialExternalId)
            material = loadedMaterial

            if (loadedMaterial == null) {
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        materialFound = false,
                        messages = listOf(
                            QaMessage(
                                isUser = false,
                                text = "Материал не найден. Открой вкладку «Материалы» и выбери существующий материал."
                            )
                        )
                    )
                }
                return@launch
            }

            _uiState.update { currentState ->
                currentState.copy(
                    isLoading = false,
                    materialFound = true,
                    materialTitle = loadedMaterial.title,
                    messages = listOf(
                        QaMessage(
                            isUser = false,
                            text = "Задай вопрос по материалу «${loadedMaterial.title}».\n\nЭто локальный демо-ассистент: он отвечает по сохранённому саммари и ключевым фактам, без настоящего LLM/RAG."
                        )
                    )
                )
            }
        }
    }

    fun onInputChange(value: String) {
        _uiState.update { currentState ->
            currentState.copy(input = value)
        }
    }

    fun send() {
        val currentMaterial = material ?: return
        val question = _uiState.value.input.trim()

        if (question.isBlank() || _uiState.value.isSending) return

        _uiState.update { currentState ->
            currentState.copy(
                input = "",
                isSending = true,
                messages = currentState.messages + QaMessage(isUser = true, text = question)
            )
        }

        viewModelScope.launch {
            delay(250L)

            val answer = assistantRepository.mockAnswer(currentMaterial, question)

            _uiState.update { currentState ->
                currentState.copy(
                    isSending = false,
                    messages = currentState.messages + QaMessage(isUser = false, text = answer)
                )
            }
        }
    }
}