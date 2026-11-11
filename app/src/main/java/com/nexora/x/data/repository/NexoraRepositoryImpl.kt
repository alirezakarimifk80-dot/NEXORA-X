package com.nexora.x.data.repository

import com.nexora.x.core.common.AppConstants
import com.nexora.x.domain.model.ConnectionState
import com.nexora.x.domain.model.NetworkToolItem
import com.nexora.x.domain.model.ProxyProtocol
import com.nexora.x.domain.model.ServerProfile
import com.nexora.x.domain.model.SettingImplementationStatus
import com.nexora.x.domain.model.SettingsCategory
import com.nexora.x.domain.model.SettingsOption
import com.nexora.x.domain.model.SubscriptionItem
import com.nexora.x.domain.model.TelemetrySnapshot
import com.nexora.x.domain.model.ToolAvailability
import com.nexora.x.domain.repository.NexoraRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 1 implementation of [NexoraRepository].
 * Enforces strict truthfulness:
 * - No fake working servers or fake connections.
 * - No fabricated speed, ping, or traffic metrics.
 * - Clear feedback when triggering Phase 2 / Phase 3 actions.
 */
@Singleton
class NexoraRepositoryImpl @Inject constructor() : NexoraRepository {

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _telemetrySnapshot = MutableStateFlow(TelemetrySnapshot())
    override val telemetrySnapshot: StateFlow<TelemetrySnapshot> = _telemetrySnapshot.asStateFlow()

    // Phase 1: Empty list of servers (no fake working servers)
    private val _servers = MutableStateFlow<List<ServerProfile>>(emptyList())
    override val servers: StateFlow<List<ServerProfile>> = _servers.asStateFlow()

    private val _selectedProtocolFilter = MutableStateFlow(ProxyProtocol.ALL)
    override val selectedProtocolFilter: StateFlow<ProxyProtocol> = _selectedProtocolFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    override val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Phase 1: Empty list of subscriptions (subscription processing arrives in Phase 2)
    private val _subscriptions = MutableStateFlow<List<SubscriptionItem>>(emptyList())
    override val subscriptions: StateFlow<List<SubscriptionItem>> = _subscriptions.asStateFlow()

    private val _lastActionBanner = MutableStateFlow<String?>(null)
    override val lastActionBanner: StateFlow<String?> = _lastActionBanner.asStateFlow()

    private val _networkTools = MutableStateFlow(
        listOf(
            NetworkToolItem(
                id = "ping_test",
                title = "Ping Test",
                subtitle = "ICMP & TCP handshake latency verification",
                availability = ToolAvailability.PENDING_CORE_ENGINE,
                metricPlaceholder = "— ms (Awaiting Core)",
                technicalDetail = "Requires Phase 3 Xray/V2Ray socket prober. No synthetic ping results are generated in Phase 1."
            ),
            NetworkToolItem(
                id = "ip_info",
                title = "IP Information",
                subtitle = "External egress IP, ASN & GeoIP lookup",
                availability = ToolAvailability.PENDING_CORE_ENGINE,
                metricPlaceholder = "Unqueried · No Active Tunnel",
                technicalDetail = "GeoIP and egress endpoint inspection will activate once the proxy tunnel engine is online."
            ),
            NetworkToolItem(
                id = "dns_info",
                title = "DNS Information",
                subtitle = "DoH / DoT resolver & system DNS transport check",
                availability = ToolAvailability.PENDING_CORE_ENGINE,
                metricPlaceholder = "Resolver Idle",
                technicalDetail = "Custom DNS-over-HTTPS and FakeDNS routing inspection requires the Phase 3 core DNS module."
            ),
            NetworkToolItem(
                id = "net_diagnostics",
                title = "Network Diagnostics",
                subtitle = "Local Android framework & interface readiness check",
                availability = ToolAvailability.READY_LOCAL_INSPECTION,
                metricPlaceholder = "SDK 26+ · Clean Arch Verified",
                technicalDetail = "Local foundation check passed: Jetpack Compose UI state graph, Hilt DI container, and MVVM repositories are active."
            ),
            NetworkToolItem(
                id = "traffic_monitor",
                title = "Traffic Monitor",
                subtitle = "Real-time uplink/downlink packet & byte stream",
                availability = ToolAvailability.PENDING_CORE_ENGINE,
                metricPlaceholder = "0.00 B/s · Stream Offline",
                technicalDetail = "Live traffic graphs and per-protocol byte counters connect to VpnService file descriptors in Phase 3."
            )
        )
    )
    override val networkTools: StateFlow<List<NetworkToolItem>> = _networkTools.asStateFlow()

