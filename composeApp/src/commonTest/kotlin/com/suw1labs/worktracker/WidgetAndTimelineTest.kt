package com.suw1labs.worktracker

import com.suw1labs.worktracker.data.model.AppSettings
import com.suw1labs.worktracker.data.model.AttendanceSession
import com.suw1labs.worktracker.data.model.ClockOutReason
import com.suw1labs.worktracker.data.report.ReportCalculator
import com.suw1labs.worktracker.data.model.TimeEntryWithDetails
import com.suw1labs.worktracker.data.model.WorkTaskWithProject
import com.suw1labs.worktracker.platform.WidgetSnapshots
import com.suw1labs.worktracker.ui.screens.TimelineItem
import com.suw1labs.worktracker.ui.screens.buildTimeline
import com.suw1labs.worktracker.util.DateRanges
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WidgetAndTimelineTest {
    private val dayStart = DateRanges.dayRange(DateRanges.startOfToday() + 12 * HOUR).start
    private fun at(hours: Double): Long = dayStart + (hours * HOUR).toLong()

    private fun entry(id: Long, start: Long, end: Long?, code: String? = "P-1", productive: Boolean = true) = TimeEntryWithDetails(
        id = id, projectId = 1, projectCode = code, projectName = "Proj", projectColor = "#000000", projectProductive = productive,
        taskId = null, taskTitle = "PLC", description = "", startTime = start, endTime = end, createdAt = start
    )

    @Test
    fun widgetSnapshot_splitsClosedAndOpenAttendance() {
        val sessions = listOf(
            AttendanceSession(id = 1, clockIn = at(8.0), clockOut = at(12.0)),
            AttendanceSession(id = 2, clockIn = at(13.0), clockOut = null)
        )
        val entries = listOf(entry(1, at(13.0), null))
        val snapshot = WidgetSnapshots.compute(sessions, entries, AppSettings(), now = at(14.0))

        assertTrue(snapshot.clockedIn)
        assertEquals(at(13.0), snapshot.clockedInSince)
        assertEquals(4 * 3600L, snapshot.attendanceSecondsBefore)
        assertEquals(5 * 3600L, snapshot.attendanceSecondsAt(at(14.0)))
        assertEquals("P-1 · Proj", snapshot.runningProject)
        assertEquals("PLC", snapshot.runningTask)
        assertEquals(at(13.0), snapshot.runningSince)
    }

    @Test
    fun widgetSnapshot_clockedOutHasNoRunningActivity() {
        val sessions = listOf(AttendanceSession(id = 1, clockIn = at(8.0), clockOut = at(12.0)))
        val snapshot = WidgetSnapshots.compute(sessions, emptyList(), AppSettings(), now = at(14.0))

        assertEquals(false, snapshot.clockedIn)
        assertNull(snapshot.clockedInSince)
        assertNull(snapshot.runningProject)
        assertEquals(4 * 3600L, snapshot.attendanceSecondsAt(at(20.0)))
    }

    @Test
    fun timeline_interleavesClockEventsNewestFirst() {
        val sessions = listOf(
            AttendanceSession(id = 1, clockIn = at(8.0), clockOut = at(12.0), clockOutReason = "LUNCH"),
            AttendanceSession(id = 2, clockIn = at(13.0), clockOut = null)
        )
        val entries = listOf(entry(10, at(13.0), null), entry(11, at(8.0), at(12.0)))

        val keys = buildTimeline(entries, sessions, now = at(14.0)).map { it.key }
        assertEquals(listOf("entry-10", "session-2-in", "session-1-out", "entry-11", "session-1-in"), keys)

        val clockOut = buildTimeline(entries, sessions, now = at(14.0)).filterIsInstance<TimelineItem.Attendance>().first { !it.isClockIn }
        assertEquals("LUNCH", clockOut.session.clockOutReason)
    }

    private fun task(id: Long, title: String, code: String = "P-1", status: String = "TODO") = WorkTaskWithProject(
        id = id, projectId = if (code == "UNPRODUCTIVE") 2 else 1, projectCode = code, projectName = if (code == "UNPRODUCTIVE") "Unproductive" else "Proj",
        projectColor = "#000000", client = "", title = title, description = "", priority = "LOW", status = status,
        estimatedHours = 0.0, deadlineTimestamp = null, reminderLeadHours = 0, reminderEnabled = false, createdAt = 0
    )

    @Test
    fun quickTasks_runningFirstThenRecentThenProductive_doneExcluded() {
        val tasks = listOf(
            task(1, "Alpha"), task(2, "Beta"), task(3, "Meeting", code = "UNPRODUCTIVE"), task(4, "Done task", status = "DONE"), task(5, "Gamma")
        )
        val entries = listOf(
            entry(10, at(13.0), null).copy(taskId = 5),          // running: Gamma
            entry(11, at(9.0), at(12.0)).copy(taskId = 3),      // used today: Meeting
            entry(12, at(8.0), at(9.0)).copy(taskId = 2)        // used earlier: Beta
        )
        val quick = WidgetSnapshots.quickTasks(tasks, entries, runningTaskId = 5, productiveProjectIds = setOf(1L))

        assertEquals(listOf("Gamma", "Meeting", "Beta", "Alpha"), quick.map { it.title })
        assertEquals("Unproductive", quick[1].projectLabel)
        assertEquals(false, quick[1].productive)
        assertEquals("P-1 · Proj", quick[3].projectLabel)
    }

    @Test
    fun lunchSeconds_countsOnlyGapsAfterLunchClockOut() {
        val sessions = listOf(
            AttendanceSession(id = 1, clockIn = at(8.0), clockOut = at(10.0), clockOutReason = ClockOutReason.BREAK.name),
            AttendanceSession(id = 2, clockIn = at(10.25), clockOut = at(12.0), clockOutReason = ClockOutReason.LUNCH.name),
            AttendanceSession(id = 3, clockIn = at(12.75), clockOut = at(17.0), clockOutReason = ClockOutReason.END_OF_DAY.name)
        )
        val day = DateRanges.dayRange(dayStart)
        assertEquals(45 * 60L, ReportCalculator.lunchSeconds(sessions, day, at(18.0)))
        assertEquals(60 * 60L, ReportCalculator.breakSeconds(sessions, day, at(18.0)))
        assertEquals(45 * 60L, ReportCalculator.compute(sessions, emptyList(), day, at(18.0)).lunchSeconds)
    }

    @Test
    fun paidBreaks_creditTaggedBreaksUpToDailyAllowance() {
        // 7h50 clocked in, two 5-minute smoke breaks and a 45-minute lunch.
        val sessions = listOf(
            AttendanceSession(id = 1, clockIn = at(8.0), clockOut = at(10.0), clockOutReason = ClockOutReason.BREAK.name),
            AttendanceSession(id = 2, clockIn = at(10.0) + 5 * 60_000L, clockOut = at(12.0), clockOutReason = ClockOutReason.LUNCH.name),
            AttendanceSession(id = 3, clockIn = at(12.75), clockOut = at(15.0), clockOutReason = ClockOutReason.BREAK.name),
            AttendanceSession(id = 4, clockIn = at(15.0) + 5 * 60_000L, clockOut = at(16.75), clockOutReason = ClockOutReason.END_OF_DAY.name)
        )
        val day = DateRanges.dayRange(dayStart)
        val unpaid = ReportCalculator.compute(sessions, emptyList(), day, at(18.0), AppSettings())
        val paid = ReportCalculator.compute(sessions, emptyList(), day, at(18.0), AppSettings(paidBreakMinutes = 10))

        assertEquals(7 * 3600L + 50 * 60L, unpaid.attendanceSeconds)
        assertEquals(0L, unpaid.paidBreakSeconds)
        assertEquals(10 * 60L, paid.paidBreakSeconds)
        assertEquals(8 * 3600L, paid.accountedSeconds)
        assertEquals(45 * 60L, paid.lunchSeconds)

        // A third break exceeds the allowance: still only 10 minutes are credited.
        val more = sessions.dropLast(1) + listOf(
            AttendanceSession(id = 4, clockIn = at(15.0) + 5 * 60_000L, clockOut = at(16.0), clockOutReason = ClockOutReason.BREAK.name),
            AttendanceSession(id = 5, clockIn = at(16.0) + 8 * 60_000L, clockOut = at(17.0))
        )
        assertEquals(10 * 60L, ReportCalculator.compute(more, emptyList(), day, at(18.0), AppSettings(paidBreakMinutes = 10)).paidBreakSeconds)
    }

    @Test
    fun summary_splitsClockedInTimeIntoWorkUnproductiveAndNoActivity() {
        val sessions = listOf(AttendanceSession(id = 1, clockIn = at(8.0), clockOut = at(16.0)))
        val entries = listOf(
            entry(1, at(8.0), at(12.0)),                                              // project + task
            entry(2, at(12.0), at(13.0)).copy(taskId = null, taskTitle = null),      // project, no task
            entry(3, at(13.0), at(14.0)).copy(projectId = null, projectCode = null, projectName = null, projectProductive = null), // no project yet
            entry(4, at(14.0), at(15.0), code = "UNPRODUCTIVE", productive = false)
                .copy(projectId = 2, projectName = "Unproductive", taskTitle = "Meeting")  // unproductive project
            // 15:00-16:00: clocked in, nothing running
        )
        val r = ReportCalculator.compute(sessions, entries, DateRanges.dayRange(dayStart), at(17.0), AppSettings())
        assertEquals(8 * 3600L, r.attendanceSeconds)
        assertEquals(6 * 3600L, r.productiveSeconds)
        assertEquals(1 * 3600L, r.noTaskProductiveSeconds)
        assertEquals(1 * 3600L, r.unassignedProductiveSeconds)
        assertEquals(1 * 3600L, r.unproductiveSeconds)
        assertEquals(1 * 3600L, r.unallocatedSeconds)
        assertEquals(r.attendanceSeconds, r.productiveSeconds + r.unproductiveSeconds + r.unallocatedSeconds)
    }

    @Test
    fun forgottenClockOut_usesLastActivityEndOrTarget() {
        val open = AttendanceSession(id = 1, clockIn = at(8.0), clockOut = null)
        // Last activity of that day ended at 16:30 -> clock out there.
        val entries = listOf(entry(1, at(8.0), at(12.0)), entry(2, at(13.0), at(16.5)))
        assertEquals(at(16.5), ReportCalculator.forgottenClockOutTime(open, entries, AppSettings()))
        // No closed activity -> clock-in plus the day's target (8 h on a weekday, capped to the day).
        val guess = ReportCalculator.forgottenClockOutTime(open, emptyList(), AppSettings())
        assertTrue(guess == at(16.0) || guess == DateRanges.dayRange(dayStart).endExclusive - 1)
    }

    private companion object {
        const val HOUR = 3600_000L
    }
}

