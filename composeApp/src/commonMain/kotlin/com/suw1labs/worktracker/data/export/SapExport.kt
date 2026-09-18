package com.suw1labs.worktracker.data.export

import com.suw1labs.worktracker.data.report.PeriodReport
import com.suw1labs.worktracker.util.DateFormats
import com.suw1labs.worktracker.util.TimeFormat
import com.suw1labs.worktracker.util.currentTimeMillis

enum class SapExportType(val label: String) {
    MONTHLY_SUMMARY("Summary per project"),
    DAILY_TIMESHEET("Daily timesheet"),
    ATTENDANCE("Attendance & overtime")
}

/** File flavour: plain CSV (comma) or Excel-friendly CSV (semicolon + UTF-8 BOM, as Excel expects in CH/DE locales). */
enum class ExportFormat(val label: String, val separator: Char, val bom: Boolean, val extension: String) {
    CSV("CSV (comma)", ',', false, "csv"),
    EXCEL_CSV("Excel (semicolon)", ';', true, "csv")
}

/**
 * Builds the files that are typed into / imported into SAP at the end of the month.
 * Pure generation – saving and sharing is delegated to [com.suw1labs.worktracker.platform.FileExporter].
 */
object SapExport {

    private class Writer(private val separator: Char, bom: Boolean) {
        private val sb = StringBuilder().apply { if (bom) append('\uFEFF') }

        fun row(vararg cells: Any?) {
            sb.append(cells.joinToString(separator.toString()) { escape(it?.toString() ?: "") }).append('\n')
        }

        fun blank() { sb.append('\n') }

        private fun escape(value: String): String {
            val special = value.contains(separator) || value.contains('"') || value.contains('\n')
            return if (special) "\"" + value.replace("\"", "\"\"") + "\"" else value
        }

        override fun toString(): String = sb.toString()
    }

    private fun isUnproductiveProject(report: PeriodReport, projectId: Long?): Boolean =
        report.days.flatMap { it.cells }.any { it.projectId == projectId && !it.isProductive }

    private fun hours(seconds: Long, roundToQuarter: Boolean): String {
        val value = if (roundToQuarter) TimeFormat.quarterHours(seconds) else TimeFormat.decimalHours(seconds)
        return TimeFormat.sapHours(value)
    }

    private fun signedHours(seconds: Long, roundToQuarter: Boolean): String {
        val text = hours(kotlin.math.abs(seconds), roundToQuarter)
        return if (seconds < 0) "-$text" else text
    }

    fun generate(
        type: SapExportType,
        report: PeriodReport,
        periodLabel: String,
        roundToQuarter: Boolean,
        format: ExportFormat
    ): String = when (type) {
        SapExportType.MONTHLY_SUMMARY -> summaryCsv(report, periodLabel, roundToQuarter, format)
        SapExportType.DAILY_TIMESHEET -> timesheetCsv(report, periodLabel, roundToQuarter, format)
        SapExportType.ATTENDANCE -> attendanceCsv(report, periodLabel, roundToQuarter, format)
    }

    /**
     * One row per SAP project number with the hours to book, followed by the unproductive
     * categories and the attendance total for cross-checking.
     */
    fun summaryCsv(report: PeriodReport, periodLabel: String, roundToQuarter: Boolean, format: ExportFormat): String {
        val w = Writer(format.separator, format.bom)
        w.row("Period:", periodLabel)
        w.row("Generated:", DateFormats.dateTime(currentTimeMillis()))
        w.row("Rounding:", if (roundToQuarter) "0.25 h" else "exact")
        w.blank()

        w.row("Project Number", "Project Name", "Hours", "Time (H:MM)")
        // Only productive projects are booked in SAP; unassigned productive time is listed so it can be booked manually.
        val projectRows = report.projects.filter { it.projectId != null && it.seconds > 0 && !isUnproductiveProject(report, it.projectId) }
        for (p in projectRows) w.row(p.code, p.name, hours(p.seconds, roundToQuarter), TimeFormat.hm(p.seconds))
        report.projects.firstOrNull { it.projectId == null }?.let { w.row("", "NOT ASSIGNED YET – book manually", hours(it.seconds, roundToQuarter), TimeFormat.hm(it.seconds)) }
        val projectTotal = projectRows.sumOf { it.seconds }
        w.row("TOTAL PROJECT HOURS", "", hours(projectTotal, roundToQuarter), TimeFormat.hm(projectTotal))
        w.blank()

        w.row("Unproductive", "Task", "Hours", "Time (H:MM)")
        for (c in report.categories.filter { !it.isProductive }) w.row("", c.name, hours(c.seconds, roundToQuarter), TimeFormat.hm(c.seconds))
        w.row("TOTAL UNPRODUCTIVE", "", hours(report.unproductiveSeconds, roundToQuarter), TimeFormat.hm(report.unproductiveSeconds))
        w.blank()

        w.row("Clocked-in time (attendance)", "", hours(report.attendanceSeconds, roundToQuarter), TimeFormat.hm(report.attendanceSeconds))
        if (report.paidBreakSeconds > 0) w.row("Paid breaks credited", "", hours(report.paidBreakSeconds, roundToQuarter), TimeFormat.hm(report.paidBreakSeconds))
        if (report.deductedBreakSeconds > 0) w.row("Missing breaks deducted", "", "-" + hours(report.deductedBreakSeconds, roundToQuarter), TimeFormat.hm(report.deductedBreakSeconds))
        w.row("Unallocated clocked-in time", "", hours(report.unallocatedSeconds, roundToQuarter), TimeFormat.hm(report.unallocatedSeconds))
        w.row("Absences credited", "", hours(report.creditedSeconds, roundToQuarter), TimeFormat.hm(report.creditedSeconds))
        w.row("Target", "", hours(report.targetSeconds, roundToQuarter), TimeFormat.hm(report.targetSeconds))
        w.row("Overtime in period", "", signedHours(report.overtimeSeconds, roundToQuarter), "")
        return w.toString()
    }

