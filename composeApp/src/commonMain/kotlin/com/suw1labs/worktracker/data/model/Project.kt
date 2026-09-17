package com.suw1labs.worktracker.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import com.suw1labs.worktracker.util.currentTimeMillis

/**
 * A project hours are booked on (SAP project number from project management), or the built-in
 * "Unproductive" project whose tasks (meeting, coffee break, …) are recorded but reported separately.
 */
@Entity(tableName = "projects", indices = [Index("code")])
@Serializable
data class Project(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** SAP project number, e.g. "P-2026-0142". */
    val code: String,
    val name: String,
    /** Customer / machine / department – free text shown next to the project. */
    val client: String = "",
    val colorHex: String = "#3B82F6",
    /** Hours planned by project management (0 = no budget). */
    val budgetHours: Double = 0.0,
    val status: String = "ACTIVE", // ACTIVE, ON_HOLD, COMPLETED
    /** False for the internal "Unproductive" project – its time never counts as project work. */
    val isProductive: Boolean = true,
    val createdAt: Long = currentTimeMillis()
) {
    companion object {
        const val UNPRODUCTIVE_CODE = "UNPRODUCTIVE"
    }
}
