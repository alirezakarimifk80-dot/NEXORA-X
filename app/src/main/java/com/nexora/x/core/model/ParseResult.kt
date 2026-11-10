package com.nexora.x.core.model

/**
 * Outcome of parsing a single URI or plain-text configuration block.
 * Preserves unsupported parameter warnings without silently discarding them.
 * Never embeds raw credentials or full URI strings inside error messages.
 */
sealed interface ParseResult {
    data class Success(
        val profile: Profile,
        val warnings: List<String> = emptyList()
    ) : ParseResult

    data class Failure(
        val errorMessage: String,
        val fieldHint: String? = null
    ) : ParseResult
}

/**
 * Outcome of importing a batch (single link or multi-line plain-text block).
 */
data class BatchImportResult(
    val importedProfiles: List<Profile>,
    val warnings: List<String>,
    val errors: List<String>
)
