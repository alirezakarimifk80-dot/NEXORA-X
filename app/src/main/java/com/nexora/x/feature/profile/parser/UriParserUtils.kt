package com.nexora.x.feature.profile.parser

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * Shared validation, URL decoding, Base64 decoding, and authority parsing utilities
 * used by the protocol parsers.
 *
 * Security Invariant:
 * Never includes raw credentials, UUIDs, passwords, or full configuration URIs in error messages.
 */
object UriParserUtils {

    val SUPPORTED_NETWORKS = setOf("tcp", "ws", "grpc", "http", "h2", "quic", "kcp", "httpupgrade")
    val SUPPORTED_SECURITIES = setOf("none", "tls", "reality")

    fun urlDecode(raw: String): String {
        return try {
            // Preserve '+' if it was part of a literal string rather than form-encoded space when appropriate,
            // while still decoding %XX sequences accurately.
            URLDecoder.decode(raw, StandardCharsets.UTF_8.name())
        } catch (_: Exception) {
            raw
        }
    }

    fun urlEncode(raw: String): String {
        return try {
            URLEncoder.encode(raw, StandardCharsets.UTF_8.name()).replace("+", "%20")
        } catch (_: Exception) {
            raw
        }
    }

    /**
     * Decodes standard or URL-safe Base64 with or without padding (`=`).
     * Returns null if input is not valid Base64.
     */
    fun decodeBase64OrNull(encoded: String): String? {
        val cleaned = encoded.trim()
            .replace("\n", "")
            .replace("\r", "")
            .replace(" ", "")
        if (cleaned.isEmpty()) return null

        val normalized = cleaned
            .replace('-', '+')
            .replace('_', '/')
        val padded = when (normalized.length % 4) {
            2 -> "$normalized=="
            3 -> "$normalized="
            0 -> normalized
            else -> return null
        }

        return try {
            val bytes = Base64.getDecoder().decode(padded)
            val text = String(bytes, StandardCharsets.UTF_8)
            if (text.any { it.code < 9 || (it.code in 14..31) }) null else text
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    fun encodeBase64(raw: String, urlSafeNoPadding: Boolean = false): String {
        val bytes = raw.toByteArray(StandardCharsets.UTF_8)
        return if (urlSafeNoPadding) {
            Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        } else {
            Base64.getEncoder().encodeToString(bytes)
        }
    }

    fun isValidPort(port: Int?): Boolean {
        return port != null && port in 1..65535
    }

    fun isValidHost(host: String): Boolean {
        val trimmed = host.trim()
        if (trimmed.isEmpty() || trimmed.contains(" ") || trimmed.contains("/")) return false
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            val ipv6 = trimmed.substring(1, trimmed.length - 1)
            return ipv6.isNotEmpty() && ipv6.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' || it == ':' }
        }
        return trimmed.all { it.isLetterOrDigit() || it == '.' || it == '-' || it == '_' }
    }

    fun isValidUuid(uuid: String): Boolean {
        val trimmed = uuid.trim()
        val uuidRegex = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
        return uuidRegex.matches(trimmed)
    }

    data class ParsedUriParts(
        val scheme: String,
        val userInfo: String,
        val host: String,
        val port: Int?,
        val queryParams: Map<String, String>,
        val fragment: String
    )

    /**
     * Safely splits a standard proxy URI: `scheme://userInfo@host:port/?query#fragment`
     * Supports IPv6 bracketed hosts `[2001:db8::1]:443` and URL-encoded fragments/userInfo.
     */
    fun parseStandardProxyUri(rawUri: String, expectedScheme: String): Result<ParsedUriParts> {
        val trimmed = rawUri.trim()
        val schemePrefix = "${expectedScheme.lowercase()}://"
        if (!trimmed.lowercase().startsWith(schemePrefix)) {
            return Result.failure(IllegalArgumentException("Link must start with $schemePrefix"))
        }

        val withoutScheme = trimmed.substring(schemePrefix.length)
        val fragmentSplitIndex = withoutScheme.indexOf('#')
        val rawFragment = if (fragmentSplitIndex >= 0) {
            urlDecode(withoutScheme.substring(fragmentSplitIndex + 1))
        } else {
            ""
        }
        val beforeFragment = if (fragmentSplitIndex >= 0) {
            withoutScheme.substring(0, fragmentSplitIndex)
        } else {
            withoutScheme
        }

        val querySplitIndex = beforeFragment.indexOf('?')
        val rawQuery = if (querySplitIndex >= 0) {
            beforeFragment.substring(querySplitIndex + 1)
        } else {
            ""
        }
        val authorityWithOptionalPath = if (querySplitIndex >= 0) {
            beforeFragment.substring(0, querySplitIndex)
        } else {
            beforeFragment
        }
        val authority = authorityWithOptionalPath.removeSuffix("/")

        val atIndex = authority.lastIndexOf('@')
        if (atIndex <= 0) {
            return Result.failure(IllegalArgumentException("Missing user credential before '@' in $expectedScheme link."))
        }

        val userInfo = urlDecode(authority.substring(0, atIndex))
        val hostPortPart = authority.substring(atIndex + 1)

        val (host, portStr) = splitHostAndPort(hostPortPart)
            ?: return Result.failure(IllegalArgumentException("Malformed server host or port."))

        val port = portStr.toIntOrNull()
        val queryMap = parseQueryParameters(rawQuery)

        return Result.success(
            ParsedUriParts(
                scheme = expectedScheme.lowercase(),
                userInfo = userInfo,
                host = host,
                port = port,
                queryParams = queryMap,
                fragment = rawFragment.trim()
            )
        )
    }

    fun splitHostAndPort(hostPort: String): Pair<String, String>? {
        val trimmed = hostPort.trim()
        if (trimmed.isEmpty()) return null
        return if (trimmed.startsWith("[")) {
            val closeBracket = trimmed.indexOf(']')
            if (closeBracket < 0 || closeBracket + 1 >= trimmed.length || trimmed[closeBracket + 1] != ':') {
                null
            } else {
                val host = trimmed.substring(1, closeBracket)
                val port = trimmed.substring(closeBracket + 2)
                host to port
            }
        } else {
            val colonIndex = trimmed.lastIndexOf(':')
            if (colonIndex <= 0 || colonIndex == trimmed.length - 1) {
                null
            } else {
                val host = trimmed.substring(0, colonIndex)
                val port = trimmed.substring(colonIndex + 1)
                host to port
            }
        }
    }

    fun parseQueryParameters(rawQuery: String): Map<String, String> {
        if (rawQuery.isBlank()) return emptyMap()
        val result = linkedMapOf<String, String>()
        rawQuery.split('&').forEach { pair ->
            if (pair.isNotBlank()) {
                val eqIdx = pair.indexOf('=')
                if (eqIdx > 0) {
                    val key = urlDecode(pair.substring(0, eqIdx)).trim()
                    val value = urlDecode(pair.substring(eqIdx + 1)).trim()
                    result[key] = value
                } else {
                    result[urlDecode(pair).trim()] = ""
                }
            }
        }
        return result
    }
}
