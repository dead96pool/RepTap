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

@Dao
interface WorkoutExerciseDao {
    @Query("""
    SELECT * FROM workout_exercises
    WHERE workoutDayId = :dayId
      AND isActive = 1
    ORDER BY orderIndex
""")
    fun getForDay(
        dayId: Long
    ): Flow<List<WorkoutExercise>>

    @Query("""
    SELECT * FROM workout_exercises
    WHERE workoutDayId = :dayId
      AND isActive = 1
    ORDER BY orderIndex
""")
    suspend fun getForDayOnce(
        dayId: Long
    ): List<WorkoutExercise>

    @Query("SELECT COUNT(*) FROM workout_exercises")
    suspend fun count(): Int

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
      AND we.isActive = 1
    ORDER BY we.orderIndex
""")
    fun getForDayWithNames(
        dayId: Long
    ): Flow<List<WorkoutExerciseWithName>>

    @Query("""
    SELECT COUNT(*)
    FROM workout_exercises
    WHERE workoutDayId = :dayId
      AND isActive = 1
""")
    suspend fun getActiveCountForDay(
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

    @Query("""
    SELECT * FROM workout_exercises
    WHERE workoutDayId = :dayId
      AND exerciseId = :exerciseId
      AND isActive = 1
    LIMIT 1
""")
    suspend fun getActiveForExercise(
        dayId: Long,
        exerciseId: Long
    ): WorkoutExercise?

    @Query("""
    SELECT * FROM workout_exercises
    WHERE workoutDayId = :dayId
      AND exerciseId = :exerciseId
      AND isActive = 0
    ORDER BY id DESC
    LIMIT 1
""")
    suspend fun getInactiveForExercise(
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