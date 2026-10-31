package com.studypulse.sync.ui.tasks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studypulse.sync.StudyPulseApplication
import com.studypulse.sync.data.local.entity.SubjectEntity
import com.studypulse.sync.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TasksUiState(
    val tasks: List<TaskEntity> = emptyList(),
    val subjects: List<SubjectEntity> = emptyList(),
    val isFormOpen: Boolean = false,
    val editingExternalId: String? = null,
    val selectedSubjectId: Long? = null,
    val subjectName: String = "",
    val title: String = "",
    val deadlineDate: String = LocalDate.now().plusDays(2).toString(),
    val effortMinutes: String = "60",
    val remainingMinutes: Int = 60,
    val priority: Int = 2,
    val status: String = "PENDING",
    val description: String = "",
    val message: String? = null
)

class TasksViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val container = (application as StudyPulseApplication).container
    private val studyRepository = container.studyRepository
    private val localRepository = container.localRepository

    private val _uiState = MutableStateFlow(TasksUiState())
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            studyRepository.tasks.collect { tasks ->
                _uiState.update { currentState ->
                    currentState.copy(tasks = tasks)
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
                deadlineDate = LocalDate.now().plusDays(2).toString(),
                effortMinutes = "60",
                remainingMinutes = 60,
                priority = 2,
                status = "PENDING",
                description = "",
                message = null
            )
        }
    }

    fun startEdit(task: TaskEntity) {
        if (task.source != "LOCAL") {
            _uiState.update { currentState ->
                currentState.copy(message = "Редактировать можно только локальные задания")
            }
            return
        }

        _uiState.update { currentState ->
            currentState.copy(
                isFormOpen = true,
                editingExternalId = task.externalId,
                selectedSubjectId = task.subjectId,
                subjectName = task.subject.orEmpty(),
                title = task.title,
                deadlineDate = task.deadlineDate,
                effortMinutes = task.effortMinutes.toString(),
                remainingMinutes = task.remainingMinutes,
                priority = task.priority,
                status = task.status,
                description = task.description.orEmpty(),
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

    fun onSubjectNameChange(value: String) {
        _uiState.update { currentState ->
            currentState.copy(
                subjectName = value,
                selectedSubjectId = null
            )
        }
    }

    fun onTitleChange(value: String) {
        _uiState.update { currentState ->
            currentState.copy(title = value)
        }
    }

    fun onDeadlineChange(value: String) {
        _uiState.update { currentState ->
            currentState.copy(deadlineDate = value)
        }
    }

    fun onEffortChange(value: String) {
        _uiState.update { currentState ->
            val parsed = value.toIntOrNull() ?: 0
            currentState.copy(
                effortMinutes = value,
                remainingMinutes = if (currentState.editingExternalId == null) parsed else currentState.remainingMinutes
            )
        }
    }

    fun onRemainingChange(value: String) {
        _uiState.update { currentState ->
            currentState.copy(remainingMinutes = value.toIntOrNull() ?: 0)
        }
    }

    fun onPriorityChange(value: Int) {
        _uiState.update { currentState ->
            currentState.copy(priority = value)
        }
    }

    fun onDescriptionChange(value: String) {
        _uiState.update { currentState ->
            currentState.copy(description = value)
        }
    }

    fun save() {
        viewModelScope.launch {
            val state = _uiState.value
            val effort = state.effortMinutes.toIntOrNull() ?: 0

            val result = if (state.editingExternalId == null) {
                localRepository.createTask(
                    subjectId = state.selectedSubjectId,
                    subjectName = state.subjectName,
                    title = state.title,
                    deadlineDate = state.deadlineDate,
                    effortMinutes = effort,
                    priority = state.priority,
                    description = state.description
                )
            } else {
                localRepository.updateTask(
                    externalId = state.editingExternalId,
                    subjectId = state.selectedSubjectId,
                    subjectName = state.subjectName,
                    title = state.title,
                    deadlineDate = state.deadlineDate,
                    effortMinutes = effort,
                    remainingMinutes = state.remainingMinutes,
                    priority = state.priority,
                    status = state.status,
                    description = state.description
                )
            }

            _uiState.update { currentState ->
                result.fold(
                    onSuccess = {
                        currentState.copy(
                            isFormOpen = false,
                            editingExternalId = null,
                            message = "Задание сохранено"
                        )
                    },
                    onFailure = { error ->
                        currentState.copy(
                            message = error.message ?: "Не удалось сохранить задание"
                        )
                    }
                )
            }
        }
    }

    fun markDone(task: TaskEntity) {
        viewModelScope.launch {
            val result = localRepository.markTaskDone(task.externalId)

            _uiState.update { currentState ->
                result.fold(
                    onSuccess = {
                        currentState.copy(message = "Задание отмечено выполненным")
                    },
                    onFailure = { error ->
                        currentState.copy(
                            message = error.message ?: "Не удалось обновить задание"
                        )
                    }
                )
            }
        }
    }

    fun delete(task: TaskEntity) {
        viewModelScope.launch {
            val result = localRepository.deleteTask(task.externalId)

            _uiState.update { currentState ->
                result.fold(
                    onSuccess = {
                        currentState.copy(message = "Задание удалено")
                    },
                    onFailure = { error ->
                        currentState.copy(
                            message = error.message ?: "Не удалось удалить задание"
                        )
                    }
                )
            }
        }
    }
}