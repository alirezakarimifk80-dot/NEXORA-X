package com.nexora.x.domain.model

/**
 * Represents the explicit Phase 1 connection state of NEXORA X.
 * In Phase 1, the V2Ray / Xray core engine is not yet integrated, so the app
 * truthfully reports DISCONNECTED and ENGINE_PENDING without faking connections.
 */
enum class ConnectionState(
    val displayTitle: String,
    val statusBadge: String,
    val description: String
) {
    DISCONNECTED(
        displayTitle = "DISCONNECTED",
        statusBadge = "ENGINE PENDING",
        description = "Core V2Ray tunnel engine is scheduled for Phase 3 integration. No active proxy tunnel."
    ),
    ENGINE_PENDING_NOTICE(
        displayTitle = "DISCONNECTED",
        statusBadge = "ENGINE PENDING",
        description = "Cannot initiate connection in Phase 1: Xray/V2Ray native core and Profile Manager (Phase 2) are required."
    )
}
