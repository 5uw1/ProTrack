package com.suw1labs.worktracker

import com.suw1labs.worktracker.data.model.AttendanceSession
import com.suw1labs.worktracker.data.report.AttendanceCover
import kotlin.test.Test
import kotlin.test.assertEquals

class AttendanceCoverTest {
    private val day = 1_700_000_000_000L
    private fun at(h: Int, m: Int = 0) = day + h * 3600_000L + m * 60_000L

    @Test
    fun anActivityStartedBeforeALateClockIn_movesTheClockInEarlier() {
        // Clocked in 09:23, the meeting really started 09:17.
        val session = AttendanceSession(id = 1, clockIn = at(9, 23), clockOut = at(12, 48))
        assertEquals(listOf(session.copy(clockIn = at(9, 17))), AttendanceCover.extended(listOf(session), at(9, 17), at(9, 58)))
        // Still clocked in (no clock-out yet) and the activity still running.
        val open = AttendanceSession(id = 2, clockIn = at(9, 23))
        assertEquals(listOf(open.copy(clockIn = at(9, 17))), AttendanceCover.extended(listOf(open), at(9, 17), null))
        // Ending exactly at the clock-in counts as touching it.
        assertEquals(listOf(session.copy(clockIn = at(9, 0))), AttendanceCover.extended(listOf(session), at(9, 0), at(9, 23)))
    }

    @Test
    fun anActivityEndingAfterTheClockOut_movesTheClockOutLater() {
        val session = AttendanceSession(id = 1, clockIn = at(8), clockOut = at(12))
        assertEquals(listOf(session.copy(clockOut = at(12, 30))), AttendanceCover.extended(listOf(session), at(11), at(12, 30)))
    }

    @Test
    fun nothingChanges_insideAPeriod_outsideAllPeriods_orAcrossTheLunchBreak() {
        val morning = AttendanceSession(id = 1, clockIn = at(8), clockOut = at(12))
        val afternoon = AttendanceSession(id = 2, clockIn = at(13), clockOut = at(17))
        val both = listOf(morning, afternoon)
        assertEquals(emptyList(), AttendanceCover.extended(both, at(9), at(10)))
        assertEquals(emptyList(), AttendanceCover.extended(both, at(18), at(19)))
        // Starting in the morning period: the afternoon clock-in stays, the gap between is the lunch break.
        assertEquals(emptyList(), AttendanceCover.extended(both, at(11), at(14)))
        // Starting during lunch reaches back only to the end of the morning period's gap.
        assertEquals(listOf(afternoon.copy(clockIn = at(12, 30))), AttendanceCover.extended(both, at(12, 30), at(14)))
    }
}
