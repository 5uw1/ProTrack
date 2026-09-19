package com.suw1labs.worktracker.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import com.suw1labs.worktracker.util.currentTimeMillis
import com.suw1labs.worktracker.data.sync.Ulid

enum class ClockOutReason(val label: String) {
    LUNCH("Lunch"),
    BREAK("Break"),
    OUT("Out of office"),
    END_OF_DAY("Go home");

    companion object {
        fun fromName(name: String?): ClockOutReason? = entries.firstOrNull { it.name == name }
    }
}

/** A clocked-in period. [clockOut] is null while clocked in. */
@Entity(tableName = "attendance_sessions", indices = [Index("clockIn"), Index("clockOut"), Index("uid", unique = true)])
@Serializable
data class AttendanceSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val clockIn: Long,
    val clockOut: Long? = null,
    val clockOutReason: String? = null,
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
    val isOpen: Boolean get() = clockOut == null
}
