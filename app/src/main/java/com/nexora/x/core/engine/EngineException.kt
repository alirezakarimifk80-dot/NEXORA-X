package com.nexora.x.core.engine

/**
 * Typed hierarchy of engine and VpnService exceptions for NEXORA X Phase 3.
 * Never embeds raw credentials, UUIDs, passwords, private keys, or full URI strings.
 */
sealed class EngineException(
    val errorCode: String,
    override val message: String,
    override val cause: Throwable? = null
) : Exception(LogRedactor.redact(message), cause) {

    class InvalidConfigurationException(
        details: String
    ) : EngineException(
        errorCode = "ERR_INVALID_CONFIG",
        message = LogRedactor.redact(details)
    )

    class LibboxBinaryUnavailableException(
        details: String = "Native sing-box library (io.nekohasekai.libbox / libbox.so) is not packaged in app/libs/libbox.aar."
    ) : EngineException(
        errorCode = "ERR_LIBBOX_UNAVAILABLE",
        message = LogRedactor.redact(details)
    )

    class EngineInitializationException(
        details: String,
        cause: Throwable? = null
    ) : EngineException(
        errorCode = "ERR_ENGINE_INIT",
        message = LogRedactor.redact(details),
        cause = cause
    )

    class VpnPermissionDeniedException(
        details: String = "Android VpnService consent was denied or preempted by another active VPN."
    ) : EngineException(
        errorCode = "ERR_VPN_PERMISSION_DENIED",
        message = LogRedactor.redact(details)
    )

    class TunEstablishmentException(
        details: String,
        cause: Throwable? = null
    ) : EngineException(
        errorCode = "ERR_TUN_ESTABLISH_FAILED",
        message = LogRedactor.redact(details),
        cause = cause
    )

    class RuntimeExecutionException(
        details: String,
        cause: Throwable? = null
    ) : EngineException(
        errorCode = "ERR_ENGINE_RUNTIME",
        message = LogRedactor.redact(details),
        cause = cause
    )
}
