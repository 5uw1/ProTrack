package com.suw1labs.worktracker.data.repository

import com.suw1labs.worktracker.data.dao.AttendanceDao
import com.suw1labs.worktracker.data.dao.DayRecordDao
import com.suw1labs.worktracker.data.dao.SettingsDao
import com.suw1labs.worktracker.data.dao.ProjectDao
import com.suw1labs.worktracker.data.dao.TimeEntryDao
import com.suw1labs.worktracker.data.dao.WorkTaskDao
import com.suw1labs.worktracker.data.model.AppSettings
import com.suw1labs.worktracker.data.model.AttendanceSession
import com.suw1labs.worktracker.data.model.DayRecord
import com.suw1labs.worktracker.data.model.Project
import com.suw1labs.worktracker.data.model.ProjectSummary
import com.suw1labs.worktracker.data.model.TimeEntry
import com.suw1labs.worktracker.data.model.TimeEntryWithDetails
import com.suw1labs.worktracker.data.model.WorkTask
import com.suw1labs.worktracker.data.model.WorkTaskWithProject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class TimeTrackerRepository(
    private val projectDao: ProjectDao,
    private val taskDao: WorkTaskDao,
    private val timeEntryDao: TimeEntryDao,
    private val attendanceDao: AttendanceDao,
    private val dayRecordDao: DayRecordDao,
    private val settingsDao: SettingsDao
) {
    val allProjects: Flow<List<Project>> = projectDao.getAllProjects()
    val activeProjects: Flow<List<Project>> = projectDao.getActiveProjects()
    val allTasksWithProject: Flow<List<WorkTaskWithProject>> = taskDao.getTasksWithProject()
    val allTimeEntries: Flow<List<TimeEntryWithDetails>> = timeEntryDao.getAllEntriesWithDetails()
    val allSessions: Flow<List<AttendanceSession>> = attendanceDao.getAllSessions()
    val openSession: Flow<AttendanceSession?> = attendanceDao.observeOpenSession()
    val allDayRecords: Flow<List<DayRecord>> = dayRecordDao.getAllRecords()
    val settings: Flow<AppSettings?> = settingsDao.observeSettings()

    /** Per-project totals (closed entries only) for the Projects screen. */
    val projectSummaries: Flow<List<ProjectSummary>> = combine(
        allProjects,
        taskDao.getAllTasks(),
        allTimeEntries
    ) { projects, tasks, entries ->
        projects.map { project ->
            val projectEntries = entries.filter { it.projectId == project.id && it.endTime != null }
            val projectTasks = tasks.filter { it.projectId == project.id }
            ProjectSummary(
                id = project.id,
                code = project.code,
                name = project.name,
                client = project.client,
                colorHex = project.colorHex,
                budgetHours = project.budgetHours,
                status = project.status,
                isProductive = project.isProductive,
                totalSeconds = projectEntries.sumOf { it.durationSeconds(it.endTime ?: it.startTime) },
                totalTasks = projectTasks.size,
                completedTasks = projectTasks.count { it.status == "DONE" }
            )
        }
    }

    // Projects
    suspend fun insertProject(project: Project): Long = projectDao.insertProject(project)
    suspend fun updateProject(project: Project) = projectDao.updateProject(project)
    suspend fun deleteProjectById(id: Long) = projectDao.deleteProjectById(id)

    // Tasks
    suspend fun insertTask(task: WorkTask): Long = taskDao.insertTask(task)
    suspend fun findTaskByTitle(projectId: Long, title: String): WorkTask? = taskDao.findTaskByTitle(projectId, title)
    suspend fun updateTask(task: WorkTask) = taskDao.updateTask(task)
    suspend fun deleteTask(task: WorkTask) = taskDao.deleteTask(task)
    suspend fun updateTaskStatus(taskId: Long, status: String) = taskDao.updateTaskStatus(taskId, status)

    /** Moves a task (and all time logged on it) to another project. */
    suspend fun moveTask(taskId: Long, projectId: Long) {
        taskDao.moveTask(taskId, projectId)
        timeEntryDao.moveEntriesOfTask(taskId, projectId)
    }

    // Time entries
    suspend fun getRunningEntry(): TimeEntry? = timeEntryDao.getRunningEntry()
    suspend fun closeRunningEntries(endTime: Long) = timeEntryDao.closeRunningEntries(endTime)
    suspend fun insertTimeEntry(entry: TimeEntry): Long = timeEntryDao.insertEntry(entry)
    suspend fun updateTimeEntry(entry: TimeEntry) = timeEntryDao.updateEntry(entry)
    suspend fun deleteTimeEntryById(id: Long) = timeEntryDao.deleteEntryById(id)

    // Attendance
    suspend fun getOpenSession(): AttendanceSession? = attendanceDao.getOpenSession()
    suspend fun closeOpenSessions(clockOut: Long, reason: String?) = attendanceDao.closeOpenSessions(clockOut, reason)
    suspend fun insertSession(session: AttendanceSession): Long = attendanceDao.insertSession(session)
    suspend fun updateSession(session: AttendanceSession) = attendanceDao.updateSession(session)
    suspend fun deleteSessionById(id: Long) = attendanceDao.deleteSessionById(id)

    // Absences
    suspend fun getDayRecord(dayStart: Long): DayRecord? = dayRecordDao.getByDay(dayStart)
    suspend fun insertDayRecord(record: DayRecord): Long = dayRecordDao.insert(record)
    suspend fun updateDayRecord(record: DayRecord) = dayRecordDao.update(record)
    suspend fun deleteDayRecordById(id: Long) = dayRecordDao.deleteById(id)

    // Settings
    suspend fun saveSettings(settings: AppSettings) = settingsDao.upsert(settings.copy(id = 1))

    /** Inserts projects whose code is not present yet; returns the number of new projects. */
    suspend fun importProjects(projects: List<Project>, existing: List<Project>): Int {
        val known = existing.map { it.code.trim().lowercase() }.toMutableSet()
        var added = 0
        for (p in projects) {
            val key = p.code.trim().lowercase()
            if (key.isEmpty() || key in known) continue
            projectDao.insertProject(p)
            known.add(key)
            added++
        }
        return added
    }
}
