package com.suw1labs.worktracker.data.model

data class TimeEntryWithDetails(
    val id: Long,
    val projectId: Long?,
    val projectCode: String?,
    val projectName: String?,
    val projectColor: String?,
    val taskId: Long?,
    val taskTitle: String?,
    val categoryId: Long?,
    val categoryName: String?,
    val categoryProductive: Boolean?,
    val description: String,
    val startTime: Long,
    val endTime: Long?,
    val createdAt: Long
) {
    val isRunning: Boolean get() = endTime == null

    /** Seconds between start and end (or [now] while running). */
    fun durationSeconds(now: Long): Long = (((endTime ?: now) - startTime) / 1000L).coerceAtLeast(0L)

    fun toEntity() = TimeEntry(
        id = id,
        projectId = projectId,
        taskId = taskId,
        categoryId = categoryId,
        description = description,
        startTime = startTime,
        endTime = endTime,
        createdAt = createdAt
    )
}

data class WorkTaskWithProject(
    val id: Long,
    val projectId: Long,
    val projectCode: String,
    val projectName: String,
    val projectColor: String,
    val client: String,
    val title: String,
    val description: String,
    val priority: String,
    val status: String,
    val estimatedHours: Double,
    val deadlineTimestamp: Long?,
    val reminderLeadHours: Int,
    val reminderEnabled: Boolean,
    val createdAt: Long,
    val loggedSeconds: Long = 0
)

data class ProjectSummary(
    val id: Long,
    val code: String,
    val name: String,
    val client: String,
    val colorHex: String,
    val budgetHours: Double,
    val status: String,
    val totalSeconds: Long,
    val totalTasks: Int,
    val completedTasks: Int
)
