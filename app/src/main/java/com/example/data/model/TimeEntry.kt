package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "time_entries",
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = WorkTask::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("projectId"), Index("taskId")]
)
data class TimeEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val taskId: Long? = null,
    val description: String = "",
    val startTime: Long,
    val endTime: Long,
    val durationSeconds: Long, // endTime - startTime in seconds
    val isBillable: Boolean = true,
    val hourlyRate: Double = 0.0,
    val tags: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
