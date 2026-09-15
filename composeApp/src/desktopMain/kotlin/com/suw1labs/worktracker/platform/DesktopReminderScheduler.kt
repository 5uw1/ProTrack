package com.suw1labs.worktracker.platform

import com.suw1labs.worktracker.data.model.WorkTaskWithProject
import com.suw1labs.worktracker.util.currentTimeMillis
import java.awt.Color
import java.awt.RenderingHints
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Deadline reminders on desktop: notifications are shown through the system tray
 * (Windows balloon tips / macOS notification center) and reminders are scheduled
 * in-process while the application is running.
 */
class DesktopReminderScheduler : ReminderScheduler {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val pendingJobs = ConcurrentHashMap<Long, Job>()

    private val trayIcon: TrayIcon? by lazy { createTrayIcon() }

    override fun scheduleTaskReminder(task: WorkTaskWithProject) {
        val reminder = DeadlinePolicy.pendingReminder(task)
        if (reminder == null) {
            cancelTaskReminder(task.id)
            return
        }

        pendingJobs.remove(task.id)?.cancel()
        pendingJobs[task.id] = scope.launch {
            val wait = reminder.triggerAtMillis - currentTimeMillis()
            if (wait > 0) delay(wait)
            showNotification(reminder.title, reminder.message)
            pendingJobs.remove(task.id)
        }
    }

    private var dailyTargetJob: Job? = null

    override fun scheduleDailyTargetReminder(triggerAtMillis: Long, title: String, message: String) {
        dailyTargetJob?.cancel()
        dailyTargetJob = scope.launch {
            val wait = triggerAtMillis - currentTimeMillis()
            if (wait > 0) delay(wait)
            showNotification(title, message)
        }
    }

    override fun cancelDailyTargetReminder() {
        dailyTargetJob?.cancel()
        dailyTargetJob = null
    }

    override fun cancelTaskReminder(taskId: Long) {
        pendingJobs.remove(taskId)?.cancel()
    }

    override fun checkAndNotifyUrgentDeadlines(tasks: List<WorkTaskWithProject>) {
        DeadlinePolicy.urgentAlerts(tasks).forEach { alert ->
            showNotification(
                DeadlinePolicy.notificationTitle(alert.taskTitle),
                DeadlinePolicy.notificationBody(alert.projectName, alert.urgencyMessage)
            )
        }
    }

    private fun showNotification(title: String, message: String) {
        val icon = trayIcon
        if (icon != null) {
            icon.displayMessage(title, message, TrayIcon.MessageType.INFO)
        } else {
            println("[WorkTracker] $title - $message")
        }
    }

    private fun createTrayIcon(): TrayIcon? {
        if (!SystemTray.isSupported()) return null
        return runCatching {
            val size = 32
            val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
            val g = image.createGraphics()
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.color = Color(0x1D, 0x4E, 0xD8)
            g.fillOval(2, 2, size - 4, size - 4)
            g.color = Color.WHITE
            g.fillRect(size / 2 - 2, 8, 4, 10)
            g.fillRect(size / 2 - 2, size / 2 - 2, 8, 4)
            g.dispose()

            TrayIcon(image, "WorkTracker").apply {
                isImageAutoSize = true
                SystemTray.getSystemTray().add(this)
            }
        }.getOrNull()
    }
}
