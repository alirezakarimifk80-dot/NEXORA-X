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
 * Dedicated Shadowsocks (`ss://`) URI parser supporting:
 * 1. SIP002 format: `ss://BASE64(method:password)@host:port/?plugin=...#name`
 * 2. Plain SIP002 userinfo: `ss://method:password@host:port#name`
 * 3. Legacy full-Base64 format: `ss://BASE64(method:password@host:port)#name`
 */
@Singleton
class ShadowsocksParser @Inject constructor() {

    companion object {
        val SUPPORTED_CIPHERS = setOf(
            "aes-128-gcm",
            "aes-192-gcm",
            "aes-256-gcm",
            "chacha20-ietf-poly1305",
            "xchacha20-ietf-poly1305",
            "2022-blake3-aes-128-gcm",
            "2022-blake3-aes-256-gcm",
            "2022-blake3-chacha20-poly1305",
            "none",
            "plain"
        )
    }

    fun parse(rawUri: String, nowEpochMillis: Long = System.currentTimeMillis()): ParseResult {
        val trimmed = rawUri.trim()
        if (!trimmed.lowercase().startsWith("ss://")) {
            return ParseResult.Failure("Link must start with ss://", "uri")
        }

        val withoutScheme = trimmed.substring("ss://".length)
        val fragmentIdx = withoutScheme.indexOf('#')
        val fragment = if (fragmentIdx >= 0) {
            UriParserUtils.urlDecode(withoutScheme.substring(fragmentIdx + 1)).trim()
        } else {
            ""
        }
        val beforeFragment = if (fragmentIdx >= 0) {
            withoutScheme.substring(0, fragmentIdx)
        } else {
            withoutScheme
        }

        val queryIdx = beforeFragment.indexOf('?')
        val rawQuery = if (queryIdx >= 0) beforeFragment.substring(queryIdx + 1) else ""
        val mainPart = (if (queryIdx >= 0) beforeFragment.substring(0, queryIdx) else beforeFragment)
            .removeSuffix("/")

        val queryMap = UriParserUtils.parseQueryParameters(rawQuery)
        val warnings = mutableListOf<String>()

        val resolvedAuthority = if (mainPart.contains('@')) {
            mainPart
        } else {
            UriParserUtils.decodeBase64OrNull(mainPart)
                ?: return ParseResult.Failure("Shadowsocks Base64 payload is malformed.", "uri")
        }

        val atIdx = resolvedAuthority.lastIndexOf('@')
        if (atIdx <= 0) {
            return ParseResult.Failure("Shadowsocks link is missing '@' separator between cipher:password and host:port.", "uri")
        }

        val rawUserInfo = UriParserUtils.urlDecode(resolvedAuthority.substring(0, atIdx))
        val hostPortPart = resolvedAuthority.substring(atIdx + 1)

        val decodedUserInfo = if (rawUserInfo.contains(':')) {
            rawUserInfo
        } else {
            UriParserUtils.decodeBase64OrNull(rawUserInfo)
                ?: return ParseResult.Failure("Shadowsocks userinfo is neither 'method:password' nor valid Base64.", "credential")
        }

        val colonIdx = decodedUserInfo.indexOf(':')
        if (colonIdx <= 0 || colonIdx == decodedUserInfo.length - 1) {
            return ParseResult.Failure("Shadowsocks userinfo must contain 'cipher:password'.", "credential")
        }

        val method = decodedUserInfo.substring(0, colonIdx).trim().lowercase()
        val password = decodedUserInfo.substring(colonIdx + 1)

        if (password.isEmpty()) {
            return ParseResult.Failure("Shadowsocks password cannot be empty.", "credential")
        }

        if (method !in SUPPORTED_CIPHERS) {
            warnings.add("Shadowsocks cipher '$method' is legacy or non-AEAD; verify Phase 3 sing-box compatibility.")
        }

        val (host, portStr) = UriParserUtils.splitHostAndPort(hostPortPart)
            ?: return ParseResult.Failure("Shadowsocks server host or port is malformed.", "serverAddress")

        if (!UriParserUtils.isValidHost(host)) {
            return ParseResult.Failure("Shadowsocks server address is invalid.", "serverAddress")
        }

        val port = portStr.toIntOrNull()
        if (!UriParserUtils.isValidPort(port)) {
            return ParseResult.Failure("Shadowsocks port must be between 1 and 65535.", "port")
        }

        val plugin = queryMap["plugin"] ?: ""
        if (plugin.isNotBlank()) {
            warnings.add("Shadowsocks SIP003 plugin parameter ('$plugin') detected; external plugins require Phase 3 support.")
        }

        queryMap.keys.forEach { k ->
            if (k != "plugin" && k != "uot") {
                warnings.add("Unrecognized Shadowsocks query parameter '$k' preserved in warning log.")
            }
        }

        val displayName = fragment.ifBlank { "SS $host:$port" }

        val profile = Profile(
            id = UUID.randomUUID().toString(),
            displayName = displayName,
            protocol = ProfileType.SHADOWSOCKS,
            serverAddress = host,
            port = port!!,
            credential = password,
            securityMethod = method,
            alterId = 0,
            tlsConfig = TlsConfig(enabled = false, securityMode = "none"),
            transportConfig = TransportConfig(networkType = "tcp"),
            unsupportedWarnings = warnings,
            createdAtEpochMillis = nowEpochMillis,
            updatedAtEpochMillis = nowEpochMillis
        )

        return ParseResult.Success(profile = profile, warnings = warnings)
    }

    fun toUri(profile: Profile): String {
        val userInfoRaw = "${profile.securityMethod.ifBlank { "aes-256-gcm" }}:${profile.credential}"
        val encodedUserInfo = UriParserUtils.encodeBase64(userInfoRaw, urlSafeNoPadding = true)
        val encodedName = UriParserUtils.urlEncode(profile.displayName)
        return "ss://$encodedUserInfo@${profile.serverAddress}:${profile.port}#$encodedName"
    }
}
