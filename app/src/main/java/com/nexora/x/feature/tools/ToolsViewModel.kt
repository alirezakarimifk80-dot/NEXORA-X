package com.nexora.x.feature.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexora.x.core.engine.EngineDiagnosticsSnapshot
import com.nexora.x.core.engine.EngineRepository
import com.nexora.x.core.model.Profile
import com.nexora.x.domain.model.NetworkToolItem
import com.nexora.x.domain.repository.NexoraRepository
import com.nexora.x.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ToolsUiState(
    val engineDiagnostics: EngineDiagnosticsSnapshot = EngineDiagnosticsSnapshot(),
    val selectedProfile: Profile? = null,
    val generatedConfigPreviewRedacted: String? = null,
    val tools: List<NetworkToolItem> = emptyList(),
    val actionBannerMessage: String? = null
)

@HiltViewModel
class ToolsViewModel @Inject constructor(
    private val nexoraRepository: NexoraRepository,
    private val engineRepository: EngineRepository,
    profileRepository: ProfileRepository
) : ViewModel() {

    private val _configPreviewRedacted = MutableStateFlow<String?>(null)
    private val _localBanner = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ToolsUiState> = combine(
        engineRepository.diagnostics,
        profileRepository.selectedProfile,
        nexoraRepository.networkTools,
        _configPreviewRedacted,
        combine(nexoraRepository.lastActionBanner, _localBanner) { b1, b2 -> b2 ?: b1 }
    ) { diag, selectedProfile, tools, preview, banner ->
        ToolsUiState(
            engineDiagnostics = diag,
            selectedProfile = selectedProfile,
            generatedConfigPreviewRedacted = preview,
            tools = tools,
            actionBannerMessage = banner
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ToolsUiState()
    )

    fun onValidateSelectedProfileClicked() {
        val profile = uiState.value.selectedProfile
        if (profile == null) {
            _localBanner.value = "No profile selected. Add or select a profile on the Servers screen first."
            return
        }
        val result = engineRepository.validateProfileConfiguration(profile)
        result.fold(
            onSuccess = { generated ->
                _configPreviewRedacted.value = generated.redactedSummary
                _localBanner.value = "Configuration validated for '${profile.displayName}'."
            },
            onFailure = { err ->
                _configPreviewRedacted.value = "INVALID: ${err.message}"
                _localBanner.value = "Validation failed: ${err.message}"
            }
        )
    }

    fun onToolClicked(toolId: String) {
        nexoraRepository.inspectTool(toolId)
    }

    fun onDismissBanner() {
        _localBanner.value = null
        nexoraRepository.dismissActionBanner()
    }
}
