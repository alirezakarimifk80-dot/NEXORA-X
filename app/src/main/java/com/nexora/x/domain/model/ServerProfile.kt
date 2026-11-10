package com.nexora.x.domain.model

/**
 * Domain model representing a V2Ray Server Profile structure.
 * Note: In Phase 1, no fake working servers are pre-populated; profile persistence arrives in Phase 2.
 */
data class ServerProfile(
    val id: String,
    val name: String,
    val address: String,
    val port: Int,
    val protocol: ProxyProtocol,
    val transport: String,
    val security: String,
    val latencyLabel: String = "— ms",
    val isSelected: Boolean = false
)
