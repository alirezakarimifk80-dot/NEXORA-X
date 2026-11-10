package com.nexora.x.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Room Database for NEXORA X Phase 2 profile management.
 */
@Database(
    entities = [ProfileEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(ProfileTypeConverters::class)
abstract class ProfileDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao

    companion object {
        const val DATABASE_NAME = "nexora_profiles.db"
    }
}
