package com.suw1labs.worktracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.suw1labs.worktracker.data.model.Project
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects WHERE deletedAt IS NULL ORDER BY code ASC, name ASC")
    fun getAllProjects(): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE deletedAt IS NULL AND id = :id")
    suspend fun getProjectById(id: Long): Project?

    @Query("SELECT * FROM projects WHERE deletedAt IS NULL AND code = :code LIMIT 1")
    suspend fun findByCode(code: String): Project?

    @Query("SELECT * FROM projects WHERE deletedAt IS NULL AND status = 'ACTIVE' ORDER BY code ASC, name ASC")
    fun getActiveProjects(): Flow<List<Project>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: Project): Long

    @Update
    suspend fun updateProject(project: Project)



    @Query("UPDATE projects SET deletedAt = :deletedAt, updatedAt = :deletedAt, deviceId = :deviceId WHERE id = :id")
    suspend fun markProjectDeleted(id: Long, deletedAt: Long, deviceId: String)

    @Query("SELECT COUNT(*) FROM projects WHERE deletedAt IS NULL")
    suspend fun getProjectCount(): Int
}
