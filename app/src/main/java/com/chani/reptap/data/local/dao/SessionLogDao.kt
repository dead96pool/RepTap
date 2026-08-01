package com.chani.reptap.data.local.dao

import androidx.room.*
import com.chani.reptap.data.local.entity.SessionLog
import kotlinx.coroutines.flow.Flow

data class SessionSummary(
    val id: Long,
    val workoutDayName: String,
    val startedAt: Long,
    val finishedAt: Long?,
    val status: String,
    val totalSets: Int
)

@Dao
interface SessionLogDao {
    @Insert
    suspend fun insert(session: SessionLog): Long

    @Update
    suspend fun update(session: SessionLog)

    @Query("SELECT * FROM session_logs ORDER BY startedAt DESC")
    fun getAll(): Flow<List<SessionLog>>

    @Query("SELECT * FROM session_logs WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SessionLog?

    @Query("""
        SELECT s.id AS id, wd.name AS workoutDayName, s.startedAt AS startedAt, 
               s.finishedAt AS finishedAt, s.status AS status,
               (SELECT COUNT(*) FROM set_logs sl WHERE sl.sessionLogId = s.id) AS totalSets
        FROM session_logs s
        JOIN workout_days wd ON s.workoutDayId = wd.id
        ORDER BY s.startedAt DESC
    """)
    fun getSessionSummaries(): Flow<List<SessionSummary>>
}