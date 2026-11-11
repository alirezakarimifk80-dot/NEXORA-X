package com.nexora.x.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexora.x.domain.model.SettingsCategory
import com.nexora.x.domain.model.SettingsOption
import com.nexora.x.domain.repository.NexoraRepository
import com.nexora.x.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val groupedSettings: Map<SettingsCategory, List<SettingsOption>> = emptyMap(),
    val isDarkTheme: Boolean = true,
    val hasAcceptedVpnDisclosure: Boolean = false,
    val showVpnDisclosureDialog: Boolean = false,
    val actionBannerMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: NexoraRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _showDisclosureDialog = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(
        repository.settingsOptions,
        profileRepository.isDarkTheme,
        profileRepository.hasAcceptedVpnDisclosure,
        _showDisclosureDialog,
        repository.lastActionBanner
    ) { options, isDark, acceptedDisclosure, showModal, banner ->
        val updatedOptions = options.map { opt ->
            when (opt.id) {
                "theme" -> opt.copy(currentValue = if (isDark) "Nexora Dark" else "Nexora Light")
                "vpn_disclosure" -> opt.copy(
                    currentValue = if (acceptedDisclosure) "Accepted · Tap to Review" else "Tap to Review"
                )
                else -> opt
            }
        }
        SettingsUiState(
            groupedSettings = updatedOptions.groupBy { it.category },
            isDarkTheme = isDark,
            hasAcceptedVpnDisclosure = acceptedDisclosure,
            showVpnDisclosureDialog = showModal,
            actionBannerMessage = banner
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    fun onSettingClicked(option: SettingsOption) {
        when (option.id) {
            "theme" -> {
                viewModelScope.launch {
                    val nextTheme = !uiState.value.isDarkTheme
                    profileRepository.setDarkTheme(nextTheme)
                }
            }
            "vpn_disclosure" -> {
                _showDisclosureDialog.value = true
            }
            else -> {
                repository.onSettingClicked(option)
            }
        }
    }

    fun onDismissVpnDisclosure() {
        _showDisclosureDialog.value = false
    }

    fun onDismissBanner() {
        repository.dismissActionBanner()
    }
}
