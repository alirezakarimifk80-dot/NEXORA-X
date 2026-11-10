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
 * Dedicated VLESS URI parser (`vless://uuid@host:port?params#name`).
 *
 * Extracts:
 * - UUID, server address, port, display name (URL-decoded)
 * - Transport (`type`: tcp, ws, grpc, http, h2, quic, kcp, httpupgrade), `host`, `path`, `serviceName`, `headerType`, `flow`
 * - TLS (`security`: none, tls, reality), `sni`, `alpn`, `fp`, `allowInsecure`, `pbk`, `sid`
 * - Emits explicit warnings for unrecognized or experimental parameters without silently discarding them.
 */
@Singleton
class VlessParser @Inject constructor() {

    companion object {
        private val RECOGNIZED_QUERY_KEYS = setOf(
            "type", "security", "encryption", "sni", "ServerName", "host", "path",
            "serviceName", "mode", "alpn", "fp", "pbk", "sid", "spx", "flow",
            "headerType", "allowInsecure", "insecure"
        )
    }

    fun parse(rawUri: String, nowEpochMillis: Long = System.currentTimeMillis()): ParseResult {
        val partsResult = UriParserUtils.parseStandardProxyUri(rawUri, "vless")
        val parts = partsResult.getOrElse { err ->
            return ParseResult.Failure(
                errorMessage = err.message ?: "Invalid VLESS URI structure.",
                fieldHint = "uri"
            )
        }

        val uuid = parts.userInfo.trim()
        if (uuid.isEmpty()) {
            return ParseResult.Failure("VLESS profile requires a non-empty UUID.", "credential")
        }
        if (!UriParserUtils.isValidUuid(uuid) && uuid.length < 6) {
            return ParseResult.Failure("VLESS UUID format is invalid or too short.", "credential")
        }

        if (!UriParserUtils.isValidHost(parts.host)) {
            return ParseResult.Failure("VLESS server address is invalid.", "serverAddress")
        }

        val port = parts.port
        if (!UriParserUtils.isValidPort(port)) {
            return ParseResult.Failure("VLESS port must be between 1 and 65535.", "port")
        }

        val q = parts.queryParams
        val warnings = mutableListOf<String>()

        if (!UriParserUtils.isValidUuid(uuid)) {
            warnings.add("Non-standard VLESS UUID format detected (custom user ID string).")
        }

        val encryption = q["encryption"]?.ifBlank { "none" } ?: "none"
        if (encryption.lowercase() != "none") {
            warnings.add("Unsupported VLESS encryption parameter '$encryption' (standard VLESS uses 'none').")
        }

        val networkType = (q["type"] ?: "tcp").lowercase().ifBlank { "tcp" }
        if (networkType !in UriParserUtils.SUPPORTED_NETWORKS) {
            warnings.add("Unsupported transport network type '$networkType'; behavior may vary in Phase 3 engine.")
        }

        val securityMode = (q["security"] ?: "none").lowercase().ifBlank { "none" }
        if (securityMode !in UriParserUtils.SUPPORTED_SECURITIES) {
            warnings.add("Unsupported VLESS security mode '$securityMode'.")
        }

        val sni = q["sni"] ?: q["ServerName"] ?: ""
        val alpn = q["alpn"] ?: ""
        val fingerprint = q["fp"] ?: ""
        val allowInsecure = q["allowInsecure"] == "1" || q["allowInsecure"]?.lowercase() == "true" ||
            q["insecure"] == "1" || q["insecure"]?.lowercase() == "true"
        val realityPublicKey = q["pbk"] ?: ""
        val realityShortId = q["sid"] ?: ""

        if (securityMode == "reality" && realityPublicKey.isBlank()) {
            return ParseResult.Failure("VLESS Reality configuration requires a public key (pbk).", "tls")
        }

        val transportHost = q["host"] ?: ""
        val transportPath = q["path"] ?: ""
        val serviceName = q["serviceName"] ?: ""
        val headerType = q["headerType"] ?: "none"
        val flow = q["flow"] ?: ""

        q.keys.forEach { key ->
            if (key !in RECOGNIZED_QUERY_KEYS) {
                warnings.add("Unrecognized VLESS parameter '$key' preserved in profile warning log.")
            }
        }

        val displayName = parts.fragment.ifBlank { "VLESS ${parts.host}:$port" }

        val profile = Profile(
            id = UUID.randomUUID().toString(),
            displayName = displayName,
            protocol = ProfileType.VLESS,
            serverAddress = parts.host,
            port = port!!,
            credential = uuid,
            securityMethod = encryption,
            alterId = 0,
            tlsConfig = TlsConfig(
                enabled = securityMode == "tls" || securityMode == "reality",
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
                host = transportHost,
                path = transportPath,
                serviceName = serviceName,
                headerType = headerType,
                flow = flow
            ),
            unsupportedWarnings = warnings,
            createdAtEpochMillis = nowEpochMillis,
            updatedAtEpochMillis = nowEpochMillis
        )

        return ParseResult.Success(profile = profile, warnings = warnings)
    }

    fun toUri(profile: Profile): String {
        val params = linkedMapOf<String, String>()
        params["encryption"] = profile.securityMethod.ifBlank { "none" }
        params["type"] = profile.transportConfig.networkType.ifBlank { "tcp" }
        params["security"] = profile.tlsConfig.securityMode.ifBlank { "none" }
        if (profile.tlsConfig.sni.isNotBlank()) params["sni"] = profile.tlsConfig.sni
        if (profile.tlsConfig.alpn.isNotBlank()) params["alpn"] = profile.tlsConfig.alpn
        if (profile.tlsConfig.fingerprint.isNotBlank()) params["fp"] = profile.tlsConfig.fingerprint
        if (profile.tlsConfig.allowInsecure) params["allowInsecure"] = "1"
        if (profile.tlsConfig.realityPublicKey.isNotBlank()) params["pbk"] = profile.tlsConfig.realityPublicKey
        if (profile.tlsConfig.realityShortId.isNotBlank()) params["sid"] = profile.tlsConfig.realityShortId
        if (profile.transportConfig.host.isNotBlank()) params["host"] = profile.transportConfig.host
        if (profile.transportConfig.path.isNotBlank()) params["path"] = profile.transportConfig.path
        if (profile.transportConfig.serviceName.isNotBlank()) params["serviceName"] = profile.transportConfig.serviceName
        if (profile.transportConfig.flow.isNotBlank()) params["flow"] = profile.transportConfig.flow

        val queryString = params.entries.joinToString("&") { (k, v) ->
            "${UriParserUtils.urlEncode(k)}=${UriParserUtils.urlEncode(v)}"
        }
        val encodedName = UriParserUtils.urlEncode(profile.displayName)
        return "vless://${UriParserUtils.urlEncode(profile.credential)}@${profile.serverAddress}:${profile.port}?$queryString#$encodedName"
    }
}
