package com.nexora.x.core.model

/**
 * Supported Phase 2 profile protocols for NEXORA X.
 * Supports real profile configurations for VLESS, VMess, Trojan, and Shadowsocks.
 */
enum class ProfileType(
    val displayName: String,
    val badgeLabel: String,
    val schemePrefix: String
) {
    VLESS("VLESS", "VLESS", "vless://"),
    VMESS("VMess", "VMESS", "vmess://"),
    TROJAN("Trojan", "TROJAN", "trojan://"),
    SHADOWSOCKS("Shadowsocks", "SS", "ss://");

    companion object {
        fun fromSchemeOrNull(uri: String): ProfileType? {
            val normalized = uri.trim().lowercase()
            return entries.firstOrNull { normalized.startsWith(it.schemePrefix) }
        }
    }
}
