package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.TimeEntry
import com.example.data.model.TimeEntryWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface TimeEntryDao {
    @Query("""
        SELECT 
            te.id, te.projectId, p.name AS projectName, p.colorHex AS projectColor, 
            p.client, te.taskId, t.title AS taskTitle, te.description, 
            te.startTime, te.endTime, te.durationSeconds, te.isBillable, 
            te.hourlyRate, te.tags, te.createdAt
        FROM time_entries te
        JOIN projects p ON te.projectId = p.id
        LEFT JOIN tasks t ON te.taskId = t.id
        ORDER BY te.startTime DESC
    """)
    fun getAllEntriesWithDetails(): Flow<List<TimeEntryWithDetails>>

    @Query("""
        SELECT 
            te.id, te.projectId, p.name AS projectName, p.colorHex AS projectColor, 
            p.client, te.taskId, t.title AS taskTitle, te.description, 
            te.startTime, te.endTime, te.durationSeconds, te.isBillable, 
            te.hourlyRate, te.tags, te.createdAt
        FROM time_entries te
        JOIN projects p ON te.projectId = p.id
        LEFT JOIN tasks t ON te.taskId = t.id
        WHERE te.startTime >= :startTimestamp AND te.startTime <= :endTimestamp
        ORDER BY te.startTime DESC
    """)
    fun getEntriesWithDetailsBetween(startTimestamp: Long, endTimestamp: Long): Flow<List<TimeEntryWithDetails>>

    @Query("""
        SELECT 
            te.id, te.projectId, p.name AS projectName, p.colorHex AS projectColor, 
            p.client, te.taskId, t.title AS taskTitle, te.description, 
            te.startTime, te.endTime, te.durationSeconds, te.isBillable, 
            te.hourlyRate, te.tags, te.createdAt
        FROM time_entries te
        JOIN projects p ON te.projectId = p.id
        LEFT JOIN tasks t ON te.taskId = t.id
        WHERE (:projectId IS NULL OR te.projectId = :projectId)
          AND te.startTime >= :startTimestamp AND te.startTime <= :endTimestamp
        ORDER BY te.startTime DESC
    """)
    suspend fun getExportEntries(projectId: Long?, startTimestamp: Long, endTimestamp: Long): List<TimeEntryWithDetails>

    @Query("SELECT * FROM time_entries WHERE id = :id")
    suspend fun getEntryById(id: Long): TimeEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: TimeEntry): Long

    @Update
    suspend fun updateEntry(entry: TimeEntry)

    @Delete
    suspend fun deleteEntry(entry: TimeEntry)

    @Query("DELETE FROM time_entries WHERE id = :id")
    suspend fun deleteEntryById(id: Long)

    @Query("SELECT COUNT(*) FROM time_entries")
    suspend fun getEntryCount(): Int
}
