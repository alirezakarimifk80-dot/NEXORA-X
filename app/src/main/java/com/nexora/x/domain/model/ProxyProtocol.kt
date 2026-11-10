package com.nexora.x.domain.model

/**
 * Supported V2Ray / Xray proxy protocols for filtering and profile architecture.
 */
enum class ProxyProtocol(val label: String, val tag: String) {
    ALL("All Protocols", "ALL"),
    VLESS("VLESS", "VLESS"),
    VMESS("VMess", "VMESS"),
    TROJAN("Trojan", "TROJAN"),
    SHADOWSOCKS("Shadowsocks", "SS"),
    WIREGUARD("WireGuard", "WG")
}
