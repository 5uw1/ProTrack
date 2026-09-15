package com.suw1labs.worktracker.data.report

import com.suw1labs.worktracker.data.model.AppSettings
import com.suw1labs.worktracker.data.model.AttendanceSession
import com.suw1labs.worktracker.data.model.DayRecord
import com.suw1labs.worktracker.data.model.TimeEntryWithDetails
import com.suw1labs.worktracker.util.DateRange
import com.suw1labs.worktracker.util.DateRanges
import com.suw1labs.worktracker.util.TimeFormat
import com.suw1labs.worktracker.util.toLocalDate
import kotlin.math.max
import kotlin.math.min
import kotlinx.datetime.isoDayNumber

const val NO_PROJECT_CODE = "—"
const val NO_PROJECT_NAME = "No project (internal / unproductive)"
const val UNCATEGORIZED_NAME = "Uncategorized"

/**
 * Break rules applied per working day (company rule based on Swiss labour law, canton of Bern):
 * more than 5 h of work requires at least 30 min break, more than 9 h requires 1 h.
 */
object WorkRules {
    const val SHORT_WORK_SECONDS = 5 * 3600L
    const val SHORT_BREAK_SECONDS = 30 * 60L
    const val LONG_WORK_SECONDS = 9 * 3600L
    const val LONG_BREAK_SECONDS = 60 * 60L

    /** Required break for [workedSeconds] of work, 0 if none is required. */
    fun requiredBreakSeconds(workedSeconds: Long): Long = when {
        workedSeconds > LONG_WORK_SECONDS -> LONG_BREAK_SECONDS
        workedSeconds > SHORT_WORK_SECONDS -> SHORT_BREAK_SECONDS
        else -> 0L
    }
}

/** Hours booked on one SAP project inside a period. */
data class ProjectHours(
    val projectId: Long?,
    val code: String,
    val name: String,
    val colorHex: String,
    val seconds: Long,
    /** Seconds per category name. */
    val categorySeconds: Map<String, Long>
)

data class CategoryHours(
    val name: String,
    val isProductive: Boolean,
    val seconds: Long
)

/** One (day, project, category) cell of the timesheet. */
data class DayCell(
    val projectId: Long?,
    val projectCode: String,
    val projectName: String,
    val categoryName: String,
    val isProductive: Boolean,
    val seconds: Long
)

enum class WarningKind { BREAK_TOO_SHORT, WEEK_OVER_LEGAL_MAX, STILL_CLOCKED_IN_PAST_DAY }

data class ComplianceWarning(
    val kind: WarningKind,
    val dayStart: Long,
    /** Worked seconds (day or week depending on [kind]). */
    val workedSeconds: Long = 0,
    val breakSeconds: Long = 0,
    val requiredBreakSeconds: Long = 0,
    val maxWeeklyHours: Double = 0.0
) {
    /** English text used in exports; the UI renders a translated version. */
    val message: String
        get() = when (kind) {
            WarningKind.BREAK_TOO_SHORT ->
                "Worked ${TimeFormat.hoursMinutes(workedSeconds)} with only ${TimeFormat.hoursMinutes(breakSeconds)} break – at least ${TimeFormat.hoursMinutes(requiredBreakSeconds)} required."
            WarningKind.WEEK_OVER_LEGAL_MAX ->
                "Week of ${com.suw1labs.worktracker.util.DateFormats.monthDay(dayStart)}: ${TimeFormat.hoursMinutes(workedSeconds)} worked – legal maximum is ${TimeFormat.sapHours(maxWeeklyHours)} h."
            WarningKind.STILL_CLOCKED_IN_PAST_DAY ->
                "Forgot to clock out on ${com.suw1labs.worktracker.util.DateFormats.monthDay(dayStart)} – fix the clock-in period."
        }
}

data class DayRow(
    val range: DateRange,
    val isWorkday: Boolean,
    val targetSeconds: Long,
    val attendanceSeconds: Long,
    /** Sum of the gaps between clock-in periods on this day (lunch, coffee, ...). */
    val breakSeconds: Long,
    val absence: DayRecord?,
    val cells: List<DayCell>
) {
    val productiveSeconds: Long get() = cells.filter { it.isProductive }.sumOf { it.seconds }
    val unproductiveSeconds: Long get() = cells.filter { !it.isProductive }.sumOf { it.seconds }
    val allocatedSeconds: Long get() = productiveSeconds + unproductiveSeconds
    val unallocatedSeconds: Long get() = max(0L, attendanceSeconds - allocatedSeconds)
    val creditedSeconds: Long get() = absence?.creditedSeconds ?: 0L

    /** Worked + credited absence − target. Positive = overtime. */
    val overtimeSeconds: Long get() = attendanceSeconds + creditedSeconds - targetSeconds
    val requiredBreakSeconds: Long get() = WorkRules.requiredBreakSeconds(attendanceSeconds)
    val breakRuleViolated: Boolean get() = requiredBreakSeconds > 0 && breakSeconds < requiredBreakSeconds
}

