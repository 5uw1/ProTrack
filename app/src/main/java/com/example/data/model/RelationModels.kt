package com.example.data.model

data class TimeEntryWithDetails(
    val id: Long,
    val projectId: Long,
    val projectName: String,
    val projectColor: String,
    val client: String,
    val taskId: Long?,
    val taskTitle: String?,
    val description: String,
    val startTime: Long,
    val endTime: Long,
    val durationSeconds: Long,
    val isBillable: Boolean,
    val hourlyRate: Double,
    val tags: String,
    val createdAt: Long
)

data class WorkTaskWithProject(
    val id: Long,
    val projectId: Long,
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
    val name: String,
    val client: String,
    val colorHex: String,
    val hourlyRate: Double,
    val budgetHours: Double,
    val status: String,
    val totalSeconds: Long,
    val billableSeconds: Long,
    val totalEarnings: Double,
    val totalTasks: Int,
    val completedTasks: Int
)
