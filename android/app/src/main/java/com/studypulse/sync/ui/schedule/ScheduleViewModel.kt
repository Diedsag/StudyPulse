package com.studypulse.sync.ui.schedule

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studypulse.sync.StudyPulseApplication
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
    val message: String? = null
)

class ScheduleViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val container = (application as StudyPulseApplication).container
    private val studyRepository = container.studyRepository
    private val localRepository = container.localRepository

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            studyRepository.upcomingOccurrences(daysAhead = 7).collect { occurrences ->
                _uiState.update { currentState ->
                    currentState.copy(occurrences = occurrences)
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

    fun toggleForm() {
        _uiState.update { currentState ->
            currentState.copy(
                isFormOpen = !currentState.isFormOpen,
                message = null
            )
        }
    }

    fun startAdd() {
        _uiState.update { currentState ->
            currentState.copy(
                isFormOpen = true,
                editingExternalId = null,
                selectedSubjectId = null,
                subjectName = "",
                lessonType = "лекция",
                dayOfWeek = LocalDate.now().dayOfWeek.value,
                startTime = "09:00",
                endTime = "10:30",
                room = "",
                teacher = "",
                parity = "EVERY",
                message = null
            )
        }
    }

    fun startEdit(occurrence: ClassOccurrence) {
        if (occurrence.source != "LOCAL") {
            _uiState.update { currentState ->
                currentState.copy(message = "Редактировать можно только локальные занятия")
            }
            return
        }

        _uiState.update { currentState ->
            currentState.copy(
                isFormOpen = true,
                editingExternalId = occurrence.templateExternalId,
                selectedSubjectId = occurrence.subjectId,
                subjectName = occurrence.subject,
                lessonType = occurrence.lessonType.orEmpty(),
                dayOfWeek = occurrence.dayOfWeek,
                startTime = occurrence.startTime,
                endTime = occurrence.endTime,
                room = occurrence.room.orEmpty(),
                teacher = occurrence.teacher.orEmpty(),
                parity = occurrence.parity ?: "EVERY",
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

    fun onLessonTypeChange(value: String) {
        _uiState.update { currentState ->
            currentState.copy(lessonType = value)
        }
    }

    fun onDayOfWeekChange(value: Int) {
        _uiState.update { currentState ->
            currentState.copy(dayOfWeek = value)
        }
    }

    fun onStartTimeChange(value: String) {
        _uiState.update { currentState ->
            currentState.copy(startTime = value)
        }
    }

    fun onEndTimeChange(value: String) {
        _uiState.update { currentState ->
            currentState.copy(endTime = value)
        }
    }

    fun onRoomChange(value: String) {
        _uiState.update { currentState ->
            currentState.copy(room = value)
        }
    }

    fun onTeacherChange(value: String) {
        _uiState.update { currentState ->
            currentState.copy(teacher = value)
        }
    }

    fun onParityChange(value: String) {
        _uiState.update { currentState ->
            currentState.copy(parity = value)
        }
    }

    fun save() {
        viewModelScope.launch {
            val state = _uiState.value

            val result = if (state.editingExternalId == null) {
                localRepository.createScheduleTemplate(
                    subjectId = state.selectedSubjectId,
                    subjectName = state.subjectName,
                    lessonType = state.lessonType,
                    dayOfWeek = state.dayOfWeek,
                    startTime = state.startTime,
                    endTime = state.endTime,
                    room = state.room,
                    teacher = state.teacher,
                    parity = state.parity
                )
            } else {
                localRepository.updateScheduleTemplate(
                    externalId = state.editingExternalId,
                    subjectId = state.selectedSubjectId,
                    subjectName = state.subjectName,
                    lessonType = state.lessonType,
                    dayOfWeek = state.dayOfWeek,
                    startTime = state.startTime,
                    endTime = state.endTime,
                    room = state.room,
                    teacher = state.teacher,
                    parity = state.parity
                )
            }

            _uiState.update { currentState ->
                result.fold(
                    onSuccess = {
                        currentState.copy(
                            isFormOpen = false,
                            editingExternalId = null,
                            message = "Занятие сохранено"
                        )
                    },
                    onFailure = { error ->
                        currentState.copy(
                            message = error.message ?: "Не удалось сохранить занятие"
                        )
                    }
                )
            }
        }
    }

    fun cancelOccurrence(occurrence: ClassOccurrence) {
        viewModelScope.launch {
            studyRepository.cancelOccurrence(
                templateExternalId = occurrence.templateExternalId,
                date = occurrence.date
            )

            _uiState.update { currentState ->
                currentState.copy(message = "Занятие отменено")
            }
        }
    }

    fun deleteTemplate(occurrence: ClassOccurrence) {
        viewModelScope.launch {
            val result = localRepository.deleteScheduleTemplate(occurrence.templateExternalId)

            _uiState.update { currentState ->
                result.fold(
                    onSuccess = {
                        currentState.copy(message = "Шаблон занятия удалён")
                    },
                    onFailure = { error ->
                        currentState.copy(
                            message = error.message ?: "Не удалось удалить шаблон"
                        )
                    }
                )
            }
        }
    }
}