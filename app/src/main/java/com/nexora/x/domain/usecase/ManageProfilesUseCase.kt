package com.nexora.x.domain.usecase

import com.nexora.x.core.model.Profile
import com.nexora.x.core.model.ProfileSortOrder
import com.nexora.x.domain.repository.ProfileRepository
import javax.inject.Inject

/**
 * Use case encapsulating profile selection, duplication, deletion, and URI export.
 */
class ManageProfilesUseCase @Inject constructor(
    private val profileRepository: ProfileRepository
) {
    suspend fun selectProfile(profileId: String) {
        profileRepository.selectProfile(profileId)
    }

    suspend fun duplicateProfile(profileId: String): Profile? {
        return profileRepository.duplicateProfile(profileId)
    }

    suspend fun deleteProfile(profileId: String) {
        profileRepository.deleteProfile(profileId)
    }

    suspend fun updateSortOrder(order: ProfileSortOrder) {
        profileRepository.setSortOrder(order)
    }

    fun exportUri(profile: Profile): String {
        return profileRepository.exportProfileUri(profile)
    }
}
