package com.nexora.x.core.engine

import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.core.content.ContextCompat
import com.nexora.x.core.engine.service.NexoraVpnService
import com.nexora.x.core.model.Profile
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates Android `VpnService.prepare(context)` consent checks and starts/stops
 * [NexoraVpnService] cleanly while keeping Android framework intents out of Compose UI.
 */
@Singleton
class EngineServiceController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val singBoxEngine: SingBoxEngine
) {
    sealed interface StartTunnelOutcome {
        data object ServiceStarted : StartTunnelOutcome
        data class VpnConsentRequired(val prepareIntent: Intent) : StartTunnelOutcome
        data class ValidationFailed(val redactedError: String) : StartTunnelOutcome
    }

    /**
     * Validates [profile] and checks `VpnService.prepare(context)`:
     * - If `VpnService.prepare` returns a non-null `Intent`, transitions the engine to
     *   [EngineState.REQUESTING_PERMISSION] and returns [StartTunnelOutcome.VpnConsentRequired]
     *   so the Activity launcher can present the system VPN consent dialog.
     * - If VPN consent is already granted (`prepare == null`), starts [NexoraVpnService] immediately.
     */
    fun initiateTunnelStart(profile: Profile?): StartTunnelOutcome {
        val validation = singBoxEngine.prepareProfileForStart(profile)
        if (validation.isFailure) {
            val err = validation.exceptionOrNull()?.message ?: "Configuration validation failed."
            return StartTunnelOutcome.ValidationFailed(LogRedactor.redact(err))
        }

        val prepareIntent = try {
            VpnService.prepare(context)
        } catch (e: Throwable) {
            singBoxEngine.transitionToError(
                "ERR_VPN_PREPARE_FAILED",
                "Failed to query Android VpnService.prepare: ${e.message ?: "unknown error"}"
            )
            return StartTunnelOutcome.ValidationFailed("Failed to query Android VpnService.prepare.")
        }

        if (prepareIntent != null) {
            singBoxEngine.onVpnPermissionRequestLaunched()
            return StartTunnelOutcome.VpnConsentRequired(prepareIntent)
        }

        dispatchStartServiceIntent()
        return StartTunnelOutcome.ServiceStarted
    }

    /**
     * Called when the user grants Android VPN permission from the system consent dialog.
     */
    fun onVpnConsentGranted() {
        dispatchStartServiceIntent()
    }

    /**
     * Called when the user denies Android VPN permission from the system consent dialog.
     */
    fun onVpnConsentDenied() {
        singBoxEngine.onVpnPermissionDenied(
            "VPN permission denied by user in Android system consent dialog."
        )
    }

    /**
     * Dispatches `ACTION_STOP_VPN` to [NexoraVpnService] and ensures the engine stops idempotently.
     */
    fun stopTunnel() {
        val stopIntent = Intent(context, NexoraVpnService::class.java).apply {
            action = NexoraVpnService.ACTION_STOP_VPN
        }
        runCatching {
            context.startService(stopIntent)
        }
        singBoxEngine.stopSession("User requested disconnect")
    }

    private fun dispatchStartServiceIntent() {
        val startIntent = Intent(context, NexoraVpnService::class.java).apply {
            action = NexoraVpnService.ACTION_START_VPN
        }
        try {
            ContextCompat.startForegroundService(context, startIntent)
        } catch (e: Throwable) {
            singBoxEngine.transitionToError(
                "ERR_SERVICE_LAUNCH",
                "Could not start NexoraVpnService: ${e.message ?: e.javaClass.simpleName}"
            )
        }
    }
}
