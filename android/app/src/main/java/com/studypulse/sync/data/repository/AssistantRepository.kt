package com.studypulse.sync.data.repository

import com.studypulse.sync.data.local.AppDatabase
import com.studypulse.sync.data.local.entity.MaterialEntity
import kotlinx.serialization.json.Json

class AssistantRepository(
    private val database: AppDatabase,
    private val studyRepository: StudyRepository
) {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    suspend fun getSlotFocusInfo(slotId: Long) =
        database.assistantDao().getSlotFocusInfo(slotId)

    suspend fun completeSlotById(slotId: Long): Result<SlotCompletionResult> {
        val info = database.assistantDao().getSlotFocusInfo(slotId)
            ?: return Result.failure(IllegalStateException("Учебный слот не найден"))

        return studyRepository.completeSlot(info.slot)
    }

    suspend fun getMaterial(materialExternalId: String): MaterialEntity? =
        database.assistantDao().getMaterialById(materialExternalId)

    fun mockAnswer(material: MaterialEntity, question: String): String {
        val normalized = question.trim().lowercase()
        val facts = parseKeyFacts(material.keyFactsJson)

        return when {
            normalized.isBlank() ->
                "Задай вопрос по материалу."

            normalized.contains("кратко") ||
                normalized.contains("саммари") ||
                normalized.contains("о чем") ||
                normalized.contains("суть") ->
                material.summaryText

            normalized.contains("факт") ||
                normalized.contains("теорем") ||
                normalized.contains("формуł") ||
                normalized.contains("формула") ||
                normalized.contains("определ") ->
                if (facts.isEmpty()) {
                    "В материале нет выделенных ключевых фактов. Вот краткое содержание:\n\n${material.summaryText}"
                } else {
                    "Ключевые факты:\n\n" + facts.joinToString(separator = "\n") { "• $it" }
                }

            normalized.contains("источник") ||
                normalized.contains("цитат") ||
                normalized.contains("где") ->
                "Источник: ${material.sourceLabel ?: material.title}.\n\nЭто локальный демо-ответ, сформированный по сохранённому материалу без использования LLM."

            normalized.contains("задали") ||
                normalized.contains("домашн") ||
                normalized.contains("задан") ->
                "В этом материале нет явного списка заданий. Проверь вкладку «Задания» — там находятся задачи из учебного пакета."

            else -> {
                val base = facts.firstOrNull() ?: material.summaryText.take(220)

                "$base\n\nДемо-ответ сформирован локально по материалам пакета. Настоящий RAG/LLM входит в пост-MVP скоуп."
            }
        }
    }

    private fun parseKeyFacts(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()

        return try {
            json.decodeFromString<List<String>>(raw)
        } catch (_: Throwable) {
            emptyList()
        }
    }
}