package com.chani.reptap.data.local.db

import androidx.room.TypeConverter
import com.chani.reptap.data.local.entity.SessionStatus

class Converters {
    @TypeConverter
    fun fromStatus(status: SessionStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): SessionStatus = SessionStatus.valueOf(value)
}