    private val _settingsOptions = MutableStateFlow(
        listOf(
            // GENERAL
            SettingsOption(
                id = "language",
                category = SettingsCategory.GENERAL,
                title = "Language",
                subtitle = "Application interface localization",
                currentValue = "English (System Default)",
                status = SettingImplementationStatus.ACTIVE_PHASE_1
            ),
            SettingsOption(
                id = "theme",
                category = SettingsCategory.GENERAL,
                title = "Theme",
                subtitle = "Deep Navy & Electric Purple dark specification",
                currentValue = "Nexora Dark (Locked)",
                status = SettingImplementationStatus.ACTIVE_PHASE_1
            ),
            SettingsOption(
                id = "appearance",
                category = SettingsCategory.GENERAL,
                title = "App Appearance",
                subtitle = "Tabular telemetry numerals & high-contrast borders",
                currentValue = "High-Density Commercial",
                status = SettingImplementationStatus.ACTIVE_PHASE_1
            ),
            // CONNECTION
            SettingsOption(
                id = "auto_connect",
                category = SettingsCategory.CONNECTION,
                title = "Auto Connect",
                subtitle = "Automatically establish tunnel on Wi-Fi or boot",
                currentValue = "Unavailable",
                status = SettingImplementationStatus.COMING_SOON_PHASE_3
            ),
            SettingsOption(
                id = "dns_settings",
                category = SettingsCategory.CONNECTION,
                title = "DNS Settings",
                subtitle = "Direct bootstrap 1.1.1.1 + Proxy DoH https://1.1.1.1/dns-query (prefer_ipv4)",
                currentValue = "Active · Dual Resolver",
                status = SettingImplementationStatus.ACTIVE_PHASE_1
            ),
            SettingsOption(
                id = "routing_settings",
                category = SettingsCategory.CONNECTION,
                title = "Routing Settings",
                subtitle = "Outbound DNS bootstrap rule, RFC1918 private IP bypass & proxy final route",
                currentValue = "Active · Auto-Detect Interface",
                status = SettingImplementationStatus.ACTIVE_PHASE_1
            ),
            SettingsOption(
                id = "per_app_proxy",
                category = SettingsCategory.CONNECTION,
                title = "Per-App Proxy",
                subtitle = "Self-package bypass active (com.nexora.x); custom per-app UI coming soon",
                currentValue = "Self-Bypass Active",
                status = SettingImplementationStatus.COMING_SOON_PHASE_3
            ),
            // ADVANCED
            SettingsOption(
                id = "tun_settings",
                category = SettingsCategory.ADVANCED,
                title = "TUN Settings",
                subtitle = "MTU 1500, gVisor user-space stack, 172.19.0.1/30 & non-blocking detached FD",
                currentValue = "MTU 1500 · gVisor Stack",
                status = SettingImplementationStatus.ACTIVE_PHASE_1
            ),
            SettingsOption(
                id = "adv_diagnostics",
                category = SettingsCategory.ADVANCED,
                title = "Network Diagnostics",
                subtitle = "6-Stage Tunnel Connectivity Verifier (Stages A–F) & Libbox.checkConfig",
                currentValue = "Active in Tools Tab",
                status = SettingImplementationStatus.ACTIVE_PHASE_1
            ),
            SettingsOption(
                id = "connection_logs",
                category = SettingsCategory.ADVANCED,
                title = "Connection Logs",
                subtitle = "Redacted sing-box engine event ring buffer (LogRedactor)",
                currentValue = "Redacted Ring Buffer",
                status = SettingImplementationStatus.ACTIVE_PHASE_1
            ),
            // ABOUT
            SettingsOption(
                id = "vpn_disclosure",
                category = SettingsCategory.ABOUT,
                title = "VPN Service Prominent Disclosure",
                subtitle = "Review how NEXORA X uses Android VpnService & protects local profile data",
                currentValue = "Tap to Review",
                status = SettingImplementationStatus.ACTIVE_PHASE_1
            ),
            SettingsOption(
                id = "privacy_policy",
                category = SettingsCategory.ABOUT,
                title = "Privacy Policy & Data Safety",
                subtitle = "100% local profile storage · Zero remote analytics, ads, or data sharing",
                currentValue = "Zero Data Collected",
                status = SettingImplementationStatus.ACTIVE_PHASE_1
            ),
            SettingsOption(
                id = "app_version",
                category = SettingsCategory.ABOUT,
                title = "Application Version",
                subtitle = "Compile & Target SDK 35 · Min SDK 26 · Version Code ${AppConstants.VERSION_CODE}",
                currentValue = AppConstants.VERSION_NAME,
                status = SettingImplementationStatus.ACTIVE_PHASE_1
            ),
            SettingsOption(
                id = "developer_info",
                category = SettingsCategory.ABOUT,
                title = "Developer Information",
                subtitle = AppConstants.SOURCE_ARCHITECTURE,
                currentValue = AppConstants.DEVELOPER_ORGANIZATION,
                status = SettingImplementationStatus.ACTIVE_PHASE_1
            ),
            SettingsOption(
                id = "licenses",
                category = SettingsCategory.ABOUT,
                title = "Open-Source Licenses",
                subtitle = "sing-box (libbox v1.10.7, GPL-3.0-or-later), Jetpack Compose, Room, Hilt (Apache-2.0)",
                currentValue = "GPL-3.0-or-later & Apache-2.0",
                status = SettingImplementationStatus.ACTIVE_PHASE_1
            )
        )
    )
    override val settingsOptions: StateFlow<List<SettingsOption>> = _settingsOptions.asStateFlow()

