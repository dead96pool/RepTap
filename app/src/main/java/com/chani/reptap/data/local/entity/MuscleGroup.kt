package com.chani.reptap.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "muscle_groups",
    indices = [
        Index(
            value = ["historyKey"],
            unique = true
        )
    ]
)
data class MuscleGroup(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /*
     * Permanent logical identity for historical
     * muscle classification.
     */
    val historyKey: String,

    val name: String
)