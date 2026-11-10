package com.nexora.x.core.engine

/**
 * Deterministic Phase 3 Connection State Machine for the native sing-box (`libbox`) VPN engine.
 *
 * State transitions come strictly from actual Android VpnService and libbox lifecycle events.
 * No arbitrary delays or simulated connection states are permitted.
 */
enum class EngineState(
    val displayTitle: String,
    val statusBadge: String,
    val isTransitioning: Boolean,
    val canStart: Boolean,
    val canStop: Boolean
) {
    DISCONNECTED(
        displayTitle = "DISCONNECTED",
        statusBadge = "READY",
        isTransitioning = false,
        canStart = true,
        canStop = false
    ),
    PREPARING(
        displayTitle = "PREPARING",
        statusBadge = "VALIDATING CONFIG",
        isTransitioning = true,
        canStart = false,
        canStop = true
    ),
    REQUESTING_PERMISSION(
        displayTitle = "REQUESTING PERMISSION",
        statusBadge = "VPN CONSENT REQUIRED",
        isTransitioning = true,
        canStart = false,
        canStop = true
    ),
    STARTING(
        displayTitle = "STARTING",
        statusBadge = "INITIALIZING TUN & LIBBOX",
        isTransitioning = true,
        canStart = false,
        canStop = true
    ),
    CONNECTED(
        displayTitle = "CONNECTED",
        statusBadge = "TUNNEL ACTIVE",
        isTransitioning = false,
        canStart = false,
        canStop = true
    ),
    STOPPING(
        displayTitle = "STOPPING",
        statusBadge = "CLOSING TUNNEL",
        isTransitioning = true,
        canStart = false,
        canStop = false
    ),
    ERROR(
        displayTitle = "ENGINE ERROR",
        statusBadge = "ENGINE ERROR",
        isTransitioning = false,
        canStart = true,
        canStop = false
    )
}

/**
 * Real-time traffic statistics reported by `io.nekohasekai.libbox.CommandClientHandler.writeStatus`.
 * When no active libbox CommandClient stream is reporting, metrics remain `null` so the UI
 * truthfully renders `"N/A"` rather than fabricating speeds or byte counts.
 */
data class EngineTrafficStatistics(
    val uplinkSpeedBytesPerSec: Long? = null,
    val downlinkSpeedBytesPerSec: Long? = null,
    val uplinkTotalBytes: Long? = null,
    val downlinkTotalBytes: Long? = null,
    val activeConnectionsCount: Int? = null,
    val memoryBytes: Long? = null
)

/**
 * Complete snapshot of the native sing-box engine & Android VpnService for UI and Diagnostics.
 */
data class EngineDiagnosticsSnapshot(
    val engineVersion: String = "N/A (libbox.aar not loaded)",
    val isLibboxBinaryLoaded: Boolean = false,
    val isEngineInitialized: Boolean = false,
    val currentVpnState: EngineState = EngineState.DISCONNECTED,
    val serviceState: String = "STOPPED",
    val activeProfileId: String? = null,
    val activeProfileName: String? = null,
    val activeProtocol: String? = null,
    val connectedSinceEpochMillis: Long? = null,
    val lastValidationSummary: String = "Not yet validated",
    val lastRedactedError: String? = null,
    val trafficStats: EngineTrafficStatistics = EngineTrafficStatistics(),
    val recentDiagnosticLogs: List<String> = emptyList()
)
