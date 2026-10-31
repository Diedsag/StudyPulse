package com.studypulse.sync.core.ux

object UserLanguageMapper {

    fun toUserSlotStatus(raw: String?): String = when (raw?.uppercase()) {
        "PROPOSED" -> "Рекомендуется"
        "PLANNED" -> "Запланировано"
        "IN_PROGRESS" -> "Сейчас"
        "DONE" -> "Выполнено"
        "MISSED" -> "Пропущено"
        null, "" -> "-"
        else -> raw
    }

    fun toUserTaskStatus(raw: String?): String = when (raw?.uppercase()) {
        "PENDING" -> "Не начато"
        "IN_PROGRESS" -> "В работе"
        "DONE" -> "Выполнено"
        "OVERDUE" -> "Просрочено"
        null, "" -> "-"
        else -> raw
    }

    fun toUserSource(raw: String?): String = when (raw?.uppercase()) {
        "LOCAL" -> "Мои данные"
        "PACKAGE" -> "Учебный курс"
        "MATERIAL" -> "Из материала"
        null, "" -> "-"
        else -> raw
    }
}