package com.nexora.x.core.engine

import com.nexora.x.core.model.Profile
import com.nexora.x.core.model.ProfileType
import com.nexora.x.feature.profile.parser.ShadowsocksParser
import com.nexora.x.feature.profile.parser.UriParserUtils
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates and validates `sing-box` v1.10.7 JSON configurations from saved Phase 2 [Profile] records.
 *
 * Supported protocols:
 * - VLESS (`vless`)
 * - VMess (`vmess`)
 * - Trojan (`trojan`)
 * - Shadowsocks (`shadowsocks`)
 *
 * Phase 4 Connectivity & Routing Fixes (sing-box v1.10.7 schema):
 * 1. DNS Bootstrap Rule (`"rules": [{ "outbound": "any", "server": "dns-direct" }]`):
 *    Prevents the circular DNS deadlock where sing-box attempts to resolve the proxy server's
 *    own hostname using `"final": "dns-remote"` (which detours through `"proxy"` before `"proxy"`
 *    can connect).
 * 2. Direct DNS Resolver (`"dns-direct"` -> `"address": "1.1.1.1"`, `"detour": "direct"`):
 *    Replaces `"address": "local"` which fails on Android when `LocalDNSTransport` is not registered,
 *    and sets `"address_resolver": "dns-direct"` on `"dns-remote"`.
 * 3. Userspace TUN Stack (`"stack": "gvisor"`):
 *    Fixes the Android kernel UID loopback blackhole that occurs when `"stack": "system"` is combined
 *    with `VpnService.Builder.addDisallowedApplication(packageName)`.
 * 4. Route Rules:
 *    Routes `"protocol": "dns"` to `"dns-out"` and `"ip_is_private": true` to `"direct"`, with
 *    `"final": "proxy"` and `"auto_detect_interface": true`.
 *
 * Security & Correctness Invariants:
 * - Never replaces missing credentials with fake/default secrets.
 * - Rejects unsupported transport or TLS combinations before starting the engine.
 * - Produces a redacted summary alongside the raw JSON so diagnostics never print credentials.
 */
@Singleton
class SingBoxConfigGenerator @Inject constructor() {

    data class GeneratedSingBoxConfig(
        val rawJson: String,
        val redactedSummary: String,
        val warnings: List<String>
    )

    companion object {
        private val SUPPORTED_SINGBOX_TRANSPORTS = setOf("tcp", "ws", "grpc", "http", "h2", "quic", "httpupgrade")
        private val SUPPORTED_TLS_MODES = setOf("none", "tls", "reality")
    }

