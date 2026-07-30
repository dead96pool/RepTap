package com.chani.reptap.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_days")
data class WorkoutDay(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val slotNumber: Int,
    val isRecovery: Boolean = false
)