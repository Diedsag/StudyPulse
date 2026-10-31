package com.studypulse.sync.data.import

import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class ParsedLesson(
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String,
    val subject: String,
    val lessonType: String?,
    val room: String?,
    val teacher: String?,
    val confidence: Confidence
) {
    enum class Confidence { HIGH, MEDIUM, LOW }
}

class ScheduleParserService {

    private val timeFormatter = DateTimeFormatter.ofPattern("H:mm")
    private val timeRangeRegex = Regex("""(\d{1,2}:\d{2})\s*[-–—]\s*(\d{1,2}:\d{2})""")
    private val roomRegex = Regex("""(?:ауд(?:итория)?\.?\s*|aud\.?\s*|каб(?:инет)?\.?\s*|#)\s*([A-Za-zА-Яа-я0-9\-/]+)""", RegexOption.IGNORE_CASE)

    private val dayMap = mapOf(
        "понедельник" to 1, "пн" to 1, "пнд" to 1, "monday" to 1, "mon" to 1,
        "вторник" to 2, "вт" to 2, "tuesday" to 2, "tue" to 2,
        "среда" to 3, "ср" to 3, "wednesday" to 3, "wed" to 3,
        "четверг" to 4, "чт" to 4, "thursday" to 4, "thu" to 4,
        "пятница" to 5, "пт" to 5, "friday" to 5, "fri" to 5,
        "суббота" to 6, "сб" to 6, "saturday" to 6, "sat" to 6,
        "воскресенье" to 7, "вс" to 7, "sunday" to 7, "sun" to 7
    )

    private val lessonTypes = listOf(
        "лекция" to "лекция",
        "сем" to "семинар",
        "семинар" to "семинар",
        "лаб" to "лабораторная",
        "лабораторная" to "лабораторная",
        "практика" to "практика",
        "экзамен" to "экзамен",
        "зачет" to "зачёт",
        "зачёт" to "зачёт",
        "consultation" to "консультация",
        "консультация" to "консультация"
    )

    fun parse(text: String): List<ParsedLesson> {
        val results = mutableListOf<ParsedLesson>()
        var currentDay: Int? = null

        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }

        for (line in lines) {
            val lower = line.lowercase()

            // Проверка: это строка с днём недели?
            val dayEntry = dayMap.entries.firstOrNull { (key, _) ->
                lower == key || lower.startsWith("$key ") || lower.startsWith("$key,")
            }

            if (dayEntry != null && !timeRangeRegex.containsMatchIn(line)) {
                currentDay = dayEntry.value
                continue
            }

            // Если есть время — парсим как занятие
            val timeMatch = timeRangeRegex.find(line)
            if (timeMatch != null && currentDay != null) {
                val start = normalizeTime(timeMatch.groupValues[1])
                val end = normalizeTime(timeMatch.groupValues[2])

                if (start == null || end == null) continue
                if (!isValidTimeRange(start, end)) continue

                // Убираем время из строки для поиска остальных полей
                val rest = line.replace(timeMatch.value, "").trim().trim(',', '.', '-')

                val lessonType = lessonTypes.firstOrNull { (keyword, _) ->
                    rest.lowercase().contains(keyword)
                }?.second

                val roomMatch = roomRegex.find(rest)
                val room = roomMatch?.groupValues?.getOrNull(1)

                val cleanedSubject = rest
                    .let { s -> if (lessonType != null) s.replace(Regex("""(?i)\b${Regex.escape(lessonType)}\b"""), "") else s }
                    .let { s -> if (roomMatch != null) s.replace(roomMatch.value, "") else s }
                    .replace(Regex("""[,.\-–—\s]+"""), " ")
                    .trim()

                // Пытаемся выделить предмет и преподавателя (преподаватель часто через запятую или в скобках)
                val subject: String
                val teacher: String?
                if (cleanedSubject.contains(",")) {
                    val parts = cleanedSubject.split(",", limit = 2)
                    subject = parts[0].trim()
                    teacher = parts.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
                } else if (cleanedSubject.contains("(") && cleanedSubject.contains(")")) {
                    val idx = cleanedSubject.indexOf("(")
                    subject = cleanedSubject.substring(0, idx).trim()
                    teacher = cleanedSubject.substring(idx + 1, cleanedSubject.lastIndexOf(")")).trim()
                        .takeIf { it.isNotBlank() }
                } else {
                    subject = cleanedSubject
                    teacher = null
                }

                if (subject.isBlank()) continue

                val confidence = when {
                    lessonType != null && room != null -> ParsedLesson.Confidence.HIGH
                    lessonType != null || room != null -> ParsedLesson.Confidence.MEDIUM
                    else -> ParsedLesson.Confidence.LOW
                }

                results += ParsedLesson(
                    dayOfWeek = currentDay,
                    startTime = start,
                    endTime = end,
                    subject = subject,
                    lessonType = lessonType,
                    room = room,
                    teacher = teacher,
                    confidence = confidence
                )
            }
        }

        return results
    }

    private fun normalizeTime(raw: String): String? {
        return try {
            LocalTime.parse(raw.trim(), timeFormatter).format(timeFormatter)
        } catch (_: Throwable) {
            null
        }
    }

    private fun isValidTimeRange(start: String, end: String): Boolean {
        val s = LocalTime.parse(start, timeFormatter)
        val e = LocalTime.parse(end, timeFormatter)
        return e > s && s >= LocalTime.of(6, 0) && e <= LocalTime.of(23, 30)
    }
}