package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.WorkTask
import com.example.data.model.WorkTaskWithProject
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkTaskDao {
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<WorkTask>>

    @Query("SELECT * FROM tasks WHERE projectId = :projectId ORDER BY status ASC, createdAt DESC")
    fun getTasksByProject(projectId: Long): Flow<List<WorkTask>>

    @Query("""
        SELECT 
            t.id, t.projectId, p.name AS projectName, p.colorHex AS projectColor, 
            p.client, t.title, t.description, t.priority, t.status, 
            t.estimatedHours, t.deadlineTimestamp, t.reminderLeadHours, 
            t.reminderEnabled, t.createdAt,
            COALESCE((SELECT SUM(te.durationSeconds) FROM time_entries te WHERE te.taskId = t.id), 0) AS loggedSeconds
        FROM tasks t
        JOIN projects p ON t.projectId = p.id
        ORDER BY 
            CASE WHEN t.status = 'DONE' THEN 1 ELSE 0 END,
            CASE WHEN t.deadlineTimestamp IS NOT NULL THEN t.deadlineTimestamp ELSE 9999999999999 END ASC,
            t.createdAt DESC
    """)
    fun getTasksWithProject(): Flow<List<WorkTaskWithProject>>

    @Query("""
        SELECT 
            t.id, t.projectId, p.name AS projectName, p.colorHex AS projectColor, 
            p.client, t.title, t.description, t.priority, t.status, 
            t.estimatedHours, t.deadlineTimestamp, t.reminderLeadHours, 
            t.reminderEnabled, t.createdAt,
            0 AS loggedSeconds
        FROM tasks t
        JOIN projects p ON t.projectId = p.id
        WHERE t.status != 'DONE' 
          AND t.deadlineTimestamp IS NOT NULL 
          AND t.deadlineTimestamp <= :thresholdTime
        ORDER BY t.deadlineTimestamp ASC
    """)
    suspend fun getPendingTasksNearDeadline(thresholdTime: Long): List<WorkTaskWithProject>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): WorkTask?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: WorkTask): Long

    @Update
    suspend fun updateTask(task: WorkTask)

    @Delete
    suspend fun deleteTask(task: WorkTask)

    @Query("UPDATE tasks SET status = :status WHERE id = :taskId")
    suspend fun updateTaskStatus(taskId: Long, status: String)

    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun getTaskCount(): Int
}
