package com.studypulse.sync.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.studypulse.sync.data.local.entity.MaterialChunkEntity

@Dao
interface ChunkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MaterialChunkEntity>)

    @Query("SELECT * FROM material_chunks WHERE materialExternalId = :materialExternalId ORDER BY orderIndex ASC")
    suspend fun getByMaterialOnce(materialExternalId: String): List<MaterialChunkEntity>

    @Query("DELETE FROM material_chunks WHERE materialExternalId = :materialExternalId")
    suspend fun deleteByMaterial(materialExternalId: String)

    @Query(
        "SELECT * FROM material_chunks " +
            "WHERE materialExternalId = :materialExternalId " +
                "AND (text LIKE '%' || :query || '%') " +
            "ORDER BY orderIndex ASC"
    )
    suspend fun searchByKeyword(materialExternalId: String, query: String): List<MaterialChunkEntity>

    @Query(
        "SELECT * FROM material_chunks " +
            "WHERE materialExternalId IN (:materialExternalIds) " +
                "AND (text LIKE '%' || :query || '%') " +
            "ORDER BY materialExternalId, orderIndex ASC"
    )
    suspend fun searchByKeywordInMaterials(materialExternalIds: List<String>, query: String): List<MaterialChunkEntity>
}