    /**
     * Validates [profile] and generates a complete sing-box v1.10.7 configuration JSON.
     * Throws [EngineException.InvalidConfigurationException] if any required field is invalid.
     */
    @Throws(EngineException.InvalidConfigurationException::class)
    fun generateAndValidate(profile: Profile): GeneratedSingBoxConfig {
        validateProfileFields(profile)

        val warnings = mutableListOf<String>()
        warnings.addAll(profile.unsupportedWarnings)

        val outboundJson = buildProxyOutboundJson(profile)
        val fullJson = buildString {
            append("{\n")
            append("  \"log\": {\n")
            append("    \"level\": \"info\",\n")
            append("    \"timestamp\": true\n")
            append("  },\n")
            append("  \"dns\": {\n")
            append("    \"servers\": [\n")
            append("      {\n")
            append("        \"tag\": \"dns-remote\",\n")
            append("        \"address\": \"https://1.1.1.1/dns-query\",\n")
            append("        \"address_resolver\": \"dns-direct\",\n")
            append("        \"strategy\": \"prefer_ipv4\",\n")
            append("        \"detour\": \"proxy\"\n")
            append("      },\n")
            append("      {\n")
            append("        \"tag\": \"dns-direct\",\n")
            append("        \"address\": \"1.1.1.1\",\n")
            append("        \"strategy\": \"prefer_ipv4\",\n")
            append("        \"detour\": \"direct\"\n")
            append("      }\n")
            append("    ],\n")
            append("    \"rules\": [\n")
            append("      {\n")
            append("        \"outbound\": \"any\",\n")
            append("        \"server\": \"dns-direct\"\n")
            append("      }\n")
            append("    ],\n")
            append("    \"final\": \"dns-remote\",\n")
            append("    \"strategy\": \"prefer_ipv4\",\n")
            append("    \"independent_cache\": true\n")
            append("  },\n")
            append("  \"inbounds\": [\n")
            append("    {\n")
            append("      \"type\": \"tun\",\n")
            append("      \"tag\": \"tun-in\",\n")
            append("      \"interface_name\": \"tun0\",\n")
            append("      \"inet4_address\": \"172.19.0.1/30\",\n")
            append("      \"mtu\": 1500,\n")
            append("      \"auto_route\": true,\n")
            append("      \"strict_route\": true,\n")
            append("      \"stack\": \"gvisor\",\n")
            append("      \"sniff\": true,\n")
            append("      \"sniff_override_destination\": false,\n")
            append("      \"domain_strategy\": \"prefer_ipv4\"\n")
            append("    }\n")
            append("  ],\n")
            append("  \"outbounds\": [\n")
            append(outboundJson.prependIndent("    ")).append(",\n")
            append("    {\n")
            append("      \"type\": \"direct\",\n")
            append("      \"tag\": \"direct\",\n")
            append("      \"domain_strategy\": \"prefer_ipv4\"\n")
            append("    },\n")
            append("    {\n")
            append("      \"type\": \"dns\",\n")
            append("      \"tag\": \"dns-out\"\n")
            append("    },\n")
            append("    {\n")
            append("      \"type\": \"block\",\n")
            append("      \"tag\": \"block\"\n")
            append("    }\n")
            append("  ],\n")
            append("  \"route\": {\n")
            append("    \"auto_detect_interface\": true,\n")
            append("    \"final\": \"proxy\",\n")
            append("    \"rules\": [\n")
            append("      {\n")
            append("        \"protocol\": \"dns\",\n")
            append("        \"outbound\": \"dns-out\"\n")
            append("      },\n")
            append("      {\n")
            append("        \"ip_is_private\": true,\n")
            append("        \"outbound\": \"direct\"\n")
            append("      }\n")
            append("    ]\n")
            append("  }\n")
            append("}")
        }

        val net = profile.transportConfig.networkType.lowercase().ifBlank { "tcp" }
        val sec = profile.tlsConfig.securityMode.lowercase().ifBlank { "none" }
        val summary =
            "VALIDATED · ${profile.protocol.displayName} -> ${profile.serverAddress}:${profile.port} (transport=$net, security=$sec, stack=gvisor, dns=bootstrap+doh, tun=172.19.0.1/30)"

        return GeneratedSingBoxConfig(
            rawJson = fullJson,
            redactedSummary = summary,
            warnings = warnings
        )
    }

