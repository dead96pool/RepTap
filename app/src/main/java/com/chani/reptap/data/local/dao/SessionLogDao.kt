package com.chani.reptap.data.local.dao

import androidx.room.*
import com.chani.reptap.data.local.entity.SessionLog
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionLogDao {
    @Insert
    suspend fun insert(session: SessionLog): Long

    @Update
    suspend fun update(session: SessionLog)

    @Query("SELECT * FROM session_logs ORDER BY startedAt DESC")
    fun getAll(): Flow<List<SessionLog>>
}