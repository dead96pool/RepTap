package com.chani.reptap.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SessionStatus { IN_PROGRESS, COMPLETED, QUIT }

@Entity(tableName = "session_logs")
data class SessionLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workoutDayId: Long,
    val startedAt: Long,
    val finishedAt: Long? = null,
    val status: SessionStatus = SessionStatus.IN_PROGRESS
)