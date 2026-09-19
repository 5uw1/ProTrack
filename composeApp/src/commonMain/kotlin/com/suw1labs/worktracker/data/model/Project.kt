package com.suw1labs.worktracker.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import com.suw1labs.worktracker.util.currentTimeMillis
import com.suw1labs.worktracker.data.sync.Ulid

/**
 * A project hours are booked on (SAP project number from project management), or the built-in
 * "Unproductive" project whose tasks (meeting, coffee break, …) are recorded but reported separately.
 */
@Entity(tableName = "projects", indices = [Index("code"), Index("uid", unique = true)])
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
    /** Pinned as "what I am working on these days": the Today tab offers only focused projects by default. */
    val isFocused: Boolean = false,
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
    companion object {
        const val UNPRODUCTIVE_CODE = "UNPRODUCTIVE"
    }
}
