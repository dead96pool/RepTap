package com.chani.reptap.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cycle_state")
data class CycleState(
    @PrimaryKey
    val id: Int = 1,
    val currentSlot: Int,
    val scheduledEpochDay: Long
)