package com.studypulse.sync.data.qa

import com.studypulse.sync.data.local.AppDatabase
import com.studypulse.sync.data.local.entity.MaterialChunkEntity
import com.studypulse.sync.data.local.entity.MaterialEntity

data class Citation(
    val materialTitle: String,
    val sourceLabel: String?,
    val pageNumber: Int?,
    val timecode: String?,
    val quotedText: String
)

data class QaAnswer(
    val text: String,
    val citations: List<Citation>,
    val isGeneral: Boolean,
    val hasRelevantContext: Boolean
)

class QaService(
    private val database: AppDatabase
) {

    private val chunkDao = database.chunkDao()
    private val materialDao = database.materialDao()

    suspend fun chunkMaterial(material: MaterialEntity): Result<Int> {
        return try {
            val text = material.rawText ?: material.summaryText

            if (text.isBlank()) {
                return Result.success(0)
            }

            chunkDao.deleteByMaterial(material.externalId)

            val chunkSize = 600
            val overlap = 100
            val chunks = mutableListOf<MaterialChunkEntity>()
            val paragraphs = text.lines()
                .map { it.trim() }
                .filter { it.isNotBlank() }

            var currentChunk = StringBuilder()
            var orderIndex = 0
            val now = System.currentTimeMillis()

            for (paragraph in paragraphs) {
                if (currentChunk.length + paragraph.length > chunkSize && currentChunk.isNotEmpty()) {
                    chunks += MaterialChunkEntity(
                        materialExternalId = material.externalId,
                        text = currentChunk.toString(),
                        orderIndex = orderIndex++,
                        pageNumber = null,
                        timecodeStartMs = null,
                        timecodeEndMs = null,
                        createdAt = now
                    )

                    val overlapText = currentChunk.takeLast(overlap)
                    currentChunk = StringBuilder(overlapText)
                }

                currentChunk.append(paragraph).append(' ')
            }

            if (currentChunk.isNotBlank()) {
                chunks += MaterialChunkEntity(
                    materialExternalId = material.externalId,
                    text = currentChunk.toString(),
                    orderIndex = orderIndex,
                    pageNumber = null,
                    timecodeStartMs = null,
                    timecodeEndMs = null,
                    createdAt = now
                )
            }

            chunkDao.insertAll(chunks)
            Result.success(chunks.size)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun askQuestion(
        materialExternalId: String?,
        question: String,
        allowGeneral: Boolean = false
    ): Result<QaAnswer> {
        return try {
            val rawQuestion = question.trim()

            if (rawQuestion.isBlank()) {
                return Result.failure(IllegalArgumentException("Вопрос не может быть пустым"))
            }

            val normalizedQuestion = rawQuestion.lowercase()
            val keywords = extractKeywords(normalizedQuestion)

            val terms = LinkedHashSet<String>()
            terms.addAll(keywords)
            terms.add(normalizedQuestion)

            val allMaterialIds: List<String>? = if (materialExternalId == null) {
                materialDao.getAllActiveOnce().map { it.externalId }
            } else {
                null
            }

            val scored = linkedMapOf<String, Pair<MaterialChunkEntity, Int>>()

            for (term in terms) {
                if (term.isBlank()) continue

                val isWholeQuestion = term == normalizedQuestion

                for (variant in caseVariants(term)) {
                    val found = if (materialExternalId != null) {
                        chunkDao.searchByKeyword(materialExternalId, variant)
                    } else {
                        chunkDao.searchByKeywordInMaterials(allMaterialIds ?: emptyList(), variant)
                    }

                    for (chunk in found) {
                        val key = "${chunk.materialExternalId}#${chunk.orderIndex}"
                        val addScore = if (isWholeQuestion) 1 else 2
                        val existing = scored[key]

                        scored[key] = if (existing == null) {
                            chunk to addScore
                        } else {
                            existing.first to (existing.second + addScore)
                        }
                    }
                }
            }

            val ranked = scored.values
                .sortedByDescending { it.second }
                .map { it.first }

            if (ranked.isEmpty()) {
                return Result.success(
                    QaAnswer(
                        text = "В ваших материалах этого нет.",
                        citations = emptyList(),
                        isGeneral = false,
                        hasRelevantContext = false
                    )
                )
            }

            val topChunks = ranked.take(3)
            val materialMap = materialDao.getAllActiveOnce().associateBy { it.externalId }

            val citations = topChunks.map { chunk ->
                val material = materialMap[chunk.materialExternalId]

                Citation(
                    materialTitle = material?.title ?: "Неизвестный материал",
                    sourceLabel = material?.sourceLabel,
                    pageNumber = chunk.pageNumber,
                    timecode = formatTimecode(chunk.timecodeStartMs),
                    quotedText = chunk.text
                )
            }

            val answerText = buildAnswer(topChunks)

            Result.success(
                QaAnswer(
                    text = answerText,
                    citations = citations,
                    isGeneral = false,
                    hasRelevantContext = true
                )
            )
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    fun answerGeneral(question: String): QaAnswer {
        val nl = System.lineSeparator()

        return QaAnswer(
            text = "Это общий ответ, не основанный на ваших материалах." +
                nl +
                "Для точного ответа добавьте материалы и задайте вопрос по ним.",
            citations = emptyList(),
            isGeneral = true,
            hasRelevantContext = false
        )
    }

    private fun caseVariants(word: String): List<String> {
        val capitalized = word.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase() else it.toString()
        }

        return listOf(
            word,
            capitalized,
            word.uppercase()
        ).distinct()
    }

    private fun extractKeywords(text: String): List<String> {
        val stopWords = setOf(
            "что", "как", "где", "когда", "почему", "зачем", "какой", "какая", "какие",
            "это", "есть", "быть", "был", "была", "были", "будет", "мне", "можно",
            "расскажи", "объясни", "пожалуйста", "ли", "же", "бы", "ну", "вот",
            "там", "тут", "все", "всё", "сам", "сама",
            "и", "или", "но", "а", "в", "на", "по", "с", "со", "к", "ко",
            "о", "об", "обо", "из", "от", "до", "за", "перед", "над", "под",
            "про", "через", "для", "при", "после", "между", "среди"
        )

        return text.split(' ')
            .map { it.lowercase().replace(Regex("[^а-яa-z0-9]"), "") }
            .filter { it.isNotBlank() && it.length >= 2 && it !in stopWords }
            .distinct()
            .take(6)
    }

    private fun buildAnswer(chunks: List<MaterialChunkEntity>): String {
        val nl = System.lineSeparator()
        val sb = StringBuilder("По вашим материалам:").append(nl)

        chunks.forEachIndexed { index, chunk ->
            sb.append(chunk.text.trim())

            if (index < chunks.lastIndex) {
                sb.append(nl).append("---").append(nl)
            }
        }

        return sb.toString()
    }

    private fun formatTimecode(timecodeMs: Long?): String? {
        if (timecodeMs == null) return null

        val totalSeconds = timecodeMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60

        return "%d:%02d".format(minutes, seconds)
    }
}