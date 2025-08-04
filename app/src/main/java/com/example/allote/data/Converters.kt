package com.example.allote.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromApplicationType(value: ApplicationType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toApplicationType(value: String?): ApplicationType? {
        return value?.let { ApplicationType.valueOf(it) }
    }

    @TypeConverter
    fun fromAprobacionStatus(value: AprobacionStatus?): String? {
        return value?.name
    }

    @TypeConverter
    fun toAprobacionStatus(value: String?): AprobacionStatus? {
        return value?.let { AprobacionStatus.valueOf(it) }
    }
}
