package com.studypulse.sync.ui.qa
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studypulse.sync.StudyPulseApplication
import com.studypulse.sync.data.qa.Citation
import com.studypulse.sync.data.qa.QaService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QaMessage(
    val isUser: Boolean,
    val text: String,
    val citations: List<Citation> = emptyList(),
    val isGeneral: Boolean = false
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
    private val qaService: QaService = container.qaService
    private var materialExternalId: String? = null
    private val _uiState = MutableStateFlow(QaUiState())
    val uiState: StateFlow<QaUiState> = _uiState.asStateFlow()

    fun start(id: String) {
        materialExternalId = id
        viewModelScope.launch {
            _uiState.update { currentState ->
                currentState.copy(isLoading = true)
            }
            val material = container.database.materialDao().getAllActiveOnce()
                .firstOrNull { it.externalId == id }
            if (material == null) {
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
            // Ключевой фикс 4.6: гарантируем, что чанки существуют до первого вопроса.
            runCatching { qaService.chunkMaterial(material) }
            _uiState.update { currentState ->
                currentState.copy(
                    isLoading = false,
                    materialFound = true,
                    materialTitle = material.title,
                    messages = listOf(
                        QaMessage(
                            isUser = false,
                            text = "Задай вопрос по материалу «${material.title}».\n" +
                                "Ответы основаны на ваших материалах с цитатами на источник."
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
        val currentMaterialId = materialExternalId
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
            val result = qaService.askQuestion(currentMaterialId, question, allowGeneral = false)
            _uiState.update { currentState ->
                result.fold(
                    onSuccess = { answer ->
                        val message = if (answer.hasRelevantContext) {
                            QaMessage(
                                isUser = false,
                                text = answer.text,
                                citations = answer.citations,
                                isGeneral = answer.isGeneral
                            )
                        } else {
                            QaMessage(
                                isUser = false,
                                text = answer.text + "\n" +
                                    "Можно попробовать режим «Ответить общо» (без цитат).",
                                citations = emptyList(),
                                isGeneral = false
                            )
                        }
                        currentState.copy(
                            isSending = false,
                            messages = currentState.messages + message
                        )
                    },
                    onFailure = { error ->
                        currentState.copy(
                            isSending = false,
                            messages = currentState.messages + QaMessage(
                                isUser = false,
                                text = "Ошибка: ${error.message ?: "Не удалось получить ответ"}"
                            )
                        )
                    }
                )
            }
        }
    }

    fun sendGeneral() {
        val question = _uiState.value.messages.lastOrNull { it.isUser }?.text ?: return
        _uiState.update { currentState ->
            currentState.copy(
                isSending = true,
                messages = currentState.messages + QaMessage(isUser = true, text = "[Общий режим] $question")
            )
        }
        viewModelScope.launch {
            val answer = qaService.answerGeneral(question)
            _uiState.update { currentState ->
                currentState.copy(
                    isSending = false,
                    messages = currentState.messages + QaMessage(
                        isUser = false,
                        text = answer.text,
                        citations = emptyList(),
                        isGeneral = true
                    )
                )
            }
        }
    }
}