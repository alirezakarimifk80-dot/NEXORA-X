package com.nexora.x.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [ProfileEntity] CRUD operations.
 */
@Dao
interface ProfileDao {

    @Query("SELECT * FROM profiles ORDER BY updated_at DESC")
    fun observeAllProfiles(): Flow<List<ProfileEntity>>

    @Query("SELECT COUNT(*) FROM profiles")
    fun observeProfileCount(): Flow<Int>

    @Query("SELECT * FROM profiles WHERE id = :profileId LIMIT 1")
    suspend fun getProfileById(profileId: String): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(entity: ProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfiles(entities: List<ProfileEntity>)

    @Update
    suspend fun updateProfile(entity: ProfileEntity)

    @Query("DELETE FROM profiles WHERE id = :profileId")
    suspend fun deleteProfileById(profileId: String)

    @Query("DELETE FROM profiles")
    suspend fun deleteAllProfiles()
}
