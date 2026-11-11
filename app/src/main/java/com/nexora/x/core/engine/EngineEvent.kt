package com.nexora.x.core.engine

/**
 * Lifecycle, permission, network, and diagnostic events emitted by the native sing-box engine
 * and Android [com.nexora.x.core.engine.service.NexoraVpnService].
 *
 * Security Invariant:
 * All error and log messages inside [EngineEvent] must pass through [LogRedactor.redact]
 * before being stored or emitted.
 */
sealed interface EngineEvent {
    data class StateChanged(
        val previousState: EngineState,
        val newState: EngineState,
        val reason: String
    ) : EngineEvent

    data class ConfigValidated(
        val profileId: String,
        val profileName: String,
        val protocol: String,
        val summary: String
    ) : EngineEvent

    data object VpnPermissionRequired : EngineEvent

    data class VpnPermissionDenied(
        val message: String = "Android VPN permission was denied by the user or another always-on VPN is locked."
    ) : EngineEvent

    data class TunInterfaceEstablished(
        val fdNumber: Int,
        val mtu: Int,
        val ipv4Address: String
    ) : EngineEvent

    data class ConnectivityVerificationCompleted(
        val stages: TunnelStageDiagnostics
    ) : EngineEvent

    data class TrafficUpdated(
        val statistics: EngineTrafficStatistics
    ) : EngineEvent

    data class NetworkAvailabilityChanged(
        val isAvailable: Boolean,
        val transportSummary: String
    ) : EngineEvent

    data class EngineFailure(
        val errorCode: String,
        val redactedMessage: String
    ) : EngineEvent
}
