package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val client: String = "",
    val colorHex: String = "#3B82F6",
    val hourlyRate: Double = 0.0,
    val budgetHours: Double = 0.0,
    val status: String = "ACTIVE", // ACTIVE, ON_HOLD, COMPLETED
    val createdAt: Long = System.currentTimeMillis()
)
