package com.nexora.x.core.engine

import android.content.Intent
import com.nexora.x.core.model.Profile
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Clean Architecture Repository contract exposing the Phase 3 native `sing-box` engine state,
 * diagnostic snapshot, and lifecycle operations through Kotlin [StateFlow] and [SharedFlow].
 */
interface EngineRepository {
    val engineState: StateFlow<EngineState>
    val diagnostics: StateFlow<EngineDiagnosticsSnapshot>
    val events: SharedFlow<EngineEvent>

    fun startVpn(profile: Profile?): EngineServiceController.StartTunnelOutcome
    fun onVpnPermissionResult(granted: Boolean)
    fun stopVpn()
    fun validateProfileConfiguration(profile: Profile): Result<SingBoxConfigGenerator.GeneratedSingBoxConfig>
}

@Singleton
class EngineRepositoryImpl @Inject constructor(
    private val singBoxEngine: SingBoxEngine,
    private val serviceController: EngineServiceController,
    private val configGenerator: SingBoxConfigGenerator
) : EngineRepository {

    override val engineState: StateFlow<EngineState> = singBoxEngine.engineState
    override val diagnostics: StateFlow<EngineDiagnosticsSnapshot> = singBoxEngine.diagnostics
    override val events: SharedFlow<EngineEvent> = singBoxEngine.events

    override fun startVpn(profile: Profile?): EngineServiceController.StartTunnelOutcome {
        return serviceController.initiateTunnelStart(profile)
    }

    override fun onVpnPermissionResult(granted: Boolean) {
        if (granted) {
            serviceController.onVpnConsentGranted()
        } else {
            serviceController.onVpnConsentDenied()
        }
    }

    override fun stopVpn() {
        serviceController.stopTunnel()
    }

    override fun validateProfileConfiguration(
        profile: Profile
    ): Result<SingBoxConfigGenerator.GeneratedSingBoxConfig> {
        return runCatching { configGenerator.generateAndValidate(profile) }
    }
}
