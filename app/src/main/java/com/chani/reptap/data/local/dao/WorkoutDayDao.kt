package com.chani.reptap.data.local.dao

import androidx.room.*
import com.chani.reptap.data.local.entity.WorkoutDay
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDayDao {
    @Query("SELECT * FROM workout_days ORDER BY slotNumber")
    fun getAll(): Flow<List<WorkoutDay>>

    @Query("SELECT * FROM workout_days WHERE slotNumber = :slot LIMIT 1")
    suspend fun getBySlot(slot: Int): WorkoutDay?

    @Query("SELECT * FROM workout_days")
    suspend fun getAllOnce(): List<WorkoutDay>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(days: List<WorkoutDay>)
}