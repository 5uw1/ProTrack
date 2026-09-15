package com.suw1labs.worktracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.suw1labs.worktracker.util.currentTimeMillis

/**
 * What kind of work an activity is (PLC, High Level Language, Meeting, ...).
 * Non-productive categories (coffee break, informal meeting, uncategorized) are still
 * recorded so the day adds up, but they are reported separately from project hours.
 */
@Entity(tableName = "work_categories")
data class WorkCategory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val isProductive: Boolean = true,
    val colorHex: String = "#64748B",
    val sortOrder: Int = 0,
    val createdAt: Long = currentTimeMillis()
)
