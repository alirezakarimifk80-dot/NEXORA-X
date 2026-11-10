package com.nexora.x.core.database

import androidx.room.TypeConverter
import com.nexora.x.core.model.ProfileType

/**
 * Room TypeConverters for [ProfileType].
 */
class ProfileTypeConverters {

    @TypeConverter
    fun fromProfileType(type: ProfileType): String = type.name

    @TypeConverter
    fun toProfileType(value: String): ProfileType =
        runCatching { ProfileType.valueOf(value) }.getOrDefault(ProfileType.VLESS)
}
