package com.suw1labs.worktracker.platform

import com.suw1labs.worktracker.data.model.WorkTaskWithProject
import com.suw1labs.worktracker.util.currentTimeMillis
import platform.Foundation.NSLog
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter

/**
 * Deadline reminders via iOS local notifications (UserNotifications framework).
 */
class IosReminderScheduler : ReminderScheduler {

    private val center: UNUserNotificationCenter
        get() = UNUserNotificationCenter.currentNotificationCenter()

    override fun scheduleTaskReminder(task: WorkTaskWithProject) {
        val reminder = DeadlinePolicy.pendingReminder(task)
        if (reminder == null) {
            cancelTaskReminder(task.id)
            return
        }

        val delaySeconds = ((reminder.triggerAtMillis - currentTimeMillis()) / 1000.0).coerceAtLeast(1.0)
        val content = UNMutableNotificationContent().apply {
            setTitle(reminder.title)
            setBody(reminder.message)
            setSound(UNNotificationSound.defaultSound())
        }
        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(delaySeconds, repeats = false)
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = reminderId(task.id),
            content = content,
            trigger = trigger
        )
        center.addNotificationRequest(request) { error ->
            if (error != null) NSLog("DeadlineScheduler: failed to schedule reminder: ${error.localizedDescription}")
        }
    }

    override fun cancelTaskReminder(taskId: Long) {
        val ids = listOf(reminderId(taskId), urgentId(taskId))
        center.removePendingNotificationRequestsWithIdentifiers(ids)
        center.removeDeliveredNotificationsWithIdentifiers(ids)
    }

    override fun checkAndNotifyUrgentDeadlines(tasks: List<WorkTaskWithProject>) {
        DeadlinePolicy.urgentAlerts(tasks).forEach { alert ->
            val content = UNMutableNotificationContent().apply {
                setTitle(DeadlinePolicy.notificationTitle(alert.taskTitle))
                setBody(DeadlinePolicy.notificationBody(alert.projectName, alert.urgencyMessage))
                setSound(UNNotificationSound.defaultSound())
            }
            // A null trigger delivers the notification immediately.
            val request = UNNotificationRequest.requestWithIdentifier(
                identifier = urgentId(alert.taskId),
                content = content,
                trigger = null
            )
            center.addNotificationRequest(request) { error ->
                if (error != null) NSLog("DeadlineScheduler: failed to show urgent alert: ${error.localizedDescription}")
            }
        }
    }

    override fun scheduleDailyTargetReminder(triggerAtMillis: Long, title: String, message: String) {
        cancelDailyTargetReminder()
        val delaySeconds = ((triggerAtMillis - currentTimeMillis()) / 1000.0).coerceAtLeast(1.0)
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(message)
            setSound(UNNotificationSound.defaultSound())
        }
        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(delaySeconds, repeats = false)
        val request = UNNotificationRequest.requestWithIdentifier(DAILY_TARGET_ID, content, trigger)
        center.addNotificationRequest(request) { error ->
            if (error != null) NSLog("DeadlineScheduler: failed to schedule daily target reminder: ${error.localizedDescription}")
        }
    }

    override fun cancelDailyTargetReminder() {
        center.removePendingNotificationRequestsWithIdentifiers(listOf(DAILY_TARGET_ID))
    }

    private companion object {
        const val DAILY_TARGET_ID = "daily-target-reached"
    }

    private fun reminderId(taskId: Long) = "task-reminder-$taskId"
    private fun urgentId(taskId: Long) = "task-urgent-$taskId"
}
