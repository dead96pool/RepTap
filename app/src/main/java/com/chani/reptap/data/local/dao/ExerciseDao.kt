package com.chani.reptap.data.local.dao

import androidx.room.*
import com.chani.reptap.data.local.entity.Exercise
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    @Query("SELECT * FROM exercises ORDER BY name")
    fun getAll(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises")
    suspend fun getAllOnce(): List<Exercise>

    @Query(
        """
        SELECT *
        FROM exercises
        WHERE id = :id
        LIMIT 1
        """
    )
    suspend fun getById(
        id: Long
    ): Exercise?

    @Query(
        """
        SELECT *
        FROM exercises
        WHERE historyKey = :historyKey
        LIMIT 1
        """
    )
    suspend fun getByHistoryKey(
        historyKey: String
    ): Exercise?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(
        exercises: List<Exercise>
    )

    @Insert
    suspend fun insert(
        exercise: Exercise
    ): Long

    @Update
    suspend fun update(
        exercise: Exercise
    )

    @Delete
    suspend fun delete(
        exercise: Exercise
    )
}