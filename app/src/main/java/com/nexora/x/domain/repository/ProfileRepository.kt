package com.nexora.x.domain.repository

import com.nexora.x.core.model.BatchImportResult
import com.nexora.x.core.model.ParseResult
import com.nexora.x.core.model.Profile
import com.nexora.x.core.model.ProfileSortOrder
import kotlinx.coroutines.flow.Flow

/**
 * Clean Architecture repository contract for real Phase 2 Profile Management.
 * Backed by Room ([com.nexora.x.core.database.ProfileDao]) and DataStore Preferences.
 */
interface ProfileRepository {
    val profiles: Flow<List<Profile>>
    val totalProfileCount: Flow<Int>
    val selectedProfile: Flow<Profile?>
    val selectedProfileId: Flow<String?>
    val sortOrder: Flow<ProfileSortOrder>
    val isDarkTheme: Flow<Boolean>
    val hasAcceptedVpnDisclosure: Flow<Boolean>

    suspend fun getProfileById(profileId: String): Profile?
    suspend fun saveProfile(profile: Profile)
    suspend fun deleteProfile(profileId: String)
    suspend fun duplicateProfile(profileId: String): Profile?
    suspend fun selectProfile(profileId: String?)
    suspend fun setSortOrder(order: ProfileSortOrder)
    suspend fun setDarkTheme(isDark: Boolean)
    suspend fun setVpnDisclosureAccepted(accepted: Boolean)

    fun parseLink(rawUri: String): ParseResult
    suspend fun importFromText(rawText: String): BatchImportResult
    fun exportProfileUri(profile: Profile): String
}
