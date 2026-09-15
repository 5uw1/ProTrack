package com.suw1labs.worktracker.platform

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.suw1labs.worktracker.data.model.WorkTaskWithProject

/**
 * Android implementation: exact alarms delivered through [DeadlineAlertReceiver] plus
 * immediate notifications for tasks that are already urgent.
 */
class AndroidReminderScheduler(private val context: Context) : ReminderScheduler {

    private companion object {
        const val TAG = "DeadlineScheduler"
        const val TARGET_REQUEST_CODE = 900_001
    }

    override fun scheduleDailyTargetReminder(triggerAtMillis: Long, title: String, message: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            TARGET_REQUEST_CODE,
            targetIntent().apply {
                putExtra(DeadlineAlertReceiver.EXTRA_TASK_ID, TARGET_REQUEST_CODE.toLong())
                putExtra(DeadlineAlertReceiver.EXTRA_TASK_TITLE, title)
                putExtra(DeadlineAlertReceiver.EXTRA_PROJECT_NAME, "")
                putExtra(DeadlineAlertReceiver.EXTRA_URGENCY, message)
                putExtra(DeadlineAlertReceiver.EXTRA_PLAIN, true)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            // Inexact alarm: fires within a few minutes and needs no SCHEDULE_EXACT_ALARM permission
            // (Play restricts exact alarms to alarm-clock / calendar apps).
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            Log.d(TAG, "Scheduled daily target reminder at $triggerAtMillis")
        } catch (e: Exception) {
            Log.w(TAG, "Cannot schedule daily target reminder: ${e.message}")
        }
    }

    override fun cancelDailyTargetReminder() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = PendingIntent.getBroadcast(
            context, TARGET_REQUEST_CODE, targetIntent(), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun targetIntent() = Intent(context, DeadlineAlertReceiver::class.java).setAction("com.suw1labs.worktracker.DAILY_TARGET")

    override fun scheduleTaskReminder(task: WorkTaskWithProject) {
        val reminder = DeadlinePolicy.pendingReminder(task)
        if (reminder == null) {
            cancelTaskReminder(task.id)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, DeadlineAlertReceiver::class.java).apply {
            putExtra(DeadlineAlertReceiver.EXTRA_TASK_ID, task.id)
            putExtra(DeadlineAlertReceiver.EXTRA_TASK_TITLE, task.title)
            putExtra(DeadlineAlertReceiver.EXTRA_PROJECT_NAME, task.projectName)
            putExtra(
                DeadlineAlertReceiver.EXTRA_URGENCY,
                "Deadline approaching in ${task.reminderLeadHours}h"
            )
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val targetTime = reminder.triggerAtMillis
        try {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetTime, pendingIntent)
            Log.d(TAG, "Scheduled deadline alarm for task ${task.id} at $targetTime")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling deadline alarm", e)
        }
    }

    override fun cancelTaskReminder(taskId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, DeadlineAlertReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    override fun checkAndNotifyUrgentDeadlines(tasks: List<WorkTaskWithProject>) {
        DeadlinePolicy.urgentAlerts(tasks).forEach { alert ->
            DeadlineAlertReceiver.showNotification(
                context = context,
                notificationId = alert.taskId,
                taskTitle = alert.taskTitle,
                projectName = alert.projectName,
                urgencyMessage = alert.urgencyMessage
            )
        }
    }
}
