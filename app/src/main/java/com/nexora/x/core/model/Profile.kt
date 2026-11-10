package com.nexora.x.core.model

/**
 * Sorting strategies for the Servers profile list.
 */
enum class ProfileSortOrder(val label: String) {
    RECENTLY_UPDATED("Recently Updated"),
    NAME_ASC("Name (A–Z)"),
    PROTOCOL("Protocol"),
    PORT_ASC("Port (Low–High)")
}

/**
 * TLS configuration for a proxy profile.
 */
data class TlsConfig(
    val enabled: Boolean = false,
    val securityMode: String = "none", // none, tls, reality
    val sni: String = "",
    val alpn: String = "",
    val fingerprint: String = "",
    val allowInsecure: Boolean = false,
    val realityPublicKey: String = "",
    val realityShortId: String = ""
)

/**
 * Transport / Network layer configuration for a proxy profile.
 */
data class TransportConfig(
    val networkType: String = "tcp", // tcp, ws, grpc, http, h2, quic, kcp
    val host: String = "",
    val path: String = "",
    val serviceName: String = "",
    val headerType: String = "none",
    val flow: String = ""
)

/**
 * Complete domain model representing a saved proxy profile in NEXORA X Phase 2.
 *
 * Security Invariant:
 * [toString] redacts [credential] so UUIDs, passwords, or secrets are never accidentally
 * printed in logcat or stack traces.
 */
data class Profile(
    val id: String,
    val displayName: String,
    val protocol: ProfileType,
    val serverAddress: String,
    val port: Int,
    val credential: String, // UUID for VLESS/VMess, Password for Trojan/Shadowsocks
    val securityMethod: String, // e.g., auto, aes-128-gcm, chacha20-ietf-poly1305, none
    val alterId: Int = 0,
    val tlsConfig: TlsConfig = TlsConfig(),
    val transportConfig: TransportConfig = TransportConfig(),
    val unsupportedWarnings: List<String> = emptyList(),
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val isSelected: Boolean = false
) {
    override fun toString(): String {
        return "Profile(id=$id, displayName=$displayName, protocol=${protocol.badgeLabel}, " +
            "endpoint=$serverAddress:$port, network=${transportConfig.networkType}, " +
            "tls=${tlsConfig.securityMode}, credential=[REDACTED], warnings=${unsupportedWarnings.size})"
    }
}
