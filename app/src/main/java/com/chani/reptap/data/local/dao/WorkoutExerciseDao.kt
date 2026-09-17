package com.chani.reptap.data.local.dao

import androidx.room.*
import com.chani.reptap.data.local.entity.WorkoutExercise
import kotlinx.coroutines.flow.Flow

data class WorkoutExerciseWithName(
    val id: Long,
    val exerciseId: Long,
    val orderIndex: Int,
    val exerciseName: String,
    val targetSets: Int,
    val targetReps: Int,
    val targetDurationSec: Int?,
    val restBetweenSetsSec: Int,
    val restBeforeNextSec: Int,
    val defaultWeightKg: Float?,
    val isPerSide: Boolean,
    val trackingType: com.chani.reptap.data.local.entity.ExerciseTrackingType
)

data class WorkoutExerciseSnapshotSource(
    val workoutExerciseId: Long,

    val sourceExerciseId: Long,
    val exerciseHistoryKey: String,
    val exerciseName: String,

    val trackingType:
    com.chani.reptap.data.local.entity.ExerciseTrackingType,

    val isBodyweight: Boolean,
    val isPerSide: Boolean,

    val sourcePrimaryMuscleId: Long,
    val primaryMuscleHistoryKey: String,
    val primaryMuscleName: String,

    val orderIndex: Int,
    val targetSets: Int,
    val targetReps: Int,
    val targetDurationSec: Int?,
    val defaultWeightKg: Float?,
    val restBetweenSetsSec: Int,
    val restBeforeNextSec: Int
)

@Dao
interface WorkoutExerciseDao {
    @Query("""
    SELECT * FROM workout_exercises
    WHERE workoutDayId = :dayId
    ORDER BY orderIndex
""")
    fun getForDay(
        dayId: Long
    ): Flow<List<WorkoutExercise>>

    @Query("""
    SELECT * FROM workout_exercises
    WHERE workoutDayId = :dayId
    ORDER BY orderIndex
""")
    suspend fun getForDayOnce(
        dayId: Long
    ): List<WorkoutExercise>

    @Query(
        """
    SELECT
        we.id AS workoutExerciseId,

        e.id AS sourceExerciseId,
        e.historyKey AS exerciseHistoryKey,
        e.name AS exerciseName,
        e.trackingType AS trackingType,
        e.isBodyweight AS isBodyweight,
        e.isPerSide AS isPerSide,

        m.id AS sourcePrimaryMuscleId,
        m.historyKey AS primaryMuscleHistoryKey,
        m.name AS primaryMuscleName,

        we.orderIndex AS orderIndex,
        we.targetSets AS targetSets,
        we.targetReps AS targetReps,
        we.targetDurationSec AS targetDurationSec,
        we.defaultWeightKg AS defaultWeightKg,
        we.restBetweenSetsSec AS restBetweenSetsSec,
        we.restBeforeNextSec AS restBeforeNextSec

    FROM workout_exercises we

    INNER JOIN exercises e
        ON we.exerciseId = e.id

    INNER JOIN muscle_groups m
        ON e.primaryMuscleId = m.id

    WHERE we.workoutDayId = :dayId

    ORDER BY we.orderIndex
    """
    )
    suspend fun getSnapshotSourcesForDay(
        dayId: Long
    ): List<WorkoutExerciseSnapshotSource>


    @Query("""
    SELECT we.id AS id,
           we.exerciseId AS exerciseId,
           we.orderIndex AS orderIndex,
           e.name AS exerciseName,
           we.targetSets AS targetSets,
           we.targetReps AS targetReps,
           we.targetDurationSec AS targetDurationSec,
           we.restBetweenSetsSec AS restBetweenSetsSec,
           we.restBeforeNextSec AS restBeforeNextSec,
           we.defaultWeightKg AS defaultWeightKg,
           e.isPerSide AS isPerSide,
           e.trackingType AS trackingType
    FROM workout_exercises we
    JOIN exercises e ON we.exerciseId = e.id
    WHERE we.workoutDayId = :dayId
    ORDER BY we.orderIndex
""")
    fun getForDayWithNames(
        dayId: Long
    ): Flow<List<WorkoutExerciseWithName>>

    @Query(
        """
    SELECT COUNT(*)
    FROM workout_exercises
    WHERE workoutDayId = :dayId
    """
    )
    suspend fun getCountForDay(
        dayId: Long
    ): Int

    @Query("""
    SELECT we.id AS id,
           we.exerciseId AS exerciseId,
           we.orderIndex AS orderIndex,
           e.name AS exerciseName,
           we.targetSets AS targetSets,
           we.targetReps AS targetReps,
           we.targetDurationSec AS targetDurationSec,
           we.restBetweenSetsSec AS restBetweenSetsSec,
           we.restBeforeNextSec AS restBeforeNextSec,
           we.defaultWeightKg AS defaultWeightKg,
           e.isPerSide AS isPerSide,
           e.trackingType AS trackingType
    FROM workout_exercises we
    JOIN exercises e ON we.exerciseId = e.id
    WHERE we.id = :id
    LIMIT 1
""")
    fun getByIdWithName(
        id: Long
    ): Flow<WorkoutExerciseWithName?>

    @Query("""
    SELECT * FROM workout_exercises
    WHERE id = :id
    LIMIT 1
""")
    suspend fun getById(
        id: Long
    ): WorkoutExercise?

    @Query(
        """
    SELECT *
    FROM workout_exercises
    WHERE workoutDayId = :dayId
      AND exerciseId = :exerciseId
    LIMIT 1
    """
    )
    suspend fun getForExercise(
        dayId: Long,
        exerciseId: Long
    ): WorkoutExercise?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<WorkoutExercise>)

    @Insert
    suspend fun insert(item: WorkoutExercise): Long

    @Update
    suspend fun update(item: WorkoutExercise)

    @Delete
    suspend fun delete(item: WorkoutExercise)
}