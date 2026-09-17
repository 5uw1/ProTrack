package com.suw1labs.worktracker.data.backup

import androidx.room.deferredTransaction
import androidx.room.immediateTransaction
import androidx.room.useReaderConnection
import androidx.room.useWriterConnection
import com.suw1labs.worktracker.data.AppDatabase
import com.suw1labs.worktracker.util.currentTimeMillis

/**
 * Reads the whole database into a [BackupFile] and replaces it from one.
 * Restoring is atomic: either every table is replaced or nothing changes.
 */
class BackupManager(private val database: AppDatabase, private val platform: String = "") {

    /** Snapshot of all tables taken inside one read transaction, so the file is internally consistent. */
    suspend fun createBackup(now: Long = currentTimeMillis()): BackupFile {
        val dao = database.backupDao()
        return database.useReaderConnection { transactor ->
            transactor.deferredTransaction {
                BackupFile(
                    exportedAt = now,
                    platform = platform,
                    settings = dao.settings(),
                    projects = dao.allProjects(),
                    tasks = dao.allTasks(),
                    timeEntries = dao.allTimeEntries(),
                    attendanceSessions = dao.allSessions(),
                    dayRecords = dao.allDayRecords()
                )
            }
        }
    }

    /**
     * Replaces all local data with [backup] in a single write transaction, keeping the ids from the
     * file so every relation survives. Afterwards the built-in defaults (settings row, "Unproductive"
     * project) are re-created if the backup did not contain them.
     */
    suspend fun restore(backup: BackupFile): BackupSummary {
        val data = backup.sanitized()
        val dao = database.backupDao()
        database.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                dao.clearTimeEntries()
                dao.clearTasks()
                dao.clearProjects()
                dao.clearSessions()
                dao.clearDayRecords()
                dao.clearSettings()

                dao.insertProjects(data.projects)
                dao.insertTasks(data.tasks)
                dao.insertTimeEntries(data.timeEntries)
                dao.insertSessions(data.attendanceSessions)
                dao.insertDayRecords(data.dayRecords)
                data.settings?.let { dao.insertSettings(it) }
            }
        }
        database.seedDefaults()
        return data.summary
    }
}
