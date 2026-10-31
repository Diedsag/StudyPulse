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
    val message: String? = null,
    val snackbar: String? = null,
    val canUndo: Boolean = false
)

class TasksViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as StudyPulseApplication).container
    private val studyRepository = container.studyRepository
    private val localRepository = container.localRepository

    private val _uiState = MutableStateFlow(TasksUiState())
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()

    private var undoAction: (suspend () -> Unit)? = null

    init {
        viewModelScope.launch {
            studyRepository.tasks.collect { tasks ->
                _uiState.update { it.copy(tasks = tasks) }
            }
        }
        viewModelScope.launch {
            localRepository.subjects.collect { subjects ->
                _uiState.update { it.copy(subjects = subjects) }
            }
        }
    }

    fun startAdd() {
        _uiState.update {
            it.copy(
                isFormOpen = true, editingExternalId = null, selectedSubjectId = null,
                subjectName = "", title = "", deadlineDate = LocalDate.now().plusDays(2).toString(),
                effortMinutes = "60", remainingMinutes = 60, priority = 2, status = "PENDING",
                description = "", message = null
            )
        }
    }

    fun startEdit(task: TaskEntity) {
        if (task.source != "LOCAL") {
            _uiState.update { it.copy(message = "Редактировать можно только локальные задания") }
            return
        }
        _uiState.update {
            it.copy(
                isFormOpen = true, editingExternalId = task.externalId, selectedSubjectId = task.subjectId,
                subjectName = task.subject.orEmpty(), title = task.title, deadlineDate = task.deadlineDate,
                effortMinutes = task.effortMinutes.toString(), remainingMinutes = task.remainingMinutes,
                priority = task.priority, status = task.status, description = task.description.orEmpty(), message = null
            )
        }
    }

    fun toggleForm() = _uiState.update { it.copy(isFormOpen = !it.isFormOpen, message = null) }
    fun selectSubject(subject: SubjectEntity?) = _uiState.update { it.copy(selectedSubjectId = subject?.id, subjectName = subject?.name.orEmpty()) }
    fun onSubjectNameChange(v: String) = _uiState.update { it.copy(subjectName = v, selectedSubjectId = null) }
    fun onTitleChange(v: String) = _uiState.update { it.copy(title = v) }
    fun onDeadlineChange(v: String) = _uiState.update { it.copy(deadlineDate = v) }
    fun onEffortChange(v: String) = _uiState.update {
        val parsed = v.toIntOrNull() ?: 0
        it.copy(effortMinutes = v, remainingMinutes = if (it.editingExternalId == null) parsed else it.remainingMinutes)
    }
    fun onRemainingChange(v: String) = _uiState.update { it.copy(remainingMinutes = v.toIntOrNull() ?: 0) }
    fun onPriorityChange(v: Int) = _uiState.update { it.copy(priority = v) }
    fun onDescriptionChange(v: String) = _uiState.update { it.copy(description = v) }

    fun save() {
        viewModelScope.launch {
            val s = _uiState.value
            val effort = s.effortMinutes.toIntOrNull() ?: 0
            val result = if (s.editingExternalId == null) {
                localRepository.createTask(s.selectedSubjectId, s.subjectName, s.title, s.deadlineDate, effort, s.priority, s.description)
            } else {
                localRepository.updateTask(
                    s.editingExternalId, s.selectedSubjectId, s.subjectName, s.title, s.deadlineDate,
                    effort, s.remainingMinutes, s.priority, s.status, s.description
                )
            }
            _uiState.update { cur ->
                result.fold(
                    onSuccess = { cur.copy(isFormOpen = false, editingExternalId = null, message = "Задание сохранено") },
                    onFailure = { e -> cur.copy(message = e.message ?: "Не удалось сохранить задание") }
                )
            }
        }
    }

    fun markDone(task: TaskEntity) {
        viewModelScope.launch {
            val snapshot = task
            val result = localRepository.markTaskDone(task.externalId)
            _uiState.update { cur ->
                result.fold(
                    onSuccess = {
                        undoAction = { localRepository.restoreTask(snapshot) }
                        cur.copy(message = "Задание выполнено", snackbar = "Задание отмечено выполненным", canUndo = true)
                    },
                    onFailure = { e -> cur.copy(message = e.message ?: "Не удалось обновить задание") }
                )
            }
        }
    }

    fun delete(task: TaskEntity) {
        viewModelScope.launch {
            val snapshot = task
            val result = localRepository.deleteTask(task.externalId)
            _uiState.update { cur ->
                result.fold(
                    onSuccess = {
                        undoAction = { localRepository.restoreTask(snapshot) }
                        cur.copy(message = "Задание удалено", snackbar = "Задание удалено", canUndo = true)
                    },
                    onFailure = { e -> cur.copy(message = e.message ?: "Не удалось удалить задание") }
                )
            }
        }
    }

    fun undo() {
        viewModelScope.launch {
            val action = undoAction ?: return@launch
            action()
            undoAction = null
            _uiState.update { it.copy(snackbar = "Действие отменено", canUndo = false) }
        }
    }

    fun consumeSnackbar() = _uiState.update { it.copy(snackbar = null, canUndo = false) }
}