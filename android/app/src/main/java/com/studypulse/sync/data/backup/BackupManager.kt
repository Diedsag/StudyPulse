package com.studypulse.sync.data.backup

import android.content.Context
import com.studypulse.sync.data.export.PlanExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant

class BackupManager(
    private val context: Context,
    private val planExporter: PlanExporter
) {

    private val backupDir: File
        get() {
            val dir = File(context.filesDir, "backups")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    suspend fun createBackup(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val exportResult = planExporter.exportPlan()
            if (exportResult.isFailure) {
                return@withContext Result.failure(exportResult.exceptionOrNull()!!)
            }

            val json = exportResult.getOrThrow()
            val timestamp = Instant.now().epochSecond
            val backupFile = File(backupDir, "backup-$timestamp.json")

            backupFile.writeText(json)

            cleanupOldBackups()

            Result.success(backupFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreBackup(backupPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(backupPath)
            if (!file.exists()) {
                return@withContext Result.failure(FileNotFoundException("Backup file not found"))
            }

            val json = file.readText()
            val importResult = planExporter.importPlan(json)

            if (importResult.isFailure) {
                return@withContext Result.failure(importResult.exceptionOrNull()!!)
            }

            val importData = importResult.getOrThrow()

            if (!importData.isValid) {
                return@withContext Result.failure(IllegalStateException("Invalid backup data"))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listBackups(): List<BackupInfo> = withContext(Dispatchers.IO) {
        try {
            backupDir.listFiles { file ->
                file.name.endsWith(".json") && file.name.startsWith("backup-")
            }?.map { file ->
                val timestamp = file.name
                    .removePrefix("backup-")
                    .removeSuffix(".json")
                    .toLongOrNull() ?: 0L

                BackupInfo(
                    path = file.absolutePath,
                    timestamp = timestamp,
                    sizeBytes = file.length()
                )
            }?.sortedByDescending { it.timestamp } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun deleteBackup(backupPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(backupPath)
            if (file.exists()) {
                file.delete()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun cleanupOldBackups() {
        val backups = backupDir.listFiles { file ->
            file.name.endsWith(".json") && file.name.startsWith("backup-")
        }?.sortedByDescending { it.lastModified() } ?: return

        if (backups.size > 5) {
            backups.drop(5).forEach { it.delete() }
        }
    }
}

data class BackupInfo(
    val path: String,
    val timestamp: Long,
    val sizeBytes: Long
) {
    fun formattedDate(): String {
        val instant = java.time.Instant.ofEpochSecond(timestamp)
        val formatter = java.time.format.DateTimeFormatter
            .ofPattern("dd.MM.yyyy HH:mm")
            .withZone(java.time.ZoneId.systemDefault())
        return formatter.format(instant)
    }

    fun formattedSize(): String {
        return when {
            sizeBytes < 1024 -> "$sizeBytes B"
            sizeBytes < 1024 * 1024 -> "${sizeBytes / 1024} KB"
            else -> "${sizeBytes / (1024 * 1024)} MB"
        }
    }
}