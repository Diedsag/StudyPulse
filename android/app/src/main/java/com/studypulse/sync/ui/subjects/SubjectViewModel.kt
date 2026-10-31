package com.studypulse.sync.ui.subjects

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studypulse.sync.StudyPulseApplication
import com.studypulse.sync.data.local.entity.SubjectEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

object SubjectPalette {
    val colors = listOf(
        0xFF3F51B5.toInt(),
        0xFF009688.toInt(),
        0xFFFF9800.toInt(),
        0xFFE91E63.toInt(),
        0xFF607D8B.toInt(),
        0xFF795548.toInt(),
        0xFF388E3C.toInt(),
        0xFF5E35B1.toInt()
    )
}

data class SubjectUiState(
    val subjects: List<SubjectEntity> = emptyList(),
    val isFormOpen: Boolean = false,
    val editingId: Long? = null,
    val name: String = "",
    val teacher: String = "",
    val colorArgb: Int = SubjectPalette.colors.first(),
    val message: String? = null
)

class SubjectViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val container = (application as StudyPulseApplication).container
    private val localRepository = container.localRepository

    private val _uiState = MutableStateFlow(SubjectUiState())
    val uiState: StateFlow<SubjectUiState> = _uiState.asStateFlow()

    init {
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
                editingId = null,
                name = "",
                teacher = "",
                colorArgb = SubjectPalette.colors.first(),
                message = null
            )
        }
    }

    fun startEdit(subject: SubjectEntity) {
        _uiState.update { currentState ->
            currentState.copy(
                isFormOpen = true,
                editingId = subject.id,
                name = subject.name,
                teacher = subject.teacher.orEmpty(),
                colorArgb = subject.colorArgb,
                message = null
            )
        }
    }

    fun closeForm() {
        _uiState.update { currentState ->
            currentState.copy(
                isFormOpen = false,
                editingId = null,
                message = null
            )
        }
    }

    fun onNameChange(value: String) {
        _uiState.update { currentState ->
            currentState.copy(name = value)
        }
    }

    fun onTeacherChange(value: String) {
        _uiState.update { currentState ->
            currentState.copy(teacher = value)
        }
    }

    fun selectColor(color: Int) {
        _uiState.update { currentState ->
            currentState.copy(colorArgb = color)
        }
    }

    fun save() {
        viewModelScope.launch {
            val state = _uiState.value
            val teacher = state.teacher.trim().takeIf { it.isNotBlank() }

            val result = if (state.editingId == null) {
                localRepository.createSubject(
                    name = state.name,
                    colorArgb = state.colorArgb,
                    teacher = teacher
                )
            } else {
                localRepository.updateSubject(
                    id = state.editingId,
                    name = state.name,
                    colorArgb = state.colorArgb,
                    teacher = teacher
                )
            }

            _uiState.update { currentState ->
                result.fold(
                    onSuccess = {
                        currentState.copy(
                            isFormOpen = false,
                            editingId = null,
                            name = "",
                            teacher = "",
                            colorArgb = SubjectPalette.colors.first(),
                            message = "Предмет сохранён"
                        )
                    },
                    onFailure = { error ->
                        currentState.copy(
                            message = error.message ?: "Не удалось сохранить предмет"
                        )
                    }
                )
            }
        }
    }

    fun delete(subject: SubjectEntity) {
        viewModelScope.launch {
            val result = localRepository.deleteSubject(subject.id)

            _uiState.update { currentState ->
                result.fold(
                    onSuccess = {
                        currentState.copy(
                            message = "Предмет удалён",
                            isFormOpen = if (currentState.editingId == subject.id) false else currentState.isFormOpen,
                            editingId = if (currentState.editingId == subject.id) null else currentState.editingId
                        )
                    },
                    onFailure = { error ->
                        currentState.copy(
                            message = error.message ?: "Не удалось удалить предмет"
                        )
                    }
                )
            }
        }
    }
}