package com.suw1labs.worktracker

import com.suw1labs.worktracker.data.model.AttendanceSession
import com.suw1labs.worktracker.data.model.TimeEntryWithDetails
import com.suw1labs.worktracker.data.report.DayGaps
import com.suw1labs.worktracker.data.report.EntryNeighbours
import com.suw1labs.worktracker.data.report.NeighbourAdjustment
import com.suw1labs.worktracker.data.report.UnassignedGap
import com.suw1labs.worktracker.ui.screens.TimelineItem
import com.suw1labs.worktracker.ui.screens.buildTimeline
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DayGapsTest {
    private val dayStart = 1_700_000_000_000L
    private fun at(hours: Double): Long = dayStart + (hours * 3600_000L).toLong()

    private fun entry(id: Long, start: Long, end: Long?) = TimeEntryWithDetails(
        id = id, projectId = 1, projectCode = "P", projectName = "Proj", projectColor = "#000000", projectProductive = true,
        taskId = null, taskTitle = null, description = "", startTime = start, endTime = end, createdAt = start
    )

    @Test
    fun gaps_areTheClockedInTimeNoActivityCovers() {
        // 08:16 in, 08:19 out (nothing logged); 09:58 in, 12:31 out with work 09:58–10:01 and 10:01–12:31
        val sessions = listOf(
            AttendanceSession(id = 1, clockIn = at(8.0), clockOut = at(8.5)),
            AttendanceSession(id = 2, clockIn = at(10.0), clockOut = at(12.5))
        )
        val entries = listOf(entry(1, at(10.0), at(10.05)), entry(2, at(10.05), at(11.0)))

        val gaps = DayGaps.compute(entries, sessions, now = at(17.0))
        assertEquals(listOf(UnassignedGap(1, at(8.0), at(8.5)), UnassignedGap(2, at(11.0), at(12.5))), gaps)
    }

    @Test
    fun gaps_ignoreActivitiesOutsideThePeriodAndTinyOnes() {
        val sessions = listOf(AttendanceSession(id = 1, clockIn = at(9.0), clockOut = at(12.0)))
        val entries = listOf(
            entry(1, at(8.0), at(9.5)),          // started before clock-in: only 09:00–09:30 counts
            entry(2, at(9.5) + 20_000L, at(10.0)), // 20 s after: not a gap
            entry(3, at(10.5), at(13.0))         // runs past clock-out
        )
        assertEquals(listOf(UnassignedGap(1, at(10.0), at(10.5))), DayGaps.compute(entries, sessions, now = at(17.0)))
    }

    @Test
    fun gaps_openPeriodWithoutActivityStaysOpen() {
        val sessions = listOf(AttendanceSession(id = 1, clockIn = at(13.0), clockOut = null))
        val gaps = DayGaps.compute(emptyList(), sessions, now = at(13.5))
        assertEquals(listOf(UnassignedGap(1, at(13.0), null)), gaps)
        assertEquals(1800L, gaps.single().durationSeconds(at(13.5)))

        // Once something is running, the gap closes at its start.
        val running = listOf(entry(1, at(13.25), null))
        assertEquals(listOf(UnassignedGap(1, at(13.0), at(13.25))), DayGaps.compute(running, sessions, now = at(13.5)))
    }

    @Test
    fun gaps_overlappingActivitiesLeaveNoFalseGap() {
        val sessions = listOf(AttendanceSession(id = 1, clockIn = at(8.0), clockOut = at(12.0)))
        val entries = listOf(entry(1, at(8.0), at(10.0)), entry(2, at(9.0), at(9.5)), entry(3, at(10.0), at(12.0)))
        assertTrue(DayGaps.compute(entries, sessions, now = at(17.0)).isEmpty())
    }

    @Test
    fun timeline_showsGapsBetweenTheEvents() {
        val sessions = listOf(AttendanceSession(id = 1, clockIn = at(8.0), clockOut = at(12.0)))
        val entries = listOf(entry(1, at(9.0), at(12.0)))
        val keys = buildTimeline(entries, sessions, now = at(17.0)).map { it.key }
        assertEquals(listOf("session-1-out", "entry-1", "gap-${at(8.0)}", "session-1-in"), keys)
        val gap = buildTimeline(entries, sessions, now = at(17.0)).filterIsInstance<TimelineItem.Gap>().single()
        assertEquals(at(9.0), gap.gap.end)
    }

    @Test
    fun neighbours_areTheContiguousActivitiesOnly() {
        val a = entry(1, at(8.0), at(9.0))
        val b = entry(2, at(9.0), at(10.0))
        val c = entry(3, at(10.0) + 30_000L, at(11.0))
        val far = entry(4, at(12.0), at(13.0))
        val all = listOf(far, c, b, a)

        assertEquals(a, EntryNeighbours.previousOf(b, all))
        assertEquals(c, EntryNeighbours.nextOf(b, all))
        assertNull(EntryNeighbours.nextOf(c, all))
        assertNull(EntryNeighbours.previousOf(a, all))
        assertNull(EntryNeighbours.nextOf(entry(2, at(9.0), null), all))
    }

    @Test
    fun adjusted_movesOnlyTheChosenNeighboursAndNeverEmptiesThem() {
        val a = entry(1, at(8.0), at(9.0))
        val c = entry(3, at(10.0), at(11.0))

        // Start moved later (gap) and end moved earlier (gap): both neighbours grow to meet the entry.
        val both = EntryNeighbours.adjusted(a, c, newStart = at(9.25), newEnd = at(9.75), choice = NeighbourAdjustment(previous = true, next = true))
        assertEquals(listOf(a.copy(endTime = at(9.25)), c.copy(startTime = at(9.75))), both)

        // Start moved earlier (overlap): the previous one is trimmed.
        assertEquals(listOf(a.copy(endTime = at(8.5))), EntryNeighbours.adjusted(a, c, at(8.5), at(10.0), NeighbourAdjustment(previous = true)))

        // Not chosen: untouched.
        assertTrue(EntryNeighbours.adjusted(a, c, at(9.25), at(9.75), NeighbourAdjustment.NONE).isEmpty())

        // Would swallow the neighbour entirely: left alone.
        assertTrue(EntryNeighbours.adjusted(a, c, at(7.5), at(11.5), NeighbourAdjustment(previous = true, next = true)).isEmpty())

        // Unchanged boundary: nothing to save.
        assertTrue(EntryNeighbours.adjusted(a, c, at(9.0), at(10.0), NeighbourAdjustment(previous = true, next = true)).isEmpty())
    }
}
