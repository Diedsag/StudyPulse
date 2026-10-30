package com.studypulse.sync.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.studypulse.sync.data.local.entity.CardEntity
import com.studypulse.sync.data.local.entity.MaterialEntity
import com.studypulse.sync.data.local.entity.ScheduleItemEntity
import com.studypulse.sync.data.local.entity.StudyPackageEntity
import com.studypulse.sync.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PackageDao {

    @Query(
        "SELECT * FROM packages " +
            "ORDER BY isDownloaded DESC, title COLLATE NOCASE ASC"
    )
    fun observeAll(): Flow<List<StudyPackageEntity>>

    @Query("SELECT * FROM packages")
    suspend fun getAllOnce(): List<StudyPackageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<StudyPackageEntity>)
}

@Dao
interface ScheduleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ScheduleItemEntity>)

    @Query("DELETE FROM schedule_items WHERE packageExternalId = :packageExternalId")
    suspend fun deleteByPackage(packageExternalId: String)
}

@Dao
interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TaskEntity>)

    @Query("DELETE FROM tasks WHERE packageExternalId = :packageExternalId")
    suspend fun deleteByPackage(packageExternalId: String)
}

@Dao
interface MaterialDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MaterialEntity>)

    @Query("DELETE FROM materials WHERE packageExternalId = :packageExternalId")
    suspend fun deleteByPackage(packageExternalId: String)
}

@Dao
interface CardDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CardEntity>)

    @Query("DELETE FROM cards WHERE packageExternalId = :packageExternalId")
    suspend fun deleteByPackage(packageExternalId: String)
}