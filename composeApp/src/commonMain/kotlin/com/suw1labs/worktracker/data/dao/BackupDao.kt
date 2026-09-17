package com.suw1labs.worktracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.suw1labs.worktracker.data.model.AppSettings
import com.suw1labs.worktracker.data.model.AttendanceSession
import com.suw1labs.worktracker.data.model.DayRecord
import com.suw1labs.worktracker.data.model.Project
import com.suw1labs.worktracker.data.model.TimeEntry
import com.suw1labs.worktracker.data.model.WorkTask

/** Whole-table reads and writes used by backup export and restore. */
@Dao
interface BackupDao {
    @Query("SELECT * FROM projects ORDER BY id")
    suspend fun allProjects(): List<Project>

    @Query("SELECT * FROM tasks ORDER BY id")
    suspend fun allTasks(): List<WorkTask>

    @Query("SELECT * FROM time_entries ORDER BY id")
    suspend fun allTimeEntries(): List<TimeEntry>

    @Query("SELECT * FROM attendance_sessions ORDER BY id")
    suspend fun allSessions(): List<AttendanceSession>

    @Query("SELECT * FROM day_records ORDER BY id")
    suspend fun allDayRecords(): List<DayRecord>

    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun settings(): AppSettings?

    // Plain inserts (ABORT on conflict): the tables were just cleared, so any conflict means the
    // file is inconsistent and the whole restore must roll back instead of silently dropping rows.
    @Insert
    suspend fun insertProjects(projects: List<Project>)

    @Insert
    suspend fun insertTasks(tasks: List<WorkTask>)

    @Insert
    suspend fun insertTimeEntries(entries: List<TimeEntry>)

    @Insert
    suspend fun insertSessions(sessions: List<AttendanceSession>)

    @Insert
    suspend fun insertDayRecords(records: List<DayRecord>)

    @Insert
    suspend fun insertSettings(settings: AppSettings)

    // Children first: time entries reference projects and tasks, tasks reference projects.
    @Query("DELETE FROM time_entries")
    suspend fun clearTimeEntries()

    @Query("DELETE FROM tasks")
    suspend fun clearTasks()

    @Query("DELETE FROM projects")
    suspend fun clearProjects()

    @Query("DELETE FROM attendance_sessions")
    suspend fun clearSessions()

    @Query("DELETE FROM day_records")
    suspend fun clearDayRecords()

    @Query("DELETE FROM app_settings")
    suspend fun clearSettings()
}
