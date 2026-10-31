package com.studypulse.sync.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.studypulse.sync.data.local.dao.CardDao
import com.studypulse.sync.data.local.dao.ClassInstanceDao
import com.studypulse.sync.data.local.dao.MaterialDao
import com.studypulse.sync.data.local.dao.PackageDao
import com.studypulse.sync.data.local.dao.PendingEventDao
import com.studypulse.sync.data.local.dao.ReviewLogDao
import com.studypulse.sync.data.local.dao.ScheduleDao
import com.studypulse.sync.data.local.dao.StudySlotDao
import com.studypulse.sync.data.local.dao.TaskDao
import com.studypulse.sync.data.local.entity.CardEntity
import com.studypulse.sync.data.local.entity.ClassInstanceEntity
import com.studypulse.sync.data.local.entity.MaterialEntity
import com.studypulse.sync.data.local.entity.PendingEventEntity
import com.studypulse.sync.data.local.entity.ReviewLogEntity
import com.studypulse.sync.data.local.entity.ScheduleItemEntity
import com.studypulse.sync.data.local.entity.StudyPackageEntity
import com.studypulse.sync.data.local.entity.StudySlotEntity
import com.studypulse.sync.data.local.entity.TaskEntity

@Database(
    entities = [
        StudyPackageEntity::class,
        ScheduleItemEntity::class,
        TaskEntity::class,
        MaterialEntity::class,
        CardEntity::class,
        ClassInstanceEntity::class,
        StudySlotEntity::class,
        ReviewLogEntity::class,
        PendingEventEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun packageDao(): PackageDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun taskDao(): TaskDao
    abstract fun materialDao(): MaterialDao
    abstract fun cardDao(): CardDao
    abstract fun classInstanceDao(): ClassInstanceDao
    abstract fun studySlotDao(): StudySlotDao
    abstract fun reviewLogDao(): ReviewLogDao
    abstract fun pendingEventDao(): PendingEventDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "studypulse.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}