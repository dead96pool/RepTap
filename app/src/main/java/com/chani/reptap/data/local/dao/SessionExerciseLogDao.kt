package com.chani.reptap.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.chani.reptap.data.local.entity.SessionExerciseLog
import kotlinx.coroutines.flow.Flow

data class HistoricalExerciseIdentity(
    val exerciseHistoryKey: String,
    val exerciseName: String,
    val startedAt: Long
)

data class HistoricalExerciseDefinition(
    val exerciseHistoryKey: String,
    val exerciseName: String,
    val trackingType: com.chani.reptap.data.local.entity.ExerciseTrackingType,
    val isBodyweight: Boolean,
    val isPerSide: Boolean,
    val primaryMuscleHistoryKey: String,
    val primaryMuscleName: String
)

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

    @Query(
        """
    SELECT
        sel.exerciseHistoryKey AS exerciseHistoryKey,
        sel.exerciseNameSnapshot AS exerciseName,
        s.startedAt AS startedAt

    FROM session_exercise_logs sel

    INNER JOIN session_logs s
        ON sel.sessionLogId = s.id

    WHERE s.status = 'COMPLETED'

      AND EXISTS (
          SELECT 1

          FROM set_logs sl

          WHERE sl.sessionExerciseLogId = sel.id
      )

    ORDER BY
        s.startedAt DESC,
        sel.orderIndex ASC
    """
    )
    fun observeHistoricalExerciseIdentities():
            Flow<List<HistoricalExerciseIdentity>>

    @Query(
        """
    SELECT
        sel.exerciseHistoryKey AS exerciseHistoryKey,
        sel.exerciseNameSnapshot AS exerciseName,
        sel.trackingTypeSnapshot AS trackingType,
        sel.isBodyweightSnapshot AS isBodyweight,
        sel.isPerSideSnapshot AS isPerSide,
        sel.primaryMuscleHistoryKey AS primaryMuscleHistoryKey,
        sel.primaryMuscleNameSnapshot AS primaryMuscleName

    FROM session_exercise_logs sel

    INNER JOIN session_logs s
        ON sel.sessionLogId = s.id

    WHERE sel.exerciseHistoryKey = :historyKey
      AND s.status = 'COMPLETED'

      AND EXISTS (
          SELECT 1
          FROM set_logs sl
          WHERE sl.sessionExerciseLogId = sel.id
      )

    ORDER BY
        s.startedAt DESC,
        sel.id DESC

    LIMIT 1
    """
    )
    suspend fun getLatestHistoricalExerciseDefinition(
        historyKey: String
    ): HistoricalExerciseDefinition?
}