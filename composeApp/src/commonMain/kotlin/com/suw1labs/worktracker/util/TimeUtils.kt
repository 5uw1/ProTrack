package com.suw1labs.worktracker.util

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/** Epoch milliseconds "now", usable from every platform. */
@OptIn(ExperimentalTime::class)
fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()

@OptIn(ExperimentalTime::class)
fun Long.toLocalDateTime(zone: TimeZone = TimeZone.currentSystemDefault()): LocalDateTime =
    Instant.fromEpochMilliseconds(this).toLocalDateTime(zone)

fun Long.toLocalDate(zone: TimeZone = TimeZone.currentSystemDefault()): LocalDate = toLocalDateTime(zone).date

fun LocalDateTime.toEpochMillis(zone: TimeZone = TimeZone.currentSystemDefault()): Long =
    toInstant(zone).toEpochMilliseconds()

fun LocalDate.startOfDayMillis(zone: TimeZone = TimeZone.currentSystemDefault()): Long =
    atStartOfDayIn(zone).toEpochMilliseconds()

/** Localised names used by [DateFormats]; the UI sets them when the language changes. */
data class DateNames(
    val monthsShort: List<String> = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
    val monthsLong: List<String> = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    ),
    val weekdaysShort: List<String> = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
    val weekdaysLong: List<String> = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
)

private val MONTH_ABBREVIATIONS get() = DateFormats.names.monthsShort
private val MONTH_NAMES get() = DateFormats.names.monthsLong
private val WEEKDAY_ABBREVIATIONS get() = DateFormats.names.weekdaysShort

private fun Int.pad2(): String = toString().padStart(2, '0')

/**
 * Multiplatform replacements for the `SimpleDateFormat` patterns used throughout the app.
 */
object DateFormats {
    /** Month / weekday names for the current UI language (English by default). */
    var names: DateNames = DateNames()

    /** `yyyy-MM-dd` */
    fun date(millis: Long): String {
        val dt = millis.toLocalDateTime()
        return "${dt.year}-${dt.month.number.pad2()}-${dt.day.pad2()}"
    }

    /** `HH:mm:ss` */
    fun time(millis: Long): String {
        val dt = millis.toLocalDateTime()
        return "${dt.hour.pad2()}:${dt.minute.pad2()}:${dt.second.pad2()}"
    }

    /** `HH:mm` */
    fun hourMinute(millis: Long): String {
        val dt = millis.toLocalDateTime()
        return "${dt.hour.pad2()}:${dt.minute.pad2()}"
    }

    /** `yyyy-MM-dd HH:mm` */
    fun dateTime(millis: Long): String = "${date(millis)} ${hourMinute(millis)}"

    /** `yyyyMMdd_HHmm` – used for export file names. */
    fun fileStamp(millis: Long): String {
        val dt = millis.toLocalDateTime()
        return "${dt.year}${dt.month.number.pad2()}${dt.day.pad2()}_${dt.hour.pad2()}${dt.minute.pad2()}"
    }

    /** `yyyy-MM` */
    fun yearMonth(millis: Long): String {
        val dt = millis.toLocalDateTime()
        return "${dt.year}-${dt.month.number.pad2()}"
    }

    /** `EEE d` e.g. "Mon 14" */
    fun weekdayDay(millis: Long): String {
        val dt = millis.toLocalDateTime()
        return "${WEEKDAY_ABBREVIATIONS[dt.dayOfWeek.isoDayNumber - 1]} ${dt.day}"
    }

    /** `MMM d` e.g. "Sep 14" */
    fun monthDay(millis: Long): String {
        val dt = millis.toLocalDateTime()
        return "${MONTH_ABBREVIATIONS[dt.month.number - 1]} ${dt.day}"
    }

    /** `MMM d, HH:mm` e.g. "Sep 14, 09:30" */
    fun monthDayTime(millis: Long): String = "${monthDay(millis)}, ${hourMinute(millis)}"

    /** `EEE, MMM d, yyyy` e.g. "Mon, Sep 14, 2026" */
    fun fullDate(millis: Long): String {
        val dt = millis.toLocalDateTime()
        return "${WEEKDAY_ABBREVIATIONS[dt.dayOfWeek.isoDayNumber - 1]}, ${monthDay(millis)}, ${dt.year}"
    }

