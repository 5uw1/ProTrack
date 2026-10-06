package com.suw1labs.worktracker.ui.timesheet

import com.suw1labs.worktracker.data.model.TimeEntry
import com.suw1labs.worktracker.data.model.TimeEntryWithDetails
import com.suw1labs.worktracker.data.report.EntryNeighbours
import com.suw1labs.worktracker.data.report.NeighbourAdjustment
import com.suw1labs.worktracker.util.startOfDayMillis
import com.suw1labs.worktracker.util.toEpochMillis
import com.suw1labs.worktracker.util.toLocalDate
import com.suw1labs.worktracker.util.toLocalDateTime
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus

/**
 * Times of day as typed into the timesheet: minutes after midnight, 0..1440 (24:00 is the end of
 * the day, so an activity can run until midnight).
 */
object ClockTime {
    const val DAY_MINUTES = 24 * 60

    /**
     * Reads what people type for a time: "8", "08", "830", "0830", "8:30", "8.30", "8h30", "8 30",
     * "24:00". Anything else, or an hour or minute out of range, is null.
     */
    fun parse(text: String): Int? {
        val clean = text.trim().lowercase()
        if (clean.isEmpty()) return null
        val parts = clean.split(':', '.', 'h', ' ').filter { it.isNotEmpty() }
        val (hours, minutes) = when {
            parts.size == 2 -> parts[0] to parts[1]
            parts.size == 1 && clean.last() == 'h' -> parts[0] to "0"
            parts.size == 1 && clean.any { it in ":. h" } -> return null
            parts.size == 1 -> {
                val digits = parts[0]
                when (digits.length) {
                    1, 2 -> digits to "0"
                    3 -> digits.substring(0, 1) to digits.substring(1)
                    4 -> digits.substring(0, 2) to digits.substring(2)
                    else -> return null
                }
            }
            else -> return null
        }
        if (hours.length > 2 || minutes.length > 2 || !hours.all { it.isDigit() } || !minutes.all { it.isDigit() }) return null
        val h = hours.toInt()
        val m = minutes.toInt()
        if (h > 24 || m > 59 || (h == 24 && m > 0)) return null
        return h * 60 + m
    }

    /** "08:30"; 1440 is "24:00". */
    fun format(minutes: Int): String = "${(minutes / 60).toString().padStart(2, '0')}:${(minutes % 60).toString().padStart(2, '0')}"

    /** The instant [minutes] after midnight on the day starting at [dayStart], in local time (DST-safe). */
    fun at(dayStart: Long, minutes: Int, zone: TimeZone = TimeZone.currentSystemDefault()): Long {
        val day = dayStart.toLocalDate(zone)
        if (minutes >= DAY_MINUTES) return day.plus(1, DateTimeUnit.DAY).startOfDayMillis(zone)
        return LocalDateTime(day, LocalTime(minutes / 60, minutes % 60)).toEpochMillis(zone)
    }

    /** Local minutes after midnight of [millis] on the day starting at [dayStart]; the next midnight is 1440. */
    fun minutesOf(millis: Long, dayStart: Long, zone: TimeZone = TimeZone.currentSystemDefault()): Int {
        if (millis <= dayStart) return 0
        val nextDay = dayStart.toLocalDate(zone).plus(1, DateTimeUnit.DAY).startOfDayMillis(zone)
        if (millis >= nextDay) return DAY_MINUTES
        val dt = millis.toLocalDateTime(zone)
        return dt.hour * 60 + dt.minute
    }

    /** The same local time of day [days] later (negative: earlier). */
    fun shiftDays(millis: Long, days: Int, zone: TimeZone = TimeZone.currentSystemDefault()): Long {
        val dt = millis.toLocalDateTime(zone)
        val date = dt.date.plus(days, DateTimeUnit.DAY)
        return LocalDateTime(date, dt.time).toEpochMillis(zone)
    }

    /** [minutes] rounded to the nearest multiple of [step]. */
    fun snap(minutes: Int, step: Int): Int = ((minutes + step / 2) / step * step).coerceIn(0, DAY_MINUTES)

    /**
     * Where a dragged edge lands: on the nearest of [edges] (where the activities around it start
     * and end, clock-in and clock-out) when one is within [threshold] minutes, so activities meet
     * without a gap or overlap; otherwise on the [step] grid.
     */
    fun magnet(minutes: Int, edges: Collection<Int>, threshold: Int, step: Int): Int =
        nearestEdge(minutes, edges, threshold) ?: snap(minutes, step)

