package com.suw1labs.worktracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.suw1labs.worktracker.data.model.TimeEntry
import com.suw1labs.worktracker.data.model.TimeEntryWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface TimeEntryDao {
    @Query("""
        SELECT 
            te.id, te.projectId, p.code AS projectCode, p.name AS projectName, p.colorHex AS projectColor, 
            p.isProductive AS projectProductive,
            te.taskId, t.title AS taskTitle,
            te.description, te.startTime, te.endTime, te.createdAt
        FROM time_entries te
        LEFT JOIN projects p ON te.projectId = p.id
        LEFT JOIN tasks t ON te.taskId = t.id
        ORDER BY te.startTime DESC
    """)
    fun getAllEntriesWithDetails(): Flow<List<TimeEntryWithDetails>>

    @Query("SELECT * FROM time_entries WHERE endTime IS NULL ORDER BY startTime DESC LIMIT 1")
    suspend fun getRunningEntry(): TimeEntry?

    @Query("UPDATE time_entries SET endTime = :endTime WHERE endTime IS NULL")
    suspend fun closeRunningEntries(endTime: Long)

    @Query("UPDATE time_entries SET projectId = :projectId WHERE taskId = :taskId")
    suspend fun moveEntriesOfTask(taskId: Long, projectId: Long)

    @Query("SELECT * FROM time_entries WHERE id = :id")
    suspend fun getEntryById(id: Long): TimeEntry?

    /** The most recently finished activity that started at or after [since] (used to continue it after a break). */
    @Query("SELECT * FROM time_entries WHERE endTime IS NOT NULL AND startTime >= :since ORDER BY endTime DESC LIMIT 1")
    suspend fun getLastClosedEntrySince(since: Long): TimeEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: TimeEntry): Long

    @Update
    suspend fun updateEntry(entry: TimeEntry)

    @Query("DELETE FROM time_entries WHERE id = :id")
    suspend fun deleteEntryById(id: Long)

    @Query("SELECT COUNT(*) FROM time_entries")
    suspend fun getEntryCount(): Int
}
