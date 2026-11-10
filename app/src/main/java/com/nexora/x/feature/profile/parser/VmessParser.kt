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
 * Dedicated VMess parser supporting both:
 * 1. V2RayN Base64-encoded JSON (`vmess://eyJ2IjoiMiIs...`)
 * 2. Standard URI-style VMess (`vmess://uuid@host:port?type=ws&security=tls#name`)
 *
 * Uses a self-contained RFC-8259 JSON object reader so unit tests execute cleanly on pure JVM
 * without depending on Android framework `org.json.JSONObject` stubs.
 */
@Singleton
class VmessParser @Inject constructor() {

    companion object {
        private val RECOGNIZED_JSON_KEYS = setOf(
            "v", "ps", "add", "port", "id", "aid", "scy", "security", "net",
            "type", "host", "path", "tls", "sni", "alpn", "fp", "allowInsecure"
        )
    }

    fun parse(rawUri: String, nowEpochMillis: Long = System.currentTimeMillis()): ParseResult {
        val trimmed = rawUri.trim()
        if (!trimmed.lowercase().startsWith("vmess://")) {
            return ParseResult.Failure("Link must start with vmess://", "uri")
        }

        val payload = trimmed.substring("vmess://".length).trim()
        if (payload.isEmpty()) {
            return ParseResult.Failure("VMess payload is empty.", "uri")
        }

        // Branch 1: Standard URI format if payload contains '@' and is not pure Base64 JSON
        if (payload.contains('@') && !payload.trimStart().startsWith("{")) {
            return parseUriStyleVmess(trimmed, nowEpochMillis)
        }

        // Branch 2: Base64 JSON format (or direct JSON after vmess://)
        val decodedJson = if (payload.startsWith("{")) {
            payload
        } else {
            UriParserUtils.decodeBase64OrNull(payload)
                ?: return ParseResult.Failure("VMess Base64 payload could not be decoded.", "uri")
        }

        val map = parseFlatJsonObject(decodedJson)
            ?: return ParseResult.Failure("VMess configuration is not a valid JSON object.", "uri")

        val host = (map["add"] ?: "").trim()
        if (!UriParserUtils.isValidHost(host)) {
            return ParseResult.Failure("VMess server address ('add') is missing or invalid.", "serverAddress")
        }

        val port = map["port"]?.trim()?.toIntOrNull()
        if (!UriParserUtils.isValidPort(port)) {
            return ParseResult.Failure("VMess port must be an integer between 1 and 65535.", "port")
        }

        val uuid = (map["id"] ?: "").trim()
        if (uuid.isEmpty()) {
            return ParseResult.Failure("VMess user ID ('id') is required.", "credential")
        }

        val warnings = mutableListOf<String>()
        if (!UriParserUtils.isValidUuid(uuid)) {
            warnings.add("VMess 'id' is not a canonical 36-character UUID.")
        }

        val alterId = map["aid"]?.trim()?.toIntOrNull() ?: 0
        if (alterId > 0) {
            warnings.add("VMess non-zero alterId ($alterId) is legacy MD5 header mode; AEAD (aid=0) is recommended.")
        }

        val cipher = (map["scy"] ?: map["security"] ?: "auto").trim().ifBlank { "auto" }
        val networkType = (map["net"] ?: "tcp").trim().lowercase().ifBlank { "tcp" }
        if (networkType !in UriParserUtils.SUPPORTED_NETWORKS) {
            warnings.add("Unsupported VMess network type '$networkType'.")
        }

        val tlsRaw = (map["tls"] ?: "").trim().lowercase()
        val tlsEnabled = tlsRaw == "tls" || tlsRaw == "true" || tlsRaw == "1"
        val securityMode = if (tlsEnabled) "tls" else "none"

        val sni = (map["sni"] ?: "").trim()
        val alpn = (map["alpn"] ?: "").trim()
        val fingerprint = (map["fp"] ?: "").trim()
        val allowInsecure = map["allowInsecure"] == "1" || map["allowInsecure"]?.lowercase() == "true"

        val headerType = (map["type"] ?: "none").trim().ifBlank { "none" }
        val transportHost = (map["host"] ?: "").trim()
        val transportPath = (map["path"] ?: "").trim()
        val serviceName = if (networkType == "grpc") transportPath else ""

        map.keys.forEach { k ->
            if (k !in RECOGNIZED_JSON_KEYS) {
                warnings.add("Unrecognized VMess JSON field '$k' preserved in warning log.")
            }
        }

        val rawName = (map["ps"] ?: "").trim()
        val displayName = UriParserUtils.urlDecode(rawName).ifBlank { "VMess $host:$port" }

        val profile = Profile(
            id = UUID.randomUUID().toString(),
            displayName = displayName,
            protocol = ProfileType.VMESS,
            serverAddress = host,
            port = port!!,
            credential = uuid,
            securityMethod = cipher,
            alterId = alterId,
            tlsConfig = TlsConfig(
                enabled = tlsEnabled,
                securityMode = securityMode,
                sni = sni,
                alpn = alpn,
                fingerprint = fingerprint,
                allowInsecure = allowInsecure
            ),
            transportConfig = TransportConfig(
                networkType = networkType,
                host = transportHost,
                path = transportPath,
                serviceName = serviceName,
                headerType = headerType
            ),
            unsupportedWarnings = warnings,
            createdAtEpochMillis = nowEpochMillis,
            updatedAtEpochMillis = nowEpochMillis
        )

        return ParseResult.Success(profile = profile, warnings = warnings)
    }

