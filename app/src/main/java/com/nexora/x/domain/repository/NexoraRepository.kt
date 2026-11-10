package com.nexora.x.domain.repository

import com.nexora.x.domain.model.ConnectionState
import com.nexora.x.domain.model.NetworkToolItem
import com.nexora.x.domain.model.ProxyProtocol
import com.nexora.x.domain.model.ServerProfile
import com.nexora.x.domain.model.SettingsOption
import com.nexora.x.domain.model.SubscriptionItem
import com.nexora.x.domain.model.TelemetrySnapshot
import kotlinx.coroutines.flow.StateFlow

/**
 * Clean Architecture Repository contract for NEXORA X Phase 1 Foundation.
 */
interface NexoraRepository {
    val connectionState: StateFlow<ConnectionState>
    val telemetrySnapshot: StateFlow<TelemetrySnapshot>
    val servers: StateFlow<List<ServerProfile>>
    val selectedProtocolFilter: StateFlow<ProxyProtocol>
    val searchQuery: StateFlow<String>
    val subscriptions: StateFlow<List<SubscriptionItem>>
    val networkTools: StateFlow<List<NetworkToolItem>>
    val settingsOptions: StateFlow<List<SettingsOption>>
    val lastActionBanner: StateFlow<String?>

    fun onConnectButtonClicked()
    fun dismissActionBanner()
    fun updateSearchQuery(query: String)
    fun selectProtocolFilter(protocol: ProxyProtocol)
    fun requestAddServerAction()
    fun requestImportQrAction()
    fun requestAddSubscriptionAction()
    fun requestUpdateSubscriptionsAction()
    fun inspectTool(toolId: String)
    fun onSettingClicked(option: SettingsOption)
}
