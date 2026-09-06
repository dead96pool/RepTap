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
    indices = [
        Index("primaryMuscleId"),
        Index(
            value = ["historyKey"],
            unique = true
        )
    ]
)
data class Exercise(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /*
     * Permanent logical identity.
     *
     * Unlike the Room row ID, this value belongs to the
     * logical exercise and survives future delete/restore
     * workflows.
     *
     * Built-in exercises use deterministic keys.
     * Future custom exercises will use generated UUIDs.
     */
    val historyKey: String,

    val name: String,
    val primaryMuscleId: Long,
    val isBodyweight: Boolean = false,
    val isPerSide: Boolean = false,
    val trackingType: ExerciseTrackingType =
        ExerciseTrackingType.REPS
)