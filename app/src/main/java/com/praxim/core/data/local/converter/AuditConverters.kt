package com.praxim.core.data.local.converter

import androidx.room.TypeConverter
import com.praxim.core.data.local.entity.EntityType

class AuditConverters {
    @TypeConverter
    fun fromEntityType(value: EntityType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toEntityType(value: String?): EntityType? {
        return try {
            value?.let { EntityType.valueOf(it) }
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}
