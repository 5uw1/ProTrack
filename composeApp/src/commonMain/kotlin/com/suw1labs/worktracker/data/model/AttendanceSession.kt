package com.suw1labs.worktracker.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.suw1labs.worktracker.util.currentTimeMillis

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
@Entity(tableName = "attendance_sessions", indices = [Index("clockIn")])
data class AttendanceSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val clockIn: Long,
    val clockOut: Long? = null,
    val clockOutReason: String? = null,
    val note: String = "",
    val createdAt: Long = currentTimeMillis()
) {
    val isOpen: Boolean get() = clockOut == null
}