data class WeekSummary(
    val range: DateRange,
    val attendanceSeconds: Long,
    val maxWeeklySeconds: Long
) {
    val overLegalMax: Boolean get() = attendanceSeconds > maxWeeklySeconds
}

data class PeriodReport(
    val range: DateRange,
    val attendanceSeconds: Long,
    val productiveSeconds: Long,
    val unproductiveSeconds: Long,
    val targetSeconds: Long,
    val creditedSeconds: Long,
    val breakSeconds: Long,
    val projects: List<ProjectHours>,
    val categories: List<CategoryHours>,
    val days: List<DayRow>,
    val weeks: List<WeekSummary>,
    val warnings: List<ComplianceWarning>
) {
    val allocatedSeconds: Long get() = productiveSeconds + unproductiveSeconds
    val unallocatedSeconds: Long get() = max(0L, attendanceSeconds - allocatedSeconds)

    /** Worked + credited absences − target for the period. Positive = overtime. */
    val overtimeSeconds: Long get() = attendanceSeconds + creditedSeconds - targetSeconds
    val absences: List<DayRecord> get() = days.mapNotNull { it.absence }

    companion object {
        fun empty(range: DateRange) = PeriodReport(range, 0, 0, 0, 0, 0, 0, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
    }
}

/**
 * Pure, platform-independent aggregation of attendance sessions, activity entries and absences.
 * Everything is clipped to the requested range, and running sessions/entries are
 * evaluated up to [now]. Days after [now] get no target (future days are not "missing").
 */
object ReportCalculator {

    fun overlapSeconds(aStart: Long, aEnd: Long, bStart: Long, bEnd: Long): Long {
        val overlap = min(aEnd, bEnd) - max(aStart, bStart)
        return if (overlap <= 0) 0L else overlap / 1000L
    }

    fun attendanceSeconds(sessions: List<AttendanceSession>, range: DateRange, now: Long): Long =
        sessions.sumOf { overlapSeconds(it.clockIn, it.clockOut ?: now, range.start, range.endExclusive) }

    /** Gaps between consecutive clock-in periods that touch [range]. */
    fun breakSeconds(sessions: List<AttendanceSession>, range: DateRange, now: Long): Long {
        val daySessions = sessions
            .filter { overlapSeconds(it.clockIn, it.clockOut ?: now, range.start, range.endExclusive) > 0 }
            .sortedBy { it.clockIn }
        var total = 0L
        for (i in 1 until daySessions.size) {
            val previousEnd = daySessions[i - 1].clockOut ?: now
            val gap = daySessions[i].clockIn - previousEnd
            if (gap > 0) total += gap / 1000L
        }
        return total
    }

    fun isWorkday(dayStart: Long, settings: AppSettings): Boolean =
        settings.isWorkDay(dayStart.toLocalDate().dayOfWeek.isoDayNumber)

    fun compute(
        sessions: List<AttendanceSession>,
        entries: List<TimeEntryWithDetails>,
        range: DateRange,
        now: Long,
        settings: AppSettings = AppSettings(),
        dayRecords: List<DayRecord> = emptyList()
    ): PeriodReport {
        val recordsByDay = dayRecords.associateBy { it.dayStart }
        val todayStart = DateRanges.dayRange(now).start

        val days = DateRanges.daysIn(range).map { dayRange ->
            val cellMap = LinkedHashMap<Pair<Long?, String>, DayCell>()
            entries.forEach { entry ->
                val secs = overlapSeconds(entry.startTime, entry.endTime ?: now, dayRange.start, dayRange.endExclusive)
                if (secs <= 0) return@forEach
                val categoryName = entry.categoryName ?: UNCATEGORIZED_NAME
                val productive = entry.categoryProductive ?: false
                val key = entry.projectId to categoryName
                val existing = cellMap[key]
                cellMap[key] = DayCell(
                    projectId = entry.projectId,
                    projectCode = entry.projectCode ?: NO_PROJECT_CODE,
                    projectName = entry.projectName ?: NO_PROJECT_NAME,
                    categoryName = categoryName,
                    isProductive = productive,
                    seconds = (existing?.seconds ?: 0L) + secs
                )
            }
            val isoDay = dayRange.start.toLocalDate().dayOfWeek.isoDayNumber
            val workday = settings.isWorkDay(isoDay)
            val inPast = dayRange.start <= todayStart
            DayRow(
                range = dayRange,
                isWorkday = workday,
                targetSeconds = if (workday && inPast) settings.targetSecondsFor(isoDay) else 0L,
                attendanceSeconds = attendanceSeconds(sessions, dayRange, now),
                breakSeconds = breakSeconds(sessions, dayRange, now),
                absence = recordsByDay[dayRange.start],
                cells = cellMap.values.sortedWith(compareBy({ it.projectId == null }, { it.projectCode }, { it.categoryName }))
            )
        }

        val allCells = days.flatMap { it.cells }

        val projects = allCells.groupBy { it.projectId }.map { (projectId, cells) ->
            val first = cells.first()
            ProjectHours(
                projectId = projectId,
                code = first.projectCode,
                name = first.projectName,
                colorHex = entries.firstOrNull { it.projectId == projectId }?.projectColor ?: "#64748B",
                seconds = cells.sumOf { it.seconds },
                categorySeconds = cells.groupBy { it.categoryName }.mapValues { (_, c) -> c.sumOf { it.seconds } }
            )
        }.sortedWith(compareBy<ProjectHours> { it.projectId == null }.thenByDescending { it.seconds })

        val categories = allCells.groupBy { it.categoryName }.map { (name, cells) ->
            CategoryHours(name = name, isProductive = cells.first().isProductive, seconds = cells.sumOf { it.seconds })
        }.sortedWith(compareByDescending<CategoryHours> { it.isProductive }.thenByDescending { it.seconds })

        // Weekly totals for the legal maximum check (weeks are evaluated on all their days, not only
        // those inside the range, so a month report sees the full week).
        val weeks = days.map { DateRanges.weekRange(it.range.start) }.distinct().map { weekRange ->
            WeekSummary(
                range = weekRange,
                attendanceSeconds = attendanceSeconds(sessions, weekRange, now),
                maxWeeklySeconds = settings.maxWeeklySeconds
            )
        }

        val warnings = mutableListOf<ComplianceWarning>()
        days.filter { it.breakRuleViolated }.forEach { day ->
            warnings.add(
                ComplianceWarning(
                    kind = WarningKind.BREAK_TOO_SHORT,
                    dayStart = day.range.start,
                    workedSeconds = day.attendanceSeconds,
                    breakSeconds = day.breakSeconds,
                    requiredBreakSeconds = day.requiredBreakSeconds
                )
            )
        }
        weeks.filter { it.overLegalMax }.forEach { week ->
            warnings.add(
                ComplianceWarning(
                    kind = WarningKind.WEEK_OVER_LEGAL_MAX,
                    dayStart = week.range.start,
                    workedSeconds = week.attendanceSeconds,
                    maxWeeklyHours = settings.maxWeeklyHours
                )
            )
        }
        sessions.filter { it.clockOut == null && it.clockIn < todayStart }.forEach { session ->
            warnings.add(
                ComplianceWarning(
                    kind = WarningKind.STILL_CLOCKED_IN_PAST_DAY,
                    dayStart = DateRanges.dayRange(session.clockIn).start
                )
            )
        }

        return PeriodReport(
            range = range,
            attendanceSeconds = days.sumOf { it.attendanceSeconds },
            productiveSeconds = days.sumOf { it.productiveSeconds },
            unproductiveSeconds = days.sumOf { it.unproductiveSeconds },
            targetSeconds = days.sumOf { it.targetSeconds },
            creditedSeconds = days.sumOf { it.creditedSeconds },
            breakSeconds = days.sumOf { it.breakSeconds },
            projects = projects,
            categories = categories,
            days = days,
            weeks = weeks,
            warnings = warnings.sortedBy { it.dayStart }
        )
    }

    /**
     * Overtime balance from the first recorded day up to and including today.
     * Days before the first record are ignored so starting to use the app mid-year does not
     * create fake minus hours.
     */
    fun overtimeBalanceToDate(
        sessions: List<AttendanceSession>,
        dayRecords: List<DayRecord>,
        now: Long,
        settings: AppSettings
    ): Long {
        val firstSession = sessions.minOfOrNull { it.clockIn }
        val firstRecord = dayRecords.minOfOrNull { it.dayStart }
        val first = listOfNotNull(firstSession, firstRecord).minOrNull() ?: return 0L
        val range = DateRange(DateRanges.dayRange(first).start, DateRanges.dayRange(now).endExclusive)
        return compute(sessions, emptyList(), range, now, settings, dayRecords).overtimeSeconds
    }
}
