package com.suw1labs.worktracker.data.report

import com.suw1labs.worktracker.data.model.AttendanceSession
import com.suw1labs.worktracker.data.model.TimeEntryWithDetails

/**
 * Clocked-in time that no activity covers. [end] is null while the gap is still growing
 * (clocked in, nothing running). Such time is shown on the timeline so it can be assigned later.
 */
data class UnassignedGap(val sessionId: Long, val start: Long, val end: Long?) {
    fun durationSeconds(now: Long): Long = (((end ?: now) - start) / 1000L).coerceAtLeast(0L)
}

object DayGaps {
    /** Gaps shorter than this are noise from switching activities and are not shown. */
    const val MIN_GAP_MILLIS = 60_000L

    /**
     * Uncovered stretches inside each clock-in period of [sessions]. Running activities and open
     * periods extend to [now]; a gap that reaches the end of an open period stays open (end = null).
     */
    fun compute(
        entries: List<TimeEntryWithDetails>,
        sessions: List<AttendanceSession>,
        now: Long,
        minMillis: Long = MIN_GAP_MILLIS
    ): List<UnassignedGap> {
        val covered = entries
            .map { it.startTime to (it.endTime ?: now) }
            .filter { (s, e) -> e > s }
            .sortedBy { it.first }
        val gaps = mutableListOf<UnassignedGap>()
        for (session in sessions) {
            val sessionEnd = session.clockOut ?: now
            if (sessionEnd <= session.clockIn) continue
            var cursor = session.clockIn
            for ((s, e) in covered) {
                if (e <= cursor) continue
                if (s >= sessionEnd) break
                if (s - cursor >= minMillis) gaps += UnassignedGap(session.id, cursor, s)
                cursor = maxOf(cursor, e)
                if (cursor >= sessionEnd) break
            }
            if (cursor < sessionEnd && sessionEnd - cursor >= minMillis) {
                gaps += UnassignedGap(session.id, cursor, if (session.clockOut == null) null else sessionEnd)
            }
        }
        return gaps
    }
}

/** Which neighbouring activities an edited entry touched, and whether to move them along with it. */
data class NeighbourAdjustment(val previous: Boolean = false, val next: Boolean = false) {
    companion object {
        val NONE = NeighbourAdjustment()
    }
}

object EntryNeighbours {
    /** Two activities are contiguous when the second starts within this of the first ending. */
    const val CONTIGUOUS_MILLIS = 60_000L

    /** The activity that ended right where [entry] starts, if any. */
    fun previousOf(entry: TimeEntryWithDetails, entries: List<TimeEntryWithDetails>): TimeEntryWithDetails? =
        entries.filter { it.id != entry.id && it.endTime != null && it.endTime <= entry.startTime && entry.startTime - it.endTime <= CONTIGUOUS_MILLIS }
            .maxByOrNull { it.endTime!! }

    /** The activity that started right where [entry] ended, if any (none while [entry] is running). */
    fun nextOf(entry: TimeEntryWithDetails, entries: List<TimeEntryWithDetails>): TimeEntryWithDetails? {
        val end = entry.endTime ?: return null
        return entries.filter { it.id != entry.id && it.startTime >= end && it.startTime - end <= CONTIGUOUS_MILLIS }
            .minByOrNull { it.startTime }
    }

    /**
     * The neighbours as they should be saved next to the edited [entry]: the previous one ends at the
     * new start and the next one starts at the new end, for the neighbours the user chose to move.
     * A neighbour that would end up empty is left untouched.
     */
    fun adjusted(
        previous: TimeEntryWithDetails?,
        next: TimeEntryWithDetails?,
        newStart: Long,
        newEnd: Long?,
        choice: NeighbourAdjustment
    ): List<TimeEntryWithDetails> {
        val out = mutableListOf<TimeEntryWithDetails>()
        if (choice.previous && previous != null && newStart > previous.startTime && previous.endTime != newStart) {
            out += previous.copy(endTime = newStart)
        }
        if (choice.next && next != null && newEnd != null && (next.endTime == null || newEnd < next.endTime) && next.startTime != newEnd) {
            out += next.copy(startTime = newEnd)
        }
        return out
    }
}
