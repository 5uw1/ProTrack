package com.suw1labs.worktracker.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import com.suw1labs.worktracker.util.currentTimeMillis
import com.suw1labs.worktracker.data.sync.Ulid

/**
 * One activity block. [endTime] is null while the activity is running.
 * [projectId] is null for time that belongs to no SAP project (breaks, internal work).
 */
@Entity(
    tableName = "time_entries",
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = WorkTask::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("projectId"), Index("taskId"), Index("startTime"), Index("endTime"), Index("uid", unique = true)]
)
@Serializable
data class TimeEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long? = null,
    val taskId: Long? = null,
    val description: String = "",
    val startTime: Long,
    val endTime: Long? = null,
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
