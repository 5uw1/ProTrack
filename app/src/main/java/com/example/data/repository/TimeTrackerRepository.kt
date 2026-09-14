package com.example.data.repository

import com.example.data.dao.ProjectDao
import com.example.data.dao.TimeEntryDao
import com.example.data.dao.WorkTaskDao
import com.example.data.model.Project
import com.example.data.model.ProjectSummary
import com.example.data.model.TimeEntry
import com.example.data.model.TimeEntryWithDetails
import com.example.data.model.WorkTask
import com.example.data.model.WorkTaskWithProject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class TimeTrackerRepository(
    private val projectDao: ProjectDao,
    private val taskDao: WorkTaskDao,
    private val timeEntryDao: TimeEntryDao
) {
    val allProjects: Flow<List<Project>> = projectDao.getAllProjects()
    val activeProjects: Flow<List<Project>> = projectDao.getActiveProjects()
    val allTasksWithProject: Flow<List<WorkTaskWithProject>> = taskDao.getTasksWithProject()
    val allTimeEntries: Flow<List<TimeEntryWithDetails>> = timeEntryDao.getAllEntriesWithDetails()

    // Computed reactive Project Summaries (combines projects, tasks, and time entries)
    val projectSummaries: Flow<List<ProjectSummary>> = combine(
        allProjects,
        taskDao.getAllTasks(),
        allTimeEntries
    ) { projects, tasks, entries ->
        projects.map { project ->
            val projectEntries = entries.filter { it.projectId == project.id }
            val projectTasks = tasks.filter { it.projectId == project.id }
            val totalSeconds = projectEntries.sumOf { it.durationSeconds }
            val billableSeconds = projectEntries.filter { it.isBillable }.sumOf { it.durationSeconds }
            val totalEarnings = projectEntries.filter { it.isBillable }.sumOf { entry ->
                (entry.durationSeconds / 3600.0) * entry.hourlyRate
            }
            val completedTasks = projectTasks.count { it.status == "DONE" }

            ProjectSummary(
                id = project.id,
                name = project.name,
                client = project.client,
                colorHex = project.colorHex,
                hourlyRate = project.hourlyRate,
                budgetHours = project.budgetHours,
                status = project.status,
                totalSeconds = totalSeconds,
                billableSeconds = billableSeconds,
                totalEarnings = totalEarnings,
                totalTasks = projectTasks.size,
                completedTasks = completedTasks
            )
        }
    }

    suspend fun insertProject(project: Project): Long = projectDao.insertProject(project)
    suspend fun updateProject(project: Project) = projectDao.updateProject(project)
    suspend fun deleteProject(project: Project) = projectDao.deleteProject(project)
    suspend fun deleteProjectById(id: Long) = projectDao.deleteProjectById(id)

    suspend fun insertTask(task: WorkTask): Long = taskDao.insertTask(task)
    suspend fun updateTask(task: WorkTask) = taskDao.updateTask(task)
    suspend fun deleteTask(task: WorkTask) = taskDao.deleteTask(task)
    suspend fun updateTaskStatus(taskId: Long, status: String) = taskDao.updateTaskStatus(taskId, status)

    suspend fun insertTimeEntry(entry: TimeEntry): Long = timeEntryDao.insertEntry(entry)
    suspend fun updateTimeEntry(entry: TimeEntry) = timeEntryDao.updateEntry(entry)
    suspend fun deleteTimeEntry(entry: TimeEntry) = timeEntryDao.deleteEntry(entry)
    suspend fun deleteTimeEntryById(id: Long) = timeEntryDao.deleteEntryById(id)

    suspend fun getExportEntries(projectId: Long?, startTimestamp: Long, endTimestamp: Long): List<TimeEntryWithDetails> {
        return timeEntryDao.getExportEntries(projectId, startTimestamp, endTimestamp)
    }

    suspend fun getPendingTasksNearDeadline(thresholdTime: Long): List<WorkTaskWithProject> {
        return taskDao.getPendingTasksNearDeadline(thresholdTime)
    }
}
