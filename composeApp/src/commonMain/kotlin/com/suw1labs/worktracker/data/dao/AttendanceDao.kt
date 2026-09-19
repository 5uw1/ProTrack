package com.suw1labs.worktracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.suw1labs.worktracker.data.model.AttendanceSession
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_sessions WHERE deletedAt IS NULL ORDER BY clockIn DESC")
    fun getAllSessions(): Flow<List<AttendanceSession>>

    @Query("SELECT * FROM attendance_sessions WHERE deletedAt IS NULL AND clockOut IS NULL ORDER BY clockIn DESC LIMIT 1")
    fun observeOpenSession(): Flow<AttendanceSession?>

    @Query("SELECT * FROM attendance_sessions WHERE deletedAt IS NULL AND clockOut IS NULL ORDER BY clockIn DESC LIMIT 1")
    suspend fun getOpenSession(): AttendanceSession?

    @Query("UPDATE attendance_sessions SET clockOut = :clockOut, clockOutReason = :reason WHERE deletedAt IS NULL AND clockOut IS NULL")
    suspend fun closeOpenSessions(clockOut: Long, reason: String?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: AttendanceSession): Long

    @Update
    suspend fun updateSession(session: AttendanceSession)

    @Query("UPDATE attendance_sessions SET deletedAt = :deletedAt, updatedAt = :deletedAt, deviceId = :deviceId WHERE id = :id")
    suspend fun markSessionDeleted(id: Long, deletedAt: Long, deviceId: String)
}
