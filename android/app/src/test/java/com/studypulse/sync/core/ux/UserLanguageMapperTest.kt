package com.studypulse.sync.core.ux

import org.junit.Assert.assertEquals
import org.junit.Test

class UserLanguageMapperTest {

    @Test
    fun mapsSlotStatusesToHumanLabels() {
        assertEquals("Рекомендуется", UserLanguageMapper.toUserSlotStatus("PROPOSED"))
        assertEquals("Запланировано", UserLanguageMapper.toUserSlotStatus("PLANNED"))
        assertEquals("Сейчас", UserLanguageMapper.toUserSlotStatus("IN_PROGRESS"))
        assertEquals("Выполнено", UserLanguageMapper.toUserSlotStatus("DONE"))
        assertEquals("Пропущено", UserLanguageMapper.toUserSlotStatus("MISSED"))
        assertEquals("-", UserLanguageMapper.toUserSlotStatus(null))
        assertEquals("-", UserLanguageMapper.toUserSlotStatus(""))
        assertEquals("WEIRD", UserLanguageMapper.toUserSlotStatus("WEIRD"))
    }

    @Test
    fun mapsTaskStatusesToHumanLabels() {
        assertEquals("Не начато", UserLanguageMapper.toUserTaskStatus("PENDING"))
        assertEquals("В работе", UserLanguageMapper.toUserTaskStatus("IN_PROGRESS"))
        assertEquals("Выполнено", UserLanguageMapper.toUserTaskStatus("DONE"))
        assertEquals("Просрочено", UserLanguageMapper.toUserTaskStatus("OVERDUE"))
        assertEquals("-", UserLanguageMapper.toUserTaskStatus(null))
        assertEquals("CUSTOM", UserLanguageMapper.toUserTaskStatus("CUSTOM"))
    }

    @Test
    fun mapsSourcesToHumanLabels() {
        assertEquals("Мои данные", UserLanguageMapper.toUserSource("LOCAL"))
        assertEquals("Учебный курс", UserLanguageMapper.toUserSource("PACKAGE"))
        assertEquals("Из материала", UserLanguageMapper.toUserSource("MATERIAL"))
        assertEquals("-", UserLanguageMapper.toUserSource(null))
        assertEquals("OTHER", UserLanguageMapper.toUserSource("OTHER"))
    }

    @Test
    fun handlesLowerCaseAndMixedCaseInternalValues() {
        assertEquals("Запланировано", UserLanguageMapper.toUserSlotStatus("planned"))
        assertEquals("Выполнено", UserLanguageMapper.toUserTaskStatus("Done"))
        assertEquals("Мои данные", UserLanguageMapper.toUserSource("local"))
    }
}