package com.studypulse.sync.data.qa

import com.studypulse.sync.data.local.entity.MaterialEntity
import com.studypulse.sync.data.local.entity.StudySlotEntity
import com.studypulse.sync.data.local.entity.TaskEntity

object MaterialRelevanceScorer {

    fun score(
        slot: StudySlotEntity,
        task: TaskEntity,
        material: MaterialEntity
    ): Int {
        var score = 0

        // Тот же предмет
        if (material.subjectId != null && task.subjectId != null && material.subjectId == task.subjectId) {
            score += 100
        }

        // Тот же пакет
        if (!material.packageExternalId.isNullOrBlank() &&
            !task.packageExternalId.isNullOrBlank() &&
            material.packageExternalId == task.packageExternalId) {
            score += 50
        }

        // Совпадение ключевых слов в названии
        val taskWords = task.title.lowercase().split(Regex("\\s+")).toSet()
        val materialWords = material.title.lowercase().split(Regex("\\s+")).toSet()
        val commonWords = taskWords.intersect(materialWords).filter { it.length > 3 }

        score += commonWords.size * 15

        // Материал имеет саммари
        if (material.summaryText.isNotBlank()) {
            score += 20
        }

        // Материал недавно добавлен (последние 7 дней)
        val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000
        if (material.createdAt >= sevenDaysAgo) {
            score += 10
        }

        return score
    }

    fun findRelevantMaterials(
        slot: StudySlotEntity,
        task: TaskEntity,
        allMaterials: List<MaterialEntity>,
        limit: Int = 3
    ): List<MaterialEntity> {
        return allMaterials
            .map { material -> material to score(slot, task, material) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }
}