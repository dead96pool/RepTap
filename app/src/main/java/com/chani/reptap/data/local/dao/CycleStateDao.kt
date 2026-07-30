package com.chani.reptap.data.local.dao

import androidx.room.*
import com.chani.reptap.data.local.entity.CycleState
import kotlinx.coroutines.flow.Flow

@Dao
interface CycleStateDao {
    @Query("SELECT * FROM cycle_state WHERE id = 1")
    fun observe(): Flow<CycleState?>

    @Query("SELECT * FROM cycle_state WHERE id = 1")
    suspend fun getOnce(): CycleState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: CycleState)
}