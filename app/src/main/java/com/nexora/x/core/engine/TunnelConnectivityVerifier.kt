package com.nexora.x.core.engine

import com.nexora.x.core.model.Profile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLParameters
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/**
 * Phase 4 End-to-End Tunnel Connectivity Verifier.
 *
 * Executes real network probes (never simulated) to diagnose Stages D, E, and F once Stages A, B,
 * and C (`NexoraVpnService`, `tun0`, and `libbox` `BoxService`) are active:
 * - **Stage D (Remote Outbound Connection)**:
 *   Opens a protected socket (via `VpnService.protect(Socket)` when provided) directly to
 *   `profile.serverAddress:profile.port` and verifies TCP reachability (plus TLS handshake when
 *   standard TLS is configured). Identifies dead servers, blocked ports, or TLS SNI mismatches.
 * - **Stage E (DNS Resolution Through Tunnel)**:
 *   Resolves standard connectivity hostnames (`cp.cloudflare.com`, `connectivitycheck.gstatic.com`)
 *   through the active system/VPN DNS resolver and verifies valid IP addresses are returned.
 * - **Stage F (Actual Internet Connectivity)**:
 *   Issues an HTTP GET request to `http://cp.cloudflare.com/generate_204` (fallback
 *   `https://connectivitycheck.gstatic.com/generate_204`) through the active VPN tunnel and verifies
 *   an HTTP `204` (or `200..299`) response is received.
 */
@Singleton
class TunnelConnectivityVerifier @Inject constructor() {

    companion object {
        const val STAGE_D_TIMEOUT_MS = 4_000
        const val STAGE_F_TIMEOUT_MS = 5_000
        private val PROBE_DNS_HOSTS = listOf(
            "cp.cloudflare.com",
            "connectivitycheck.gstatic.com"
        )
        private val PROBE_HTTP_URLS = listOf(
            "http://cp.cloudflare.com/generate_204",
            "https://connectivitycheck.gstatic.com/generate_204"
        )
    }

    data class StageDResult(
        val passed: Boolean,
        val latencyMs: Long? = null,
        val failureReason: String? = null
    )

    data class StageEResult(
        val passed: Boolean,
        val resolvedCount: Int = 0,
        val failureReason: String? = null
    )

    data class StageFResult(
        val passed: Boolean,
        val httpStatusCode: Int? = null,
        val failureReason: String? = null
    )

    /**
     * Runs Stages D, E, and F sequentially on [Dispatchers.IO] and populates [TunnelStageDiagnostics].
     */
    suspend fun verifyActiveTunnel(
        profile: Profile,
        baseStages: TunnelStageDiagnostics,
        protectSocket: ((Socket) -> Boolean)? = null
    ): TunnelStageDiagnostics = withContext(Dispatchers.IO) {
        // Stage D: Verify remote proxy server outbound reachability
        val stageD = checkRemoteOutboundStageD(profile, protectSocket)
        if (!stageD.passed) {
            return@withContext baseStages.copy(
                stageDRemoteOutboundWorking = false,
                stageEDnsResolutionWorking = false,
                stageFInternetConnectivityWorking = false,
                outboundHandshakeLatencyMs = null,
                failureStageLabel = "STAGE D (REMOTE OUTBOUND)",
                failureReasonRedacted = LogRedactor.redact(
                    stageD.failureReason ?: "Remote proxy outbound unreachable (${profile.serverAddress}:${profile.port})"
                )
            )
        }

        // Stage E: Verify DNS resolution while tunnel is active
        val stageE = checkDnsResolutionStageE()
        if (!stageE.passed) {
            return@withContext baseStages.copy(
                stageDRemoteOutboundWorking = true,
                stageEDnsResolutionWorking = false,
                stageFInternetConnectivityWorking = false,
                outboundHandshakeLatencyMs = stageD.latencyMs,
                resolvedDnsAddressesCount = 0,
                failureStageLabel = "STAGE E (DNS RESOLUTION)",
                failureReasonRedacted = LogRedactor.redact(
                    stageE.failureReason ?: "DNS resolution failed inside active VPN tunnel"
                )
            )
        }

        // Stage F: Verify actual HTTP 204 internet traffic through the tunnel
        val stageF = checkInternetConnectivityStageF()
        if (!stageF.passed) {
            return@withContext baseStages.copy(
                stageDRemoteOutboundWorking = true,
                stageEDnsResolutionWorking = true,
                stageFInternetConnectivityWorking = false,
                outboundHandshakeLatencyMs = stageD.latencyMs,
                resolvedDnsAddressesCount = stageE.resolvedCount,
                probeHttpStatusCode = stageF.httpStatusCode,
                failureStageLabel = "STAGE F (TUNNEL INTERNET TRAFFIC)",
                failureReasonRedacted = LogRedactor.redact(
                    stageF.failureReason ?: "Outbound server reachable, but no internet traffic returned through proxy tunnel"
                )
            )
        }

        baseStages.copy(
            stageDRemoteOutboundWorking = true,
            stageEDnsResolutionWorking = true,
            stageFInternetConnectivityWorking = true,
            outboundHandshakeLatencyMs = stageD.latencyMs,
            resolvedDnsAddressesCount = stageE.resolvedCount,
            probeHttpStatusCode = stageF.httpStatusCode,
            failureStageLabel = null,
            failureReasonRedacted = null
        )
    }

