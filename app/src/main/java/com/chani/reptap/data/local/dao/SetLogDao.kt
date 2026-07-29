package com.chani.reptap.data.local.dao

import androidx.room.*
import com.chani.reptap.data.local.entity.SetLog
import kotlinx.coroutines.flow.Flow

data class MuscleProgress(
    val muscleName: String,
    val totalReps: Int,
    val totalSets: Int,
    val avgWeight: Float
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
}