    private fun parseUriStyleVmess(rawUri: String, nowEpochMillis: Long): ParseResult {
        val parts = UriParserUtils.parseStandardProxyUri(rawUri, "vmess").getOrElse { err ->
            return ParseResult.Failure(err.message ?: "Invalid VMess URI.", "uri")
        }
        val uuid = parts.userInfo.trim()
        if (uuid.isEmpty()) {
            return ParseResult.Failure("VMess UUID is required.", "credential")
        }
        if (!UriParserUtils.isValidHost(parts.host)) {
            return ParseResult.Failure("VMess server address is invalid.", "serverAddress")
        }
        val port = parts.port
        if (!UriParserUtils.isValidPort(port)) {
            return ParseResult.Failure("VMess port must be between 1 and 65535.", "port")
        }

        val q = parts.queryParams
        val networkType = (q["type"] ?: q["net"] ?: "tcp").lowercase()
        val securityMode = (q["security"] ?: q["tls"] ?: "none").lowercase()
        val cipher = q["encryption"] ?: q["scy"] ?: "auto"
        val displayName = parts.fragment.ifBlank { "VMess ${parts.host}:$port" }

        val profile = Profile(
            id = UUID.randomUUID().toString(),
            displayName = displayName,
            protocol = ProfileType.VMESS,
            serverAddress = parts.host,
            port = port!!,
            credential = uuid,
            securityMethod = cipher,
            alterId = q["aid"]?.toIntOrNull() ?: 0,
            tlsConfig = TlsConfig(
                enabled = securityMode == "tls",
                securityMode = if (securityMode == "tls") "tls" else "none",
                sni = q["sni"] ?: "",
                alpn = q["alpn"] ?: "",
                fingerprint = q["fp"] ?: ""
            ),
            transportConfig = TransportConfig(
                networkType = networkType,
                host = q["host"] ?: "",
                path = q["path"] ?: "",
                serviceName = q["serviceName"] ?: ""
            ),
            createdAtEpochMillis = nowEpochMillis,
            updatedAtEpochMillis = nowEpochMillis
        )
        return ParseResult.Success(profile)
    }

    fun toUri(profile: Profile): String {
        val jsonFields = linkedMapOf(
            "v" to "2",
            "ps" to profile.displayName,
            "add" to profile.serverAddress,
            "port" to profile.port.toString(),
            "id" to profile.credential,
            "aid" to profile.alterId.toString(),
            "scy" to profile.securityMethod.ifBlank { "auto" },
            "net" to profile.transportConfig.networkType.ifBlank { "tcp" },
            "type" to profile.transportConfig.headerType.ifBlank { "none" },
            "host" to profile.transportConfig.host,
            "path" to profile.transportConfig.path,
            "tls" to if (profile.tlsConfig.enabled) "tls" else "",
            "sni" to profile.tlsConfig.sni,
            "alpn" to profile.tlsConfig.alpn,
            "fp" to profile.tlsConfig.fingerprint
        )
        val jsonString = buildString {
            append("{")
            jsonFields.entries.forEachIndexed { idx, (k, v) ->
                if (idx > 0) append(",")
                append("\"").append(escapeJson(k)).append("\":\"").append(escapeJson(v)).append("\"")
            }
            append("}")
        }
        return "vmess://${UriParserUtils.encodeBase64(jsonString)}"
    }

    private fun escapeJson(s: String): String =
        s.replace("\\", "\\\\").replace("\"", "\\\"")

    /**
     * Lightweight, dependency-free flat JSON object parser supporting string, number, and boolean values.
     */
    internal fun parseFlatJsonObject(json: String): Map<String, String>? {
        val trimmed = json.trim()
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return null
        val content = trimmed.substring(1, trimmed.length - 1).trim()
        if (content.isEmpty()) return emptyMap()

        val result = linkedMapOf<String, String>()
        var i = 0
        val n = content.length

        fun skipWhitespace() {
            while (i < n && content[i].isWhitespace()) i++
        }

        fun readString(): String? {
            skipWhitespace()
            if (i >= n || content[i] != '"') return null
            i++ // skip opening quote
            val sb = StringBuilder()
            while (i < n) {
                val c = content[i++]
                if (c == '\\') {
                    if (i >= n) return null
                    when (val esc = content[i++]) {
                        '"', '\\', '/' -> sb.append(esc)
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            if (i + 4 > n) return null
                            val hex = content.substring(i, i + 4)
                            val code = hex.toIntOrNull(16) ?: return null
                            sb.append(code.toChar())
                            i += 4
                        }
                        else -> sb.append(esc)
                    }
                } else if (c == '"') {
                    return sb.toString()
                } else {
                    sb.append(c)
                }
            }
            return null
        }

        while (i < n) {
            skipWhitespace()
            if (i >= n) break
            val key = readString() ?: return null
            skipWhitespace()
            if (i >= n || content[i] != ':') return null
            i++ // skip ':'
            skipWhitespace()
            if (i >= n) return null

            val value: String = if (content[i] == '"') {
                readString() ?: return null
            } else {
                val start = i
                while (i < n && content[i] != ',' && content[i] != '}') {
                    i++
                }
                content.substring(start, i).trim()
            }
            result[key] = value
            skipWhitespace()
            if (i < n) {
                if (content[i] == ',') {
                    i++
                } else {
                    return null
                }
            }
        }
        return result
    }
}
