package com.chani.reptap.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "set_logs",
    foreignKeys = [
        ForeignKey(
            entity = SessionLog::class,
            parentColumns = ["id"],
            childColumns = ["sessionLogId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = WorkoutExercise::class,
            parentColumns = ["id"],
            childColumns = ["workoutExerciseId"],
            onDelete = ForeignKey.RESTRICT
        )
    ]
)
data class SetLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionLogId: Long,
    val workoutExerciseId: Long,
    val setNumber: Int,
    val repsCompleted: Int,
    val weightKg: Float,
    val timestamp: Long
)