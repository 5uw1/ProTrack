package com.example.data.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.model.WorkTaskWithProject

object DeadlineReminderScheduler {
    private const val TAG = "DeadlineScheduler"

    fun scheduleTaskReminder(context: Context, task: WorkTaskWithProject) {
        if (!task.reminderEnabled || task.deadlineTimestamp == null || task.status == "DONE") {
            cancelTaskReminder(context, task.id)
            return
        }

        val leadMillis = task.reminderLeadHours * 3600 * 1000L
        val reminderTime = task.deadlineTimestamp - leadMillis
        val currentTime = System.currentTimeMillis()

        // If the calculated reminder time is in the past, but the deadline is still upcoming,
        // we can schedule for immediately or the deadline time.
        val targetTime = if (reminderTime > currentTime) {
            reminderTime
        } else if (task.deadlineTimestamp > currentTime) {
            currentTime + 5000L // Trigger near immediately
        } else {
            // Already overdue
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

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        targetTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, targetTime, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    targetTime,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled deadline alarm for task ${task.id} at $targetTime")
        } catch (e: SecurityException) {
            Log.w(TAG, "Cannot schedule exact alarm: ${e.message}")
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, targetTime, pendingIntent)
            } catch (ignored: Exception) {}
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling deadline alarm", e)
        }
    }

    fun cancelTaskReminder(context: Context, taskId: Long) {
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

    fun checkAndNotifyUrgentDeadlines(context: Context, tasks: List<WorkTaskWithProject>) {
        val now = System.currentTimeMillis()
        tasks.filter { it.status != "DONE" && it.deadlineTimestamp != null && it.reminderEnabled }
            .forEach { task ->
                val deadline = task.deadlineTimestamp ?: return@forEach
                val diffHours = (deadline - now) / (1000.0 * 3600.0)

                val urgencyMsg = when {
                    diffHours < 0 -> "⚠️ Overdue by ${String.format("%.1f", -diffHours)} hours"
                    diffHours <= 2.0 -> "🚨 Urgent: Due in under 2 hours!"
                    diffHours <= 24.0 -> "⏰ Due in ${diffHours.toInt()} hours"
                    else -> null
                }

                if (urgencyMsg != null && diffHours <= task.reminderLeadHours) {
                    DeadlineAlertReceiver.showNotification(
                        context = context,
                        notificationId = task.id,
                        taskTitle = task.title,
                        projectName = task.projectName,
                        urgencyMessage = urgencyMsg
                    )
                }
            }
    }
}
