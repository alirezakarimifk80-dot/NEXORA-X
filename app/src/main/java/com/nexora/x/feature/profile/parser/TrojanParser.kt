package com.nexora.x.feature.profile.parser

import com.nexora.x.core.model.ParseResult
import com.nexora.x.core.model.Profile
import com.nexora.x.core.model.ProfileType
import com.nexora.x.core.model.TlsConfig
import com.nexora.x.core.model.TransportConfig
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dedicated Trojan URI parser (`trojan://password@host:port?params#name`).
 *
 * Extracts:
 * - Password (URL-decoded), server address, port, display name
 * - TLS configuration (defaults to `tls` unless overridden by `security=reality` or `security=none`)
 * - SNI (`sni` or `peer`), ALPN, Fingerprint (`fp`), `allowInsecure`
 * - Transport options (`type`: tcp, ws, grpc, h2), `host`, `path`, `serviceName`
 * - Preserves warnings for unsupported query parameters.
 */
@Singleton
class TrojanParser @Inject constructor() {

    companion object {
        private val RECOGNIZED_QUERY_KEYS = setOf(
            "security", "type", "sni", "peer", "host", "path", "serviceName",
            "alpn", "fp", "allowInsecure", "insecure", "pbk", "sid", "spx", "headerType", "flow"
        )
    }

    fun parse(rawUri: String, nowEpochMillis: Long = System.currentTimeMillis()): ParseResult {
        val partsResult = UriParserUtils.parseStandardProxyUri(rawUri, "trojan")
        val parts = partsResult.getOrElse { err ->
            return ParseResult.Failure(
                errorMessage = err.message ?: "Invalid Trojan URI structure.",
                fieldHint = "uri"
            )
        }

        val password = parts.userInfo
        if (password.isBlank()) {
            return ParseResult.Failure("Trojan profile requires a non-empty password.", "credential")
        }

        if (!UriParserUtils.isValidHost(parts.host)) {
            return ParseResult.Failure("Trojan server address is invalid.", "serverAddress")
        }

        val port = parts.port
        if (!UriParserUtils.isValidPort(port)) {
            return ParseResult.Failure("Trojan port must be between 1 and 65535.", "port")
        }

        val q = parts.queryParams
        val warnings = mutableListOf<String>()

        val networkType = (q["type"] ?: "tcp").lowercase().ifBlank { "tcp" }
        if (networkType !in UriParserUtils.SUPPORTED_NETWORKS) {
            warnings.add("Unsupported Trojan transport network '$networkType'.")
        }

        // Trojan defaults to TLS enabled unless explicitly specified otherwise
        val securityMode = (q["security"] ?: "tls").lowercase().ifBlank { "tls" }
        if (securityMode !in UriParserUtils.SUPPORTED_SECURITIES) {
            warnings.add("Unsupported Trojan security mode '$securityMode'.")
        }

        val sni = q["sni"] ?: q["peer"] ?: parts.host
        val alpn = q["alpn"] ?: ""
        val fingerprint = q["fp"] ?: ""
        val allowInsecure = q["allowInsecure"] == "1" || q["allowInsecure"]?.lowercase() == "true" ||
            q["insecure"] == "1" || q["insecure"]?.lowercase() == "true"

        val realityPublicKey = q["pbk"] ?: ""
        val realityShortId = q["sid"] ?: ""

        q.keys.forEach { key ->
            if (key !in RECOGNIZED_QUERY_KEYS) {
                warnings.add("Unrecognized Trojan parameter '$key' preserved in warning log.")
            }
        }

        val displayName = parts.fragment.ifBlank { "Trojan ${parts.host}:$port" }

        val profile = Profile(
            id = UUID.randomUUID().toString(),
            displayName = displayName,
            protocol = ProfileType.TROJAN,
            serverAddress = parts.host,
            port = port!!,
            credential = password,
            securityMethod = "tls",
            alterId = 0,
            tlsConfig = TlsConfig(
                enabled = securityMode != "none",
                securityMode = securityMode,
                sni = sni,
                alpn = alpn,
                fingerprint = fingerprint,
                allowInsecure = allowInsecure,
                realityPublicKey = realityPublicKey,
                realityShortId = realityShortId
            ),
            transportConfig = TransportConfig(
                networkType = networkType,
                host = q["host"] ?: "",
                path = q["path"] ?: "",
                serviceName = q["serviceName"] ?: "",
                headerType = q["headerType"] ?: "none",
                flow = q["flow"] ?: ""
            ),
            unsupportedWarnings = warnings,
            createdAtEpochMillis = nowEpochMillis,
            updatedAtEpochMillis = nowEpochMillis
        )

        return ParseResult.Success(profile = profile, warnings = warnings)
    }

    fun toUri(profile: Profile): String {
        val params = linkedMapOf<String, String>()
        params["security"] = profile.tlsConfig.securityMode.ifBlank { "tls" }
        params["type"] = profile.transportConfig.networkType.ifBlank { "tcp" }
        if (profile.tlsConfig.sni.isNotBlank()) params["sni"] = profile.tlsConfig.sni
        if (profile.tlsConfig.alpn.isNotBlank()) params["alpn"] = profile.tlsConfig.alpn
        if (profile.tlsConfig.fingerprint.isNotBlank()) params["fp"] = profile.tlsConfig.fingerprint
        if (profile.tlsConfig.allowInsecure) params["allowInsecure"] = "1"
        if (profile.transportConfig.host.isNotBlank()) params["host"] = profile.transportConfig.host
        if (profile.transportConfig.path.isNotBlank()) params["path"] = profile.transportConfig.path
        if (profile.transportConfig.serviceName.isNotBlank()) params["serviceName"] = profile.transportConfig.serviceName

        val queryString = params.entries.joinToString("&") { (k, v) ->
            "${UriParserUtils.urlEncode(k)}=${UriParserUtils.urlEncode(v)}"
        }
        val encodedName = UriParserUtils.urlEncode(profile.displayName)
        return "trojan://${UriParserUtils.urlEncode(profile.credential)}@${profile.serverAddress}:${profile.port}?$queryString#$encodedName"
    }
}
