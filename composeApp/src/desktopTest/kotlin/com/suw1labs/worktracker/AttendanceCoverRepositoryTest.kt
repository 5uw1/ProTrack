package com.suw1labs.worktracker

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.suw1labs.worktracker.data.AppDatabase
import com.suw1labs.worktracker.data.DatabaseCreationTracker
import com.suw1labs.worktracker.data.buildAppDatabase
import com.suw1labs.worktracker.data.model.AttendanceSession
import com.suw1labs.worktracker.data.model.TimeEntry
import com.suw1labs.worktracker.data.repository.TimeTrackerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** Clocked in late, then the activity's start is corrected: the clock-in follows (real SQLite). */
class AttendanceCoverRepositoryTest {
    private val day = 1_800_000_000_000L
    private fun at(h: Int, m: Int = 0) = day + h * 3600_000L + m * 60_000L

    @Test
    fun correctingAnActivityToBeforeALateClockIn_movesTheClockIn() = runTest {
        val db = Room.inMemoryDatabaseBuilder<AppDatabase>().setDriver(BundledSQLiteDriver()).buildAppDatabase(DatabaseCreationTracker(), Dispatchers.IO)
        val repo = TimeTrackerRepository(db.projectDao(), db.workTaskDao(), db.timeEntryDao(), db.attendanceDao(), db.dayRecordDao(), db.settingsDao())
        val morning = db.attendanceDao().insertSession(AttendanceSession(clockIn = at(9, 23), clockOut = at(12, 48)))
        val afternoon = db.attendanceDao().insertSession(AttendanceSession(clockIn = at(13, 30), clockOut = at(17)))
        val id = db.timeEntryDao().insertEntry(TimeEntry(startTime = at(9, 23), endTime = at(9, 58)))

        val corrected = db.backupDao().allTimeEntries().single { it.id == id }.copy(startTime = at(9, 17))
        repo.updateTimeEntry(corrected)
        repo.coverWithAttendance(listOf(corrected))

        val sessions = db.backupDao().allSessions().associateBy { it.id }
        assertEquals(at(9, 17), sessions.getValue(morning).clockIn)
        assertEquals(at(12, 48), sessions.getValue(morning).clockOut)
        assertEquals(at(13, 30), sessions.getValue(afternoon).clockIn)
        db.close()
    }
}