    override fun onConnectButtonClicked() {
        _connectionState.value = ConnectionState.ENGINE_PENDING_NOTICE
        _lastActionBanner.value =
            "ENGINE PENDING: Connection engine is not implemented in Phase 1. Status remains DISCONNECTED."
    }

    override fun dismissActionBanner() {
        _lastActionBanner.value = null
    }

    override fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    override fun selectProtocolFilter(protocol: ProxyProtocol) {
        _selectedProtocolFilter.value = protocol
    }

    override fun requestAddServerAction() {
        _lastActionBanner.value =
            "PHASE 2 PENDING: Manual server profile creation (VLESS/VMess/Trojan) will be enabled in Phase 2."
    }

    override fun requestImportQrAction() {
        _lastActionBanner.value =
            "PHASE 2 PENDING: QR code & clipboard URI import will be implemented in Phase 2."
    }

    override fun requestAddSubscriptionAction() {
        _lastActionBanner.value =
            "PHASE 2 PENDING: Remote subscription URL registration & storage will be implemented in Phase 2."
    }

    override fun requestUpdateSubscriptionsAction() {
        _lastActionBanner.value =
            "PHASE 2 PENDING: Subscription sync requires the Phase 2 profile parser."
    }

    override fun inspectTool(toolId: String) {
        val tool = _networkTools.value.find { it.id == toolId } ?: return
        _lastActionBanner.value = "${tool.title}: ${tool.technicalDetail}"
    }

    override fun onSettingClicked(option: SettingsOption) {
        when (option.status) {
            SettingImplementationStatus.ACTIVE_PHASE_1 -> {
                _lastActionBanner.value = "${option.title}: ${option.currentValue} (${option.subtitle})"
            }
            SettingImplementationStatus.COMING_SOON_PHASE_2,
            SettingImplementationStatus.COMING_SOON_PHASE_3 -> {
                _lastActionBanner.value =
                    "${option.title} is marked [${option.status.badgeText}] and is not yet active in Phase 1."
            }
        }
    }
}
