package com.studypulse.sync.ui.ocr

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studypulse.sync.StudyPulseApplication
import com.studypulse.sync.data.import.MockOcrResult
import com.studypulse.sync.data.import.MockOcrService
import com.studypulse.sync.data.local.entity.SubjectEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MockOcrUiState(
    val subjects: List<SubjectEntity> = emptyList(),
    val selectedSubjectId: Long? = null,
    val subjectName: String = "",
    val title: String = "",
    val sourceLabel: String = "Фото конспекта",
    val isProcessing: Boolean = false,
    val processingSteps: List<String> = emptyList(),
    val result: MockOcrResult? = null,
    val isSaving: Boolean = false,
    val message: String? = null,
    val saved: Boolean = false
)

class MockOcrViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as StudyPulseApplication).container
    private val mockOcrService = MockOcrService(container.qaService)
    private val localRepository = container.localRepository

    private val _uiState = MutableStateFlow(MockOcrUiState())
    val uiState: StateFlow<MockOcrUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            localRepository.subjects.collect { subjects ->
                _uiState.update { it.copy(subjects = subjects) }
            }
        }
    }

    fun selectSubject(subject: SubjectEntity?) {
        _uiState.update {
            it.copy(
                selectedSubjectId = subject?.id,
                subjectName = subject?.name.orEmpty()
            )
        }
    }

    fun onTitleChange(value: String) = _uiState.update { it.copy(title = value) }
    fun onSourceLabelChange(value: String) = _uiState.update { it.copy(sourceLabel = value) }

    fun process() {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.update { it.copy(message = "Укажите название материала") }
            return
        }

        _uiState.update {
            it.copy(
                isProcessing = true,
                processingSteps = emptyList(),
                result = null,
                message = null,
                saved = false
            )
        }

        viewModelScope.launch {
            val result = mockOcrService.processPhoto(
                title = state.title,
                subjectName = state.subjectName.takeIf { it.isNotBlank() }
            )

            _uiState.update {
                it.copy(
                    isProcessing = false,
                    result = result,
                    processingSteps = result.processingSteps
                )
            }
        }
    }

    fun save() {
        val state = _uiState.value
        val result = state.result ?: return

        _uiState.update { it.copy(isSaving = true, message = null) }

        viewModelScope.launch {
            val saveResult = mockOcrService.saveAsMaterial(
                title = state.title,
                subjectId = state.selectedSubjectId,
                subjectName = state.subjectName,
                sourceLabel = state.sourceLabel,
                result = result
            )

            _uiState.update { currentState ->
                saveResult.fold(
                    onSuccess = {
                        currentState.copy(
                            isSaving = false,
                            saved = true,
                            message = "Материал сохранён и разбит на чанки"
                        )
                    },
                    onFailure = { error ->
                        currentState.copy(
                            isSaving = false,
                            message = error.message ?: "Не удалось сохранить"
                        )
                    }
                )
            }
        }
    }
}