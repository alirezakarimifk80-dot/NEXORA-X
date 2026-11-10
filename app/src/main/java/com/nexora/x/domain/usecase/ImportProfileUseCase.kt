package com.nexora.x.domain.usecase

import com.nexora.x.core.model.BatchImportResult
import com.nexora.x.core.model.ParseResult
import com.nexora.x.core.model.Profile
import com.nexora.x.domain.repository.ProfileRepository
import javax.inject.Inject

/**
 * Use case encapsulating profile link validation and batch plain-text / clipboard import.
 */
class ImportProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository
) {
    fun previewSingleLink(rawUri: String): ParseResult {
        return profileRepository.parseLink(rawUri)
    }

    suspend fun importBatch(rawText: String): BatchImportResult {
        return profileRepository.importFromText(rawText)
    }

    suspend fun saveValidatedProfile(profile: Profile) {
        profileRepository.saveProfile(profile)
    }
}
