package com.chani.reptap.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.chani.reptap.data.local.entity.SessionExerciseLog

@Dao
interface SessionExerciseLogDao {

    @Insert
    suspend fun insertAll(
        items: List<SessionExerciseLog>
    ): List<Long>

    @Query(
        """
        SELECT *
        FROM session_exercise_logs
        WHERE sessionLogId = :sessionId
        ORDER BY orderIndex
        """
    )
    suspend fun getForSessionOnce(
        sessionId: Long
    ): List<SessionExerciseLog>
}