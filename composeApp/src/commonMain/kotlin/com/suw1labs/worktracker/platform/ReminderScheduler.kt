package com.suw1labs.worktracker.platform

import com.suw1labs.worktracker.data.model.WorkTaskWithProject
import com.suw1labs.worktracker.util.currentTimeMillis
import com.suw1labs.worktracker.util.formatFixed

/**
 * Platform-specific deadline reminder delivery (AlarmManager + notifications on Android,
 * UNUserNotificationCenter on iOS, system tray notifications on desktop).
 */
interface ReminderScheduler {
    fun scheduleTaskReminder(task: WorkTaskWithProject)
    fun cancelTaskReminder(taskId: Long)
    fun checkAndNotifyUrgentDeadlines(tasks: List<WorkTaskWithProject>)

    /** Notifies once at [triggerAtMillis] that today's target hours are reached (while clocked in). */
    fun scheduleDailyTargetReminder(triggerAtMillis: Long, title: String, message: String)
    fun cancelDailyTargetReminder()
}

/** Used when a platform has no notification support (or in tests). */
object NoOpReminderScheduler : ReminderScheduler {
    override fun scheduleTaskReminder(task: WorkTaskWithProject) = Unit
    override fun cancelTaskReminder(taskId: Long) = Unit
    override fun checkAndNotifyUrgentDeadlines(tasks: List<WorkTaskWithProject>) = Unit
    override fun scheduleDailyTargetReminder(triggerAtMillis: Long, title: String, message: String) = Unit
    override fun cancelDailyTargetReminder() = Unit
}

/** A reminder that a platform scheduler should deliver at [triggerAtMillis]. */
data class PendingReminder(
    val taskId: Long,
    val title: String,
    val message: String,
    val triggerAtMillis: Long
)

/** An urgent-deadline notification that should be shown right away. */
data class UrgentAlert(
    val taskId: Long,
    val taskTitle: String,
    val projectName: String,
    val urgencyMessage: String
)

/**
 * Shared, platform-independent reminder rules. Every [ReminderScheduler] implementation
 * uses this so Android, iOS and desktop behave identically.
 */
object DeadlinePolicy {
    const val TITLE_PREFIX = "⏰ Deadline Alert: "

    fun notificationTitle(taskTitle: String) = "$TITLE_PREFIX$taskTitle"
    fun notificationBody(projectName: String, urgencyMessage: String) = "[$projectName] $urgencyMessage"
    fun notificationBigText(projectName: String, taskTitle: String, urgencyMessage: String) =
        "Project: $projectName\nTask: $taskTitle\nStatus: $urgencyMessage. Don't forget to track your work!"

    /**
     * Computes the reminder to schedule for [task], or null when the reminder should be
     * cancelled instead (reminders disabled, no deadline, task done, or already overdue).
     */
    fun pendingReminder(task: WorkTaskWithProject, now: Long = currentTimeMillis()): PendingReminder? {
        if (!task.reminderEnabled || task.deadlineTimestamp == null || task.status == "DONE") return null

        val leadMillis = task.reminderLeadHours * 3600 * 1000L
        val reminderTime = task.deadlineTimestamp - leadMillis

        val targetTime = when {
            reminderTime > now -> reminderTime
            task.deadlineTimestamp > now -> now + 5000L // trigger near immediately
            else -> return null // already overdue
        }

        return PendingReminder(
            taskId = task.id,
            title = notificationTitle(task.title),
            message = notificationBody(task.projectName, "Deadline approaching in ${task.reminderLeadHours}h"),
            triggerAtMillis = targetTime
        )
    }

    /** Returns the tasks that deserve an immediate "urgent" notification right now. */
    fun urgentAlerts(tasks: List<WorkTaskWithProject>, now: Long = currentTimeMillis()): List<UrgentAlert> {
        return tasks
            .filter { it.status != "DONE" && it.deadlineTimestamp != null && it.reminderEnabled }
            .mapNotNull { task ->
                val deadline = task.deadlineTimestamp ?: return@mapNotNull null
                val diffHours = (deadline - now) / (1000.0 * 3600.0)

                val urgencyMsg = when {
                    diffHours < 0 -> "⚠️ Overdue by ${(-diffHours).formatFixed(1)} hours"
                    diffHours <= 2.0 -> "🚨 Urgent: Due in under 2 hours!"
                    diffHours <= 24.0 -> "⏰ Due in ${diffHours.toInt()} hours"
                    else -> null
                }

                if (urgencyMsg != null && diffHours <= task.reminderLeadHours) {
                    UrgentAlert(
                        taskId = task.id,
                        taskTitle = task.title,
                        projectName = task.projectName,
                        urgencyMessage = urgencyMsg
                    )
                } else null
            }
    }
}
