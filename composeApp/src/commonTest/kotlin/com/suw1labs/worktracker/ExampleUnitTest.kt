package com.suw1labs.worktracker

import com.suw1labs.worktracker.data.import.ProjectImporter
import com.suw1labs.worktracker.data.model.AppSettings
import com.suw1labs.worktracker.data.model.AttendanceSession
import com.suw1labs.worktracker.data.model.DayRecord
import com.suw1labs.worktracker.data.model.AbsenceType
import com.suw1labs.worktracker.data.report.WarningKind
import com.suw1labs.worktracker.data.report.WorkRules
import com.suw1labs.worktracker.data.model.TimeEntryWithDetails
import com.suw1labs.worktracker.data.report.ReportCalculator
import com.suw1labs.worktracker.util.DateRange
import com.suw1labs.worktracker.util.TimeFormat
import com.suw1labs.worktracker.util.formatFixed
import com.suw1labs.worktracker.util.parseHexColor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Common unit tests that run on every target (JVM/desktop, Android host tests, iOS simulator).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun durationFormatting_padsHoursMinutesSeconds() {
        assertEquals("00:00:05", TimeFormat.hms(5))
        assertEquals("01:01:01", TimeFormat.hms(3661))
        assertEquals("27:00:00", TimeFormat.hms(27 * 3600))
        assertEquals("6:05", TimeFormat.hm(6 * 3600 + 5 * 60))
    }

    @Test
    fun sapHours_roundToQuarterHours() {
        assertEquals(1.5, TimeFormat.quarterHours(90 * 60))
        assertEquals(1.0, TimeFormat.quarterHours(65 * 60))   // 1h05 -> 1
        assertEquals(1.25, TimeFormat.quarterHours(70 * 60))  // 1h10 -> 1.25
        assertEquals("1.5", TimeFormat.sapHours(1.5))
        assertEquals("2", TimeFormat.sapHours(2.0))
        assertEquals("0.25", TimeFormat.sapHours(0.25))
    }

    @Test
    fun fixedFormatting_matchesJavaStringFormat() {
        assertEquals("1.5", 1.5.formatFixed(1))
        assertEquals("2.35", 2.345.formatFixed(2))
        assertEquals("85", 85.0.formatFixed(0))
        assertEquals("0.00", 0.0.formatFixed(2))
        assertEquals("-3.3", (-3.25).formatFixed(1))
    }

    @Test
    fun hexColorParsing_handlesRgbAndInvalidInput() {
        assertEquals(0xFF3B82F6L.toULong() shl 32, parseHexColor("#3B82F6")!!.value)
        assertNull(parseHexColor("not-a-color"))
    }

    @Test
    fun reportCalculator_splitsAttendanceIntoProjectUnproductiveAndUnallocated() {
        val hour = 3600_000L
        // Align to a local calendar day so the range maps to exactly one DayRow.
        val dayStart = com.suw1labs.worktracker.util.DateRanges.dayRange(1_800_000_000_000L).start
        val range = DateRange(dayStart, dayStart + 24 * hour)
        val now = dayStart + 20 * hour

        val sessions = listOf(
            AttendanceSession(id = 1, clockIn = dayStart + 8 * hour, clockOut = dayStart + 12 * hour, clockOutReason = "LUNCH"),
            AttendanceSession(id = 2, clockIn = dayStart + 13 * hour, clockOut = null) // still clocked in, evaluated up to `now`
        )
        val entries = listOf(
            entry(1, projectId = 10, code = "P-1", task = "PLC", productive = true, start = dayStart + 8 * hour, end = dayStart + 11 * hour),
            entry(2, projectId = 10, code = "P-1", task = "Meeting", productive = true, start = dayStart + 11 * hour, end = dayStart + 12 * hour),
            entry(3, projectId = 99, code = "UNPRODUCTIVE", task = "Coffee", productive = false, start = dayStart + 13 * hour, end = dayStart + 13 * hour + 30 * 60_000L),
            entry(4, projectId = 20, code = "P-2", task = "PLC", productive = true, start = dayStart + 14 * hour, end = null) // running
        )

        val report = ReportCalculator.compute(sessions, entries, range, now)

        assertEquals(11 * 3600L, report.attendanceSeconds)          // 4h + 7h
        assertEquals(10 * 3600L, report.productiveSeconds)          // 3 + 1 + 6 (running until now)
        assertEquals(1800L, report.unproductiveSeconds)
        assertEquals(1800L, report.unallocatedSeconds)              // 13:30 - 14:00 gap
        assertEquals(listOf("P-2", "P-1", "UNPRODUCTIVE"), report.projects.map { it.code })
        assertEquals(4 * 3600L, report.projects.first { it.code == "P-1" }.seconds)
        assertEquals(1, report.days.size)
    }

    @Test
    fun workRules_requireBreaksAboveFiveAndNineHours() {
        assertEquals(0L, WorkRules.requiredBreakSeconds(5 * 3600L))
        assertEquals(30 * 60L, WorkRules.requiredBreakSeconds(5 * 3600L + 1))
        assertEquals(30 * 60L, WorkRules.requiredBreakSeconds(9 * 3600L))
        assertEquals(60 * 60L, WorkRules.requiredBreakSeconds(9 * 3600L + 1))
    }

    @Test
    fun reportCalculator_flagsShortBreaksAndComputesOvertime() {
        val hour = 3600_000L
        // 2026-09-14 (Monday) 00:00 local ~ use a fixed epoch; targets only depend on weekday and settings.
        val dayStart = com.suw1labs.worktracker.util.DateRanges.dayRange(1_789_400_000_000L).start
        val range = DateRange(dayStart, dayStart + 24 * hour)
        val now = dayStart + 23 * hour
        val settings = AppSettings(fullTimeWeeklyHours = 40.0)

        // 08:00-13:00 and 13:15-18:00 = 9h45 worked with only 15 min break.
        val sessions = listOf(
            AttendanceSession(id = 1, clockIn = dayStart + 8 * hour, clockOut = dayStart + 13 * hour),
            AttendanceSession(id = 2, clockIn = dayStart + 13 * hour + 15 * 60_000L, clockOut = dayStart + 18 * hour)
        )
        val report = ReportCalculator.compute(sessions, emptyList(), range, now, settings)
        val day = report.days.single()

        assertEquals(15 * 60L, day.breakSeconds)
        if (day.isWorkday) {
            assertEquals(8 * 3600L, day.targetSeconds)
            // 45 min of the required 1 h break were not taken and are deducted: 9 h count, 1 h overtime.
            assertEquals(9 * 3600L - 8 * 3600L, day.overtimeSeconds)
        }
        assertTrue(report.warnings.any { it.kind == WarningKind.BREAK_TOO_SHORT })
        // 1 h required, 15 min taken: 45 min are deducted from the counted time.
        assertEquals(45 * 60L, day.deductedBreakSeconds)
        assertEquals(9 * 3600L, day.accountedSeconds)
        assertEquals(45 * 60L, report.warnings.first { it.kind == WarningKind.BREAK_TOO_SHORT }.deductedSeconds)

        // Deduction switched off: nothing is taken off, the warning stays.
        val kept = ReportCalculator.compute(sessions, emptyList(), range, now, settings.copy(deductMissingBreak = false))
        assertEquals(0L, kept.days.single().deductedBreakSeconds)
        assertEquals(9 * 3600L + 45 * 60L, kept.days.single().accountedSeconds)
        assertTrue(kept.warnings.any { it.kind == WarningKind.BREAK_TOO_SHORT })
    }

    @Test
    fun breakRules_areAdjustableAndDeductOnlyTheMissingPart() {
        val hour = 3600_000L
        val dayStart = com.suw1labs.worktracker.util.DateRanges.dayRange(1_789_400_000_000L).start
        val range = DateRange(dayStart, dayStart + 24 * hour)
        val now = dayStart + 23 * hour

        // 08:00-12:30, 13:00-18:00: 9 h 30 clocked in with a 30 min break.
        val sessions = listOf(
            AttendanceSession(id = 1, clockIn = dayStart + 8 * hour, clockOut = dayStart + 12 * hour + 30 * 60_000L),
            AttendanceSession(id = 2, clockIn = dayStart + 13 * hour, clockOut = dayStart + 18 * hour)
        )
        val default = ReportCalculator.compute(sessions, emptyList(), range, now, AppSettings()).days.single()
        assertEquals(60 * 60L, default.requiredBreakSeconds)
        assertEquals(30 * 60L, default.deductedBreakSeconds)
        assertEquals(9 * 3600L, default.accountedSeconds)

        // Own rules: 15 min after 5.5 h, 45 min after 8 h – only 15 min are missing.
        val custom = AppSettings(breakRules = "5.5:15,8:45")
        assertEquals(listOf(8 * 3600L, (5.5 * 3600).toLong()), custom.breakRuleList.map { it.afterSeconds })
        val day = ReportCalculator.compute(sessions, emptyList(), range, now, custom).days.single()
        assertEquals(45 * 60L, day.requiredBreakSeconds)
        assertEquals(15 * 60L, day.deductedBreakSeconds)

        // No rules at all: nothing required, nothing deducted; malformed parts are ignored.
        assertEquals(0L, WorkRules.requiredBreakSeconds(10 * 3600L, AppSettings(breakRules = "").breakRuleList))
        assertEquals(1, AppSettings(breakRules = "abc,6:30,7").breakRuleList.size)
        assertEquals("5.5:15,8:45", AppSettings.breakRulesString(custom.breakRuleList))
    }

    @Test
    fun absences_creditHoursExceptCompensation() {
        val hour = 3600_000L
        val dayStart = com.suw1labs.worktracker.util.DateRanges.dayRange(1_789_400_000_000L).start
        val range = DateRange(dayStart, dayStart + 24 * hour)
        val now = dayStart + 23 * hour
        val settings = AppSettings()

        val sick = ReportCalculator.compute(emptyList(), emptyList(), range, now, settings,
            listOf(DayRecord(dayStart = dayStart, type = AbsenceType.SICK.name, hours = 8.0)))
        val compensation = ReportCalculator.compute(emptyList(), emptyList(), range, now, settings,
            listOf(DayRecord(dayStart = dayStart, type = AbsenceType.COMPENSATION.name, hours = 8.0)))

        assertEquals(8 * 3600L, sick.creditedSeconds)
        assertEquals(0L, compensation.creditedSeconds)
        // Sick day is neutral; compensation consumes one day of overtime (on a working day).
        assertEquals(sick.attendanceSeconds + sick.creditedSeconds - sick.targetSeconds, sick.overtimeSeconds)
        assertEquals(-sick.targetSeconds, compensation.overtimeSeconds)
    }

    @Test
    fun projectImporter_parsesExcelStyleInput() {
        val text = "Project number\tName\tCustomer\nP-2026-0142\tSpindle retrofit\tCustomer AG\nP-2026-0150;New HMI\n\"P-1, special\",\"Name, with comma\""
        val parsed = ProjectImporter.parse(text)
        assertEquals(3, parsed.size)
        assertEquals(ProjectImporter.ParsedProject("P-2026-0142", "Spindle retrofit", "Customer AG"), parsed[0])
        assertEquals(ProjectImporter.ParsedProject("P-2026-0150", "New HMI", ""), parsed[1])
        assertEquals("P-1, special", parsed[2].code)
        assertEquals("Name, with comma", parsed[2].name)
    }

    private fun entry(
        id: Long,
        projectId: Long?,
        code: String?,
        task: String,
        productive: Boolean,
        start: Long,
        end: Long?
    ) = TimeEntryWithDetails(
        id = id,
        projectId = projectId,
        projectCode = code,
        projectName = code?.let { "Project $it" },
        projectColor = "#3B82F6",
        projectProductive = if (projectId == null) null else productive,
        taskId = null,
        taskTitle = task,
        description = "",
        startTime = start,
        endTime = end,
        createdAt = start
    )
}
