package com.nexora.x.core.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result of a real TCP handshake latency check against a profile's server host and port.
 * Never contains or logs credentials, UUIDs, passwords, or full configuration URIs.
 */
data class LatencyCheckResult(
    val profileId: String,
    val host: String,
    val port: Int,
    val latencyMillis: Long? = null,
    val isChecking: Boolean = false,
    val errorReason: String? = null,
    val checkedAtEpochMillis: Long = System.currentTimeMillis()
) {
    val isReachable: Boolean
        get() = latencyMillis != null && latencyMillis >= 0L && errorReason == null

    fun formatForDisplay(): String {
        return when {
            isChecking -> "Checking..."
            latencyMillis != null -> "${latencyMillis} ms"
            errorReason != null -> "Timeout"
            else -> "N/A"
        }
    }
}

/**
 * Network latency checker utility that measures real TCP socket connect (SYN -> SYN-ACK)
 * round-trip time to the selected server profile's host and port on [Dispatchers.IO].
 */
@Singleton
class NetworkLatencyChecker @Inject constructor() {

    companion object {
        const val DEFAULT_TIMEOUT_MS = 3_500
        const val DEFAULT_POLL_INTERVAL_MS = 10_000L
    }

    /**
     * Performs a non-blocking TCP handshake latency check against [host]:[port].
     */
    suspend fun checkProfileLatency(
        profileId: String,
        host: String,
        port: Int,
        timeoutMs: Int = DEFAULT_TIMEOUT_MS
    ): LatencyCheckResult = withContext(Dispatchers.IO) {
        val cleanHost = host.trim().removeSurrounding("[", "]")
        if (cleanHost.isEmpty() || port !in 1..65535) {
            return@withContext LatencyCheckResult(
                profileId = profileId,
                host = cleanHost,
                port = port,
                latencyMillis = null,
                isChecking = false,
                errorReason = "Invalid endpoint"
            )
        }

        val startNano = System.nanoTime()
        try {
            Socket().use { socket ->
                socket.reuseAddress = true
                socket.tcpNoDelay = true
                socket.connect(InetSocketAddress(cleanHost, port), timeoutMs)
            }
            val elapsedMs = ((System.nanoTime() - startNano) / 1_000_000L).coerceAtLeast(1L)
            LatencyCheckResult(
                profileId = profileId,
                host = cleanHost,
                port = port,
                latencyMillis = elapsedMs,
                isChecking = false,
                errorReason = null
            )
        } catch (e: Throwable) {
            LatencyCheckResult(
                profileId = profileId,
                host = cleanHost,
                port = port,
                latencyMillis = null,
                isChecking = false,
                errorReason = LogRedactor.redact(e.javaClass.simpleName)
            )
        }
    }
}
