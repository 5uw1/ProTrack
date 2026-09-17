package com.suw1labs.worktracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Personal work schedule (single row, id = 1).
 *
 * Instead of a fixed "days per week", every weekday has its own target hours so any
 * arrangement can be modelled: 3 working days + education, 80 % with a day off,
 * 90 % with a half day, and so on. Defaults follow a 100 % position in the canton of
 * Bern: 5 × 8 h = 40 h, legal maximum 45 h per week.
 */
@Entity(tableName = "app_settings")
@Serializable
data class AppSettings(
    @PrimaryKey
    val id: Long = 1,
    /** Contract hours for a 100 % position (used to show the workload percentage). */
    val fullTimeWeeklyHours: Double = 40.0,
    /** Target hours for Monday … Sunday, comma separated (0 = day off). */
    val weekdayHours: String = DEFAULT_WEEKDAY_HOURS,
    /** Legal maximum working hours per week (ArG: 45 h for office / technical staff). */
    val maxWeeklyHours: Double = 45.0,
    /** UI language code: en, de or fr. */
    val language: String = "en",
    /**
     * Minutes of short breaks (coffee, smoke) per day the company credits as working time.
     * 0 = breaks are unpaid. Only pauses tagged as "Break" count; lunch never does.
     */
    val paidBreakMinutes: Int = 0
) {
    val paidBreakSecondsPerDay: Long get() = paidBreakMinutes.coerceAtLeast(0) * 60L

    /** Target hours per ISO weekday (index 0 = Monday). Always 7 entries. */
    val weekdayHoursList: List<Double>
        get() {
            val parsed = weekdayHours.split(',').map { it.trim().replace(',', '.').toDoubleOrNull() ?: 0.0 }
            return List(7) { parsed.getOrElse(it) { 0.0 }.coerceAtLeast(0.0) }
        }

    fun targetHoursFor(isoDayNumber: Int): Double = weekdayHoursList.getOrElse(isoDayNumber - 1) { 0.0 }
    fun targetSecondsFor(isoDayNumber: Int): Long = (targetHoursFor(isoDayNumber) * 3600).toLong()
    fun isWorkDay(isoDayNumber: Int): Boolean = targetHoursFor(isoDayNumber) > 0.0

    val workDaySet: Set<Int> get() = (1..7).filter { isWorkDay(it) }.toSet()
    val workDaysPerWeek: Int get() = workDaySet.size

    /** Hours to work per week. */
    val weeklyTargetHours: Double get() = weekdayHoursList.sum()

    /** Employment percentage derived from the weekly target. */
    val workloadPercent: Double get() = if (fullTimeWeeklyHours <= 0) 0.0 else weeklyTargetHours / fullTimeWeeklyHours * 100.0

    /** Average hours of a working day (used as the default for a full-day absence). */
    val dailyTargetHours: Double get() = if (workDaysPerWeek == 0) 0.0 else weeklyTargetHours / workDaysPerWeek

    val maxWeeklySeconds: Long get() = (maxWeeklyHours * 3600).toLong()

    companion object {
        const val DEFAULT_WEEKDAY_HOURS = "8,8,8,8,8,0,0"
        fun weekdayHoursString(hours: List<Double>): String =
            List(7) { hours.getOrElse(it) { 0.0 } }.joinToString(",") { com.suw1labs.worktracker.util.TimeFormat.sapHours(it) }
    }
}
