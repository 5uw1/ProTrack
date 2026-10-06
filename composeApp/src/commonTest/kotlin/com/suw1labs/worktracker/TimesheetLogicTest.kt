package com.suw1labs.worktracker

import com.suw1labs.worktracker.data.model.TimeEntryWithDetails
import com.suw1labs.worktracker.ui.timesheet.ClockTime
import com.suw1labs.worktracker.ui.timesheet.Lane
import com.suw1labs.worktracker.ui.timesheet.TimesheetEdits
import com.suw1labs.worktracker.ui.timesheet.TimesheetLayout
import com.suw1labs.worktracker.util.startOfDayMillis
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TimesheetLogicTest {
    private val zurich = TimeZone.of("Europe/Zurich")

    @Test
    fun parse_acceptsWhatPeopleType() {
        val cases = mapOf(
            "8" to 480, "08" to 480, "8h" to 480, "830" to 510, "0830" to 510, "8:30" to 510, "8.30" to 510,
            "8h30" to 510, "8 30" to 510, " 17:05 " to 1025, "0" to 0, "24:00" to 1440, "2400" to 1440, "23:59" to 1439
        )
        cases.forEach { (text, minutes) -> assertEquals(minutes, ClockTime.parse(text), "\"$text\"") }
    }

    @Test
    fun parse_rejectsWhatIsNoTime() {
        listOf("", "  ", "25", "8:60", "24:01", "12345", "ab", "8:3:0", "-1", "8:", ":30x").forEach {
            assertNull(ClockTime.parse(it), "\"$it\"")
        }
    }

    @Test
    fun format_padsAndShowsMidnightAsTheEndOfTheDay() {
        assertEquals("08:05", ClockTime.format(485))
        assertEquals("24:00", ClockTime.format(1440))
    }

    @Test
    fun clockMath_followsLocalTimeAcrossTheDstChange() {
        // 29 March 2026: Zurich skips 02:00–03:00, so the day is 23 hours long.
        val day = LocalDate(2026, 3, 29).startOfDayMillis(zurich)
        val nine = ClockTime.at(day, 9 * 60, zurich)
        assertEquals(8 * 3600_000L, nine - day)
        assertEquals(9 * 60, ClockTime.minutesOf(nine, day, zurich))
        assertEquals(LocalDate(2026, 3, 30).startOfDayMillis(zurich), ClockTime.at(day, 1440, zurich))
        // Moving an activity to the next day keeps its time of day, not its distance in hours.
        val friday9 = ClockTime.at(LocalDate(2026, 3, 27).startOfDayMillis(zurich), 540, zurich)
        assertEquals(ClockTime.at(LocalDate(2026, 3, 30).startOfDayMillis(zurich), 540, zurich), ClockTime.shiftDays(friday9, 3, zurich))
    }

    @Test
    fun snap_roundsToTheGrid() {
        assertEquals(480, ClockTime.snap(482, 5))
        assertEquals(485, ClockTime.snap(483, 5))
        assertEquals(1440, ClockTime.snap(1442, 5))
    }

    @Test
    fun magnet_pullsAnEdgeOntoTheNeighbour_whenClose() {
        val edges = listOf(10 * 60 + 47, 12 * 60 + 5)   // the next activity starts 10:47, clock-out 12:05
        assertEquals(647, ClockTime.magnet(644, edges, threshold = 8, step = 5))   // 10:44 -> 10:47, not 10:45
        assertEquals(725, ClockTime.magnet(731, edges, threshold = 8, step = 5))   // 12:11 -> 12:05
        assertEquals(615, ClockTime.magnet(613, edges, threshold = 8, step = 5))   // nothing near: grid
    }

    @Test
    fun magnetMove_alignsWhicheverEdgeIsCloser() {
        val edges = listOf(600, 720)   // 10:00 and 12:00
        // A 60-minute block dropped at 10:57: its end (11:57) is 3 min from 12:00, its start 57 from anything.
        assertEquals(660, ClockTime.magnetMove(657, 60, edges, threshold = 8, step = 5))
        // Dropped at 10:03: its start meets 10:00.
        assertEquals(600, ClockTime.magnetMove(603, 60, edges, threshold = 8, step = 5))
        // Far from both: on the grid, inside the day.
        assertEquals(840, ClockTime.magnetMove(842, 60, edges, threshold = 8, step = 5))
        assertEquals(1380, ClockTime.magnetMove(1430, 60, emptyList(), threshold = 8, step = 5))
    }

    @Test
    fun lanes_putOverlappingActivitiesSideBySide() {
        val lanes = TimesheetLayout.lanes(
            mapOf(
                1L to (0L to 10L),
                2L to (5L to 15L),   // overlaps 1
                3L to (10L to 20L),  // overlaps 2, reuses 1's lane
                4L to (30L to 40L)   // alone
            )
        )
        assertEquals(Lane(0, 2), lanes[1])
        assertEquals(Lane(1, 2), lanes[2])
        assertEquals(Lane(0, 2), lanes[3])
        assertEquals(Lane(0, 1), lanes[4])
    }

    private val day = 1_700_000_000_000L
    private fun at(hours: Double) = day + (hours * 3600_000L).toLong()
    private fun entry(id: Long, start: Double, end: Double?) = TimeEntryWithDetails(
        id = id, projectId = 1, projectCode = "P", projectName = "Proj", projectColor = "#000000", projectProductive = true,
        taskId = null, taskTitle = null, description = "", startTime = at(start), endTime = end?.let { at(it) }, createdAt = 0
    )

    @Test
    fun retimed_shortensTheNeighbourItRunsInto_andLeavesAGapOtherwise() {
        val a = entry(1, 8.0, 10.0)
        val b = entry(2, 10.0, 12.0)
        val c = entry(3, 12.0, 14.0)
        val all = listOf(a, b, c)

        // b ends later: c starts later too. b starts later: a is left alone, a gap opens.
        val saved = TimesheetEdits.retimed(b, at(10.5), at(12.5), all)!!
        assertEquals(listOf(2L to (at(10.5) to at(12.5)), 3L to (at(12.5) to at(14.0))), saved.map { it.id to (it.startTime to it.endTime) })

        // b starts earlier: a ends earlier.
        val earlier = TimesheetEdits.retimed(b, at(9.5), at(12.0), all)!!
        assertEquals(listOf(2L to (at(9.5) to at(12.0)), 1L to (at(8.0) to at(9.5))), earlier.map { it.id to (it.startTime to it.endTime) })
    }

    @Test
    fun retimed_refusesAnEmptyActivity_andKeepsARunningOneRunning() {
        val a = entry(1, 8.0, 10.0)
        assertNull(TimesheetEdits.retimed(a, at(10.0), at(10.0), listOf(a)))
        assertEquals(emptyList(), TimesheetEdits.retimed(a, a.startTime, a.endTime, listOf(a)))
        val running = entry(2, 9.0, null)
        assertEquals(listOf<Long?>(null), TimesheetEdits.retimed(running, at(8.5), at(11.0), listOf(running))!!.map { it.endTime })
    }

    @Test
    fun moved_toAnotherDay_leavesTheOldNeighboursAlone() {
        val a = entry(1, 8.0, 10.0)
        val b = entry(2, 10.0, 12.0)
        val saved = TimesheetEdits.moved(b, ClockTime.shiftDays(b.startTime, 1), listOf(a, b))!!
        assertEquals(1, saved.size)
        assertEquals(2 * 3600_000L, saved.single().endTime!! - saved.single().startTime)
        assertNull(TimesheetEdits.moved(entry(3, 13.0, null), at(14.0), emptyList()))
        // Later on the same day: the activity it now runs into is shortened.
        val sameDay = TimesheetEdits.moved(a, at(9.0), listOf(a, b))!!
        assertEquals(listOf(1L to (at(9.0) to at(11.0)), 2L to (at(11.0) to at(12.0))), sameDay.map { it.id to (it.startTime to it.endTime) })
    }
}
