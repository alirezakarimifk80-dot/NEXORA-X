package com.nexora.x.core.engine

import com.nexora.x.core.model.Profile
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Core Engine Lifecycle Abstraction & State Machine for NEXORA X Phase 3 & Phase 4.
 *
 * Enforces deterministic transitions across:
 * - [EngineState.DISCONNECTED]
 * - [EngineState.PREPARING]
 * - [EngineState.REQUESTING_PERMISSION]
 * - [EngineState.STARTING]
 * - [EngineState.VPN_ACTIVE] (Stages A, B, and C established)
 * - [EngineState.CONNECTIVITY_CHECK] (Probing Stage D outbound, Stage E DNS, Stage F HTTP 204)
 * - [EngineState.INTERNET_AVAILABLE] (All Stages A–F verified; real traffic passes through tunnel)
 * - [EngineState.CONNECTED] (Alias state for active tunnel session compatibility)
 * - [EngineState.NO_INTERNET] (Stages A–C up, but Stage D, E, or F failed — no internet traffic)
 * - [EngineState.STOPPING]
 * - [EngineState.ERROR]
 *
 * Prevents concurrent duplicate start/stop calls, validates configurations before requesting
 * VPN permission or starting the service, tracks 6-stage connectivity diagnostics (Stages A–F),
 * and applies [LogRedactor] to all diagnostic events.
 */
