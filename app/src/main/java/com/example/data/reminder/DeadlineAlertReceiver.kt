package com.example.data.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

class DeadlineAlertReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Task deadline alert"
        val projectName = intent.getStringExtra(EXTRA_PROJECT_NAME) ?: "Project"
        val urgencyMessage = intent.getStringExtra(EXTRA_URGENCY) ?: "Deadline is approaching!"
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, 0L)

        showNotification(context, taskId, taskTitle, projectName, urgencyMessage)
    }

    companion object {
        const val CHANNEL_ID = "work_deadlines_channel"
        const val CHANNEL_NAME = "Task Deadlines & Reminders"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_PROJECT_NAME = "extra_project_name"
        const val EXTRA_URGENCY = "extra_urgency"

        fun showNotification(
            context: Context,
            notificationId: Long,
            taskTitle: String,
            projectName: String,
            urgencyMessage: String
        ) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Automated reminders for tasks nearing their deadlines"
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_TASK_ID, notificationId)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId.toInt(),
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("⏰ Deadline Alert: $taskTitle")
                .setContentText("[$projectName] $urgencyMessage")
                .setStyle(NotificationCompat.BigTextStyle().bigText("Project: $projectName\nTask: $taskTitle\nStatus: $urgencyMessage. Don't forget to track your work!"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            notificationManager.notify(notificationId.toInt(), notification)
        }
    }
}
