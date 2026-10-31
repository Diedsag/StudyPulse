package com.studypulse.sync.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.studypulse.sync.data.local.entity.CardEntity
import com.studypulse.sync.data.local.entity.ClassInstanceEntity
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

    @Query(
        "SELECT s.* FROM schedule_items s " +
            "INNER JOIN packages p ON s.packageExternalId = p.externalId " +
            "WHERE p.isDownloaded = 1 " +
            "ORDER BY s.dayOfWeek, s.startTime"
    )
    fun observeDownloaded(): Flow<List<ScheduleItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ScheduleItemEntity>)

    @Query("DELETE FROM schedule_items WHERE packageExternalId = :packageExternalId")
    suspend fun deleteByPackage(packageExternalId: String)
}

@Dao
interface TaskDao {

    @Query(
        "SELECT t.* FROM tasks t " +
            "INNER JOIN packages p ON t.packageExternalId = p.externalId " +
            "WHERE p.isDownloaded = 1 " +
            "ORDER BY t.deadlineDate, t.priority DESC"
    )
    fun observeDownloaded(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE externalId = :externalId")
    suspend fun getById(externalId: String): TaskEntity?

    @Query(
        "UPDATE tasks " +
            "SET status = :status, remainingMinutes = :remainingMinutes " +
            "WHERE externalId = :externalId"
    )
    suspend fun updateStatus(
        externalId: String,
        status: String,
        remainingMinutes: Int
    )

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

@Dao
interface ClassInstanceDao {

    @Query("SELECT * FROM class_instances")
    fun observeAll(): Flow<List<ClassInstanceEntity>>

    @Query(
        "SELECT * FROM class_instances " +
            "WHERE templateExternalId = :templateExternalId AND date = :date"
    )
    suspend fun getOne(
        templateExternalId: String,
        date: String
    ): ClassInstanceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ClassInstanceEntity)
}