    /** The edge closest to [minutes], if one lies within [threshold]. */
    fun nearestEdge(minutes: Int, edges: Collection<Int>, threshold: Int): Int? =
        edges.filter { kotlin.math.abs(it - minutes) <= threshold }.minByOrNull { kotlin.math.abs(it - minutes) }

    /**
     * Start of a block of [length] minutes moved to [start]: whichever of its two edges is closer to
     * one of [edges] (within [threshold]) is pulled onto it; otherwise the start goes on the [step]
     * grid. The block stays inside the day.
     */
    fun magnetMove(start: Int, length: Int, edges: Collection<Int>, threshold: Int, step: Int): Int {
        val byStart = nearestEdge(start, edges, threshold)
        val byEnd = nearestEdge(start + length, edges, threshold)?.minus(length)
        val snapped = when {
            byStart != null && byEnd != null -> if (kotlin.math.abs(byStart - start) <= kotlin.math.abs(byEnd - start)) byStart else byEnd
            byStart != null -> byStart
            byEnd != null -> byEnd
            else -> snap(start, step)
        }
        return snapped.coerceIn(0, DAY_MINUTES - length)
    }
}

/** Column of an activity drawn side by side with the ones it overlaps: [index] of [count]. */
data class Lane(val index: Int, val count: Int)

object TimesheetLayout {
    /**
     * Lanes for overlapping activities, as calendars draw them: activities that overlap each other
     * (directly or through a chain) share the width of the column, each in the first free lane.
     * [ranges] maps an id to its start and end.
     */
    fun lanes(ranges: Map<Long, Pair<Long, Long>>): Map<Long, Lane> {
        val sorted = ranges.entries.sortedWith(compareBy({ it.value.first }, { it.value.second }))
        val result = HashMap<Long, Lane>()
        var cluster = mutableListOf<Pair<Long, Int>>()
        var laneEnds = mutableListOf<Long>()
        var clusterEnd = Long.MIN_VALUE

        fun closeCluster() {
            cluster.forEach { (id, lane) -> result[id] = Lane(lane, laneEnds.size) }
            cluster = mutableListOf()
            laneEnds = mutableListOf()
        }

        for ((id, range) in sorted) {
            val (start, end) = range
            if (start >= clusterEnd) closeCluster()
            val free = laneEnds.indexOfFirst { it <= start }
            val lane = if (free >= 0) free else laneEnds.size.also { laneEnds.add(0L) }
            laneEnds[lane] = end
            cluster.add(id to lane)
            clusterEnd = maxOf(clusterEnd, end)
        }
        closeCluster()
        return result
    }
}

object TimesheetEdits {
    /** Keyboard nudges and the drag grid. */
    const val NUDGE_MINUTES = 15
    const val DRAG_STEP_MINUTES = 5

    /**
     * [entry] with a new start and end, ready to save, plus the activities right next to it that
     * had to move along. As in the edit dialog, a neighbour that ended (or started) where the entry
     * did is shortened when the entry now runs into it, and left alone when a gap opens.
     *
     * Null when the times are not a valid activity (end not after start). A running entry keeps
     * running: its [newEnd] is ignored.
     */
    fun retimed(entry: TimeEntryWithDetails, newStart: Long, newEnd: Long?, others: List<TimeEntryWithDetails>): List<TimeEntry>? {
        val end = if (entry.isRunning) null else newEnd
        if (end != null && end <= newStart) return null
        if (newStart == entry.startTime && end == entry.endTime) return emptyList()
        val previous = EntryNeighbours.previousOf(entry, others)
        val next = EntryNeighbours.nextOf(entry, others)
        val choice = NeighbourAdjustment(
            previous = previous?.endTime != null && newStart < previous.endTime,
            next = next != null && end != null && end > next.startTime
        )
        val moved = EntryNeighbours.adjusted(previous, next, newStart, end, choice)
        return listOf(entry.copy(startTime = newStart, endTime = end).toEntity()) + moved.map { it.toEntity() }
    }

    /** [entry] moved as a whole to start at [newStart], its length unchanged. Running entries stay put. */
    fun moved(entry: TimeEntryWithDetails, newStart: Long, others: List<TimeEntryWithDetails>): List<TimeEntry>? {
        if (entry.isRunning) return null
        val length = entry.endTime!! - entry.startTime
        // Moving to another day leaves the old neighbours behind; only same-day moves push them.
        if (newStart.toLocalDate() != entry.startTime.toLocalDate()) {
            return listOf(entry.copy(startTime = newStart, endTime = newStart + length).toEntity())
        }
        return retimed(entry, newStart, newStart + length, others)
    }
}
