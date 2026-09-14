package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
    indices = [Index("projectId")]
)
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
    val createdAt: Long = System.currentTimeMillis()
)
