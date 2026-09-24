package com.suw1labs.worktracker

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.suw1labs.worktracker.data.AppDatabase
import com.suw1labs.worktracker.data.DatabaseCreationTracker
import com.suw1labs.worktracker.data.buildAppDatabase
import com.suw1labs.worktracker.data.model.AttendanceSession
import com.suw1labs.worktracker.data.model.DayRecord
import com.suw1labs.worktracker.data.model.Project
import com.suw1labs.worktracker.data.model.TimeEntry
import com.suw1labs.worktracker.data.model.WorkTask
import com.suw1labs.worktracker.data.repository.TimeTrackerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * What every write has to leave behind for a later merge: an id that means the same record on
 * another device, when it last changed, and – for a deletion – a tombstone rather than a gap.
 */
class SyncMetadataTest {

    private fun database(): AppDatabase = Room.inMemoryDatabaseBuilder<AppDatabase>()
        .setDriver(BundledSQLiteDriver())
        .buildAppDatabase(DatabaseCreationTracker(), Dispatchers.IO)

    private fun AppDatabase.repository() = TimeTrackerRepository(
        projectDao(), workTaskDao(), timeEntryDao(), attendanceDao(), dayRecordDao(), settingsDao()
    )

    @Test
    fun everyWriteGetsAUidTheTimeAndTheDevice() = runTest {
        val db = database()
        val repository = db.repository()
        db.seedDefaults()

        val projectId = repository.insertProject(Project(code = "P-1", name = "Retrofit"))
        val taskId = repository.insertTask(WorkTask(projectId = projectId, title = "PLC"))
        repository.insertTimeEntry(TimeEntry(projectId = projectId, taskId = taskId, startTime = 1_000, endTime = 2_000))
        repository.insertSession(AttendanceSession(clockIn = 1_000, clockOut = 2_000))
        repository.insertDayRecord(DayRecord(dayStart = 86_400_000L, type = "SICK", hours = 8.0))

        val project = repository.allProjects.first().first { it.code == "P-1" }
        val task = db.workTaskDao().getTaskById(taskId)
        val entry = db.backupDao().allTimeEntries().first()
        val session = repository.allSessions.first().first()
        val record = repository.allDayRecords.first().first()

        assertEquals(26, project.uid.length, "a project should carry a generated uid")
        assertTrue(project.updatedAt > 0 && project.deviceId.isNotEmpty(), "project: $project")
        assertNotNull(task)
        assertTrue(task.uid.isNotEmpty() && task.updatedAt > 0 && task.deviceId.isNotEmpty(), "task: $task")
        assertTrue(entry.uid.isNotEmpty() && entry.updatedAt > 0, "time entry: $entry")
        assertTrue(session.uid.isNotEmpty() && session.updatedAt > 0, "session: $session")
        assertTrue(record.uid.isNotEmpty() && record.updatedAt > 0, "day record: $record")

        // All five come from the same install, so they agree on who wrote them.
        assertEquals(1, setOf(project.deviceId, task.deviceId, entry.deviceId, session.deviceId, record.deviceId).size)
        db.close()
    }

    @Test
    fun deletingLeavesATombstoneInsteadOfAGap() = runTest {
        val db = database()
        val repository = db.repository()
        db.seedDefaults()

        val projectId = repository.insertProject(Project(code = "P-1", name = "Retrofit"))
        repository.insertTimeEntry(TimeEntry(projectId = projectId, startTime = 1_000, endTime = 2_000))
        val entry = db.backupDao().allTimeEntries().first()

        repository.deleteTimeEntryById(entry.id)
        repository.deleteProjectById(projectId)

        assertTrue(repository.allTimeEntries.first().isEmpty(), "a deleted entry must not show up any more")
        assertTrue(repository.allProjects.first().none { it.id == projectId }, "a deleted project must not show up any more")

        // The row is still there, marked – which is the only way another device can learn about it.
        val deleted = db.backupDao().allTimeEntries().first { it.id == entry.id }
        assertNotNull(deleted.deletedAt, "the row should be tombstoned, not removed")
        assertEquals(deleted.deletedAt, deleted.updatedAt, "a deletion is the record's latest change")
        assertTrue(deleted.deviceId.isNotEmpty(), "a tombstone says which device made it")
        db.close()
    }

    @Test
    fun editingMovesTheRecordForwardAndKeepsItsUid() = runTest {
        val db = database()
        val repository = db.repository()
        db.seedDefaults()

        val projectId = repository.insertProject(Project(code = "P-1", name = "Retrofit"))
        val created = repository.allProjects.first().first { it.id == projectId }
        repository.updateProject(created.copy(name = "Retrofit II"))
        val updated = repository.allProjects.first().first { it.id == projectId }

        assertEquals(created.uid, updated.uid, "an edit must not change what the record is")
        assertTrue(updated.updatedAt > created.updatedAt, "an edit has to sort after the creation")
        db.close()
    }

    @Test
    fun theBuiltInProjectHasTheSameUidOnEveryInstall() = runTest {
        val first = database()
        val second = database()
        first.seedDefaults()
        second.seedDefaults()

        val here = first.projectDao().findByCode(Project.UNPRODUCTIVE_CODE)
        val there = second.projectDao().findByCode(Project.UNPRODUCTIVE_CODE)
        assertNotNull(here)
        assertNotNull(there)
        assertEquals(here.uid, there.uid, "two installs would otherwise merge into two Unproductive projects")

        val hereTasks = first.workTaskDao().getAllTasks().first().map { it.uid }.sorted()
        val thereTasks = second.workTaskDao().getAllTasks().first().map { it.uid }.sorted()
        assertEquals(hereTasks, thereTasks, "the built-in tasks have to match as well")
        first.close()
        second.close()
    }
}
