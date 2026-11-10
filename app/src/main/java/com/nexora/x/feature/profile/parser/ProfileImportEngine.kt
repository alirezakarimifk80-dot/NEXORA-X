package com.nexora.x.feature.profile.parser

import com.nexora.x.core.model.BatchImportResult
import com.nexora.x.core.model.ParseResult
import com.nexora.x.core.model.Profile
import com.nexora.x.core.model.ProfileType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates single-link, multi-line plain-text, and Base64 subscription block imports
 * across [VlessParser], [VmessParser], [TrojanParser], and [ShadowsocksParser].
 * Also serializes any [Profile] back into a standard shareable URI link.
 */
@Singleton
class ProfileImportEngine @Inject constructor(
    private val vlessParser: VlessParser,
    private val vmessParser: VmessParser,
    private val trojanParser: TrojanParser,
    private val shadowsocksParser: ShadowsocksParser
) {

    fun parseSingleLink(rawInput: String, nowEpochMillis: Long = System.currentTimeMillis()): ParseResult {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) {
            return ParseResult.Failure("Configuration input is empty.", "uri")
        }

        return when (ProfileType.fromSchemeOrNull(trimmed)) {
            ProfileType.VLESS -> vlessParser.parse(trimmed, nowEpochMillis)
            ProfileType.VMESS -> vmessParser.parse(trimmed, nowEpochMillis)
            ProfileType.TROJAN -> trojanParser.parse(trimmed, nowEpochMillis)
            ProfileType.SHADOWSOCKS -> shadowsocksParser.parse(trimmed, nowEpochMillis)
            null -> ParseResult.Failure(
                errorMessage = "Unsupported or missing protocol scheme. Supported schemes: vless://, vmess://, trojan://, ss://",
                fieldHint = "uri"
            )
        }
    }

    /**
     * Parses a plain-text configuration block (one or multiple lines, or a Base64-encoded list of links).
     */
    fun parsePlainTextBatch(rawText: String, nowEpochMillis: Long = System.currentTimeMillis()): BatchImportResult {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) {
            return BatchImportResult(
                importedProfiles = emptyList(),
                warnings = emptyList(),
                errors = listOf("Configuration text is empty.")
            )
        }

        // If the entire block does not contain "://", check if it is a Base64-encoded multi-line subscription block
        val resolvedBlock = if (!trimmed.contains("://")) {
            UriParserUtils.decodeBase64OrNull(trimmed) ?: trimmed
        } else {
            trimmed
        }

        val lines = resolvedBlock.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") && !it.startsWith("//") }

        if (lines.isEmpty()) {
            return BatchImportResult(
                importedProfiles = emptyList(),
                warnings = emptyList(),
                errors = listOf("No valid configuration lines found.")
            )
        }

        val profiles = mutableListOf<Profile>()
        val warnings = mutableListOf<String>()
        val errors = mutableListOf<String>()

        lines.forEachIndexed { index, line ->
            when (val result = parseSingleLink(line, nowEpochMillis + index)) {
                is ParseResult.Success -> {
                    profiles.add(result.profile)
                    result.warnings.forEach { w ->
                        warnings.add("[${result.profile.displayName}] $w")
                    }
                }
                is ParseResult.Failure -> {
                    errors.add("Line ${index + 1}: ${result.errorMessage}")
                }
            }
        }

        return BatchImportResult(
            importedProfiles = profiles,
            warnings = warnings,
            errors = errors
        )
    }

    fun exportToUri(profile: Profile): String {
        return when (profile.protocol) {
            ProfileType.VLESS -> vlessParser.toUri(profile)
            ProfileType.VMESS -> vmessParser.toUri(profile)
            ProfileType.TROJAN -> trojanParser.toUri(profile)
            ProfileType.SHADOWSOCKS -> shadowsocksParser.toUri(profile)
        }
    }
}
