package com.studypulse.sync.ui.schedule

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studypulse.sync.StudyPulseApplication
import com.studypulse.sync.data.local.entity.ScheduleItemEntity
import com.studypulse.sync.data.local.entity.SubjectEntity
import com.studypulse.sync.data.repository.ClassOccurrence
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ScheduleUiState(
    val occurrences: List<ClassOccurrence> = emptyList(),
    val subjects: List<SubjectEntity> = emptyList(),
    val isFormOpen: Boolean = false,
    val editingExternalId: String? = null,
    val selectedSubjectId: Long? = null,
    val subjectName: String = "",
    val lessonType: String = "лекция",
    val dayOfWeek: Int = LocalDate.now().dayOfWeek.value,
    val startTime: String = "09:00",
    val endTime: String = "10:30",
    val room: String = "",
    val teacher: String = "",
    val parity: String = "EVERY",
    val message: String? = null,
    val snackbar: String? = null,
    val canUndo: Boolean = false
)

class ScheduleViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as StudyPulseApplication).container
    private val studyRepository = container.studyRepository
    private val localRepository = container.localRepository

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    private var undoAction: (suspend () -> Unit)? = null

    init {
        viewModelScope.launch {
            studyRepository.upcomingOccurrences(daysAhead = 7).collect { occurrences ->
                _uiState.update { it.copy(occurrences = occurrences) }
            }
        }
        viewModelScope.launch {
            localRepository.subjects.collect { subjects ->
                _uiState.update { it.copy(subjects = subjects) }
            }
        }
    }

    fun toggleForm() = _uiState.update { it.copy(isFormOpen = !it.isFormOpen, message = null) }

    fun startAdd() {
        _uiState.update {
            it.copy(
                isFormOpen = true, editingExternalId = null, selectedSubjectId = null, subjectName = "",
                lessonType = "лекция", dayOfWeek = LocalDate.now().dayOfWeek.value, startTime = "09:00",
                endTime = "10:30", room = "", teacher = "", parity = "EVERY", message = null
            )
        }
    }

    fun startEdit(occurrence: ClassOccurrence) {
        if (occurrence.source != "LOCAL") {
            _uiState.update { it.copy(message = "Редактировать можно только локальные занятия") }
            return
        }
        _uiState.update {
            it.copy(
                isFormOpen = true, editingExternalId = occurrence.templateExternalId, selectedSubjectId = occurrence.subjectId,
                subjectName = occurrence.subject, lessonType = occurrence.lessonType.orEmpty(), dayOfWeek = occurrence.dayOfWeek,
                startTime = occurrence.startTime, endTime = occurrence.endTime, room = occurrence.room.orEmpty(),
                teacher = occurrence.teacher.orEmpty(), parity = occurrence.parity ?: "EVERY", message = null
            )
        }
    }

    fun selectSubject(subject: SubjectEntity?) = _uiState.update { it.copy(selectedSubjectId = subject?.id, subjectName = subject?.name.orEmpty()) }
    fun onSubjectNameChange(v: String) = _uiState.update { it.copy(subjectName = v, selectedSubjectId = null) }
    fun onLessonTypeChange(v: String) = _uiState.update { it.copy(lessonType = v) }
    fun onDayOfWeekChange(v: Int) = _uiState.update { it.copy(dayOfWeek = v) }
    fun onStartTimeChange(v: String) = _uiState.update { it.copy(startTime = v) }
    fun onEndTimeChange(v: String) = _uiState.update { it.copy(endTime = v) }
    fun onRoomChange(v: String) = _uiState.update { it.copy(room = v) }
    fun onTeacherChange(v: String) = _uiState.update { it.copy(teacher = v) }
    fun onParityChange(v: String) = _uiState.update { it.copy(parity = v) }

    fun save() {
        viewModelScope.launch {
            val s = _uiState.value
            val result = if (s.editingExternalId == null) {
                localRepository.createScheduleTemplate(
                    s.selectedSubjectId, s.subjectName, s.lessonType, s.dayOfWeek,
                    s.startTime, s.endTime, s.room, s.teacher, s.parity
                )
            } else {
                localRepository.updateScheduleTemplate(
                    s.editingExternalId, s.selectedSubjectId, s.subjectName, s.lessonType, s.dayOfWeek,
                    s.startTime, s.endTime, s.room, s.teacher, s.parity
                )
            }
            _uiState.update { cur ->
                result.fold(
                    onSuccess = { cur.copy(isFormOpen = false, editingExternalId = null, message = "Занятие сохранено") },
                    onFailure = { e -> cur.copy(message = e.message ?: "Не удалось сохранить занятие") }
                )
            }
        }
    }

    fun cancelOccurrence(occurrence: ClassOccurrence) {
        viewModelScope.launch {
            val result = studyRepository.cancelOccurrence(occurrence.templateExternalId, occurrence.date)
            _uiState.update { cur ->
                result.fold(
                    onSuccess = {
                        undoAction = { studyRepository.restoreOccurrence(occurrence.templateExternalId, occurrence.date) }
                        cur.copy(message = "Занятие отменено", snackbar = "Занятие отменено", canUndo = true)
                    },
                    onFailure = { e -> cur.copy(message = e.message ?: "Не удалось отменить занятие") }
                )
            }
        }
    }

    fun deleteTemplate(occurrence: ClassOccurrence) {
        viewModelScope.launch {
            val snapshot = localRepository.getScheduleTemplate(occurrence.templateExternalId)
            val result = localRepository.deleteScheduleTemplate(occurrence.templateExternalId)
            _uiState.update { cur ->
                result.fold(
                    onSuccess = {
                        undoAction = { snapshot?.let { localRepository.restoreScheduleTemplate(it) } }
                        cur.copy(message = "Шаблон занятия удалён", snackbar = "Занятие удалено", canUndo = true)
                    },
                    onFailure = { e -> cur.copy(message = e.message ?: "Не удалось удалить шаблон") }
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