    @Throws(EngineException.InvalidConfigurationException::class)
    fun validateProfileFields(profile: Profile) {
        val host = profile.serverAddress.trim()
        if (!UriParserUtils.isValidHost(host)) {
            throw EngineException.InvalidConfigurationException(
                "Invalid server address for profile '${profile.displayName}'."
            )
        }

        if (!UriParserUtils.isValidPort(profile.port)) {
            throw EngineException.InvalidConfigurationException(
                "Invalid port (${profile.port}) for profile '${profile.displayName}'. Port must be 1..65535."
            )
        }

        val cred = profile.credential.trim()
        if (cred.isEmpty()) {
            throw EngineException.InvalidConfigurationException(
                "Missing required credential for ${profile.protocol.displayName} profile '${profile.displayName}'."
            )
        }

        when (profile.protocol) {
            ProfileType.VLESS, ProfileType.VMESS -> {
                if (!UriParserUtils.isValidUuid(cred)) {
                    throw EngineException.InvalidConfigurationException(
                        "${profile.protocol.displayName} requires a valid 36-character UUID in sing-box."
                    )
                }
            }
            ProfileType.TROJAN -> {
                if (profile.tlsConfig.securityMode.lowercase() == "none") {
                    throw EngineException.InvalidConfigurationException(
                        "Trojan protocol requires TLS or Reality security mode in sing-box."
                    )
                }
            }
            ProfileType.SHADOWSOCKS -> {
                val method = profile.securityMethod.trim().lowercase()
                if (method.isEmpty() || method !in ShadowsocksParser.SUPPORTED_CIPHERS) {
                    throw EngineException.InvalidConfigurationException(
                        "Unsupported Shadowsocks cipher '$method' for sing-box."
                    )
                }
            }
        }

        val networkType = profile.transportConfig.networkType.trim().lowercase().ifBlank { "tcp" }
        if (networkType !in SUPPORTED_SINGBOX_TRANSPORTS) {
            throw EngineException.InvalidConfigurationException(
                "Unsupported sing-box transport '$networkType' in profile '${profile.displayName}'."
            )
        }

        val securityMode = profile.tlsConfig.securityMode.trim().lowercase().ifBlank { "none" }
        if (securityMode !in SUPPORTED_TLS_MODES) {
            throw EngineException.InvalidConfigurationException(
                "Unsupported TLS security mode '$securityMode' in profile '${profile.displayName}'."
            )
        }

        if (securityMode == "reality") {
            if (profile.tlsConfig.realityPublicKey.isBlank()) {
                throw EngineException.InvalidConfigurationException(
                    "Reality TLS requires a non-empty Public Key (pbk) in profile '${profile.displayName}'."
                )
            }
            if (profile.tlsConfig.sni.isBlank()) {
                throw EngineException.InvalidConfigurationException(
                    "Reality TLS requires a Server Name Indication (SNI) handshake target."
                )
            }
        }
    }

    private fun buildProxyOutboundJson(profile: Profile): String {
        val sb = StringBuilder()
        sb.append("{\n")
        when (profile.protocol) {
            ProfileType.VLESS -> {
                sb.append("  \"type\": \"vless\",\n")
                sb.append("  \"tag\": \"proxy\",\n")
                sb.append("  \"server\": \"${esc(profile.serverAddress.trim())}\",\n")
                sb.append("  \"server_port\": ${profile.port},\n")
                sb.append("  \"uuid\": \"${esc(profile.credential.trim())}\",\n")
                sb.append("  \"packet_encoding\": \"xudp\"")
                if (profile.transportConfig.flow.isNotBlank()) {
                    sb.append(",\n  \"flow\": \"${esc(profile.transportConfig.flow.trim())}\"")
                }
            }
            ProfileType.VMESS -> {
                sb.append("  \"type\": \"vmess\",\n")
                sb.append("  \"tag\": \"proxy\",\n")
                sb.append("  \"server\": \"${esc(profile.serverAddress.trim())}\",\n")
                sb.append("  \"server_port\": ${profile.port},\n")
                sb.append("  \"uuid\": \"${esc(profile.credential.trim())}\",\n")
                sb.append("  \"security\": \"${esc(profile.securityMethod.ifBlank { "auto" })}\",\n")
                sb.append("  \"alter_id\": ${profile.alterId},\n")
                sb.append("  \"packet_encoding\": \"xudp\"")
            }
            ProfileType.TROJAN -> {
                sb.append("  \"type\": \"trojan\",\n")
                sb.append("  \"tag\": \"proxy\",\n")
                sb.append("  \"server\": \"${esc(profile.serverAddress.trim())}\",\n")
                sb.append("  \"server_port\": ${profile.port},\n")
                sb.append("  \"password\": \"${esc(profile.credential)}\"")
            }
            ProfileType.SHADOWSOCKS -> {
                sb.append("  \"type\": \"shadowsocks\",\n")
                sb.append("  \"tag\": \"proxy\",\n")
                sb.append("  \"server\": \"${esc(profile.serverAddress.trim())}\",\n")
                sb.append("  \"server_port\": ${profile.port},\n")
                sb.append("  \"method\": \"${esc(profile.securityMethod.lowercase())}\",\n")
                sb.append("  \"password\": \"${esc(profile.credential)}\"")
            }
        }

        val tlsBlock = buildTlsJsonBlock(profile)
        if (tlsBlock != null) {
            sb.append(",\n  \"tls\": ").append(tlsBlock.prependIndent("  ").trimStart())
        }

        val transportBlock = buildTransportJsonBlock(profile)
        if (transportBlock != null) {
            sb.append(",\n  \"transport\": ").append(transportBlock.prependIndent("  ").trimStart())
        }

        sb.append("\n}")
        return sb.toString()
    }

