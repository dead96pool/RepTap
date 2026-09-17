package com.chani.reptap.data.local.dao

import androidx.room.*
import com.chani.reptap.data.local.entity.ExerciseTrackingType
import com.chani.reptap.data.local.entity.SetLog
import kotlinx.coroutines.flow.Flow

data class MuscleProgress(
    val muscleName: String,
    val totalReps: Int,
    val totalSets: Int,
    val avgWeight: Float
)

data class SetLogDetail(
    val exerciseName: String,
    val orderIndex: Int,
    val setNumber: Int,
    val repsCompleted: Int,
    val durationCompletedSec: Int?,
    val weightKg: Float?
)

data class ExerciseSessionVolume(
    val exerciseHistoryKey: String,
    val exerciseName: String,
    val isBodyweight: Boolean,
    val sessionLogId: Long,
    val startedAt: Long,
    val sessionVolume: Float
)

data class ExerciseDetailStats(
    val exerciseName: String,
    val isBodyweight: Boolean,
    val trackingType: ExerciseTrackingType,
    val averageReps: Double,
    val bestSingleSetReps: Int,
    val averageDurationSec: Double?,
    val bestDurationSec: Int?,
    val sessionsLogged: Int,
    val totalReps: Int,
    val lastPerformedAt: Long
)

data class ExerciseSessionDataPoint(
    val sessionId: Long,
    val startedAt: Long,
    val avgWeightKg: Float,
    val avgReps: Double,
    val totalVolume: Double,
    val maxWeightKg: Float,
    val maxReps: Int,
    val avgDurationSec: Double?,
    val maxDurationSec: Int?,
    val setsCount: Int
)

data class ExerciseSetDetail(
    val sessionLogId: Long,
    val setNumber: Int,
    val weightKg: Float?,
    val repsCompleted: Int,
    val durationCompletedSec: Int?
)

data class ExerciseTrendSession(
    val exerciseHistoryKey: String,
    val exerciseName: String,
    val trackingType: ExerciseTrackingType,
    val sessionLogId: Long,
    val startedAt: Long,
    val sessionVolume: Double,
    val averageDurationSec: Double?
)

@Dao
interface SetLogDao {

    @Insert
    suspend fun insert(
        setLog: SetLog
    ): Long

    @Query(
        """
        SELECT sl.*

        FROM set_logs sl

        INNER JOIN session_exercise_logs sel
            ON sl.sessionExerciseLogId = sel.id

        WHERE sel.sessionLogId = :sessionId

        ORDER BY
            sel.orderIndex,
            sl.setNumber
        """
    )
    fun getForSession(
        sessionId: Long
    ): Flow<List<SetLog>>

    @Query(
        """
        SELECT
            sel.primaryMuscleNameSnapshot AS muscleName,
            SUM(s.repsCompleted) AS totalReps,
            COUNT(s.id) AS totalSets,
            AVG(s.weightKg) AS avgWeight

        FROM set_logs s

        INNER JOIN session_exercise_logs sel
            ON s.sessionExerciseLogId = sel.id

        WHERE s.timestamp
            BETWEEN :startTime AND :endTime

        GROUP BY
            sel.primaryMuscleHistoryKey,
            sel.primaryMuscleNameSnapshot
        """
    )
    fun getMuscleProgress(
        startTime: Long,
        endTime: Long
    ): Flow<List<MuscleProgress>>

    @Query(
        """
        SELECT
            sel.exerciseNameSnapshot AS exerciseName,
            sel.orderIndex AS orderIndex,
            sl.setNumber AS setNumber,
            sl.repsCompleted AS repsCompleted,
            sl.durationCompletedSec AS durationCompletedSec,
            sl.weightKg AS weightKg

        FROM set_logs sl

        INNER JOIN session_exercise_logs sel
            ON sl.sessionExerciseLogId = sel.id

        WHERE sel.sessionLogId = :sessionId

        ORDER BY
            sel.orderIndex,
            sl.setNumber
        """
    )
    fun getSetDetailsForSession(
        sessionId: Long
    ): Flow<List<SetLogDetail>>

    @Query(
        """
        SELECT
            sel.exerciseHistoryKey AS exerciseHistoryKey,
            sel.exerciseNameSnapshot AS exerciseName,
            sel.isBodyweightSnapshot AS isBodyweight,

            s.id AS sessionLogId,
            s.startedAt AS startedAt,

            SUM(
                CASE
                    WHEN sel.isBodyweightSnapshot = 1
                        THEN sl.repsCompleted

                    ELSE
                        sl.repsCompleted *
                            sl.weightKg
                END
            ) AS sessionVolume

        FROM set_logs sl

        INNER JOIN session_exercise_logs sel
            ON sl.sessionExerciseLogId = sel.id

        INNER JOIN session_logs s
            ON sel.sessionLogId = s.id

        WHERE s.status = 'COMPLETED'
          AND sel.trackingTypeSnapshot = 'REPS'

        GROUP BY
            sel.exerciseHistoryKey,
            sel.exerciseNameSnapshot,
            sel.isBodyweightSnapshot,
            s.id,
            s.startedAt
        """
    )
    suspend fun getAllExerciseSessionVolumes():
            List<ExerciseSessionVolume>

