package com.nexora.x.feature.servers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexora.x.core.model.Profile
import com.nexora.x.core.model.ProfileSortOrder
import com.nexora.x.core.model.ProfileType
import com.nexora.x.domain.repository.ProfileRepository
import com.nexora.x.domain.usecase.ManageProfilesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ServersUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val selectedProtocolFilter: ProfileType? = null,
    val sortOrder: ProfileSortOrder = ProfileSortOrder.RECENTLY_UPDATED,
    val filteredProfiles: List<Profile> = emptyList(),
    val selectedProfile: Profile? = null,
    val totalSavedCount: Int = 0,
    val pendingDeleteProfile: Profile? = null,
    val actionBannerMessage: String? = null
)

@HiltViewModel
class ServersViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val manageProfilesUseCase: ManageProfilesUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedProtocolFilter = MutableStateFlow<ProfileType?>(null)
    private val _pendingDeleteProfile = MutableStateFlow<Profile?>(null)
    private val _actionBanner = MutableStateFlow<String?>(null)
    private val _errorState = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ServersUiState> = combine(
        profileRepository.profiles,
        profileRepository.sortOrder,
        _searchQuery,
        _selectedProtocolFilter,
        combine(_pendingDeleteProfile, _actionBanner, _errorState) { del, banner, err ->
            Triple(del, banner, err)
        }
    ) { allProfiles, sortOrder, query, protoFilter, triple ->
        val (pendingDelete, banner, errorMsg) = triple

        val filtered = allProfiles.filter { p ->
            val matchesProtocol = protoFilter == null || p.protocol == protoFilter
            val matchesQuery = query.isBlank() ||
                p.displayName.contains(query, ignoreCase = true) ||
                p.serverAddress.contains(query, ignoreCase = true) ||
                p.tlsConfig.sni.contains(query, ignoreCase = true)
            matchesProtocol && matchesQuery
        }

        val sorted = when (sortOrder) {
            ProfileSortOrder.RECENTLY_UPDATED -> filtered.sortedByDescending { it.updatedAtEpochMillis }
            ProfileSortOrder.NAME_ASC -> filtered.sortedBy { it.displayName.lowercase() }
            ProfileSortOrder.PROTOCOL -> filtered.sortedBy { it.protocol.name }
            ProfileSortOrder.PORT_ASC -> filtered.sortedBy { it.port }
        }

        ServersUiState(
            isLoading = false,
            errorMessage = errorMsg,
            searchQuery = query,
            selectedProtocolFilter = protoFilter,
            sortOrder = sortOrder,
            filteredProfiles = sorted,
            selectedProfile = allProfiles.firstOrNull { it.isSelected },
            totalSavedCount = allProfiles.size,
            pendingDeleteProfile = pendingDelete,
            actionBannerMessage = banner
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ServersUiState(isLoading = true)
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onProtocolFilterSelected(protocol: ProfileType?) {
        _selectedProtocolFilter.value = protocol
    }

    fun onSortOrderSelected(order: ProfileSortOrder) {
        viewModelScope.launch {
            manageProfilesUseCase.updateSortOrder(order)
        }
    }

    fun onSelectProfile(profile: Profile) {
        viewModelScope.launch {
            manageProfilesUseCase.selectProfile(profile.id)
            _actionBanner.value = "Selected profile '${profile.displayName}' (${profile.protocol.displayName})."
        }
    }

    fun onDuplicateProfile(profile: Profile) {
        viewModelScope.launch {
            val copy = manageProfilesUseCase.duplicateProfile(profile.id)
            if (copy != null) {
                _actionBanner.value = "Duplicated '${profile.displayName}' as '${copy.displayName}'."
            }
        }
    }

    fun onRequestDeleteProfile(profile: Profile) {
        _pendingDeleteProfile.value = profile
    }

    fun onCancelDeleteProfile() {
        _pendingDeleteProfile.value = null
    }

    fun onConfirmDeleteProfile() {
        val target = _pendingDeleteProfile.value ?: return
        _pendingDeleteProfile.value = null
        viewModelScope.launch {
            manageProfilesUseCase.deleteProfile(target.id)
            _actionBanner.value = "Deleted profile '${target.displayName}'."
        }
    }

    fun buildShareableUri(profile: Profile): String {
        val uri = manageProfilesUseCase.exportUri(profile)
        _actionBanner.value = "Copied ${profile.protocol.displayName} share link for '${profile.displayName}' to clipboard."
        return uri
    }

    fun onDismissBanner() {
        _actionBanner.value = null
        _errorState.value = null
    }
}
