package com.studypulse.sync.ui.schedule.import

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studypulse.sync.StudyPulseApplication
import com.studypulse.sync.data.import.ParsedLesson
import com.studypulse.sync.data.import.ScheduleParserService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ImportLessonState(
    val parsed: ParsedLesson,
    var selectedSubjectId: Long? = null,
    var subjectName: String = parsed.subject,
    var lessonType: String = parsed.lessonType.orEmpty(),
    var dayOfWeek: Int = parsed.dayOfWeek,
    var startTime: String = parsed.startTime,
    var endTime: String = parsed.endTime,
    var room: String = parsed.room.orEmpty(),
    var teacher: String = parsed.teacher.orEmpty(),
    var parity: String = "EVERY",
    var isChecked: Boolean = true
)

data class ScheduleImportUiState(
    val inputText: String = "",
    val lessons: List<ImportLessonState> = emptyList(),
    val isParsing: Boolean = false,
    val isImporting: Boolean = false,
    val message: String? = null,
    val importedCount: Int = 0
)

class ScheduleImportViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as StudyPulseApplication).container
    private val localRepository = container.localRepository
    private val parser = ScheduleParserService()

    private val _uiState = MutableStateFlow(ScheduleImportUiState())
    val uiState: StateFlow<ScheduleImportUiState> = _uiState.asStateFlow()

    fun onInputChange(value: String) {
        _uiState.update { it.copy(inputText = value, lessons = emptyList(), message = null, importedCount = 0) }
    }

    fun parse() {
        val text = _uiState.value.inputText
        if (text.isBlank()) {
            _uiState.update { it.copy(message = "Вставьте текст расписания") }
            return
        }

        _uiState.update { it.copy(isParsing = true, message = null) }

        viewModelScope.launch {
            val parsed = parser.parse(text)

            if (parsed.isEmpty()) {
                _uiState.update { it.copy(isParsing = false, message = "Не удалось распознать занятия. Проверьте формат.") }
                return@launch
            }

            val states = parsed.map { lesson -> ImportLessonState(parsed = lesson) }
            _uiState.update {
                it.copy(
                    isParsing = false,
                    lessons = states,
                    message = "Распознано ${parsed.size} занятий"
                )
            }
        }
    }

    fun toggleLesson(index: Int) {
        _uiState.update { state ->
            val updated = state.lessons.toMutableList()
            if (index in updated.indices) {
                val lesson = updated[index]
                updated[index] = lesson.copy(isChecked = !lesson.isChecked)
            }
            state.copy(lessons = updated)
        }
    }

    // ИСПРАВЛЕНО: теперь принимает готовый объект newLesson, а не функцию
    fun updateLesson(index: Int, newLesson: ImportLessonState) {
        _uiState.update { state ->
            val updated = state.lessons.toMutableList()
            if (index in updated.indices) {
                updated[index] = newLesson
            }
            state.copy(lessons = updated)
        }
    }

    fun import() {
        val lessons = _uiState.value.lessons.filter { it.isChecked }
        if (lessons.isEmpty()) {
            _uiState.update { it.copy(message = "Выберите хотя бы одно занятие для импорта") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, message = null) }

            var successCount = 0
            var failCount = 0

            for (lesson in lessons) {
                val result = localRepository.createScheduleTemplate(
                    subjectId = lesson.selectedSubjectId,
                    subjectName = lesson.subjectName,
                    lessonType = lesson.lessonType,
                    dayOfWeek = lesson.dayOfWeek,
                    startTime = lesson.startTime,
                    endTime = lesson.endTime,
                    room = lesson.room,
                    teacher = lesson.teacher,
                    parity = lesson.parity
                )
                if (result.isSuccess) successCount++ else failCount++
            }

            _uiState.update {
                it.copy(
                    isImporting = false,
                    importedCount = successCount,
                    lessons = emptyList(),
                    inputText = "",
                    message = "Импортировано $successCount занятий" +
                        if (failCount > 0) ", не удалось: $failCount" else ""
                )
            }
        }
    }
}