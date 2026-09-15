package com.suw1labs.worktracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.suw1labs.worktracker.data.model.WorkCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkCategoryDao {
    @Query("SELECT * FROM work_categories ORDER BY isProductive DESC, sortOrder ASC, name ASC")
    fun getAllCategories(): Flow<List<WorkCategory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: WorkCategory): Long

    @Update
    suspend fun updateCategory(category: WorkCategory)

    @Query("DELETE FROM work_categories WHERE id = :id")
    suspend fun deleteCategoryById(id: Long)

    @Query("SELECT COUNT(*) FROM work_categories")
    suspend fun getCategoryCount(): Int
}
