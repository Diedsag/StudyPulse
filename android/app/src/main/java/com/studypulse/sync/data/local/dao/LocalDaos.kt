package com.studypulse.sync.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studypulse.sync.data.local.entity.CardEntity
import com.studypulse.sync.data.local.entity.ClassInstanceEntity
import com.studypulse.sync.data.local.entity.MaterialEntity
import com.studypulse.sync.data.local.entity.PendingEventEntity
import com.studypulse.sync.data.local.entity.ReviewLogEntity
import com.studypulse.sync.data.local.entity.ScheduleItemEntity
import com.studypulse.sync.data.local.entity.StudyPackageEntity
import com.studypulse.sync.data.local.entity.StudySlotEntity
import com.studypulse.sync.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

data class SlotWithTaskMaterial(
    @Embedded val slot: StudySlotEntity,
    val taskTitle: String,
    val taskSubject: String?,
    val materialTitle: String?
)

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

    @Query(
        "SELECT s.* FROM schedule_items s " +
            "INNER JOIN packages p ON s.packageExternalId = p.externalId " +
            "WHERE p.isDownloaded = 1"
    )
    suspend fun getAllDownloadedOnce(): List<ScheduleItemEntity>

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

    @Query(
        "SELECT t.* FROM tasks t " +
            "INNER JOIN packages p ON t.packageExternalId = p.externalId " +
            "WHERE p.isDownloaded = 1 " +
                "AND t.status <> 'DONE' " +
                "AND t.remainingMinutes > 0 " +
            "ORDER BY t.deadlineDate ASC, t.priority DESC, t.remainingMinutes DESC"
    )
    suspend fun getActiveOnce(): List<TaskEntity>

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

    @Query(
        "SELECT m.* FROM materials m " +
            "INNER JOIN packages p ON m.packageExternalId = p.externalId " +
            "WHERE p.isDownloaded = 1 " +
            "ORDER BY m.title COLLATE NOCASE ASC"
    )
    fun observeDownloaded(): Flow<List<MaterialEntity>>

    @Query(
        "SELECT m.* FROM materials m " +
            "INNER JOIN packages p ON m.packageExternalId = p.externalId " +
            "WHERE p.isDownloaded = 1"
    )
    suspend fun getAllDownloadedOnce(): List<MaterialEntity>

    @Query(
        "SELECT * FROM materials " +
            "WHERE packageExternalId = :packageExternalId " +
            "ORDER BY title COLLATE NOCASE ASC " +
            "LIMIT 1"
    )
    suspend fun getFirstByPackage(packageExternalId: String): MaterialEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MaterialEntity>)

    @Query("DELETE FROM materials WHERE packageExternalId = :packageExternalId")
    suspend fun deleteByPackage(packageExternalId: String)
}

@Dao
interface CardDao {

    @Query(
        "SELECT * FROM cards " +
            "WHERE materialExternalId = :materialExternalId " +
                "AND dueAt <= :now " +
            "ORDER BY position ASC"
    )
    suspend fun getDueByMaterialOnce(
        materialExternalId: String,
        now: Long
    ): List<CardEntity>

    @Query(
        "SELECT * FROM cards " +
            "WHERE packageExternalId = :packageExternalId " +
                "AND dueAt <= :now " +
            "ORDER BY position ASC"
    )
    suspend fun getDueByPackageOnce(
        packageExternalId: String,
        now: Long
    ): List<CardEntity>

    @Query(
        "SELECT * FROM cards " +
            "WHERE dueAt <= :now " +
            "ORDER BY packageExternalId ASC, position ASC"
    )
    suspend fun getDueAllOnce(now: Long): List<CardEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CardEntity>)

    @Update
    suspend fun update(card: CardEntity)

    @Query("DELETE FROM cards WHERE packageExternalId = :packageExternalId")
    suspend fun deleteByPackage(packageExternalId: String)
}

@Dao
interface ClassInstanceDao {

    @Query("SELECT * FROM class_instances")
    fun observeAll(): Flow<List<ClassInstanceEntity>>

    @Query("SELECT * FROM class_instances")
    suspend fun getAllOnce(): List<ClassInstanceEntity>

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

@Dao
interface StudySlotDao {

    @Query(
        "SELECT s.*, t.title AS taskTitle, t.subject AS taskSubject, m.title AS materialTitle " +
            "FROM study_slots s " +
            "INNER JOIN tasks t ON s.taskExternalId = t.externalId " +
            "LEFT JOIN materials m ON s.materialExternalId = m.externalId " +
            "WHERE s.status = :status " +
            "ORDER BY s.startDateTime ASC"
    )
    fun observeWithTaskMaterial(status: String): Flow<List<SlotWithTaskMaterial>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<StudySlotEntity>)

    @Query("DELETE FROM study_slots WHERE status = :status")
    suspend fun deleteByStatus(status: String)

    @Query("UPDATE study_slots SET status = :newStatus WHERE status = :oldStatus")
    suspend fun updateStatus(oldStatus: String, newStatus: String): Int

    @Query("UPDATE study_slots SET status = :status WHERE id = :id")
    suspend fun markStatus(id: Long, status: String): Int

    @Query("SELECT * FROM study_slots WHERE status = :status")
    suspend fun getAllByStatus(status: String): List<StudySlotEntity>
}

@Dao
interface ReviewLogDao {

    @Insert
    suspend fun insert(item: ReviewLogEntity)
}

@Dao
interface PendingEventDao {

    @Insert
    suspend fun insert(item: PendingEventEntity)

    @Query("SELECT COUNT(*) FROM pending_events WHERE synced = 0")
    fun observeUnsyncedCount(): Flow<Int>

    @Query(
        "SELECT * FROM pending_events " +
            "WHERE synced = 0 " +
            "ORDER BY occurredAt ASC " +
            "LIMIT :limit"
    )
    suspend fun getUnsyncedOnce(limit: Int): List<PendingEventEntity>

    @Query(
        "UPDATE pending_events " +
            "SET synced = 1 " +
            "WHERE clientEventId IN (:ids)"
    )
    suspend fun markSynced(ids: List<String>)

    @Query(
        "UPDATE pending_events " +
            "SET retryCount = retryCount + 1, lastError = :error " +
            "WHERE clientEventId IN (:ids)"
    )
    suspend fun markFailed(ids: List<String>, error: String)
}