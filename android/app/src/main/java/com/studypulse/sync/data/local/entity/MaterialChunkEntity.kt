package com.studypulse.sync.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "material_chunks",
    foreignKeys = [
        ForeignKey(
            entity = MaterialEntity::class,
            parentColumns = ["externalId"],
            childColumns = ["materialExternalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("materialExternalId"),
        Index("orderIndex")
    ]
)
data class MaterialChunkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val materialExternalId: String,

    val text: String,
    val orderIndex: Int,

    val pageNumber: Int? = null,
    val timecodeStartMs: Long? = null,
    val timecodeEndMs: Long? = null,

    val createdAt: Long
)