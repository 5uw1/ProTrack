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

        assertEquals(
            listOf(
                "INTERNAL\t\t1000\t0.50\t0.50\t\t\t",
                "PROJECT\tP-1001.1\t\t3.00\t5.00\t\t\t",
                "PROJECT\tP-1002.1\t\t\t1.50\t\t\t",
                "PROJECT\tP-1003.1\t\t3.95\t0.98\t\t\t"
            ),
            text.trimEnd('\n').lines()
        )
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
        val lines = text.trimEnd('\n').lines()
        assertTrue(lines.count { it.startsWith("# ") } >= 2, "one header per week in a month: $text")
        assertTrue(lines.any { it.startsWith("! NOT ASSIGNED YET") && it.contains("1.25") }, text)
        assertTrue(lines.any { it == "SERV\tP-1001.1\t\t1.00\t\t\t\t" }, text)
    }
}