@Singleton
class SingBoxEngine @Inject constructor(
    private val configGenerator: SingBoxConfigGenerator,
    private val libboxManager: LibboxManager,
    private val connectivityVerifier: TunnelConnectivityVerifier
) {
    constructor(
        configGenerator: SingBoxConfigGenerator,
        libboxManager: LibboxManager
    ) : this(
        configGenerator = configGenerator,
        libboxManager = libboxManager,
        connectivityVerifier = TunnelConnectivityVerifier()
    )

    private val _diagnostics = MutableStateFlow(
        EngineDiagnosticsSnapshot(
            engineVersion = libboxManager.queryEngineVersion(),
            isLibboxBinaryLoaded = libboxManager.isNativeLibboxLoaded(),
            isEngineInitialized = libboxManager.isInitialized()
        )
    )
    val diagnostics: StateFlow<EngineDiagnosticsSnapshot> = _diagnostics.asStateFlow()

    private val _engineState = MutableStateFlow(EngineState.DISCONNECTED)
    val engineState: StateFlow<EngineState> = _engineState.asStateFlow()

    private val _events = MutableSharedFlow<EngineEvent>(extraBufferCapacity = 32)
    val events: SharedFlow<EngineEvent> = _events.asSharedFlow()

    private val startStopLock = Any()
    private val isStopping = AtomicBoolean(false)

    @Volatile
    private var preparedProfile: Profile? = null

    @Volatile
    private var preparedConfigJson: String? = null

    /**
     * Step 1: Validates [profile] and generates the sing-box v1.10.7 JSON configuration.
     * Transitions from `DISCONNECTED` or `ERROR` -> `PREPARING`.
     * Ignores duplicate start requests when already `PREPARING`, `STARTING`, or `isTunnelRunning`.
     */
    fun prepareProfileForStart(profile: Profile?): Result<SingBoxConfigGenerator.GeneratedSingBoxConfig> {
        synchronized(startStopLock) {
            val current = _engineState.value
            if (!current.canStart) {
                return Result.failure(
                    IllegalStateException("Engine is currently in state ${current.name}; start request ignored.")
                )
            }

            if (profile == null) {
                transitionToError(
                    errorCode = "ERR_NO_PROFILE_SELECTED",
                    rawErrorMessage = "Select or import a server profile before starting the VPN."
                )
                return Result.failure(
                    EngineException.InvalidConfigurationException("No profile selected.")
                )
            }

            transitionState(EngineState.PREPARING, "Validating profile '${profile.displayName}'")

            return try {
                val generated = configGenerator.generateAndValidate(profile)
                libboxManager.checkConfigWithNativeLibboxIfAvailable(generated.rawJson)

                preparedProfile = profile
                preparedConfigJson = generated.rawJson

                appendDiagnosticLog(generated.redactedSummary)
                _diagnostics.update {
                    it.copy(
                        activeProfileId = profile.id,
                        activeProfileName = profile.displayName,
                        activeProtocol = profile.protocol.displayName,
                        lastValidationSummary = generated.redactedSummary,
                        lastRedactedError = null,
                        stageDiagnostics = TunnelStageDiagnostics()
                    )
                }
                _events.tryEmit(
                    EngineEvent.ConfigValidated(
                        profileId = profile.id,
                        profileName = profile.displayName,
                        protocol = profile.protocol.displayName,
                        summary = generated.redactedSummary
                    )
                )
                Result.success(generated)
            } catch (e: EngineException.InvalidConfigurationException) {
                transitionToError(
                    errorCode = e.errorCode,
                    rawErrorMessage = e.message,
                    extraSecrets = listOf(profile.credential, profile.tlsConfig.realityPublicKey)
                )
                Result.failure(e)
            } catch (e: Throwable) {
                transitionToError(
                    errorCode = "ERR_VALIDATION_UNEXPECTED",
                    rawErrorMessage = e.message ?: "Unexpected configuration validation error",
                    extraSecrets = listOf(profile.credential)
                )
                Result.failure(e)
            }
        }
    }

    fun onVpnPermissionRequestLaunched() {
        synchronized(startStopLock) {
            if (_engineState.value == EngineState.PREPARING) {
                transitionState(EngineState.REQUESTING_PERMISSION, "Requesting Android VpnService user consent")
                _events.tryEmit(EngineEvent.VpnPermissionRequired)
            }
        }
    }

    fun onVpnPermissionDenied(reason: String = "Android VPN permission denied by user.") {
        synchronized(startStopLock) {
            preparedConfigJson = null
            val redacted = LogRedactor.redact(reason)
            _events.tryEmit(EngineEvent.VpnPermissionDenied(redacted))
            transitionToError("ERR_VPN_PERMISSION_DENIED", redacted)
        }
    }

    /**
     * Records Stage B (`tunInterfaceEstablished = true`) when `NexoraVpnService.openTunInterface`
     * establishes the non-blocking virtual interface via `VpnService.Builder.establish()`.
     */
    fun onTunInterfaceOpened(fd: Int, mtu: Int, ipv4Address: String, ipv4Prefix: Int) {
        val cidr = "$ipv4Address/$ipv4Prefix"
        appendDiagnosticLog("Stage B PASS: TUN interface established (fd=$fd, cidr=$cidr, mtu=$mtu)")
        _diagnostics.update { current ->
            current.copy(
                stageDiagnostics = current.stageDiagnostics.copy(
                    stageAVpnServiceStarted = true,
                    stageBTunInterfaceEstablished = fd >= 0,
                    tunFileDescriptor = fd,
                    tunMtu = mtu,
                    tunIpv4Cidr = cidr
                )
            )
        }
        _events.tryEmit(EngineEvent.TunInterfaceEstablished(fdNumber = fd, mtu = mtu, ipv4Address = cidr))
    }

    /**
     * Step 2: Called by [com.nexora.x.core.engine.service.NexoraVpnService] on a background dispatcher
     * once the foreground service is promoted and ready to establish the TUN interface and start `libbox`.
     *
     * Verifies Stages A, B, and C, transitioning to [EngineState.VPN_ACTIVE] once the native
     * `BoxService` is running.
     */
    @Throws(EngineException::class)
    fun startPreparedSessionInService(callbacks: NexoraTunPlatformCallbacks) {
        val configJson: String
        val profile: Profile
        synchronized(startStopLock) {
            val current = _engineState.value
            if (current.isTunnelRunning || current == EngineState.STARTING) {
                return
            }
            configJson = preparedConfigJson ?: throw EngineException.InvalidConfigurationException(
                "No validated sing-box configuration available for service startup."
            )
            profile = preparedProfile ?: throw EngineException.InvalidConfigurationException(
                "No active profile bound to engine session."
            )
            transitionState(EngineState.STARTING, "Stage A PASS: Starting NexoraVpnService & libbox core")
            _diagnostics.update {
                it.copy(
                    serviceState = "STARTING_FOREGROUND",
                    stageDiagnostics = TunnelStageDiagnostics(stageAVpnServiceStarted = true)
                )
            }
        }

        try {
            libboxManager.startNativeBoxService(configJson, callbacks)
            val now = System.currentTimeMillis()
            synchronized(startStopLock) {
                val stageBReady = _diagnostics.value.stageDiagnostics.stageBTunInterfaceEstablished
                val updatedStages = _diagnostics.value.stageDiagnostics.copy(
                    stageAVpnServiceStarted = true,
                    stageBTunInterfaceEstablished = stageBReady,
                    stageCSingBoxEngineRunning = true
                )
                transitionState(
                    EngineState.VPN_ACTIVE,
                    "Stage C PASS: sing-box BoxService running for '${profile.displayName}' (awaiting Stages D–F connectivity check)"
                )
                _diagnostics.update {
                    it.copy(
                        engineVersion = libboxManager.queryEngineVersion(),
                        isLibboxBinaryLoaded = libboxManager.isNativeLibboxLoaded(),
                        isEngineInitialized = libboxManager.isInitialized(),
                        serviceState = "RUNNING_FOREGROUND",
                        connectedSinceEpochMillis = now,
                        lastRedactedError = null,
                        stageDiagnostics = updatedStages
                    )
                }
            }
        } catch (e: EngineException) {
            transitionToError(
                errorCode = e.errorCode,
                rawErrorMessage = e.message,
                extraSecrets = listOf(profile.credential)
            )
            throw e
        } catch (e: Throwable) {
            val err = EngineException.RuntimeExecutionException(
                e.message ?: "Unexpected native engine error",
                e
            )
            transitionToError(
                errorCode = err.errorCode,
                rawErrorMessage = err.message,
                extraSecrets = listOf(profile.credential)
            )
            throw err
        }
    }

    /**
     * Step 3 (Phase 4): Executes real end-to-end connectivity verification across Stages D, E, and F
     * while the VPN tunnel is active (`VPN_ACTIVE`, `CONNECTED`, `INTERNET_AVAILABLE`, or `NO_INTERNET`).
     *
     * Transitions:
     * - `VPN_ACTIVE` -> `CONNECTIVITY_CHECK`
     * - If Stages D, E, and F pass -> `INTERNET_AVAILABLE`
     * - If Stages A–C are running (`VPN_ACTIVE`), but Stage D, E, or F fails -> `NO_INTERNET`
     */
    suspend fun verifyActiveTunnelConnectivity(
        protectSocket: ((Socket) -> Boolean)? = null
    ): TunnelStageDiagnostics {
        val activeProfile: Profile
        val baseStages: TunnelStageDiagnostics
        synchronized(startStopLock) {
            val current = _engineState.value
            val prof = preparedProfile
            if (!current.isTunnelRunning || prof == null) {
                return _diagnostics.value.stageDiagnostics
            }
            activeProfile = prof
            baseStages = _diagnostics.value.stageDiagnostics.copy(
                stageAVpnServiceStarted = true,
                stageBTunInterfaceEstablished = true,
                stageCSingBoxEngineRunning = true
            )
            transitionState(
                EngineState.CONNECTIVITY_CHECK,
                "Running Stages D–F connectivity probes for '${activeProfile.displayName}'..."
            )
        }

        val verifiedStages = connectivityVerifier.verifyActiveTunnel(
            profile = activeProfile,
            baseStages = baseStages,
            protectSocket = protectSocket
        )

        synchronized(startStopLock) {
            val current = _engineState.value
            if (!current.isTunnelRunning) {
                return verifiedStages
            }

            if (verifiedStages.stageDRemoteOutboundWorking == true &&
                verifiedStages.stageEDnsResolutionWorking == true &&
                verifiedStages.stageFInternetConnectivityWorking == true
            ) {
                val summary =
                    "Stages A–F PASS: Outbound ${verifiedStages.outboundHandshakeLatencyMs ?: 0}ms, DNS (${verifiedStages.resolvedDnsAddressesCount ?: 1} IP), HTTP ${verifiedStages.probeHttpStatusCode ?: 204}"
                transitionState(EngineState.INTERNET_AVAILABLE, summary)
                _diagnostics.update {
                    it.copy(
                        stageDiagnostics = verifiedStages,
                        lastRedactedError = null
                    )
                }
            } else {
                val failReason = verifiedStages.failureReasonRedacted
                    ?: "Tunnel is active (Stages A–C PASS), but internet traffic probe failed."
                transitionState(EngineState.NO_INTERNET, failReason)
                _diagnostics.update {
                    it.copy(
                        stageDiagnostics = verifiedStages,
                        lastRedactedError = "[${verifiedStages.failureStageLabel ?: "NO_INTERNET"}] $failReason"
                    )
                }
            }
            _events.tryEmit(EngineEvent.ConnectivityVerificationCompleted(verifiedStages))
        }

        return verifiedStages
    }

    /**
     * Stops the running `libbox` service and resets telemetry cleanly and idempotently.
     */
    fun stopSession(reason: String = "User requested disconnect") {
        if (!isStopping.compareAndSet(false, true)) {
            return
        }
        try {
            synchronized(startStopLock) {
                val current = _engineState.value
                if (current == EngineState.DISCONNECTED) {
                    return
                }
                transitionState(EngineState.STOPPING, reason)
            }

            libboxManager.stopNativeBoxService()

            synchronized(startStopLock) {
                preparedConfigJson = null
                transitionState(EngineState.DISCONNECTED, "Tunnel stopped ($reason)")
                _diagnostics.update {
                    it.copy(
                        serviceState = "STOPPED",
                        connectedSinceEpochMillis = null,
                        stageDiagnostics = TunnelStageDiagnostics(),
                        trafficStats = EngineTrafficStatistics()
                    )
                }
            }
        } finally {
            isStopping.set(false)
        }
    }

    fun onTrafficStatisticsReceived(stats: EngineTrafficStatistics) {
        if (!_engineState.value.isTunnelRunning) return
        _diagnostics.update { it.copy(trafficStats = stats) }
        _events.tryEmit(EngineEvent.TrafficUpdated(stats))
    }

    fun onNetworkStateChanged(isAvailable: Boolean, transportSummary: String) {
        val msg = if (isAvailable) {
            "Underlying network active ($transportSummary)"
        } else {
            "Underlying network lost ($transportSummary)"
        }
        appendDiagnosticLog(msg)
        _events.tryEmit(EngineEvent.NetworkAvailabilityChanged(isAvailable, transportSummary))
    }

    fun recordDiagnosticLog(rawLog: String) {
        appendDiagnosticLog(rawLog)
    }

    fun transitionToError(
        errorCode: String,
        rawErrorMessage: String,
        extraSecrets: Collection<String> = emptyList()
    ) {
        synchronized(startStopLock) {
            val redacted = LogRedactor.redact(rawErrorMessage, extraSecrets)
            val prev = _engineState.value
            _engineState.value = EngineState.ERROR
            appendDiagnosticLog("[$errorCode] $redacted")
            _diagnostics.update {
                it.copy(
                    engineVersion = libboxManager.queryEngineVersion(),
                    isLibboxBinaryLoaded = libboxManager.isNativeLibboxLoaded(),
                    isEngineInitialized = libboxManager.isInitialized(),
                    currentVpnState = EngineState.ERROR,
                    serviceState = "STOPPED_ON_ERROR",
                    connectedSinceEpochMillis = null,
                    lastRedactedError = "[$errorCode] $redacted",
                    stageDiagnostics = it.stageDiagnostics.copy(
                        stageCSingBoxEngineRunning = false,
                        failureStageLabel = errorCode,
                        failureReasonRedacted = redacted
                    ),
                    trafficStats = EngineTrafficStatistics()
                )
            }
            _events.tryEmit(EngineEvent.StateChanged(prev, EngineState.ERROR, redacted))
            _events.tryEmit(EngineEvent.EngineFailure(errorCode, redacted))
        }
    }

    private fun transitionState(target: EngineState, reason: String) {
        val redactedReason = LogRedactor.redact(reason)
        val previous = _engineState.value
        _engineState.value = target
        appendDiagnosticLog("${previous.name} -> ${target.name}: $redactedReason")
        _diagnostics.update {
            it.copy(
                engineVersion = libboxManager.queryEngineVersion(),
                isLibboxBinaryLoaded = libboxManager.isNativeLibboxLoaded(),
                isEngineInitialized = libboxManager.isInitialized(),
                currentVpnState = target
            )
        }
        _events.tryEmit(EngineEvent.StateChanged(previous, target, redactedReason))
    }

    private fun appendDiagnosticLog(rawEntry: String) {
        val sanitized = LogRedactor.redact(rawEntry)
        _diagnostics.update { current ->
            val updated = (listOf(sanitized) + current.recentDiagnosticLogs).take(30)
            current.copy(recentDiagnosticLogs = updated)
        }
    }
}
