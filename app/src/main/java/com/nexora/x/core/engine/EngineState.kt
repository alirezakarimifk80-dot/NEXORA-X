package com.nexora.x.core.engine

/**
 * Deterministic Phase 3 & Phase 4 Connection State Machine for the native sing-box (`libbox`) VPN engine.
 *
 * Strictly separates Android VPN service activation, TUN establishment, sing-box engine execution,
 * and actual end-to-end tunnel internet traffic verification (Stages A through F):
 * - [DISCONNECTED]
 * - [PREPARING]
 * - [REQUESTING_PERMISSION]
 * - [STARTING]
 * - [VPN_ACTIVE] (Stages A, B, and C up; TUN & libbox running)
 * - [CONNECTIVITY_CHECK] (Probing Stage D outbound, Stage E DNS, Stage F HTTP 204)
 * - [INTERNET_AVAILABLE] (All Stages A–F verified; real traffic passes through tunnel)
 * - [CONNECTED] (Alias state for active tunnel session compatibility)
 * - [NO_INTERNET] (Stages A–C up, but Stage D, E, or F failed — tunnel has no internet traffic)
 * - [STOPPING]
 * - [ERROR]
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
    VPN_ACTIVE(
        displayTitle = "VPN ACTIVE",
        statusBadge = "TUN & ENGINE UP",
        isTransitioning = false,
        canStart = false,
        canStop = true
    ),
    CONNECTIVITY_CHECK(
        displayTitle = "CONNECTIVITY CHECK",
        statusBadge = "VERIFYING TUNNEL (A-F)",
        isTransitioning = true,
        canStart = false,
        canStop = true
    ),
    INTERNET_AVAILABLE(
        displayTitle = "INTERNET AVAILABLE",
        statusBadge = "VERIFIED TRAFFIC (204)",
        isTransitioning = false,
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
    NO_INTERNET(
        displayTitle = "NO INTERNET",
        statusBadge = "TUNNEL UP · NO TRAFFIC",
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
    );

    /**
     * Returns `true` whenever the native `sing-box` session and Android `VpnService` TUN interface
     * are currently active (`VPN_ACTIVE`, `CONNECTIVITY_CHECK`, `INTERNET_AVAILABLE`, `CONNECTED`, `NO_INTERNET`).
     */
    val isTunnelRunning: Boolean
        get() = this == VPN_ACTIVE ||
            this == CONNECTIVITY_CHECK ||
            this == INTERNET_AVAILABLE ||
            this == CONNECTED ||
            this == NO_INTERNET
}

/**
 * Six-stage Phase 4 connectivity diagnostics distinguishing local service/TUN startup from actual
 * remote proxy reachability, DNS resolution, and end-to-end internet traffic through the tunnel:
 * - Stage A: VPN service started (`NexoraVpnService` promoted to foreground)
 * - Stage B: TUN interface established (`VpnService.Builder.establish()` returned valid non-blocking fd)
 * - Stage C: `sing-box` engine running (`Libbox.newService(...).start()` active)
 * - Stage D: Remote outbound connection working (protected socket TCP/TLS reachability to proxy host:port)
 * - Stage E: DNS resolution working (domain resolution via active tunnel DNS)
 * - Stage F: Actual internet connectivity working (HTTP 204 probe through active tunnel)
 */
data class TunnelStageDiagnostics(
    val stageAVpnServiceStarted: Boolean = false,
    val stageBTunInterfaceEstablished: Boolean = false,
    val stageCSingBoxEngineRunning: Boolean = false,
    val stageDRemoteOutboundWorking: Boolean? = null,
    val stageEDnsResolutionWorking: Boolean? = null,
    val stageFInternetConnectivityWorking: Boolean? = null,
    val tunFileDescriptor: Int? = null,
    val tunMtu: Int? = null,
    val tunIpv4Cidr: String? = null,
    val outboundHandshakeLatencyMs: Long? = null,
    val resolvedDnsAddressesCount: Int? = null,
    val probeHttpStatusCode: Int? = null,
    val failureStageLabel: String? = null,
    val failureReasonRedacted: String? = null
) {
    fun formatStageStatus(value: Boolean?): String = when (value) {
        true -> "PASS"
        false -> "FAIL"
        null -> "PENDING"
    }
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
    val stageDiagnostics: TunnelStageDiagnostics = TunnelStageDiagnostics(),
    val trafficStats: EngineTrafficStatistics = EngineTrafficStatistics(),
    val recentDiagnosticLogs: List<String> = emptyList()
)
