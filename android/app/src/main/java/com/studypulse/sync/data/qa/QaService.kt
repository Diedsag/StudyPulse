package com.studypulse.sync.data.qa
import com.studypulse.sync.data.local.AppDatabase
import com.studypulse.sync.data.local.entity.MaterialChunkEntity
import com.studypulse.sync.data.local.entity.MaterialEntity
data class Citation(val materialTitle: String, val sourceLabel: String?, val pageNumber: Int?, val timecode: String?, val quotedText: String)
data class QaAnswer(val text: String, val citations: List<Citation>, val isGeneral: Boolean, val hasRelevantContext: Boolean)
class QaService(private val database: AppDatabase) {
    private val chunkDao = database.chunkDao()
    private val materialDao = database.materialDao()
    suspend fun chunkMaterial(material: MaterialEntity): Result<Int> {
        return try {
            val text = material.rawText ?: material.summaryText
            if (text.isBlank()) return Result.success(0)
            chunkDao.deleteByMaterial(material.externalId)
            val chunkSize = 600; val overlap = 100
            val chunks = mutableListOf<MaterialChunkEntity>()
            val paragraphs = text.split("\n", "\r\n").map { it.trim() }.filter { it.isNotBlank() }
            var current = StringBuilder(); var order = 0; val now = System.currentTimeMillis()
            for (p in paragraphs) {
                if (current.length + p.length > chunkSize && current.isNotEmpty()) {
                    chunks += MaterialChunkEntity(material.externalId, current.toString(), order++, null, null, null, now)
                    current = StringBuilder(current.takeLast(overlap))
                }
                current.append(p).append("\n")
            }
            if (current.isNotBlank()) chunks += MaterialChunkEntity(material.externalId, current.toString(), order, null, null, null, now)
            chunkDao.insertAll(chunks); Result.success(chunks.size)
        } catch (e: Throwable) { Result.failure(e) }
    }
    suspend fun askQuestion(materialExternalId: String?, question: String, allowGeneral: Boolean = false): Result<QaAnswer> {
        return try {
            val raw = question.trim(); if (raw.isBlank()) return Result.failure(IllegalArgumentException("Вопрос не может быть пустым"))
            val norm = raw.lowercase(); val keywords = extractKeywords(norm)
            val terms = LinkedHashSet<String>(); terms.addAll(keywords); terms.add(norm)
            val allIds: List<String>? = if (materialExternalId == null) materialDao.getAllActiveOnce().map { it.externalId } else null
            val scored = LinkedHashMap<String, Pair<MaterialChunkEntity, Int>>()
            for (term in terms) {
                if (term.isBlank()) continue
                val whole = term == norm
                for (v in caseVariants(term)) {
                    val found = if (materialExternalId != null) chunkDao.searchByKeyword(materialExternalId, v) else chunkDao.searchByKeywordInMaterials(allIds ?: emptyList(), v)
                    for (c in found) {
                        val key = "${c.materialExternalId}#${c.orderIndex}"; val add = if (whole) 1 else 2; val ex = scored[key]
                        scored[key] = if (ex == null) Pair(c, add) else Pair(ex.first, ex.second + add)
                    }
                }
            }
            val ranked = scored.values.sortedByDescending { it.second }.map { it.first }
            if (ranked.isEmpty()) return Result.success(QaAnswer("В ваших материалах этого нет.", emptyList(), false, false))
            val top = ranked.take(3); val map = materialDao.getAllActiveOnce().associateBy { it.externalId }
            val cites = top.map { c -> val m = map[c.materialExternalId]; Citation(m?.title ?: "Неизвестный материал", m?.sourceLabel, c.pageNumber, formatTimecode(c.timecodeStartMs), c.text) }
            Result.success(QaAnswer(buildAnswer(top), cites, false, true))
        } catch (e: Throwable) { Result.failure(e) }
    }
    fun answerGeneral(question: String) = QaAnswer("Это общий ответ, не основанный на ваших материалах.\nДля точного ответа добавьте материалы и задайте вопрос по ним.", emptyList(), true, false)
    private fun caseVariants(w: String): List<String> { val cap = w.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }; return listOf(w, cap, w.uppercase()).distinct() }
    private fun extractKeywords(text: String): List<String> {
        val stop = setOf("что","как","где","когда","почему","зачем","какой","какая","какие","это","есть","быть","был","была","были","будет","и","или","но","а","в","на","по","с","со","к","ко","о","об","обо","из","от","до","за","перед","над","под","про","через","для","при","после","между","среди")
        return text.split(Regex("\\s+")).map { it.lowercase().replace(Regex("[^а-яa-z0-9]"),"") }.filter { it.length >= 2 && it !in stop }.distinct().take(6)
    }
    private fun buildAnswer(chunks: List<MaterialChunkEntity>): String { val sb = StringBuilder("По вашим материалам:\n"); chunks.forEachIndexed { i, c -> sb.append(c.text.trim()); if (i < chunks.lastIndex) sb.append("\n---\n") }; return sb.toString() }
    private fun formatTimecode(ms: Long?): String? { if (ms == null) return null; val s = ms / 1000; return "%d:%02d".format(s / 60, s % 60) }
}