    fun checkRemoteOutboundStageD(
        profile: Profile,
        protectSocket: ((Socket) -> Boolean)? = null
    ): StageDResult {
        val host = profile.serverAddress.trim()
        val port = profile.port
        val startNanos = System.nanoTime()
        return try {
            Socket().use { rawSocket ->
                rawSocket.reuseAddress = true
                rawSocket.tcpNoDelay = true
                if (protectSocket != null) {
                    runCatching { protectSocket(rawSocket) }
                }
                val address = InetSocketAddress(host, port)
                if (address.isUnresolved) {
                    return StageDResult(
                        passed = false,
                        failureReason = "Stage D: Cannot resolve proxy server hostname '$host'"
                    )
                }
                rawSocket.connect(address, STAGE_D_TIMEOUT_MS)

                val securityMode = profile.tlsConfig.securityMode.lowercase().ifBlank { "none" }
                if (securityMode == "tls" && !profile.tlsConfig.allowInsecure) {
                    val sniHost = profile.tlsConfig.sni.ifBlank { host }.trim()
                    val sslFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
                    (sslFactory.createSocket(rawSocket, sniHost, port, true) as SSLSocket).use { sslSocket ->
                        sslSocket.soTimeout = STAGE_D_TIMEOUT_MS
                        runCatching {
                            val params: SSLParameters = sslSocket.sslParameters
                            params.serverNames = listOf(SNIHostName(sniHost))
                            sslSocket.sslParameters = params
                        }
                        sslSocket.startHandshake()
                    }
                }

                val elapsedMs = ((System.nanoTime() - startNanos) / 1_000_000L).coerceAtLeast(1L)
                StageDResult(passed = true, latencyMs = elapsedMs)
            }
        } catch (_: SocketTimeoutException) {
            StageDResult(
                passed = false,
                failureReason = "Stage D: Remote proxy server $host:$port timed out (${STAGE_D_TIMEOUT_MS}ms)"
            )
        } catch (e: Throwable) {
            StageDResult(
                passed = false,
                failureReason = "Stage D: Remote outbound $host:$port failed (${e.javaClass.simpleName}: ${e.message ?: "connection error"})"
            )
        }
    }

    fun checkDnsResolutionStageE(): StageEResult {
        var lastError: String? = null
        for (domain in PROBE_DNS_HOSTS) {
            try {
                val addresses = InetAddress.getAllByName(domain)
                if (!addresses.isNullOrEmpty()) {
                    return StageEResult(passed = true, resolvedCount = addresses.size)
                }
            } catch (e: Throwable) {
                lastError = "${e.javaClass.simpleName}: ${e.message ?: "unable to resolve $domain"}"
            }
        }
        return StageEResult(
            passed = false,
            resolvedCount = 0,
            failureReason = "Stage E: Tunnel DNS resolution failed (${lastError ?: "no records returned"})"
        )
    }

    fun checkInternetConnectivityStageF(): StageFResult {
        var lastError: String? = null
        for (probeUrl in PROBE_HTTP_URLS) {
            var connection: HttpURLConnection? = null
            try {
                connection = (URL(probeUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = STAGE_F_TIMEOUT_MS
                    readTimeout = STAGE_F_TIMEOUT_MS
                    instanceFollowRedirects = false
                    useCaches = false
                    setRequestProperty("Cache-Control", "no-cache")
                    setRequestProperty("Connection", "close")
                }
                val statusCode = connection.responseCode
                if (statusCode == 204 || statusCode in 200..299) {
                    return StageFResult(passed = true, httpStatusCode = statusCode)
                }
                lastError = "Unexpected HTTP status $statusCode from $probeUrl"
            } catch (e: Throwable) {
                lastError = "${e.javaClass.simpleName}: ${e.message ?: "HTTP probe failed"}"
            } finally {
                runCatching { connection?.disconnect() }
            }
        }
        return StageFResult(
            passed = false,
            httpStatusCode = null,
            failureReason = "Stage F: HTTP 204 connectivity probe failed through tunnel (${lastError ?: "timeout"})"
        )
    }
}