    /**
     * SAP CATS style rows: one line per day, project and category.
     */
    fun timesheetCsv(report: PeriodReport, periodLabel: String, roundToQuarter: Boolean, format: ExportFormat): String {
        val w = Writer(format.separator, format.bom)
        w.row("Period:", periodLabel)
        w.blank()
        w.row("Date", "Project Number", "Project Name", "Task", "Productive", "Hours", "Time (H:MM)", "Clocked-in (H:MM)")
        for (day in report.days) {
            if (day.attendanceSeconds == 0L && day.cells.isEmpty() && day.absence == null) continue
            val date = DateFormats.date(day.range.start)
            for (cell in day.cells) {
                w.row(
                    date,
                    if (cell.projectId == null) "" else cell.projectCode,
                    cell.projectName,
                    cell.taskName,
                    if (cell.isProductive) "Yes" else "No",
                    hours(cell.seconds, roundToQuarter),
                    TimeFormat.hm(cell.seconds),
                    TimeFormat.hm(day.attendanceSeconds)
                )
            }
            day.absence?.let { absence ->
                w.row(date, "", "ABSENCE", absence.displayLabel, "", TimeFormat.sapHours(absence.hours), "", TimeFormat.hm(day.attendanceSeconds))
            }
            if (day.cells.isEmpty() && day.absence == null) {
                w.row(date, "", "", "", "", "0", "0:00", TimeFormat.hm(day.attendanceSeconds))
            }
        }
        return w.toString()
    }

    /**
     * One line per day with attendance, breaks, absences, target and overtime.
     */
    fun attendanceCsv(report: PeriodReport, periodLabel: String, roundToQuarter: Boolean, format: ExportFormat): String {
        val w = Writer(format.separator, format.bom)
        w.row("Period:", periodLabel)
        w.blank()
        w.row("Date", "Workday", "Clocked-in (H:MM)", "Clocked-in (h)", "Breaks (H:MM)", "Absence", "Absence hours", "Target (h)", "Overtime (h)", "Warnings")
        for (day in report.days) {
            val warnings = report.warnings.filter { it.dayStart == day.range.start }.joinToString(" | ") { it.message }
            w.row(
                DateFormats.date(day.range.start),
                if (day.isWorkday) "Yes" else "No",
                TimeFormat.hm(day.attendanceSeconds),
                hours(day.attendanceSeconds, roundToQuarter),
                TimeFormat.hm(day.breakSeconds),
                day.absence?.displayLabel ?: "",
                day.absence?.let { TimeFormat.sapHours(it.hours) } ?: "",
                hours(day.targetSeconds, roundToQuarter),
                signedHours(day.overtimeSeconds, roundToQuarter),
                warnings
            )
        }
        w.blank()
        w.row("TOTAL", "", TimeFormat.hm(report.attendanceSeconds), hours(report.attendanceSeconds, roundToQuarter), TimeFormat.hm(report.breakSeconds), "", TimeFormat.sapHours(report.creditedSeconds / 3600.0), hours(report.targetSeconds, roundToQuarter), signedHours(report.overtimeSeconds, roundToQuarter), "")
        return w.toString()
    }
}
