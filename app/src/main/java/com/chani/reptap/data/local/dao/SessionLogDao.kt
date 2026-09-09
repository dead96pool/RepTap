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

data class SessionRecap(
    val id: Long,
    val workoutDayId: Long,
    val workoutDayName: String,
    val startedAt: Long,
    val finishedAt: Long,
    val totalSets: Int,
    val totalReps: Int,
    val avgWeightKg: Float
)

data class SessionDateAndDay(
    val startedAt: Long,
    val workoutDayId: Long
)

@Dao
interface SessionLogDao {

    @Insert
    suspend fun insert(
        session: SessionLog
    ): Long

    @Update
    suspend fun update(
        session: SessionLog
    )

    @Query(
        """
        SELECT *
        FROM session_logs
        ORDER BY startedAt DESC
        """
    )
    fun getAll():
            Flow<List<SessionLog>>

    @Query(
        """
        SELECT *
        FROM session_logs
        WHERE id = :id
        LIMIT 1
        """
    )
    suspend fun getById(
        id: Long
    ): SessionLog?

    @Query(
        """
        SELECT
            s.id AS id,
            s.workoutDayNameSnapshot AS workoutDayName,
            s.startedAt AS startedAt,
            s.finishedAt AS finishedAt,
            s.status AS status,

            (
                SELECT COUNT(*)
                FROM set_logs sl

                INNER JOIN session_exercise_logs sel
                    ON sl.sessionExerciseLogId = sel.id

                WHERE sel.sessionLogId = s.id
            ) AS totalSets

        FROM session_logs s

        ORDER BY s.startedAt DESC
        """
    )
    fun getSessionSummaries():
            Flow<List<SessionSummary>>

    @Query(
        """
        SELECT
            s.id AS id,
            s.workoutDayId AS workoutDayId,
            s.workoutDayNameSnapshot AS workoutDayName,
            s.startedAt AS startedAt,
            s.finishedAt AS finishedAt,

            (
                SELECT COUNT(*)
                FROM set_logs sl

                INNER JOIN session_exercise_logs sel
                    ON sl.sessionExerciseLogId = sel.id

                WHERE sel.sessionLogId = s.id
            ) AS totalSets,

            (
                SELECT COALESCE(
                    SUM(sl.repsCompleted),
                    0
                )
                FROM set_logs sl

                INNER JOIN session_exercise_logs sel
                    ON sl.sessionExerciseLogId = sel.id

                WHERE sel.sessionLogId = s.id
            ) AS totalReps,

            (
                SELECT COALESCE(
                    AVG(sl.weightKg),
                    0
                )
                FROM set_logs sl

                INNER JOIN session_exercise_logs sel
                    ON sl.sessionExerciseLogId = sel.id

                WHERE sel.sessionLogId = s.id
            ) AS avgWeightKg

        FROM session_logs s

        WHERE s.status = 'COMPLETED'

        ORDER BY s.startedAt DESC
        """
    )
    fun getCompletedSessionRecaps():
            Flow<List<SessionRecap>>

    @Query(
        """
        SELECT
            s.id AS id,
            s.workoutDayId AS workoutDayId,
            s.workoutDayNameSnapshot AS workoutDayName,
            s.startedAt AS startedAt,
            s.finishedAt AS finishedAt,

            (
                SELECT COUNT(*)
                FROM set_logs sl

                INNER JOIN session_exercise_logs sel
                    ON sl.sessionExerciseLogId = sel.id

                WHERE sel.sessionLogId = s.id
            ) AS totalSets,

            (
                SELECT COALESCE(
                    SUM(sl.repsCompleted),
                    0
                )
                FROM set_logs sl

                INNER JOIN session_exercise_logs sel
                    ON sl.sessionExerciseLogId = sel.id

                WHERE sel.sessionLogId = s.id
            ) AS totalReps,

            (
                SELECT COALESCE(
                    AVG(sl.weightKg),
                    0
                )
                FROM set_logs sl

                INNER JOIN session_exercise_logs sel
                    ON sl.sessionExerciseLogId = sel.id

                WHERE sel.sessionLogId = s.id
            ) AS avgWeightKg

        FROM session_logs s

        WHERE s.status = 'COMPLETED'
          AND s.workoutDayId = :workoutDayId
          AND s.startedAt < :currentStartedAt

        ORDER BY s.startedAt DESC

        LIMIT 1
        """
    )
    suspend fun getPreviousSessionForDay(
        workoutDayId: Long,
        currentStartedAt: Long
    ): SessionRecap?

    @Query(
        """
        SELECT
            s.id AS id,
            s.workoutDayId AS workoutDayId,
            s.workoutDayNameSnapshot AS workoutDayName,
            s.startedAt AS startedAt,
            s.finishedAt AS finishedAt,

            (
                SELECT COUNT(*)
                FROM set_logs sl

                INNER JOIN session_exercise_logs sel
                    ON sl.sessionExerciseLogId = sel.id

                WHERE sel.sessionLogId = s.id
            ) AS totalSets,

            (
                SELECT COALESCE(
                    SUM(sl.repsCompleted),
                    0
                )
                FROM set_logs sl

                INNER JOIN session_exercise_logs sel
                    ON sl.sessionExerciseLogId = sel.id

                WHERE sel.sessionLogId = s.id
            ) AS totalReps,

            (
                SELECT COALESCE(
                    AVG(sl.weightKg),
                    0
                )
                FROM set_logs sl

                INNER JOIN session_exercise_logs sel
                    ON sl.sessionExerciseLogId = sel.id

                WHERE sel.sessionLogId = s.id
            ) AS avgWeightKg

        FROM session_logs s

        WHERE s.id = :sessionId
        """
    )
    suspend fun getSessionRecapById(
        sessionId: Long
    ): SessionRecap?

    @Query(
        """
        SELECT
            startedAt,
            workoutDayId

        FROM session_logs

        WHERE status = 'COMPLETED'

        ORDER BY startedAt ASC
        """
    )
    suspend fun getAllCompletedSessionsOrdered():
            List<SessionDateAndDay>

    @Query(
        """
        SELECT AVG(
            finishedAt - startedAt
        )

        FROM session_logs

        WHERE status = 'COMPLETED'
          AND finishedAt IS NOT NULL
        """
    )
    suspend fun getAverageDurationMillis():
            Long?

    @Query(
        """
        SELECT COUNT(*)
        FROM session_logs
        WHERE status = 'COMPLETED'
        """
    )
    suspend fun getTotalSessionsCount():
            Int

    @Query(
        """
        SELECT MIN(startedAt)
        FROM session_logs
        WHERE status = 'COMPLETED'
        """
    )
    suspend fun getFirstSessionDate():
            Long?

    @Query(
        """
        SELECT COUNT(*)

        FROM session_logs

        WHERE status = 'COMPLETED'
          AND workoutDayId IN (
              SELECT id
              FROM workout_days
              WHERE isRecovery = 0
          )
        """
    )
    suspend fun getCompletedNonRecoverySessionsCount():
            Int

    @Query(
        """
        DELETE FROM session_logs
        """
    )
    suspend fun deleteAll()
}