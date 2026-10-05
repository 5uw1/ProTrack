package com.suw1labs.worktracker.data.report

import com.suw1labs.worktracker.data.model.AttendanceSession

/**
 * Only clocked-in time counts, so an activity edited to start before its clock-in (someone clocked
 * in late and corrects the start) must take the clock-in along – otherwise those minutes are lost
 * and the clock-in shows up after the activity. The same holds for an end after the clock-out.
 */
object AttendanceCover {
    /**
     * The clock-in periods to save so that [start]..[end] (end null: running) is clocked-in time.
     * Only periods the activity overlaps or touches are stretched: the first one to an earlier
     * [start] (unless another period already covers it), the last one to a later [end]. An activity
     * outside any period changes nothing – a day without clocking in gets no attendance invented.
     */
    fun extended(sessions: List<AttendanceSession>, start: Long, end: Long?): List<AttendanceSession> {
        val until = end ?: Long.MAX_VALUE
        val touched = sessions.filter { it.clockIn <= until && (it.clockOut ?: Long.MAX_VALUE) >= start }.sortedBy { it.clockIn }
        if (touched.isEmpty()) return emptyList()
        val covered = { t: Long -> sessions.any { it.clockIn <= t && (it.clockOut ?: Long.MAX_VALUE) >= t } }

        val changed = LinkedHashMap<Long, AttendanceSession>()
        val first = touched.first()
        if (start < first.clockIn && !covered(start)) changed[first.id] = first.copy(clockIn = start)
        val last = touched.last()
        val lastOut = last.clockOut
        if (end != null && lastOut != null && end > lastOut && !covered(end)) {
            changed[last.id] = (changed[last.id] ?: last).copy(clockOut = end)
        }
        return changed.values.toList()
    }
}
