package com.chani.reptap.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutDay::class,
            parentColumns = ["id"],
            childColumns = ["workoutDayId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("workoutDayId"), Index("exerciseId")]
)
data class WorkoutExercise(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workoutDayId: Long,
    val exerciseId: Long,
    val orderIndex: Int,
    val targetSets: Int,
    val repRangeMin: Int,
    val repRangeMax: Int,
    val restBetweenSetsSec: Int,
    val restBeforeNextSec: Int,
    val defaultWeightKg: Float = 5.0f
)