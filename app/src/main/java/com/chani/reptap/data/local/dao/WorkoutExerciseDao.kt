package com.chani.reptap.data.local.dao

import androidx.room.*
import com.chani.reptap.data.local.entity.WorkoutExercise
import kotlinx.coroutines.flow.Flow

data class WorkoutExerciseWithName(
    val id: Long,
    val orderIndex: Int,
    val exerciseName: String,
    val targetSets: Int,
    val repRangeMin: Int,
    val repRangeMax: Int,
    val targetDurationMinSec: Int?,
    val targetDurationMaxSec: Int?,
    val restBetweenSetsSec: Int,
    val restBeforeNextSec: Int,
    val defaultWeightKg: Float?,
    val isPerSide: Boolean,
    val trackingType: com.chani.reptap.data.local.entity.ExerciseTrackingType
)

@Dao
interface WorkoutExerciseDao {
    @Query("SELECT * FROM workout_exercises WHERE workoutDayId = :dayId ORDER BY orderIndex")
    fun getForDay(dayId: Long): Flow<List<WorkoutExercise>>

    @Query("SELECT COUNT(*) FROM workout_exercises")
    suspend fun count(): Int

    @Query("""
    SELECT we.id AS id, we.orderIndex AS orderIndex, e.name AS exerciseName,
           we.targetSets AS targetSets, we.repRangeMin AS repRangeMin, we.repRangeMax AS repRangeMax,
           we.targetDurationMinSec AS targetDurationMinSec,
           we.targetDurationMaxSec AS targetDurationMaxSec,
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
    fun getForDayWithNames(dayId: Long): Flow<List<WorkoutExerciseWithName>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<WorkoutExercise>)

    @Insert
    suspend fun insert(item: WorkoutExercise): Long

    @Update
    suspend fun update(item: WorkoutExercise)

    @Delete
    suspend fun delete(item: WorkoutExercise)
}