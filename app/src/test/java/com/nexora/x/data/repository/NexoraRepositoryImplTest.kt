package com.nexora.x.data.repository

import com.nexora.x.core.common.AppConstants
import com.nexora.x.domain.model.ConnectionState
import com.nexora.x.domain.model.ProxyProtocol
import com.nexora.x.domain.model.ToolAvailability
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests verifying Phase 1 architectural invariants for NEXORA X:
 * 1. Initial connection status is strictly DISCONNECTED.
 * 2. Clicking the connect button does NOT fake a connection; it reports ENGINE PENDING.
 * 3. Telemetry placeholders do NOT invent fake speeds or pings.
 * 4. Servers and Subscriptions start empty in Phase 1 (no fake servers).
 * 5. Tools and Settings properly distinguish Phase 1 ready items from Phase 2/3 items.
 */
class NexoraRepositoryImplTest {

    private lateinit var repository: NexoraRepositoryImpl

    @Before
    fun setUp() {
        repository = NexoraRepositoryImpl()
    }

    @Test
    fun `initial state is DISCONNECTED with placeholder telemetry and zero fake servers`() {
        assertEquals(ConnectionState.DISCONNECTED, repository.connectionState.value)
        assertEquals(AppConstants.PLACEHOLDER_SPEED, repository.telemetrySnapshot.value.downloadSpeed)
        assertEquals(AppConstants.PLACEHOLDER_SPEED, repository.telemetrySnapshot.value.uploadSpeed)
        assertEquals(AppConstants.PLACEHOLDER_LATENCY, repository.telemetrySnapshot.value.latency)
        assertTrue(repository.servers.value.isEmpty())
        assertTrue(repository.subscriptions.value.isEmpty())
    }

    @Test
    fun `clicking connect button preserves DISCONNECTED status and emits ENGINE PENDING notice`() {
        repository.onConnectButtonClicked()
        assertEquals(ConnectionState.ENGINE_PENDING_NOTICE, repository.connectionState.value)
        assertEquals("DISCONNECTED", repository.connectionState.value.displayTitle)
        assertEquals("ENGINE PENDING", repository.connectionState.value.statusBadge)
        assertTrue(repository.lastActionBanner.value?.contains("ENGINE PENDING") == true)
    }

    @Test
    fun `protocol filter and search query update state flows cleanly`() {
        repository.selectProtocolFilter(ProxyProtocol.VLESS)
        repository.updateSearchQuery("tokyo-vless")

        assertEquals(ProxyProtocol.VLESS, repository.selectedProtocolFilter.value)
        assertEquals("tokyo-vless", repository.searchQuery.value)
    }

    @Test
    fun `network tools distinguish local inspection from core-dependent tools`() {
        val tools = repository.networkTools.value
        assertEquals(5, tools.size)
        val readyTools = tools.filter { it.availability == ToolAvailability.READY_LOCAL_INSPECTION }
        val pendingTools = tools.filter { it.availability == ToolAvailability.PENDING_CORE_ENGINE }
        assertEquals(1, readyTools.size)
        assertEquals(4, pendingTools.size)
    }
}
