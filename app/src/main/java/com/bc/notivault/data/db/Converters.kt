package com.bc.notivault.data.db

import androidx.room.TypeConverter
import com.bc.notivault.data.model.InteractionStatus

class Converters {
    @TypeConverter
    fun fromInteractionStatus(status: InteractionStatus?): String {
        return status?.name ?: InteractionStatus.UNKNOWN.name
    }

    @TypeConverter
    fun toInteractionStatus(value: String?): InteractionStatus {
        return try {
            if (value != null) InteractionStatus.valueOf(value) else InteractionStatus.UNKNOWN
        } catch (_: IllegalArgumentException) {
            InteractionStatus.UNKNOWN
        }
    }
}
