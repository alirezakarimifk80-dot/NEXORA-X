package com.nexora.x.core.engine

import com.nexora.x.core.model.Profile
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Core Engine Lifecycle Abstraction & State Machine for NEXORA X Phase 3.
 *
 * Enforces deterministic transitions across:
 * - [EngineState.DISCONNECTED]
 * - [EngineState.PREPARING]
 * - [EngineState.REQUESTING_PERMISSION]
 * - [EngineState.STARTING]
 * - [EngineState.CONNECTED]
 * - [EngineState.STOPPING]
 * - [EngineState.ERROR]
 *
 * Prevents concurrent duplicate start/stop calls, validates configurations before requesting
 * VPN permission or starting the service, and applies [LogRedactor] to all diagnostic events.
 */
@Singleton
class SingBoxEngine @Inject constructor(
    private val configGenerator: SingBoxConfigGenerator,
    private val libboxManager: LibboxManager
) {
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
     * Step 1: Validates [profile] and generates the sing-box JSON configuration.
     * Transitions from `DISCONNECTED` or `ERROR` -> `PREPARING`.
     * Ignores duplicate start requests when already `PREPARING`, `STARTING`, or `CONNECTED`.
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
                        lastRedactedError = null
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
     * Step 2: Called by [com.nexora.x.core.engine.service.NexoraVpnService] on a background dispatcher
     * once the foreground service is promoted and ready to establish the TUN interface and start `libbox`.
     */
    @Throws(EngineException::class)
    fun startPreparedSessionInService(callbacks: NexoraTunPlatformCallbacks) {
        val configJson: String
        val profile: Profile
        synchronized(startStopLock) {
            val current = _engineState.value
            if (current == EngineState.CONNECTED || current == EngineState.STARTING) {
                return
            }
            configJson = preparedConfigJson ?: throw EngineException.InvalidConfigurationException(
                "No validated sing-box configuration available for service startup."
            )
            profile = preparedProfile ?: throw EngineException.InvalidConfigurationException(
                "No active profile bound to engine session."
            )
            transitionState(EngineState.STARTING, "Starting NexoraVpnService & libbox core")
            _diagnostics.update { it.copy(serviceState = "STARTING_FOREGROUND") }
        }

        try {
            libboxManager.startNativeBoxService(configJson, callbacks)
            val now = System.currentTimeMillis()
            synchronized(startStopLock) {
                transitionState(EngineState.CONNECTED, "Tunnel established for '${profile.displayName}'")
                _diagnostics.update {
                    it.copy(
                        engineVersion = libboxManager.queryEngineVersion(),
                        isLibboxBinaryLoaded = libboxManager.isNativeLibboxLoaded(),
                        isEngineInitialized = libboxManager.isInitialized(),
                        serviceState = "RUNNING_FOREGROUND",
                        connectedSinceEpochMillis = now,
                        lastRedactedError = null
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
                        trafficStats = EngineTrafficStatistics()
                    )
                }
            }
        } finally {
            isStopping.set(false)
        }
    }

    fun onTrafficStatisticsReceived(stats: EngineTrafficStatistics) {
        if (_engineState.value != EngineState.CONNECTED) return
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
