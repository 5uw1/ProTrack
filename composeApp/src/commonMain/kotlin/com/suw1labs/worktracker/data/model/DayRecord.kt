package com.suw1labs.worktracker.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import com.suw1labs.worktracker.util.currentTimeMillis
import com.suw1labs.worktracker.data.sync.Ulid

/**
 * Reason a (whole or partial) working day was not worked.
 * [creditsHours] = the hours count towards the weekly target (paid absence).
 * Compensation is time taken *from* the overtime balance, so it credits nothing.
 */
enum class AbsenceType(val label: String, val creditsHours: Boolean) {
    SICK("Sick", true),
    HOLIDAY("Holiday (own vacation)", true),
    PUBLIC_HOLIDAY("Public holiday (paid by company)", true),
    COMPENSATION("Compensation (from overtime)", false),
    EDUCATION("Education / training", true),
    OTHER("Other reason", true);

    companion object {
        fun fromName(name: String?): AbsenceType = entries.firstOrNull { it.name == name } ?: OTHER
    }
}

/** An absence booked on a calendar day (identified by the local day start in epoch millis). */
@Entity(tableName = "day_records", indices = [Index("dayStart", unique = true), Index("uid", unique = true)])
@Serializable
data class DayRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dayStart: Long,
    val type: String = AbsenceType.OTHER.name,
    /** Hours of the day covered by this absence (a full day = daily target). */
    val hours: Double,
    /** Free text reason when [type] is OTHER (e.g. "Military service"). */
    val label: String = "",
    val note: String = "",
    /** Identity across devices; generated here so no row can ever be written without one. */
    val uid: String = Ulid.generate(),
    /** Logical timestamp of the last change, from the device's [com.suw1labs.worktracker.data.sync.SyncClock]. */
    val updatedAt: Long = 0,
    /** Set instead of deleting the row, so other devices learn about the deletion. */
    val deletedAt: Long? = null,
    /** Which device made the last change (its ULID), for tie-breaks. */
    val deviceId: String = "",
    val createdAt: Long = currentTimeMillis()
) {
    val absenceType: AbsenceType get() = AbsenceType.fromName(type)

    val displayLabel: String
        get() = if (absenceType == AbsenceType.OTHER && label.isNotBlank()) label else absenceType.label

    val creditedSeconds: Long get() = if (absenceType.creditsHours) (hours * 3600).toLong() else 0L
}
