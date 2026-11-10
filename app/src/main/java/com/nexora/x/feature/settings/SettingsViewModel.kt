package com.nexora.x.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexora.x.domain.model.SettingsCategory
import com.nexora.x.domain.model.SettingsOption
import com.nexora.x.domain.repository.NexoraRepository
import com.nexora.x.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val groupedSettings: Map<SettingsCategory, List<SettingsOption>> = emptyMap(),
    val isDarkTheme: Boolean = true,
    val actionBannerMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: NexoraRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        repository.settingsOptions,
        profileRepository.isDarkTheme,
        repository.lastActionBanner
    ) { options, isDark, banner ->
        val updatedOptions = options.map { opt ->
            if (opt.id == "theme") {
                opt.copy(currentValue = if (isDark) "Nexora Dark" else "Nexora Light")
            } else {
                opt
            }
        }
        SettingsUiState(
            groupedSettings = updatedOptions.groupBy { it.category },
            isDarkTheme = isDark,
            actionBannerMessage = banner
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    fun onSettingClicked(option: SettingsOption) {
        if (option.id == "theme") {
            viewModelScope.launch {
                val nextTheme = !uiState.value.isDarkTheme
                profileRepository.setDarkTheme(nextTheme)
            }
        } else {
            repository.onSettingClicked(option)
        }
    }

    fun onDismissBanner() {
        repository.dismissActionBanner()
    }
}
