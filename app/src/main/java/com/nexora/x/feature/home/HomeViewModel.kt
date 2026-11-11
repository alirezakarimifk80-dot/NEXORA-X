package com.nexora.x.feature.home

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexora.x.core.engine.EngineDiagnosticsSnapshot
import com.nexora.x.core.engine.EngineRepository
import com.nexora.x.core.engine.EngineServiceController
import com.nexora.x.core.engine.EngineState
import com.nexora.x.core.engine.LatencyCheckResult
import com.nexora.x.core.engine.NetworkLatencyChecker
import com.nexora.x.core.engine.service.VpnNotificationManager
import com.nexora.x.core.model.Profile
import com.nexora.x.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val engineState: EngineState = EngineState.DISCONNECTED,
    val diagnostics: EngineDiagnosticsSnapshot = EngineDiagnosticsSnapshot(),
    val selectedProfile: Profile? = null,
    val totalSavedProfiles: Int = 0,
    val sessionDurationFormatted: String = "00:00:00",
    val latencyFormatted: String = "N/A",
    val latencyMillis: Long? = null,
    val isCheckingLatency: Boolean = false,
    val downloadSpeedFormatted: String = "N/A",
    val uploadSpeedFormatted: String = "N/A",
    val totalDownloadedFormatted: String = "N/A",
    val totalUploadedFormatted: String = "N/A",
    val actionBannerMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val engineRepository: EngineRepository,
    private val profileRepository: ProfileRepository,
    private val networkLatencyChecker: NetworkLatencyChecker
) : ViewModel() {

    private val _actionBanner = MutableStateFlow<String?>(null)
    private val _latencyState = MutableStateFlow<LatencyCheckResult?>(null)
    private var manualPingJob: Job? = null

    private val _vpnConsentIntentEvents = MutableSharedFlow<Intent>(extraBufferCapacity = 1)
    val vpnConsentIntentEvents: SharedFlow<Intent> = _vpnConsentIntentEvents.asSharedFlow()

    init {
        startPeriodicLatencyMonitor()
    }

    // Emits current epoch time every second so session duration updates ONLY while tunnel is active
    private val tickerFlow = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1_000L)
        }
    }

    private val auxiliaryUiFlow = combine(
        _actionBanner,
        tickerFlow,
        _latencyState
    ) { banner, nowMillis, latency ->
        Triple(banner, nowMillis, latency)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        engineRepository.engineState,
        engineRepository.diagnostics,
        profileRepository.selectedProfile,
        profileRepository.totalProfileCount,
        auxiliaryUiFlow
    ) { state, diag, selectedProfile, totalCount, (banner, nowMillis, latency) ->
        val durationStr = if (state.isTunnelRunning && diag.connectedSinceEpochMillis != null) {
            val elapsedSec = ((nowMillis - diag.connectedSinceEpochMillis) / 1000L).coerceAtLeast(0L)
            val hours = elapsedSec / 3600L
            val mins = (elapsedSec % 3600L) / 60L
            val secs = elapsedSec % 60L
            String.format("%02d:%02d:%02d", hours, mins, secs)
        } else {
            "00:00:00"
        }

        val matchingLatency = if (selectedProfile != null && latency?.profileId == selectedProfile.id) {
            latency
        } else {
            null
        }

        val stats = diag.trafficStats
        HomeUiState(
            engineState = state,
            diagnostics = diag,
            selectedProfile = selectedProfile,
            totalSavedProfiles = totalCount,
            sessionDurationFormatted = durationStr,
            latencyFormatted = matchingLatency?.formatForDisplay() ?: "N/A",
            latencyMillis = matchingLatency?.latencyMillis,
            isCheckingLatency = matchingLatency?.isChecking == true,
            downloadSpeedFormatted = VpnNotificationManager.formatBytesPerSec(stats.downlinkSpeedBytesPerSec),
            uploadSpeedFormatted = VpnNotificationManager.formatBytesPerSec(stats.uplinkSpeedBytesPerSec),
            totalDownloadedFormatted = VpnNotificationManager.formatByteCount(stats.downlinkTotalBytes),
            totalUploadedFormatted = VpnNotificationManager.formatByteCount(stats.uplinkTotalBytes),
            actionBannerMessage = banner ?: diag.lastRedactedError
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    private fun startPeriodicLatencyMonitor() {
        viewModelScope.launch {
            profileRepository.selectedProfile.collectLatest { profile ->
                if (profile == null) {
                    _latencyState.value = null
                    return@collectLatest
                }
                while (isActive) {
                    _latencyState.value = LatencyCheckResult(
                        profileId = profile.id,
                        host = profile.serverAddress,
                        port = profile.port,
                        latencyMillis = _latencyState.value?.takeIf { it.profileId == profile.id }?.latencyMillis,
                        isChecking = true
                    )
                    val result = networkLatencyChecker.checkProfileLatency(
                        profileId = profile.id,
                        host = profile.serverAddress,
                        port = profile.port
                    )
                    _latencyState.value = result
                    delay(NetworkLatencyChecker.DEFAULT_POLL_INTERVAL_MS)
                }
            }
        }
    }

    fun onRefreshLatencyClicked() {
        val profile = uiState.value.selectedProfile ?: return
        manualPingJob?.cancel()
        manualPingJob = viewModelScope.launch {
            _latencyState.value = LatencyCheckResult(
                profileId = profile.id,
                host = profile.serverAddress,
                port = profile.port,
                latencyMillis = _latencyState.value?.takeIf { it.profileId == profile.id }?.latencyMillis,
                isChecking = true
            )
            _latencyState.value = networkLatencyChecker.checkProfileLatency(
                profileId = profile.id,
                host = profile.serverAddress,
                port = profile.port
            )
            if (uiState.value.engineState.isTunnelRunning) {
                engineRepository.verifyActiveTunnelConnectivity()
            }
        }
    }

    fun onToggleConnectionClicked() {
        val current = uiState.value.engineState
        if (current.canStop) {
            engineRepository.stopVpn()
            _actionBanner.value = "Stopping NEXORA X VPN tunnel..."
            return
        }
        if (!current.canStart) {
            return
        }

        val selected = uiState.value.selectedProfile
        when (val outcome = engineRepository.startVpn(selected)) {
            is EngineServiceController.StartTunnelOutcome.ServiceStarted -> {
                _actionBanner.value = null
                onRefreshLatencyClicked()
            }
            is EngineServiceController.StartTunnelOutcome.VpnConsentRequired -> {
                _vpnConsentIntentEvents.tryEmit(outcome.prepareIntent)
            }
            is EngineServiceController.StartTunnelOutcome.ValidationFailed -> {
                _actionBanner.value = outcome.redactedError
            }
        }
    }

    fun onVpnPermissionResult(granted: Boolean) {
        engineRepository.onVpnPermissionResult(granted)
    }

    fun onDismissBanner() {
        _actionBanner.value = null
    }
}
