package com.chani.reptap.data.local.dao

import androidx.room.*
import com.chani.reptap.data.local.entity.MuscleGroup
import kotlinx.coroutines.flow.Flow

@Dao
interface MuscleGroupDao {

    @Query("SELECT * FROM muscle_groups ORDER BY name")
    fun getAll(): Flow<List<MuscleGroup>>

    @Query("SELECT * FROM muscle_groups")
    suspend fun getAllOnce(): List<MuscleGroup>

    @Query(
        """
        SELECT *
        FROM muscle_groups
        WHERE id = :id
        LIMIT 1
        """
    )
    suspend fun getById(
        id: Long
    ): MuscleGroup?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(
        groups: List<MuscleGroup>
    )
}