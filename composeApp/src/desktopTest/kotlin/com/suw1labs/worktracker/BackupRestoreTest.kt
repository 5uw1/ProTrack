package com.suw1labs.worktracker

import androidx.room.Room
import androidx.room.execSQL
import androidx.room.useWriterConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.suw1labs.worktracker.data.AppDatabase
import com.suw1labs.worktracker.data.DatabaseCreationTracker
import com.suw1labs.worktracker.data.backup.BackupCodec
import com.suw1labs.worktracker.data.backup.BackupFile
import com.suw1labs.worktracker.data.backup.BackupManager
import com.suw1labs.worktracker.data.buildAppDatabase
import com.suw1labs.worktracker.data.model.AppSettings
import com.suw1labs.worktracker.data.model.AttendanceSession
import com.suw1labs.worktracker.data.model.DayRecord
import com.suw1labs.worktracker.data.model.Project
import com.suw1labs.worktracker.data.model.TimeEntry
import com.suw1labs.worktracker.data.model.WorkTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Export on one database, import on another: the round trip the user makes between two devices. */
class BackupRestoreTest {

    private fun openDatabase(): AppDatabase = Room.inMemoryDatabaseBuilder<AppDatabase>()
        .setDriver(BundledSQLiteDriver())
        .buildAppDatabase(DatabaseCreationTracker(), Dispatchers.IO)

    private suspend fun AppDatabase.fillWithSampleData(): Long {
        seedDefaults()
        val projectId = projectDao().insertProject(Project(code = "P-2026-0142", name = "Spindle retrofit", client = "Customer AG", budgetHours = 80.0))
        val taskId = workTaskDao().insertTask(WorkTask(projectId = projectId, title = "PLC", deadlineTimestamp = 9_999_999_999_999L))
        timeEntryDao().insertEntry(TimeEntry(projectId = projectId, taskId = taskId, description = "wiring", startTime = 1_000, endTime = 5_000))
        timeEntryDao().insertEntry(TimeEntry(projectId = null, taskId = null, startTime = 6_000, endTime = null))
        attendanceDao().insertSession(AttendanceSession(clockIn = 1_000, clockOut = 5_000, clockOutReason = "LUNCH"))
        attendanceDao().insertSession(AttendanceSession(clockIn = 6_000))
        dayRecordDao().insert(DayRecord(dayStart = 86_400_000L, type = "SICK", hours = 8.0, note = "flu"))
        settingsDao().upsert(AppSettings(language = "de", paidBreakMinutes = 15, weekdayHours = "8.5,8.5,8.5,8.5,0,0,0"))
        return taskId
    }

    @Test
    fun exportThenRestoreOnAnotherDevice_reproducesEveryTable() = runTest {
        val source = openDatabase()
        val taskId = source.fillWithSampleData()
        val backup = BackupManager(source, "desktop").createBackup(now = 1_800_000_000_000L)
        val text = BackupCodec.encode(backup)
        source.close()

        val target = openDatabase()
        // The other device already has its own (different) data.
        target.seedDefaults()
        target.projectDao().insertProject(Project(code = "OLD", name = "Left over"))
        target.timeEntryDao().insertEntry(TimeEntry(startTime = 1, endTime = 2))

        val summary = BackupManager(target, "android").restore(BackupCodec.decode(text))

        assertEquals(backup.summary, summary)
        assertEquals(backup.projects, target.backupDao().allProjects())
        assertEquals(backup.tasks, target.backupDao().allTasks())
        assertEquals(backup.timeEntries, target.backupDao().allTimeEntries())
        assertEquals(backup.attendanceSessions, target.backupDao().allSessions())
        assertEquals(backup.dayRecords, target.backupDao().allDayRecords())
        assertEquals(backup.settings, target.backupDao().settings())

        // Relations still resolve through the joined queries the UI uses.
        val entries = target.timeEntryDao().getAllEntriesWithDetails().first()
        val wiring = entries.first { it.description == "wiring" }
        assertEquals("P-2026-0142", wiring.projectCode)
        assertEquals("PLC", wiring.taskTitle)
        assertEquals(taskId, wiring.taskId)
        assertNotNull(target.attendanceDao().getOpenSession())
        assertNull(target.projectDao().findByCode("OLD"))
        target.close()
    }

    @Test
    fun restore_ofBackupWithoutDefaults_recreatesBuiltInProjectAndSettings() = runTest {
        val db = openDatabase()
        db.fillWithSampleData()

        BackupManager(db).restore(BackupFile(exportedAt = 1L, projects = listOf(Project(id = 5, code = "P-9", name = "Only project"))))

        assertEquals(listOf("P-9", Project.UNPRODUCTIVE_CODE), db.backupDao().allProjects().map { it.code })
        assertEquals(AppSettings(), db.backupDao().settings())
        assertTrue(db.backupDao().allTimeEntries().isEmpty())
        assertEquals(AppDatabase.DEFAULT_UNPRODUCTIVE_TASKS.size, db.backupDao().allTasks().size)
        db.close()
    }

    @Test
    fun restore_isAtomic_whenAWriteFailsHalfway() = runTest {
        val db = openDatabase()
        db.fillWithSampleData()
        val before = BackupManager(db).createBackup(now = 1L)

        // Simulate a failure in the middle of the restore (constraint violation, full disk, …).
        db.useWriterConnection { it.execSQL("CREATE TRIGGER fail_restore BEFORE INSERT ON day_records BEGIN SELECT RAISE(ABORT, 'disk full'); END") }
        val failed = runCatching { BackupManager(db).restore(before) }.isFailure
        db.useWriterConnection { it.execSQL("DROP TRIGGER fail_restore") }

        assertTrue(failed)
        assertEquals(before.copy(exportedAt = 2L), BackupManager(db).createBackup(now = 2L))
        db.close()
    }
}
