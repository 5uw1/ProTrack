package com.suw1labs.worktracker

import com.suw1labs.worktracker.data.backup.AutoBackup
import com.suw1labs.worktracker.data.backup.BackupCodec
import com.suw1labs.worktracker.data.backup.BackupError
import com.suw1labs.worktracker.data.backup.BackupException
import com.suw1labs.worktracker.data.backup.BackupFile
import com.suw1labs.worktracker.data.model.Project
import com.suw1labs.worktracker.platform.BackupFolder
import com.suw1labs.worktracker.platform.BackupFolderStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AutoBackupTest {
    /** In-memory folder: files by name, optional failure, count of writes. */
    private class FakeStore(initial: BackupFolder? = BackupFolder("ref", "Drive")) : BackupFolderStore {
        override val folder = MutableStateFlow(initial)
        val files = mutableMapOf<String, String>()
        var writes = 0
        var failWrites = false
        override suspend fun pickFolder(): BackupFolder? = null
        override fun clearFolder() { folder.value = null }
        override suspend fun write(filename: String, content: String) {
            if (failWrites) throw IllegalStateException("folder gone")
            writes++
            files[filename] = content
        }
        override suspend fun read(filename: String): String? = files[filename]
    }

    private var projects = listOf(Project(id = 1, code = "P1", name = "One", client = "", colorHex = "#000000", budgetHours = 0.0))
    private fun backup() = BackupFile(exportedAt = 1_000L, platform = "test", settings = null, projects = projects, tasks = emptyList(), timeEntries = emptyList(), attendanceSessions = emptyList(), dayRecords = emptyList())

    @Test
    fun backupNow_writesOnceForUnchangedData() = runTest {
        val store = FakeStore()
        val auto = AutoBackup(store, ::backup, now = { 42L }, settleMillis = 0)

        assertTrue(auto.backupNow())
        assertFalse(auto.backupNow(), "nothing changed, nothing written")
        assertEquals(1, store.writes)
        assertEquals(42L, auto.state.value.lastWrittenAt)
        assertEquals(1, BackupCodec.decode(store.files.getValue(AutoBackup.FILE_NAME)).projects.size)

        projects = projects + Project(id = 2, code = "P2", name = "Two", client = "", colorHex = "#000000", budgetHours = 0.0)
        assertTrue(auto.backupNow())
        assertEquals(2, store.writes)
    }

    @Test
    fun backupNow_doesNothingWithoutFolder_andFlagsFailures() = runTest {
        val store = FakeStore(initial = null)
        val auto = AutoBackup(store, ::backup, settleMillis = 0)
        assertFalse(auto.backupNow())
        assertEquals(0, store.writes)

        store.folder.value = BackupFolder("ref", "Drive")
        store.failWrites = true
        assertFalse(auto.backupNow())
        assertTrue(auto.state.value.lastError)

        store.failWrites = false
        assertTrue(auto.backupNow())
        assertFalse(auto.state.value.lastError)
    }

    @Test
    fun start_collapsesABurstOfChangesIntoOneWrite() = runTest {
        val store = FakeStore()
        val changes = MutableSharedFlow<Unit>()
        val auto = AutoBackup(store, ::backup, settleMillis = 5_000)
        auto.start(backgroundScope, changes)
        runCurrent()

        repeat(5) { changes.emit(Unit); advanceTimeBy(1_000) }
        assertEquals(0, store.writes, "still settling")
        advanceTimeBy(5_001)
        assertEquals(1, store.writes)

        // Choosing a (new) folder writes there even though the data is unchanged.
        store.folder.value = BackupFolder("other", "iCloud")
        advanceTimeBy(5_001)
        assertEquals(2, store.writes)
    }

    @Test
    fun readLatest_returnsNullWithoutFile_andIoErrorWhenUnreadable() = runTest {
        val store = FakeStore()
        val auto = AutoBackup(store, ::backup, settleMillis = 0)
        assertNull(auto.readLatest())
        auto.backupNow()
        assertEquals(projects, auto.readLatest()?.projects)

        store.files[AutoBackup.FILE_NAME] = "not json"
        val error = try { auto.readLatest(); null } catch (e: BackupException) { e.error }
        assertEquals(BackupError.NOT_A_BACKUP, error)
    }
}
