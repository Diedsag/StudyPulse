package com.studypulse.sync.data.qa

import com.studypulse.sync.data.local.entity.MaterialEntity
import com.studypulse.sync.data.local.entity.StudySlotEntity
import com.studypulse.sync.data.local.entity.TaskEntity
import org.junit.Assert.assertTrue
import org.junit.Test

class MaterialRelevanceScorerTest {

    @Test
    fun `scores material higher when subject matches`() {
        val slot = StudySlotEntity(1, "task1", "pkg1", 0, 0, "PLANNED", null, 0)
        val task = TaskEntity("task1", "pkg1", "Лаба", "Физика", "2026-11-01", 90, 2, "PENDING", 90, 1L, "LOCAL", null, 0, 0)
        val physicsMaterial = MaterialEntity("m1", null, "Лекция 5", null, "Саммари", null, 1L, "LOCAL", null, 0, 0)
        val historyMaterial = MaterialEntity("m2", null, "История", null, "Саммари", null, 2L, "LOCAL", null, 0, 0)

        val scorePhysics = MaterialRelevanceScorer.score(slot, task, physicsMaterial)
        val scoreHistory = MaterialRelevanceScorer.score(slot, task, historyMaterial)

        assertTrue("Physics material should score higher", scorePhysics > scoreHistory)
    }

    @Test
    fun `prioritizes materials with summaries`() {
        val slot = StudySlotEntity(1, "task1", "pkg1", 0, 0, "PLANNED", null, 0)
        val task = TaskEntity("task1", "pkg1", "Лаба", "Физика", "2026-11-01", 90, 2, "PENDING", 90, 1L, "LOCAL", null, 0, 0)
        val withSummary = MaterialEntity("m1", null, "Лекция", null, "Есть саммари", null, 1L, "LOCAL", null, 0, 0)
        val withoutSummary = MaterialEntity("m2", null, "Лекция", null, "", null, 2L, "LOCAL", null, 0, 0)

        val scoreWith = MaterialRelevanceScorer.score(slot, task, withSummary)
        val scoreWithout = MaterialRelevanceScorer.score(slot, task, withoutSummary)

        assertTrue("Material with summary should score higher", scoreWith > scoreWithout)
    }
}