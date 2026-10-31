package com.studypulse.sync.ui.materials

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studypulse.sync.StudyPulseApplication
import com.studypulse.sync.data.local.entity.MaterialEntity
import com.studypulse.sync.data.local.entity.SubjectEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MaterialsUiState(
    val materials: List<MaterialEntity> = emptyList(),
    val subjects: List<SubjectEntity> = emptyList(),
    val isFormOpen: Boolean = false,
    val editingExternalId: String? = null,
    val selectedSubjectId: Long? = null,
    val subjectName: String = "",
    val title: String = "",
    val sourceLabel: String = "",
    val text: String = "",
    val message: String? = null
)

class MaterialsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val container = (application as StudyPulseApplication).container
    private val studyRepository = container.studyRepository
    private val localRepository = container.localRepository

    private val _uiState = MutableStateFlow(MaterialsUiState())
    val uiState: StateFlow<MaterialsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            studyRepository.materials.collect { materials ->
                _uiState.update { currentState ->
                    currentState.copy(materials = materials)
                }
            }
        }

        viewModelScope.launch {
            localRepository.subjects.collect { subjects ->
                _uiState.update { currentState ->
                    currentState.copy(subjects = subjects)
                }
            }
        }
    }

    fun startAdd() {
        _uiState.update { currentState ->
            currentState.copy(
                isFormOpen = true,
                editingExternalId = null,
                selectedSubjectId = null,
                subjectName = "",
                title = "",
                sourceLabel = "",
                text = "",
                message = null
            )
        }
    }

    fun startEdit(material: MaterialEntity) {
        if (material.source != "LOCAL") {
            _uiState.update { currentState ->
                currentState.copy(message = "Редактировать можно только локальные материалы")
            }
            return
        }

        _uiState.update { currentState ->
            currentState.copy(
                isFormOpen = true,
                editingExternalId = material.externalId,
                selectedSubjectId = material.subjectId,
                subjectName = "",
                title = material.title,
                sourceLabel = material.sourceLabel.orEmpty(),
                text = material.rawText ?: material.summaryText,
                message = null
            )
        }
    }

    fun toggleForm() {
        _uiState.update { currentState ->
            currentState.copy(
                isFormOpen = !currentState.isFormOpen,
                message = null
            )
        }
    }

    fun selectSubject(subject: SubjectEntity?) {
        _uiState.update { currentState ->
            currentState.copy(
                selectedSubjectId = subject?.id,
                subjectName = subject?.name.orEmpty()
            )
        }
    }

    fun onTitleChange(value: String) {
        _uiState.update { currentState ->
            currentState.copy(title = value)
        }
    }

    fun onSourceLabelChange(value: String) {
        _uiState.update { currentState ->
            currentState.copy(sourceLabel = value)
        }
    }

    fun onTextChange(value: String) {
        _uiState.update { currentState ->
            currentState.copy(text = value)
        }
    }

    fun save() {
        viewModelScope.launch {
            val state = _uiState.value

            val result = if (state.editingExternalId == null) {
                localRepository.createMaterial(
                    subjectId = state.selectedSubjectId,
                    subjectName = state.subjectName,
                    title = state.title,
                    text = state.text,
                    sourceLabel = state.sourceLabel
                )
            } else {
                localRepository.updateMaterial(
                    externalId = state.editingExternalId,
                    subjectId = state.selectedSubjectId,
                    subjectName = state.subjectName,
                    title = state.title,
                    text = state.text,
                    sourceLabel = state.sourceLabel
                )
            }

            _uiState.update { currentState ->
                result.fold(
                    onSuccess = {
                        currentState.copy(
                            isFormOpen = false,
                            editingExternalId = null,
                            message = "Материал сохранён"
                        )
                    },
                    onFailure = { error ->
                        currentState.copy(
                            message = error.message ?: "Не удалось сохранить материал"
                        )
                    }
                )
            }
        }
    }

    fun delete(material: MaterialEntity) {
        viewModelScope.launch {
            val result = localRepository.deleteMaterial(material.externalId)

            _uiState.update { currentState ->
                result.fold(
                    onSuccess = {
                        currentState.copy(message = "Материал удалён")
                    },
                    onFailure = { error ->
                        currentState.copy(
                            message = error.message ?: "Не удалось удалить материал"
                        )
                    }
                )
            }
        }
    }
}