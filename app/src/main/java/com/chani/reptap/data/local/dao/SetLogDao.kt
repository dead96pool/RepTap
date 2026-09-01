package com.chani.reptap.data.local.dao

import androidx.room.*
import com.chani.reptap.data.local.entity.SetLog
import com.chani.reptap.data.local.entity.ExerciseTrackingType
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
    val exerciseId: Long,
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
    val exerciseId: Long,
    val exerciseName: String,
    val trackingType: com.chani.reptap.data.local.entity.ExerciseTrackingType,
    val sessionLogId: Long,
    val startedAt: Long,
    val sessionVolume: Double,
    val averageDurationSec: Double?
)

@Dao
interface SetLogDao {
    @Insert
    suspend fun insert(setLog: SetLog): Long

    @Query("SELECT * FROM set_logs WHERE sessionLogId = :sessionId ORDER BY setNumber")
    fun getForSession(sessionId: Long): Flow<List<SetLog>>

    @Query("""
        SELECT m.name AS muscleName, 
               SUM(s.repsCompleted) AS totalReps, 
               COUNT(s.id) AS totalSets, 
               AVG(s.weightKg) AS avgWeight
        FROM set_logs s
        JOIN workout_exercises we ON s.workoutExerciseId = we.id
        JOIN exercises e ON we.exerciseId = e.id
        JOIN muscle_groups m ON e.primaryMuscleId = m.id
        WHERE s.timestamp BETWEEN :startTime AND :endTime
        GROUP BY m.name
    """)
    fun getMuscleProgress(startTime: Long, endTime: Long): Flow<List<MuscleProgress>>

    @Query("""
    SELECT e.name AS exerciseName,
           we.orderIndex AS orderIndex,
           sl.setNumber AS setNumber,
           sl.repsCompleted AS repsCompleted,
           sl.durationCompletedSec AS durationCompletedSec,
           sl.weightKg AS weightKg
    FROM set_logs sl
    JOIN workout_exercises we ON sl.workoutExerciseId = we.id
    JOIN exercises e ON we.exerciseId = e.id
    WHERE sl.sessionLogId = :sessionId
    ORDER BY we.orderIndex, sl.setNumber
""")
    fun getSetDetailsForSession(sessionId: Long): Flow<List<SetLogDetail>>

    @Query("""
    SELECT
        e.id AS exerciseId,
        e.name AS exerciseName,
        e.isBodyweight AS isBodyweight,
        s.id AS sessionLogId,
        s.startedAt AS startedAt,
        SUM(
            CASE WHEN e.isBodyweight = 1
                 THEN sl.repsCompleted
                 ELSE sl.repsCompleted * sl.weightKg
            END
        ) AS sessionVolume
    FROM set_logs sl
    JOIN workout_exercises we ON sl.workoutExerciseId = we.id
    JOIN exercises e ON we.exerciseId = e.id
    JOIN session_logs s ON sl.sessionLogId = s.id
    WHERE s.status = 'COMPLETED'
      AND e.trackingType = 'REPS'
    GROUP BY e.id, s.id
""")
    suspend fun getAllExerciseSessionVolumes(): List<ExerciseSessionVolume>

    @Query("""
    SELECT
        e.name AS exerciseName,
        e.isBodyweight AS isBodyweight,
        e.trackingType AS trackingType,
        AVG(sl.repsCompleted) AS averageReps,
        MAX(sl.repsCompleted) AS bestSingleSetReps,
        AVG(sl.durationCompletedSec) AS averageDurationSec,
        MAX(sl.durationCompletedSec) AS bestDurationSec,
        COUNT(DISTINCT s.id) AS sessionsLogged,
        SUM(sl.repsCompleted) AS totalReps,
        MAX(s.startedAt) AS lastPerformedAt
    FROM set_logs sl
    JOIN workout_exercises we ON sl.workoutExerciseId = we.id
    JOIN exercises e ON we.exerciseId = e.id
    JOIN session_logs s ON sl.sessionLogId = s.id
    WHERE s.status = 'COMPLETED'
      AND e.id = :exerciseId
    GROUP BY e.id
""")
    suspend fun getExerciseDetailStats(exerciseId: Long): ExerciseDetailStats?

    @Query("""
    SELECT
        s.sessionLogId AS sessionId,
        sl.startedAt AS startedAt,
        COALESCE(AVG(s.weightKg), 0) AS avgWeightKg,
        AVG(s.repsCompleted) AS avgReps,
        COALESCE(SUM(s.weightKg * s.repsCompleted), 0) AS totalVolume,
        COALESCE(MAX(s.weightKg), 0) AS maxWeightKg,
        MAX(s.repsCompleted) AS maxReps,
        AVG(s.durationCompletedSec) AS avgDurationSec,
        MAX(s.durationCompletedSec) AS maxDurationSec,
        COUNT(s.id) AS setsCount
    FROM set_logs s
    INNER JOIN workout_exercises we
        ON s.workoutExerciseId = we.id
    INNER JOIN session_logs sl
        ON s.sessionLogId = sl.id
    WHERE we.exerciseId = :exerciseId
      AND sl.status = 'COMPLETED'
    GROUP BY s.sessionLogId
    ORDER BY sl.startedAt ASC
""")
    suspend fun getExerciseSessionDataPoints(
        exerciseId: Long
    ): List<ExerciseSessionDataPoint>

    @Query("""
    SELECT
        s.sessionLogId AS sessionLogId,
        s.setNumber AS setNumber,
        s.weightKg AS weightKg,
        s.repsCompleted AS repsCompleted,
        s.durationCompletedSec AS durationCompletedSec
    FROM set_logs s
    INNER JOIN workout_exercises we
        ON s.workoutExerciseId = we.id
    INNER JOIN session_logs sl
        ON s.sessionLogId = sl.id
    WHERE we.exerciseId = :exerciseId
      AND sl.status = 'COMPLETED'
    ORDER BY sl.startedAt ASC, s.setNumber ASC
""")
    suspend fun getExerciseSetDetails(
        exerciseId: Long
    ): List<ExerciseSetDetail>

    @Query("""
    SELECT
        e.id AS exerciseId,
        e.name AS exerciseName,
        e.trackingType AS trackingType,
        s.id AS sessionLogId,
        s.startedAt AS startedAt,
        COALESCE(
            SUM(
                CASE
                    WHEN e.isBodyweight = 1
                        THEN sl.repsCompleted
                    ELSE
                        sl.repsCompleted * COALESCE(sl.weightKg, 0)
                END
            ),
            0
        ) AS sessionVolume,
        AVG(sl.durationCompletedSec) AS averageDurationSec
    FROM set_logs sl
    JOIN workout_exercises we
        ON sl.workoutExerciseId = we.id
    JOIN exercises e
        ON we.exerciseId = e.id
    JOIN session_logs s
        ON sl.sessionLogId = s.id
    WHERE s.status = 'COMPLETED'
    GROUP BY
        e.id,
        e.name,
        e.trackingType,
        s.id,
        s.startedAt
""")
    suspend fun getAllExerciseTrendSessions(): List<ExerciseTrendSession>
}