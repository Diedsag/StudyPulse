package com.studypulse.sync.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.studypulse.sync.data.local.entity.StatsSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StatsSnapshotDao {

    @Query(
        "SELECT * FROM stats_snapshots " +
            "WHERE period = :period " +
            "LIMIT 1"
    )
    fun observeByPeriod(period: String): Flow<List<StatsSnapshotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: StatsSnapshotEntity)
}