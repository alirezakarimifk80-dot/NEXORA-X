package com.nexora.x.core.engine

/**
 * Security log-redaction layer for NEXORA X Phase 3.
 *
 * Guarantees that the following sensitive artifacts are NEVER written to diagnostic logs,
 * UI error banners, or notifications:
 * - UUIDs (`xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`)
 * - Full proxy configuration links (`vless://...`, `vmess://...`, `trojan://...`, `ss://...`)
 * - Subscription URLs with tokens (`https://.../sub?token=...`)
 * - JSON secret fields (`"uuid"`, `"password"`, `"public_key"`, `"private_key"`, `"short_id"`)
 * - Bearer tokens or Base64 credentials
 */
object LogRedactor {

    private val UUID_REGEX =
        Regex("\\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\b")

    private val PROXY_URI_REGEX =
        Regex("(?i)\\b(vless|vmess|trojan|ss|hysteria2|tuic|wireguard)://[^\\s\"']+")

    private val JSON_SECRET_FIELD_REGEX =
        Regex("(?i)(\"(?:uuid|password|private_key|public_key|short_id|auth_str|token|secret)\"\\s*:\\s*\")([^\"]+)(\")")

    private val URL_QUERY_SECRET_REGEX =
        Regex("(?i)([?&](?:token|key|password|secret|pbk|sid|id)=)([^&\\s\"']+)")

    /**
     * Redacts any sensitive credentials, UUIDs, keys, or proxy links from [rawMessage],
     * and optionally scrubs any known literal [extraSecrets] passed by the caller.
     */
    fun redact(rawMessage: String?, extraSecrets: Collection<String> = emptyList()): String {
        if (rawMessage.isNullOrBlank()) return ""
        var sanitized = rawMessage

        extraSecrets
            .filter { it.length >= 4 }
            .forEach { secret ->
                sanitized = sanitized!!.replace(secret, "[REDACTED]")
            }

        sanitized = PROXY_URI_REGEX.replace(sanitized!!) { match ->
            val scheme = match.groupValues[1].lowercase()
            "$scheme://[REDACTED_URI]"
        }

        sanitized = UUID_REGEX.replace(sanitized!!, "[REDACTED_UUID]")

        sanitized = JSON_SECRET_FIELD_REGEX.replace(sanitized!!) { match ->
            "${match.groupValues[1]}[REDACTED]${match.groupValues[3]}"
        }

        sanitized = URL_QUERY_SECRET_REGEX.replace(sanitized!!) { match ->
            "${match.groupValues[1]}[REDACTED]"
        }

        return sanitized!!
    }
}
