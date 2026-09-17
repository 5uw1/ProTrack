package com.suw1labs.worktracker.platform

import com.suw1labs.worktracker.data.model.AppSettings
import com.suw1labs.worktracker.data.model.AttendanceSession
import com.suw1labs.worktracker.data.model.Project
import com.suw1labs.worktracker.data.model.TimeEntryWithDetails
import com.suw1labs.worktracker.data.model.WorkTaskWithProject
import com.suw1labs.worktracker.data.report.ReportCalculator
import com.suw1labs.worktracker.data.repository.TimeTrackerRepository
import com.suw1labs.worktracker.util.DateRanges
import com.suw1labs.worktracker.util.currentTimeMillis
import com.suw1labs.worktracker.util.toLocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.datetime.isoDayNumber

/**
 * Everything a home-screen widget shows. The widget cannot query the database on every tick, so
 * the running totals are expressed as "fixed part + start time" and the widget adds the elapsed
 * time itself (iOS renders a live timer from [clockedInSince]).
 */
data class WidgetSnapshot(
    val clockedIn: Boolean,
    /** Start of the open clock-in period (epoch millis), or null when clocked out. */
    val clockedInSince: Long?,
    /** Clocked-in seconds of today from periods that are already closed. */
    val attendanceSecondsBefore: Long,
    val targetSeconds: Long,
    /** "P-2026-0142 · Spindle retrofit", "Unproductive", or null when no activity runs. */
    val runningProject: String?,
    val runningTask: String?,
    val runningSince: Long?,
    val runningProjectId: Long? = null,
    val runningTaskId: Long? = null,
    /** Tasks offered as one-tap switches on the widget (running one first, then recently used). */
    val quickTasks: List<QuickTask> = emptyList(),
    /** UI language code (en / de / fr) so the widgets speak the app's language. */
    val language: String = "en",
    val updatedAt: Long
) {
    /** Total clocked-in seconds of today at [now]. */
    fun attendanceSecondsAt(now: Long): Long {
        val open = clockedInSince?.let { ((now - it) / 1000L).coerceAtLeast(0L) } ?: 0L
        return attendanceSecondsBefore + open
    }

    companion object {
        val EMPTY = WidgetSnapshot(false, null, 0L, 0L, null, null, null, updatedAt = 0L)
        /** How many tasks a widget offers for switching. */
        const val MAX_QUICK_TASKS = 6
    }
}

/** A task the widget can switch to with one tap. */
data class QuickTask(
    val taskId: Long,
    val projectId: Long,
    val title: String,
    /** "P-2026-0142 · Spindle retrofit" or the plain name for the unproductive project. */
    val projectLabel: String,
    val productive: Boolean
)

/** Pushes the current state to the platform's home-screen widget (Glance on Android, WidgetKit on iOS). */
interface WidgetBridge {
    fun publish(snapshot: WidgetSnapshot)
}

/** Desktop and tests have no widgets. */
object NoOpWidgetBridge : WidgetBridge {
    override fun publish(snapshot: WidgetSnapshot) = Unit
}

object WidgetSnapshots {
    fun compute(
        sessions: List<AttendanceSession>,
        entries: List<TimeEntryWithDetails>,
        settings: AppSettings,
        now: Long = currentTimeMillis(),
        tasks: List<WorkTaskWithProject> = emptyList(),
        projects: List<Project> = emptyList()
    ): WidgetSnapshot {
        val today = DateRanges.dayRange(now)
        val open = sessions.firstOrNull { it.clockOut == null }
        val closedToday = ReportCalculator.attendanceSeconds(sessions.filter { it.clockOut != null }, today, now)
        val running = entries.firstOrNull { it.isRunning }
        val runningProject = running?.let { entry ->
            entry.projectCode?.let { code -> if (entry.isProductive) "$code · ${entry.projectName}" else entry.projectName }
        }
        return WidgetSnapshot(
            clockedIn = open != null,
            // An open period from a previous day only counts from midnight, like the reports do.
            clockedInSince = open?.let { maxOf(it.clockIn, today.start) },
            attendanceSecondsBefore = closedToday,
            targetSeconds = settings.targetSecondsFor(today.start.toLocalDate().dayOfWeek.isoDayNumber),
            runningProject = runningProject,
            runningTask = running?.taskTitle,
            runningSince = running?.startTime,
            runningProjectId = running?.projectId,
            runningTaskId = running?.taskId,
            quickTasks = quickTasks(tasks, entries, running?.taskId, projects.filter { it.isProductive }.map { it.id }.toSet()),
            language = settings.language,
            updatedAt = now
        )
    }

    /**
     * Running task first, then the tasks worked on most recently, then the remaining open tasks
     * (productive projects before the unproductive one), capped at [WidgetSnapshot.MAX_QUICK_TASKS].
     */
    fun quickTasks(
        tasks: List<WorkTaskWithProject>,
        entries: List<TimeEntryWithDetails>,
        runningTaskId: Long?,
        productiveProjectIds: Set<Long>
    ): List<QuickTask> {
        val lastUsed = entries.filter { it.taskId != null }.groupBy { it.taskId!! }.mapValues { (_, e) -> e.maxOf { it.startTime } }
        val productive = tasks.associate { it.id to (it.projectId in productiveProjectIds) }
        return tasks
            .filter { it.status != "DONE" }
            .sortedWith(
                compareByDescending<WorkTaskWithProject> { it.id == runningTaskId }
                    .thenByDescending { lastUsed[it.id] ?: Long.MIN_VALUE }
                    .thenByDescending { productive[it.id] == true }
                    .thenBy { it.projectCode }
                    .thenBy { it.title }
            )
            .take(WidgetSnapshot.MAX_QUICK_TASKS)
            .map { task ->
                val isProductive = productive[task.id] == true
                QuickTask(
                    taskId = task.id,
                    projectId = task.projectId,
                    title = task.title,
                    projectLabel = if (isProductive) "${task.projectCode} · ${task.projectName}" else task.projectName,
                    productive = isProductive
                )
            }
    }

    /** Emits a fresh snapshot whenever sessions, activities, tasks, projects or the schedule change. */
    fun flow(repository: TimeTrackerRepository): Flow<WidgetSnapshot> =
        combine(
            repository.allSessions, repository.allTimeEntries, repository.settings, repository.allTasksWithProject, repository.allProjects
        ) { sessions, entries, settings, tasks, projects ->
            compute(sessions, entries, settings ?: AppSettings(), tasks = tasks, projects = projects)
        }.distinctUntilChanged { old, new -> old.copy(updatedAt = 0L) == new.copy(updatedAt = 0L) }
}
