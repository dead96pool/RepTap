package com.chani.reptap.data.local.db

import androidx.room.TypeConverter
import com.chani.reptap.data.local.entity.ExerciseTrackingType
import com.chani.reptap.data.local.entity.SessionStatus

class Converters {
    @TypeConverter
    fun fromStatus(status: SessionStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): SessionStatus = SessionStatus.valueOf(value)

    @TypeConverter
    fun fromExerciseTrackingType(type: ExerciseTrackingType): String =
        type.name

    @TypeConverter
    fun toExerciseTrackingType(value: String): ExerciseTrackingType =
        ExerciseTrackingType.valueOf(value)
}