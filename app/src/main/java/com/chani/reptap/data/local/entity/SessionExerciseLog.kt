package com.chani.reptap.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "session_exercise_logs",
    foreignKeys = [
        ForeignKey(
            entity = SessionLog::class,
            parentColumns = ["id"],
            childColumns = ["sessionLogId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        /*
         * A session must have one unambiguous historical
         * exercise order.
         *
         * This composite index also covers sessionLogId for
         * the foreign-key lookup.
         */
        Index(
            value = [
                "sessionLogId",
                "orderIndex"
            ],
            unique = true
        ),

        /*
         * Progress will eventually group historical
         * performance by this permanent logical identity.
         */
        Index("exerciseHistoryKey"),

        /*
         * Future muscle analytics can group historical
         * performance without depending on the current
         * MuscleGroup row.
         */
        Index("primaryMuscleHistoryKey")
    ]
)
data class SessionExerciseLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /*
     * The historical workout session this exercise belongs to.
     */
    val sessionLogId: Long,

    /*
     * Soft reference to the current Exercise Room row.
     *
     * Intentionally NOT a foreign key.
     *
     * The current Exercise may eventually be deleted while
     * this historical record must survive.
     */
    val sourceExerciseId: Long?,

    /*
     * Permanent logical exercise identity.
     *
     * This is what will allow Progress to continue across
     * exercise deletion and later restoration.
     */
    val exerciseHistoryKey: String,

    /*
     * Immutable exercise definition snapshots.
     */
    val exerciseNameSnapshot: String,
    val trackingTypeSnapshot: ExerciseTrackingType,
    val isBodyweightSnapshot: Boolean,
    val isPerSideSnapshot: Boolean,

    /*
     * Historical muscle identity.
     *
     * sourcePrimaryMuscleId is deliberately only a soft
     * reference. The permanent key and name snapshot are
     * what make the historical record independent.
     */
    val sourcePrimaryMuscleId: Long?,
    val primaryMuscleHistoryKey: String,
    val primaryMuscleNameSnapshot: String,

    /*
     * Workout configuration at the moment the session began.
     */
    val orderIndex: Int,
    val targetSets: Int,
    val targetReps: Int,
    val targetDurationSec: Int?,
    val defaultWeightKg: Float?,
    val restBetweenSetsSec: Int,
    val restBeforeNextSec: Int
)