package com.suw1labs.worktracker.data.backup

import com.suw1labs.worktracker.data.model.AbsenceType
import com.suw1labs.worktracker.data.model.AppSettings
import com.suw1labs.worktracker.data.model.AttendanceSession
import com.suw1labs.worktracker.data.model.ClockOutReason
import com.suw1labs.worktracker.data.model.DayRecord
import com.suw1labs.worktracker.data.model.Project
import com.suw1labs.worktracker.data.model.TimeEntry
import com.suw1labs.worktracker.data.model.WorkTask
import com.suw1labs.worktracker.util.DateRanges
import com.suw1labs.worktracker.util.currentTimeMillis
import com.suw1labs.worktracker.util.toLocalDate
import kotlinx.datetime.isoDayNumber

/**
 * Sample data for store screenshots and demos (fictional projects, clients and numbers): a month of
 * realistic engineering days, a
 * running activity right now and a task due soon. Built as a [BackupFile] so it is applied with
 * the normal restore path and never touches production code paths. Debug / simulator builds only.
 */
object DemoData {
    private const val HOUR = 3600_000L
    private const val MINUTE = 60_000L

    suspend fun seed(manager: BackupManager, language: String?, now: Long = currentTimeMillis()) {
        manager.restore(build(now, language ?: "en"))
    }