    @Query(
        """
        SELECT
            sel.exerciseNameSnapshot AS exerciseName,
            sel.isBodyweightSnapshot AS isBodyweight,
            sel.trackingTypeSnapshot AS trackingType,

            AVG(
                sl.repsCompleted
            ) AS averageReps,

            MAX(
                sl.repsCompleted
            ) AS bestSingleSetReps,

            AVG(
                sl.durationCompletedSec
            ) AS averageDurationSec,

            MAX(
                sl.durationCompletedSec
            ) AS bestDurationSec,

            COUNT(
                DISTINCT s.id
            ) AS sessionsLogged,

            SUM(
                sl.repsCompleted
            ) AS totalReps,

            MAX(
                s.startedAt
            ) AS lastPerformedAt

        FROM set_logs sl

        INNER JOIN session_exercise_logs sel
            ON sl.sessionExerciseLogId = sel.id

        INNER JOIN session_logs s
            ON sel.sessionLogId = s.id

        WHERE s.status = 'COMPLETED'
          AND sel.exerciseHistoryKey = :exerciseHistoryKey

        GROUP BY
            sel.exerciseHistoryKey
        """
    )
    suspend fun getExerciseDetailStats(
        exerciseHistoryKey: String
    ): ExerciseDetailStats?

    @Query(
        """
        SELECT
            sel.sessionLogId AS sessionId,
            sl.startedAt AS startedAt,

            COALESCE(
                AVG(s.weightKg),
                0
            ) AS avgWeightKg,

            AVG(
                s.repsCompleted
            ) AS avgReps,

            COALESCE(
                SUM(
                    s.weightKg *
                        s.repsCompleted
                ),
                0
            ) AS totalVolume,

            COALESCE(
                MAX(s.weightKg),
                0
            ) AS maxWeightKg,

            MAX(
                s.repsCompleted
            ) AS maxReps,

            AVG(
                s.durationCompletedSec
            ) AS avgDurationSec,

            MAX(
                s.durationCompletedSec
            ) AS maxDurationSec,

            COUNT(
                s.id
            ) AS setsCount

        FROM set_logs s

        INNER JOIN session_exercise_logs sel
            ON s.sessionExerciseLogId = sel.id

        INNER JOIN session_logs sl
            ON sel.sessionLogId = sl.id

        WHERE sel.exerciseHistoryKey = :exerciseHistoryKey
          AND sl.status = 'COMPLETED'

        GROUP BY
            sel.sessionLogId,
            sl.startedAt

        ORDER BY
            sl.startedAt ASC
        """
    )
    suspend fun getExerciseSessionDataPoints(
        exerciseHistoryKey: String
    ): List<ExerciseSessionDataPoint>

    @Query(
        """
        SELECT
            sel.sessionLogId AS sessionLogId,
            s.setNumber AS setNumber,
            s.weightKg AS weightKg,
            s.repsCompleted AS repsCompleted,
            s.durationCompletedSec AS durationCompletedSec

        FROM set_logs s

        INNER JOIN session_exercise_logs sel
            ON s.sessionExerciseLogId = sel.id

        INNER JOIN session_logs sl
            ON sel.sessionLogId = sl.id

        WHERE sel.exerciseHistoryKey = :exerciseHistoryKey
          AND sl.status = 'COMPLETED'

        ORDER BY
            sl.startedAt ASC,
            s.setNumber ASC
        """
    )
    suspend fun getExerciseSetDetails(
        exerciseHistoryKey: String
    ): List<ExerciseSetDetail>

    @Query(
        """
        SELECT
            sel.exerciseHistoryKey AS exerciseHistoryKey,

            sel.exerciseNameSnapshot AS exerciseName,
            sel.trackingTypeSnapshot AS trackingType,

            s.id AS sessionLogId,
            s.startedAt AS startedAt,

            COALESCE(
                SUM(
                    CASE
                        WHEN sel.isBodyweightSnapshot = 1
                            THEN sl.repsCompleted

                        ELSE
                            sl.repsCompleted *
                                COALESCE(
                                    sl.weightKg,
                                    0
                                )
                    END
                ),
                0
            ) AS sessionVolume,

            AVG(
                sl.durationCompletedSec
            ) AS averageDurationSec

        FROM set_logs sl

        INNER JOIN session_exercise_logs sel
            ON sl.sessionExerciseLogId = sel.id

        INNER JOIN session_logs s
            ON sel.sessionLogId = s.id

        WHERE s.status = 'COMPLETED'

        GROUP BY
            sel.exerciseHistoryKey,
            sel.exerciseNameSnapshot,
            sel.trackingTypeSnapshot,
            s.id,
            s.startedAt
        """
    )
    suspend fun getAllExerciseTrendSessions():
            List<ExerciseTrendSession>
}