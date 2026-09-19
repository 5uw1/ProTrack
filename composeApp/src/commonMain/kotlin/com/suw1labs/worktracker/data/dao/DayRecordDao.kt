package com.suw1labs.worktracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.suw1labs.worktracker.data.model.DayRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface DayRecordDao {
    @Query("SELECT * FROM day_records WHERE deletedAt IS NULL ORDER BY dayStart DESC")
    fun getAllRecords(): Flow<List<DayRecord>>

    @Query("SELECT * FROM day_records WHERE deletedAt IS NULL AND dayStart = :dayStart LIMIT 1")
    suspend fun getByDay(dayStart: Long): DayRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: DayRecord): Long

    @Update
    suspend fun update(record: DayRecord)

    @Query("UPDATE day_records SET deletedAt = :deletedAt, updatedAt = :deletedAt, deviceId = :deviceId WHERE id = :id")
    suspend fun markDeleted(id: Long, deletedAt: Long, deviceId: String)
}
