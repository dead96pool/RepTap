package com.chani.reptap.data.local.dao

import androidx.room.*
import com.chani.reptap.data.local.entity.WorkoutDay
import kotlinx.coroutines.flow.Flow

data class WorkoutDaySlot(
    val id: Long,
    val slotNumber: Int
)

@Dao
interface WorkoutDayDao {
    @Query("SELECT * FROM workout_days ORDER BY slotNumber")
    fun getAll(): Flow<List<WorkoutDay>>

    @Query("SELECT * FROM workout_days WHERE slotNumber = :slot LIMIT 1")
    suspend fun getBySlot(slot: Int): WorkoutDay?

    @Query("""
    SELECT * FROM workout_days
    ORDER BY slotNumber
""")
    suspend fun getAllOnce(): List<WorkoutDay>

    @Query("SELECT id, slotNumber FROM workout_days ORDER BY slotNumber")
    suspend fun getIdToSlotMap(): List<WorkoutDaySlot>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(days: List<WorkoutDay>)

    @Insert
    suspend fun insert(
        day: WorkoutDay
    ): Long

    @Query("""
    SELECT * FROM workout_days
    WHERE id = :id
    LIMIT 1
""")
    suspend fun getById(
        id: Long
    ): WorkoutDay?

    @Update
    suspend fun update(
        day: WorkoutDay
    )
}