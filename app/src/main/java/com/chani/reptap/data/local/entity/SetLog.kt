package com.chani.reptap.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "set_logs",
    foreignKeys = [
        ForeignKey(
            entity = SessionExerciseLog::class,
            parentColumns = ["id"],
            childColumns = ["sessionExerciseLogId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("sessionExerciseLogId")
    ]
)
data class SetLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /*
     * Every performed set belongs to the immutable
     * SessionExerciseLog captured when this workout began.
     *
     * The parent SessionLog is obtained through:
     *
     * SetLog
     *   -> SessionExerciseLog
     *   -> SessionLog
     */
    val sessionExerciseLogId: Long,

    val setNumber: Int,
    val repsCompleted: Int,
    val durationCompletedSec: Int? = null,
    val weightKg: Float?,
    val timestamp: Long
)