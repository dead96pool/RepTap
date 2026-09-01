package com.chani.reptap.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ExerciseTrackingType {
    REPS,
    TIME
}

@Entity(
    tableName = "exercises",
    foreignKeys = [
        ForeignKey(
            entity = MuscleGroup::class,
            parentColumns = ["id"],
            childColumns = ["primaryMuscleId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("primaryMuscleId")]
)
data class Exercise(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val primaryMuscleId: Long,
    val isBodyweight: Boolean = false,
    val isPerSide: Boolean = false,
    val trackingType: ExerciseTrackingType = ExerciseTrackingType.REPS
)