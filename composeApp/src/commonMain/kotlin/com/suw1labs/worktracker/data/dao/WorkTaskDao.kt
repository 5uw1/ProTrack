package com.suw1labs.worktracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.suw1labs.worktracker.data.model.WorkTask
import com.suw1labs.worktracker.data.model.WorkTaskWithProject
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkTaskDao {
    @Query("SELECT * FROM tasks WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<WorkTask>>

    @Query("""
        SELECT 
            t.id, t.projectId, p.code AS projectCode, p.name AS projectName, p.colorHex AS projectColor, 
            p.client, t.title, t.description, t.priority, t.status, 
            t.estimatedHours, t.deadlineTimestamp, t.reminderLeadHours, 
            t.reminderEnabled, t.createdAt,
            COALESCE((SELECT SUM((te.endTime - te.startTime) / 1000) FROM time_entries te WHERE te.taskId = t.id AND te.endTime IS NOT NULL), 0) AS loggedSeconds
        FROM tasks t
        JOIN projects p ON t.projectId = p.id
        ORDER BY 
            CASE WHEN t.status = 'DONE' THEN 1 ELSE 0 END,
            CASE WHEN t.deadlineTimestamp IS NOT NULL THEN t.deadlineTimestamp ELSE 9999999999999 END ASC,
            t.createdAt DESC
    """)
    fun getTasksWithProject(): Flow<List<WorkTaskWithProject>>

    @Query("SELECT * FROM tasks WHERE deletedAt IS NULL AND id = :id")
    suspend fun getTaskById(id: Long): WorkTask?

    @Query("SELECT * FROM tasks WHERE deletedAt IS NULL AND projectId = :projectId AND title = :title COLLATE NOCASE LIMIT 1")
    suspend fun findTaskByTitle(projectId: Long, title: String): WorkTask?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: WorkTask): Long

    @Update
    suspend fun updateTask(task: WorkTask)

    @Query("UPDATE tasks SET deletedAt = :deletedAt, updatedAt = :deletedAt, deviceId = :deviceId WHERE id = :id")
    suspend fun markTaskDeleted(id: Long, deletedAt: Long, deviceId: String)

    @Query("UPDATE tasks SET status = :status WHERE deletedAt IS NULL AND id = :taskId")
    suspend fun updateTaskStatus(taskId: Long, status: String)

    @Query("UPDATE tasks SET projectId = :projectId WHERE deletedAt IS NULL AND id = :taskId")
    suspend fun moveTask(taskId: Long, projectId: Long)

    @Query("SELECT COUNT(*) FROM tasks WHERE deletedAt IS NULL")
    suspend fun getTaskCount(): Int
}
