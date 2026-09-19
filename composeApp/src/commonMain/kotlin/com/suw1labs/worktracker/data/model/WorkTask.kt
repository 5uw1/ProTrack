package com.suw1labs.worktracker.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import com.suw1labs.worktracker.util.currentTimeMillis
import com.suw1labs.worktracker.data.sync.Ulid

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId"), Index("uid", unique = true)]
)
@Serializable
data class WorkTask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val title: String,
    val description: String = "",
    val priority: String = "MEDIUM", // LOW, MEDIUM, HIGH, URGENT
    val status: String = "TODO", // TODO, IN_PROGRESS, REVIEW, DONE
    val estimatedHours: Double = 0.0,
    val deadlineTimestamp: Long? = null, // epoch millis
    val reminderLeadHours: Int = 24, // 2, 24, 48 hours before deadline
    val reminderEnabled: Boolean = true,
    /** Identity across devices; generated here so no row can ever be written without one. */
    val uid: String = Ulid.generate(),
    /** Logical timestamp of the last change, from the device's [com.suw1labs.worktracker.data.sync.SyncClock]. */
    val updatedAt: Long = 0,
    /** Set instead of deleting the row, so other devices learn about the deletion. */
    val deletedAt: Long? = null,
    /** Which device made the last change (its ULID), for tie-breaks. */
    val deviceId: String = "",
    val createdAt: Long = currentTimeMillis()
)
