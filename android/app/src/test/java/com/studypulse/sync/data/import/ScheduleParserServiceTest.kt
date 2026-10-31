package com.studypulse.sync.data.import

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleParserServiceTest {

    private val parser = ScheduleParserService()

    @Test
    fun `parses simple schedule with Russian day names`() {
        val text = "Понедельник\n10:00-11:30 Физика, лекция, ауд 204, Иванов А.П.\n14:00-15:30 История, семинар, ауд 312"
        val result = parser.parse(text)
        assertEquals(2, result.size)
        val physics = result[0]
        assertEquals(1, physics.dayOfWeek)
        assertEquals("10:00", physics.startTime)
        assertEquals("11:30", physics.endTime)
        assertTrue(physics.subject.contains("Физика"))
        assertEquals("лекция", physics.lessonType)
        assertEquals("204", physics.room)
        assertTrue(physics.teacher?.contains("Иванов") == true)
    }

    @Test
    fun `returns empty for invalid text`() {
        val result = parser.parse("Просто случайный текст без времени")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `handles multiple days correctly`() {
        val text = "Понедельник\n09:00-10:30 Математика\nСреда\n13:00-14:30 Программирование"
        val result = parser.parse(text)
        assertEquals(2, result.size)
        assertEquals(1, result[0].dayOfWeek)
        assertEquals(3, result[1].dayOfWeek)
    }
}