    private fun buildTlsJsonBlock(profile: Profile): String? {
        val mode = profile.tlsConfig.securityMode.lowercase().ifBlank { "none" }
        if (!profile.tlsConfig.enabled && mode == "none") return null

        val sni = profile.tlsConfig.sni.ifBlank { profile.serverAddress }.trim()
        val alpnList = profile.tlsConfig.alpn
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        return buildString {
            append("{\n")
            append("  \"enabled\": true,\n")
            append("  \"server_name\": \"${esc(sni)}\",\n")
            append("  \"insecure\": ${profile.tlsConfig.allowInsecure}")
            if (alpnList.isNotEmpty()) {
                val alpnJson = alpnList.joinToString(", ") { "\"${esc(it)}\"" }
                append(",\n  \"alpn\": [$alpnJson]")
            }
            if (profile.tlsConfig.fingerprint.isNotBlank() || mode == "reality") {
                val fp = profile.tlsConfig.fingerprint.ifBlank { "chrome" }
                append(",\n  \"utls\": {\n")
                append("    \"enabled\": true,\n")
                append("    \"fingerprint\": \"${esc(fp)}\"\n")
                append("  }")
            }
            if (mode == "reality") {
                append(",\n  \"reality\": {\n")
                append("    \"enabled\": true,\n")
                append("    \"public_key\": \"${esc(profile.tlsConfig.realityPublicKey.trim())}\",\n")
                append("    \"short_id\": \"${esc(profile.tlsConfig.realityShortId.trim())}\"\n")
                append("  }")
            }
            append("\n}")
        }
    }

    private fun buildTransportJsonBlock(profile: Profile): String? {
        val net = profile.transportConfig.networkType.lowercase().ifBlank { "tcp" }
        if (net == "tcp") return null

        return when (net) {
            "ws" -> buildString {
                append("{\n")
                append("  \"type\": \"ws\",\n")
                append("  \"path\": \"${esc(profile.transportConfig.path.ifBlank { "/" })}\"")
                if (profile.transportConfig.host.isNotBlank()) {
                    append(",\n  \"headers\": {\n")
                    append("    \"Host\": \"${esc(profile.transportConfig.host.trim())}\"\n")
                    append("  }")
                }
                append("\n}")
            }
            "grpc" -> buildString {
                append("{\n")
                append("  \"type\": \"grpc\",\n")
                append("  \"service_name\": \"${esc(profile.transportConfig.serviceName.ifBlank { profile.transportConfig.path })}\"\n")
                append("}")
            }
            "http", "h2" -> buildString {
                append("{\n")
                append("  \"type\": \"http\",\n")
                if (profile.transportConfig.host.isNotBlank()) {
                    append("  \"host\": [\"${esc(profile.transportConfig.host.trim())}\"],\n")
                }
                append("  \"path\": \"${esc(profile.transportConfig.path.ifBlank { "/" })}\"\n")
                append("}")
            }
            "httpupgrade" -> buildString {
                append("{\n")
                append("  \"type\": \"httpupgrade\",\n")
                if (profile.transportConfig.host.isNotBlank()) {
                    append("  \"host\": \"${esc(profile.transportConfig.host.trim())}\",\n")
                }
                append("  \"path\": \"${esc(profile.transportConfig.path.ifBlank { "/" })}\"\n")
                append("}")
            }
            "quic" -> "{\n  \"type\": \"quic\"\n}"
            else -> null
        }
    }

    private fun esc(s: String): String =
        s.replace("\\", "\\\\").replace("\"", "\\\"")
}
