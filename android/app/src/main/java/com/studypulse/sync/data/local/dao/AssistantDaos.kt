package com.studypulse.sync.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import com.studypulse.sync.data.local.entity.MaterialEntity
import com.studypulse.sync.data.local.entity.StudySlotEntity

data class SlotFocusInfo(
    @Embedded val slot: StudySlotEntity,
    val taskTitle: String
)

@Dao
interface AssistantDao {

    @Query(
        "SELECT s.*, t.title AS taskTitle " +
            "FROM study_slots s " +
            "INNER JOIN tasks t ON s.taskExternalId = t.externalId " +
            "WHERE s.id = :slotId"
    )
    suspend fun getSlotFocusInfo(slotId: Long): SlotFocusInfo?

    @Query("SELECT * FROM materials WHERE externalId = :materialExternalId")
    suspend fun getMaterialById(materialExternalId: String): MaterialEntity?
}