    /** `EEEE, d MMM yyyy` e.g. "Tuesday, 16 Sep 2026" – used as the Today title. */
    fun weekdayLongDate(millis: Long): String {
        val dt = millis.toLocalDateTime()
        return "${names.weekdaysLong[dt.dayOfWeek.isoDayNumber - 1]}, ${dt.day} ${MONTH_ABBREVIATIONS[dt.month.number - 1]} ${dt.year}"
    }

    /** `MMMM yyyy` e.g. "September 2026" */
    fun monthYear(millis: Long): String {
        val dt = millis.toLocalDateTime()
        return "${MONTH_NAMES[dt.month.number - 1]} ${dt.year}"
    }
}

/** Half-open time range `[start, endExclusive)` in epoch millis. */
data class DateRange(val start: Long, val endExclusive: Long) {
    fun contains(millis: Long) = millis >= start && millis < endExclusive
}

enum class ReportPeriodType(val label: String) {
    DAY("Day"),
    WEEK("Week"),
    MONTH("Month")
}

/**
 * Calendar range helpers (local time zone) replacing the `java.util.Calendar` arithmetic.
 */
object DateRanges {
    fun dayRange(anchor: Long): DateRange {
        val day = anchor.toLocalDate()
        return DateRange(day.startOfDayMillis(), day.plus(1, DateTimeUnit.DAY).startOfDayMillis())
    }

    /** Monday 00:00 .. next Monday 00:00 of the week containing [anchor]. */
    fun weekRange(anchor: Long): DateRange {
        val day = anchor.toLocalDate()
        val monday = day.minus(day.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
        return DateRange(monday.startOfDayMillis(), monday.plus(1, DateTimeUnit.WEEK).startOfDayMillis())
    }

    fun monthRange(anchor: Long): DateRange {
        val day = anchor.toLocalDate()
        val first = LocalDate(day.year, day.month.number, 1)
        return DateRange(first.startOfDayMillis(), first.plus(1, DateTimeUnit.MONTH).startOfDayMillis())
    }

    fun rangeFor(type: ReportPeriodType, anchor: Long): DateRange = when (type) {
        ReportPeriodType.DAY -> dayRange(anchor)
        ReportPeriodType.WEEK -> weekRange(anchor)
        ReportPeriodType.MONTH -> monthRange(anchor)
    }

    /** Moves [anchor] by [delta] periods of [type] (negative = back in time). */
    fun shiftAnchor(type: ReportPeriodType, anchor: Long, delta: Int): Long {
        val day = anchor.toLocalDate()
        val shifted = when (type) {
            ReportPeriodType.DAY -> day.plus(delta, DateTimeUnit.DAY)
            ReportPeriodType.WEEK -> day.plus(delta, DateTimeUnit.WEEK)
            ReportPeriodType.MONTH -> day.plus(delta, DateTimeUnit.MONTH)
        }
        return shifted.startOfDayMillis()
    }

    /** Human readable label for the period containing [anchor]. */
    fun label(type: ReportPeriodType, anchor: Long): String = when (type) {
        ReportPeriodType.DAY -> DateFormats.fullDate(anchor)
        ReportPeriodType.WEEK -> {
            val range = weekRange(anchor)
            val lastDay = range.endExclusive - 1
            "${DateFormats.monthDay(range.start)} – ${DateFormats.monthDay(lastDay)}, ${lastDay.toLocalDate().year}"
        }
        ReportPeriodType.MONTH -> DateFormats.monthYear(anchor)
    }

    /** Every calendar day inside [range], as day ranges. */
    fun daysIn(range: DateRange): List<DateRange> {
        val result = mutableListOf<DateRange>()
        var day = range.start.toLocalDate()
        val end = range.endExclusive
        while (day.startOfDayMillis() < end) {
            val next = day.plus(1, DateTimeUnit.DAY)
            result.add(DateRange(day.startOfDayMillis(), next.startOfDayMillis()))
            day = next
        }
        return result
    }

    fun startOfToday(now: Long = currentTimeMillis()): Long = dayRange(now).start
}