class ActivitySearchTest {
    private fun task(id: Long, title: String, projectId: Long, code: String, name: String, status: String = "TODO") =
        com.suw1labs.worktracker.data.model.WorkTaskWithProject(
            id = id, projectId = projectId, projectCode = code, projectName = name, projectColor = "#000000", client = "Nordwind",
            title = title, description = "", priority = "LOW", status = status, estimatedHours = 0.0, deadlineTimestamp = null,
            reminderLeadHours = 0, reminderEnabled = false, createdAt = 0
        )

    @kotlin.test.Test
    fun search_matchesEveryWordAcrossTaskAndProjectTexts_tasksFirst() {
        val projects = listOf(
            com.suw1labs.worktracker.data.model.Project(id = 1, code = "4711", name = "Aurora", client = "Nordwind AG"),
            com.suw1labs.worktracker.data.model.Project(id = 2, code = "2380", name = "Helix", client = "Baumann")
        )
        val tasks = listOf(task(10, "Commissioning", 1, "4711", "Aurora"), task(11, "Commissioning", 2, "2380", "Helix"), task(12, "Old", 1, "4711", "Aurora", status = "DONE"))

        val hits = com.suw1labs.worktracker.ui.screens.searchProjectsAndTasks("aur comm", projects, tasks)
        kotlin.test.assertEquals(listOf(10L), hits.map { it.taskId })

        val byCode = com.suw1labs.worktracker.ui.screens.searchProjectsAndTasks("2380", projects, tasks)
        kotlin.test.assertEquals(listOf(11L, null), byCode.map { it.taskId })
        kotlin.test.assertEquals(2L, byCode.last().projectId)

        kotlin.test.assertTrue(com.suw1labs.worktracker.ui.screens.searchProjectsAndTasks("old", projects, tasks).isEmpty(), "done tasks are not offered")
        kotlin.test.assertTrue(com.suw1labs.worktracker.ui.screens.searchProjectsAndTasks("   ", projects, tasks).isEmpty())
    }
}
