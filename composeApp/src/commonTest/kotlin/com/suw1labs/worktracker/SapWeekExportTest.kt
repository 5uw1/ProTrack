package com.suw1labs.worktracker

import com.suw1labs.worktracker.data.export.ExportFormat
import com.suw1labs.worktracker.data.export.SapExport
import com.suw1labs.worktracker.data.export.SapExportType
import com.suw1labs.worktracker.data.model.AppSettings
import com.suw1labs.worktracker.data.model.AttendanceSession
import com.suw1labs.worktracker.data.model.TimeEntryWithDetails
import com.suw1labs.worktracker.data.report.ReportCalculator
import com.suw1labs.worktracker.util.DateRanges
import com.suw1labs.worktracker.util.startOfDayMillis
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SapWeekExportTest {
    private val hour = 3600_000L
    private val minute = 60_000L
    /** Booking codes are per employer, so the test states its own. */
    private val bookingSettings = AppSettings(
        sapProductiveType = "PROJECT",
        sapUnproductiveType = "INTERNAL",
        sapUnproductiveNumber = "1000",
    )

    /** Monday 9 March 2026 – a week whose export the user showed us. */
    private val monday = LocalDate(2026, 3, 9).startOfDayMillis()

    private fun entry(id: Long, dayOffset: Int, startHour: Double, minutes: Int, code: String?, productive: Boolean, project: String = code ?: "") = TimeEntryWithDetails(
        id = id, projectId = if (code == null) null else code.hashCode().toLong(), projectCode = code, projectName = project, projectColor = "#000000",
        projectProductive = if (code == null) null else productive, taskId = null, taskTitle = null, description = "",
        startTime = monday + dayOffset * 24 * hour + (startHour * hour).toLong(), endTime = monday + dayOffset * 24 * hour + (startHour * hour).toLong() + minutes * minute, createdAt = 0
    )

    @Test
    fun weekPaste_matchesTheSapWeeklySheet() {
        // Monday: 30 min office, 3 h P-1001, 3 h 57 P-1003. Tuesday: 30 min office, 5 h P-1001, 59 min P-1003, 1 h 30 P-1002.
        val entries = listOf(
            entry(1, 0, 8.0, 30, "UNPRODUCTIVE", productive = false, project = "Unproductive"),
            entry(2, 0, 9.0, 180, "P-1001.1", productive = true),
            entry(3, 0, 13.0, 237, "P-1003.1", productive = true),
            entry(4, 1, 8.0, 30, "UNPRODUCTIVE", productive = false, project = "Unproductive"),
            entry(5, 1, 9.0, 300, "P-1001.1", productive = true),
            entry(6, 1, 14.0, 59, "P-1003.1", productive = true),
            entry(7, 1, 15.0, 90, "P-1002.1", productive = true)
        )
        val sessions = listOf(
            AttendanceSession(id = 1, clockIn = monday + 8 * hour, clockOut = monday + 17 * hour),
            AttendanceSession(id = 2, clockIn = monday + 24 * hour + 8 * hour, clockOut = monday + 24 * hour + 17 * hour)
        )
        val report = ReportCalculator.compute(sessions, entries, DateRanges.weekRange(monday), now = monday + 10 * 24 * hour, settings = bookingSettings)
        val text = SapExport.generate(SapExportType.SAP_WEEK, report, "week", roundToQuarter = false, format = ExportFormat.CSV, settings = bookingSettings)

        // The exact clipboard text: CRLF after every row, the last one included – what an Excel copy looks like.
        assertEquals(
            "INTERNAL\t\t1000\t0.50\t0.50\t\t\t\r\n" +
                "PROJECT\tP-1001.1\t\t3.00\t5.00\t\t\t\r\n" +
                "PROJECT\tP-1002.1\t\t\t1.50\t\t\t\r\n" +
                "PROJECT\tP-1003.1\t\t3.95\t0.98\t\t\t\r\n",
            text
        )
    }

    @Test
    fun weekPaste_lastRowEndsWithItsFridayValueAndCrlf() {
        // The reported case: hours on the last project's Friday – the very last cell of the paste.
        val entries = listOf(
            entry(1, 0, 8.0, 30, "UNPRODUCTIVE", productive = false, project = "Unproductive"),
            entry(2, 0, 9.0, 180, "P-1001.1", productive = true),
            entry(3, 4, 9.0, 210, "P-1003.1", productive = true)   // Friday, 3 h 30
        )
        val report = ReportCalculator.compute(emptyList(), entries, DateRanges.weekRange(monday), now = monday + 10 * 24 * hour, settings = bookingSettings)
        val text = SapExport.weekPaste(report, bookingSettings)

        assertEquals(
            "INTERNAL\t\t1000\t0.50\t\t\t\t\r\n" +
                "PROJECT\tP-1001.1\t\t3.00\t\t\t\t\r\n" +
                "PROJECT\tP-1003.1\t\t\t\t\t\t3.50\r\n",
            text
        )
        assertTrue(text.endsWith("\t3.50\r\n"), "last cell must be followed by CRLF: ${text.replace("\r", "\\r").replace("\n", "\\n")}")
        // No bare LF anywhere: Android and iOS put the text on the clipboard unchanged.
        assertEquals(text.count { it == '\n' }, Regex("\r\n").findAll(text).count())
        // Every row has the same number of cells (3 booking columns + 5 days).
        assertTrue(text.removeSuffix("\r\n").split("\r\n").all { it.split('\t').size == 8 }, text)
    }

    @Test
    fun weekPaste_monthExport_keepsFridayOfTheWeekThatRunsIntoNextMonth() {
        // April 2026 ends on Thursday 30; Friday 1 May is the last cell of that week's row.
        val mon27Apr = LocalDate(2026, 4, 27).startOfDayMillis()
        val fri1May = LocalDate(2026, 5, 1).startOfDayMillis()
        fun at(day: Long, minutes: Int, id: Long) = TimeEntryWithDetails(
            id = id, projectId = 1L, projectCode = "P-1001.1", projectName = "P-1001.1", projectColor = "#000000",
            projectProductive = true, taskId = null, taskTitle = null, description = "",
            startTime = day + 9 * hour, endTime = day + 9 * hour + minutes * minute, createdAt = 0
        )
        val entries = listOf(at(mon27Apr, 120, 1), at(fri1May, 210, 2))
        val april = DateRanges.monthRange(mon27Apr)
        val now = fri1May + 30 * 24 * hour

        // Whole weeks: Monday 30 March .. Monday 4 May.
        val range = SapExport.weekPasteRange(april)
        assertEquals(LocalDate(2026, 3, 30).startOfDayMillis(), range.start)
        assertEquals(LocalDate(2026, 5, 4).startOfDayMillis(), range.endExclusive)

        val text = SapExport.weekPaste(ReportCalculator.compute(emptyList(), entries, range, now, bookingSettings), bookingSettings)
        val rows = text.split("\r\n").filter { it.startsWith("PROJECT\t") }
        assertEquals(listOf("PROJECT\tP-1001.1\t\t2.00\t\t\t\t3.50"), rows, text)

        // The old month-only report: the 1 May cell came out empty – the "missing last cell".
        val monthOnly = SapExport.weekPaste(ReportCalculator.compute(emptyList(), entries, april, now, bookingSettings), bookingSettings)
        assertEquals(listOf("PROJECT\tP-1001.1\t\t2.00\t\t\t\t"), monthOnly.split("\r\n").filter { it.startsWith("PROJECT\t") })
    }

    @Test
    fun weekPasteRange_weekPeriodIsUnchanged() {
        val week = DateRanges.weekRange(monday)
        assertEquals(week, SapExport.weekPasteRange(week))
        assertEquals(week, SapExport.weekPasteRange(DateRanges.dayRange(monday + 2 * 24 * hour)))
    }

    @Test
    fun weekPaste_weekendWeek_lastCellIsSunday() {
        val entries = listOf(
            entry(1, 0, 9.0, 60, "P-1001.1", productive = true),
            entry(2, 6, 9.0, 45, "P-1001.1", productive = true)    // Sunday, 45 min
        )
        val report = ReportCalculator.compute(emptyList(), entries, DateRanges.weekRange(monday), now = monday + 10 * 24 * hour, settings = bookingSettings)

        assertEquals("PROJECT\tP-1001.1\t\t1.00\t\t\t\t\t\t0.75\r\n", SapExport.weekPaste(report, bookingSettings))
    }

    @Test
    fun weekPaste_flagsUnassignedTime_andSplitsMonthIntoWeeks() {
        val entries = listOf(
            entry(1, 0, 9.0, 75, null, productive = true),            // Monday, no project yet
            entry(2, 7, 9.0, 60, "P-1001.1", productive = true)   // next Monday
        )
        val range = DateRanges.monthRange(monday)
        val report = ReportCalculator.compute(emptyList(), entries, range, now = monday + 30 * 24 * hour, settings = AppSettings(sapProductiveType = "SERV", sapUnproductiveType = "UNP", sapUnproductiveNumber = "1"))
        val text = SapExport.generate(SapExportType.SAP_WEEK, report, "month", false, ExportFormat.CSV, AppSettings(sapProductiveType = "SERV"))
        assertTrue(text.endsWith("\r\n") && !Regex("(?<!\r)\n").containsMatchIn(text), "CRLF line ends only: $text")
        val lines = text.removeSuffix("\r\n").split("\r\n")
        assertTrue(lines.count { it.startsWith("# ") } >= 2, "one header per week in a month: $text")
        assertTrue(lines.any { it.startsWith("! NOT ASSIGNED YET") && it.contains("1.25") }, text)
        assertTrue(lines.any { it == "SERV\tP-1001.1\t\t1.00\t\t\t\t" }, text)
    }
}