    fun build(now: Long, language: String): BackupFile {
        val today = DateRanges.dayRange(now).start
        fun day(offset: Int) = today - offset * 24 * HOUR
        fun at(dayStart: Long, hours: Double) = dayStart + (hours * HOUR).toLong()

        val projects = listOf(
            Project(id = 1, code = "4711", name = "Aurora", client = "Nordwind AG", colorHex = "#2F6BFF", budgetHours = 320.0, isFocused = true),
            Project(id = 2, code = "2380", name = "Helix", client = "Baumann Technik", colorHex = "#10B981", budgetHours = 120.0, isFocused = true),
            Project(id = 3, code = "1905", name = "Kestrel", client = "Rheintal Energie", colorHex = "#8B5CF6", budgetHours = 80.0),
            Project(id = 4, code = Project.UNPRODUCTIVE_CODE, name = "Unproductive", client = "", colorHex = "#F59E0B", isProductive = false)
        )
        val tasks = listOf(
            WorkTask(id = 10, projectId = 1, title = "General", priority = "LOW", reminderEnabled = false),
            WorkTask(id = 11, projectId = 1, title = "Commissioning", priority = "HIGH", status = "IN_PROGRESS", estimatedHours = 40.0, deadlineTimestamp = at(day(-2), 17.0)),
            WorkTask(id = 12, projectId = 1, title = "Site documentation", priority = "MEDIUM", estimatedHours = 12.0, deadlineTimestamp = at(day(-12), 17.0)),
            WorkTask(id = 13, projectId = 2, title = "Control cabinet layout", priority = "MEDIUM", status = "IN_PROGRESS", estimatedHours = 24.0, deadlineTimestamp = at(day(-9), 17.0)),
            WorkTask(id = 14, projectId = 2, title = "Safety review", priority = "HIGH", status = "DONE", estimatedHours = 8.0),
            WorkTask(id = 15, projectId = 3, title = "Software update", priority = "MEDIUM", status = "REVIEW", estimatedHours = 16.0),
            WorkTask(id = 16, projectId = 3, title = "Wiring diagrams", priority = "LOW", estimatedHours = 6.0),
            WorkTask(id = 17, projectId = 4, title = "Meeting", priority = "LOW", reminderEnabled = false),
            WorkTask(id = 18, projectId = 4, title = "Coffee / Smoke break", priority = "LOW", reminderEnabled = false),
            WorkTask(id = 19, projectId = 4, title = "Informal meeting", priority = "LOW", reminderEnabled = false),
            WorkTask(id = 20, projectId = 4, title = "Uncategorized", priority = "LOW", reminderEnabled = false)
        )

        val sessions = mutableListOf<AttendanceSession>()
        val entries = mutableListOf<TimeEntry>()
        var nextId = 1000L
        fun session(clockIn: Long, clockOut: Long?, reason: ClockOutReason?) {
            sessions += AttendanceSession(id = nextId++, clockIn = clockIn, clockOut = clockOut, clockOutReason = reason?.name, createdAt = clockIn)
        }
        fun entry(projectId: Long, taskId: Long, start: Long, end: Long?, note: String = "") {
            entries += TimeEntry(id = nextId++, projectId = projectId, taskId = taskId, description = note, startTime = start, endTime = end, createdAt = start)
        }

        // Absences: a holiday and a sick day in the past weeks (weekdays are found below).
        val absences = mutableListOf<DayRecord>()
        var holidayDone = false
        var sickDone = false

        // The past 30 days: Monday to Friday, two clock-in periods with lunch between.
        for (offset in 30 downTo 1) {
            val d = day(offset)
            val weekday = d.toLocalDate().dayOfWeek.isoDayNumber
            if (weekday > 5) continue
            if (!holidayDone && offset in 8..12) {
                absences += DayRecord(id = nextId++, dayStart = d, type = AbsenceType.HOLIDAY.name, hours = 8.0, createdAt = d)
                holidayDone = true
                continue
            }
            if (!sickDone && offset in 15..19) {
                absences += DayRecord(id = nextId++, dayStart = d, type = AbsenceType.SICK.name, hours = 8.0, createdAt = d)
                sickDone = true
                continue
            }
            val jitter = (offset * 7) % 4            // 0..3: a few minutes of variation per day
            val morningIn = at(d, 7.9) + jitter * 3 * MINUTE
            val lunchOut = at(d, 12.0) + jitter * 2 * MINUTE
            val afternoonIn = lunchOut + 40 * MINUTE
            val eveningOut = at(d, 17.1) + jitter * 8 * MINUTE + if (weekday == 5) -45 * MINUTE else 0L
            session(morningIn, lunchOut, ClockOutReason.LUNCH)
            session(afternoonIn, eveningOut, ClockOutReason.END_OF_DAY)

            val split1 = morningIn + 95 * MINUTE
            val split2 = split1 + 30 * MINUTE
            entry(1, if (offset % 3 == 0) 12L else 11L, morningIn, split1)
            entry(4, 17, split1, split2, note = "Daily stand-up")
            entry(2, 13, split2, lunchOut)
            val split3 = afternoonIn + 150 * MINUTE
            entry(3, if (offset % 2 == 0) 15L else 16L, afternoonIn, split3)
            entry(1, 11, split3, eveningOut)
        }

        // Today: clocked in this morning, lunch, back at work – with an activity running right now.
        val morningIn = at(today, 8.0) + 2 * MINUTE
        val lunchOut = at(today, 12.0) + 5 * MINUTE
        val afternoonIn = lunchOut + 35 * MINUTE
        fun clip(start: Long, end: Long): Pair<Long, Long?>? = when {
            start >= now -> null
            end > now -> start to null
            else -> start to end
        }
        listOf(
            Triple(1L, 10L, morningIn to morningIn + 88 * MINUTE),
            Triple(2L, 13L, morningIn + 88 * MINUTE to morningIn + 165 * MINUTE),
            Triple(1L, 11L, morningIn + 165 * MINUTE to lunchOut)
        ).forEach { (p, t, range) -> clip(range.first, range.second)?.let { (s, e) -> entry(p, t, s, e) } }
        if (morningIn < now) session(morningIn, lunchOut.takeIf { it <= now }, ClockOutReason.LUNCH.takeIf { lunchOut <= now })
        if (afternoonIn < now) {
            session(afternoonIn, null, null)
            val switchAt = maxOf(afternoonIn + 10 * MINUTE, now - 80 * MINUTE)
            if (switchAt > afternoonIn) entry(3, 16, afternoonIn, minOf(switchAt, now), note = "Panel wiring")
            if (switchAt < now) entry(1, 11, switchAt, null, note = "Loop checks, line 2")
        }

        return BackupFile(
            exportedAt = now,
            platform = "demo",
            settings = AppSettings(language = language, paidBreakMinutes = 10),
            projects = projects,
            tasks = tasks,
            timeEntries = entries,
            attendanceSessions = sessions,
            dayRecords = absences
        )
    }
}
