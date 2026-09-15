package com.suw1labs.worktracker.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.suw1labs.worktracker.util.currentTimeMillis

/** An SAP project as defined by project management. */
@Entity(tableName = "projects", indices = [Index("code")])
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
    val createdAt: Long = currentTimeMillis()
)
