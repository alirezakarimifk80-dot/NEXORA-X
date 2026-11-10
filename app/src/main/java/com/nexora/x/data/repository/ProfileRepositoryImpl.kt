package com.nexora.x.data.repository

import com.nexora.x.core.database.NexoraPreferencesDataSource
import com.nexora.x.core.database.ProfileDao
import com.nexora.x.core.database.ProfileMapper
import com.nexora.x.core.model.BatchImportResult
import com.nexora.x.core.model.ParseResult
import com.nexora.x.core.model.Profile
import com.nexora.x.core.model.ProfileSortOrder
import com.nexora.x.domain.repository.ProfileRepository
import com.nexora.x.feature.profile.parser.ProfileImportEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production Phase 2 implementation of [ProfileRepository].
 * Coordinates Room database persistence ([ProfileDao]), DataStore preferences
 * ([NexoraPreferencesDataSource]), and the multi-protocol [ProfileImportEngine].
 *
 * Security Invariant:
 * Never logs credentials, UUIDs, passwords, or full configuration URIs.
 */
@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val profileDao: ProfileDao,
    private val preferencesDataSource: NexoraPreferencesDataSource,
    private val importEngine: ProfileImportEngine
) : ProfileRepository {

    override val selectedProfileId: Flow<String?> = preferencesDataSource.selectedProfileId

    override val sortOrder: Flow<ProfileSortOrder> = preferencesDataSource.sortOrder

    override val isDarkTheme: Flow<Boolean> = preferencesDataSource.isDarkTheme

    override val totalProfileCount: Flow<Int> = profileDao.observeProfileCount()

    override val profiles: Flow<List<Profile>> = combine(
        profileDao.observeAllProfiles(),
        preferencesDataSource.selectedProfileId
    ) { entities, selectedId ->
        val effectiveSelectedId = when {
            selectedId != null && entities.any { it.id == selectedId } -> selectedId
            entities.isNotEmpty() -> entities.first().id
            else -> null
        }
        entities.map { entity ->
            ProfileMapper.toDomain(entity, effectiveSelectedId)
        }
    }

    override val selectedProfile: Flow<Profile?> = profiles.combine(selectedProfileId) { list, _ ->
        list.firstOrNull { it.isSelected } ?: list.firstOrNull()
    }

    override suspend fun getProfileById(profileId: String): Profile? {
        val entity = profileDao.getProfileById(profileId) ?: return null
        val selectedId = preferencesDataSource.selectedProfileId.firstOrNull()
        return ProfileMapper.toDomain(entity, selectedId)
    }

    override suspend fun saveProfile(profile: Profile) {
        val now = System.currentTimeMillis()
        val updated = profile.copy(updatedAtEpochMillis = now)
        profileDao.upsertProfile(ProfileMapper.toEntity(updated))

        val currentSelected = preferencesDataSource.selectedProfileId.firstOrNull()
        if (currentSelected.isNullOrBlank() || profile.isSelected) {
            preferencesDataSource.setSelectedProfileId(updated.id)
        }
    }

    override suspend fun deleteProfile(profileId: String) {
        profileDao.deleteProfileById(profileId)
        val currentSelected = preferencesDataSource.selectedProfileId.firstOrNull()
        if (currentSelected == profileId) {
            val remaining = profileDao.observeAllProfiles().firstOrNull().orEmpty()
            preferencesDataSource.setSelectedProfileId(remaining.firstOrNull()?.id)
        }
    }

    override suspend fun duplicateProfile(profileId: String): Profile? {
        val existing = getProfileById(profileId) ?: return null
        val now = System.currentTimeMillis()
        val duplicated = existing.copy(
            id = UUID.randomUUID().toString(),
            displayName = "${existing.displayName} (Copy)",
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
            isSelected = false
        )
        profileDao.upsertProfile(ProfileMapper.toEntity(duplicated))
        return duplicated
    }

    override suspend fun selectProfile(profileId: String?) {
        preferencesDataSource.setSelectedProfileId(profileId)
    }

    override suspend fun setSortOrder(order: ProfileSortOrder) {
        preferencesDataSource.setSortOrder(order)
    }

    override suspend fun setDarkTheme(isDark: Boolean) {
        preferencesDataSource.setDarkTheme(isDark)
    }

    override fun parseLink(rawUri: String): ParseResult {
        return importEngine.parseSingleLink(rawUri)
    }

    override suspend fun importFromText(rawText: String): BatchImportResult {
        val batchResult = importEngine.parsePlainTextBatch(rawText)
        if (batchResult.importedProfiles.isNotEmpty()) {
            val entities = batchResult.importedProfiles.map { ProfileMapper.toEntity(it) }
            profileDao.upsertProfiles(entities)

            val currentSelected = preferencesDataSource.selectedProfileId.firstOrNull()
            if (currentSelected.isNullOrBlank()) {
                preferencesDataSource.setSelectedProfileId(batchResult.importedProfiles.first().id)
            }
        }
        return batchResult
    }

    override fun exportProfileUri(profile: Profile): String {
        return importEngine.exportToUri(profile)
    }
}
