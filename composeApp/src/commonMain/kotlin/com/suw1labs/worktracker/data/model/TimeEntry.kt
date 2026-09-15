package com.suw1labs.worktracker.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.suw1labs.worktracker.util.currentTimeMillis

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
        ),
        ForeignKey(
            entity = WorkCategory::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("projectId"), Index("taskId"), Index("categoryId"), Index("startTime")]
)
data class TimeEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long? = null,
    val taskId: Long? = null,
    val categoryId: Long? = null,
    val description: String = "",
    val startTime: Long,
    val endTime: Long? = null,
    val createdAt: Long = currentTimeMillis()
)
