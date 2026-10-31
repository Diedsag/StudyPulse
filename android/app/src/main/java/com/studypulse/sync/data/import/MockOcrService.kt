package com.studypulse.sync.data.import

import com.studypulse.sync.data.local.entity.MaterialEntity
import com.studypulse.sync.data.qa.QaService
import kotlinx.coroutines.delay
import java.util.UUID

data class MockOcrResult(
    val rawText: String,
    val summary: String,
    val keyFacts: List<String>,
    val processingSteps: List<String>
)

class MockOcrService(
    private val qaService: QaService
) {

    private val demoTemplates = listOf(
        DemoTemplate(
            keywords = listOf("физик", "термо", "энерги", "работа"),
            summary = "На лекции разобрали основы термодинамики: первое начало, внутреннюю энергию, работу газа и количество теплоты. Основной акцент сделан на законе сохранения энергии в изолированных системах.",
            facts = listOf(
                "Внутренняя энергия зависит от температуры и агрегатного состояния.",
                "Первое начало термодинамики: ΔU = Q - A.",
                "В изолированной системе полная энергия сохраняется.",
                "Работа газа положительна при расширении."
            )
        ),
        DemoTemplate(
            keywords = listOf("истор", "билет", "реформ", "событ"),
            summary = "Краткие ответы по билетам: ключевые даты, личности, причины событий и последствия для развития государства. Важно знать структуру: дата → причина → событие → итог.",
            facts = listOf(
                "Билеты 1–3 охватывают ранний период.",
                "Билеты 4–6 связаны с реформами.",
                "Билеты 7–10 посвящены внешнеполитическим событиям.",
                "Для каждого билета важна дата, причина и следствие."
            )
        ),
        DemoTemplate(
            keywords = listOf("матем", "теорем", "функци", "производн"),
            summary = "На лекции рассмотрены основные теоремы математического анализа: теорема о среднем значении, теорема Ролля, формула Лагранжа. Разобраны примеры применения к исследованию функций.",
            facts = listOf(
                "Теорема Ролля: если f(a) = f(b), существует точка с f'(c) = 0.",
                "Формула Лагранжа связывает приращение функции с производной.",
                "Теорема о среднем применяется для оценок.",
                "Каждая теорема требует проверки условий применимости."
            )
        )
    )

    suspend fun processPhoto(title: String, subjectName: String?): MockOcrResult {
        val steps = mutableListOf<String>()

        steps += "Загружаем фото..."
        delay(700)

        steps += "Распознаём текст (ML Kit)..."
        delay(900)

        val rawText = generateRawText(title)

        steps += "Извлекаем ключевые фрагменты..."
        delay(600)

        steps += "Строим саммари (локальный шаблон)..."
        delay(500)

        val template = demoTemplates.firstOrNull { tmpl ->
            val combined = (title + " " + (subjectName ?: "")).lowercase()
            tmpl.keywords.any { combined.contains(it) }
        } ?: demoTemplates.first()

        val summary = "Источник: ${subjectName ?: "без предмета"}.\n\n${template.summary}"
        val facts = template.facts

        steps += "Готово"
        delay(300)

        return MockOcrResult(
            rawText = rawText,
            summary = summary,
            keyFacts = facts,
            processingSteps = steps
        )
    }

    suspend fun saveAsMaterial(
        title: String,
        subjectId: Long?,
        subjectName: String?,
        sourceLabel: String,
        result: MockOcrResult
    ): Result<MaterialEntity> {
        val externalId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val entity = MaterialEntity(
            externalId = externalId,
            packageExternalId = null,
            title = title,
            sourceLabel = sourceLabel,
            summaryText = result.summary,
            keyFactsJson = result.keyFacts.joinToString(
                prefix = "[", postfix = "]"
            ) { "\"${it.replace("\"", "\\\"")}\"" },
            subjectId = subjectId,
            source = "LOCAL",
            rawText = result.rawText,
            createdAt = now,
            updatedAt = now
        )

        return try {
            qaService.chunkMaterial(entity)
            Result.success(entity)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    private fun generateRawText(title: String): String {
        return buildString {
            append("Конспект: ").append(title).append("\n\n")
            append("1. Введение\n")
            append("На занятии рассматривались основные понятия темы, связанные с ").append(title.lowercase()).append(".\n\n")
            append("2. Основные определения\n")
            append("- Ключевое понятие формулируется через набор необходимых и достаточных условий.\n")
            append("- Каждое определение подкрепляется примером из практики.\n\n")
            append("3. Ключевые теоремы\n")
            append("- Формулировка теоремы с указанием условий применимости.\n")
            append("- Схема доказательства в три шага.\n\n")
            append("4. Применение\n")
            append("Разобраны два типовых примера и один контрпример для границы условий.\n\n")
            append("5. Домашнее задание\n")
            "Задачи 1–5 из семинара, повторить определения к следующему занятию."
                .also { append(it) }
        }
    }

    private data class DemoTemplate(
        val keywords: List<String>,
        val summary: String,
        val facts: List<String>
    )
}