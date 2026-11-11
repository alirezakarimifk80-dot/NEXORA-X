package com.nexora.x.feature.home

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexora.x.core.common.AppConstants
import com.nexora.x.core.design.theme.NexoraColors
import com.nexora.x.core.engine.EngineState
import com.nexora.x.core.ui.components.ConnectButton
import com.nexora.x.core.ui.components.ConnectionStatusCard
import com.nexora.x.core.ui.components.NexoraTopBar
import com.nexora.x.core.ui.components.PremiumButton
import com.nexora.x.core.ui.components.RealProfileCard
import com.nexora.x.core.ui.components.SectionHeader
import com.nexora.x.core.ui.components.ServerCard
import com.nexora.x.core.ui.components.TrafficMetricCard
import com.nexora.x.core.ui.components.VpnDisclosureDialog
import kotlinx.coroutines.flow.collectLatest

/**
 * NEXORA X Production Home Dashboard Screen.
 * Connected to the real [com.nexora.x.core.engine.SingBoxEngine], [com.nexora.x.core.engine.NetworkLatencyChecker],
 * and Room [com.nexora.x.domain.repository.ProfileRepository].
 *
 * Displays:
 * - Google Play VpnService Prominent Disclosure Dialog before first VpnService.prepare() invocation
 * - Selected profile name & protocol
 * - Periodic & on-demand TCP handshake LATENCY (`ms`, `Checking...`, `Timeout`, or `N/A`)
 * - Real connection state (`DISCONNECTED`, `PREPARING`, `REQUESTING_PERMISSION`, `STARTING`, `VPN ACTIVE`,
 *   `CONNECTIVITY CHECK`, `INTERNET AVAILABLE`, `NO INTERNET`, `STOPPING`, `ENGINE ERROR`)
 * - Connection duration (`00:00:00` or live elapsed time when tunnel is active)
 * - Download speed, Upload speed, Total downloaded data, Total uploaded data (`N/A` when unavailable; never fabricated)
 * - Connect & Disconnect controls wired to Android `VpnService.prepare()` consent launcher
 */
@Composable
fun HomeScreen(
    onNavigateToServers: () -> Unit,
    onNavigateToAddProfile: () -> Unit = onNavigateToServers,
    onNavigateToSubscriptions: () -> Unit,
    onNavigateToTools: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colorScheme = MaterialTheme.colorScheme
    val selectedProfile = uiState.selectedProfile
    val stages = uiState.diagnostics.stageDiagnostics

    // Google Play VpnService Prominent Disclosure modal (shown before requesting VpnService.prepare() consent)
    if (uiState.showVpnDisclosureDialog) {
        VpnDisclosureDialog(
            onAcceptAndContinue = viewModel::onAcceptVpnDisclosureAndConnect,
            onDecline = viewModel::onDeclineVpnDisclosure,
            isReadOnlyReview = false
        )
    }

    // Android 13+ (API 33+) runtime notification permission launcher for Foreground VpnService notification
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Foreground VpnService still starts even if notification permission is declined
    }

    // Android VpnService.prepare() consent launcher
    val vpnConsentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val granted = result.resultCode == Activity.RESULT_OK
        viewModel.onVpnPermissionResult(granted)
    }

    LaunchedEffect(viewModel) {
        viewModel.vpnConsentIntentEvents.collectLatest { prepareIntent ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            vpnConsentLauncher.launch(prepareIntent)
        }
    }

    val statusDescription = when (uiState.engineState) {
        EngineState.DISCONNECTED -> "No active VPN session. Select a saved profile and tap Connect to validate and start sing-box."
        EngineState.PREPARING -> "Validating profile and generating sing-box v1.10.7 JSON configuration..."
        EngineState.REQUESTING_PERMISSION -> "Waiting for Android VpnService user consent..."
        EngineState.STARTING -> "Promoting foreground service, opening non-blocking TUN interface, and starting libbox..."
        EngineState.VPN_ACTIVE -> "Stages A–C UP: TUN & sing-box running for '${selectedProfile?.displayName ?: "Profile"}'. Starting traffic check..."
        EngineState.CONNECTIVITY_CHECK -> "Probing Stage D (outbound handshake), Stage E (tunnel DNS), and Stage F (HTTP 204 connectivity)..."
        EngineState.INTERNET_AVAILABLE -> "All Stages A–F VERIFIED: Internet traffic is passing through '${selectedProfile?.displayName ?: "Profile"}' (HTTP ${stages.probeHttpStatusCode ?: 204})."
        EngineState.CONNECTED -> "Active sing-box TUN session established for '${selectedProfile?.displayName ?: "Profile"}'."
        EngineState.NO_INTERNET -> stages.failureReasonRedacted
            ?: uiState.diagnostics.lastRedactedError
            ?: "Tunnel is up (Stages A–C PASS), but no internet traffic passed through the remote outbound."
        EngineState.STOPPING -> "Closing TUN file descriptor and stopping libbox service..."
        EngineState.ERROR -> uiState.diagnostics.lastRedactedError
            ?: "Engine encountered an error. Inspect Network Tools -> Diagnostics for details."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        NexoraTopBar(
            title = AppConstants.APP_NAME,
            subtitle = "v${AppConstants.VERSION_NAME} · ${uiState.engineState.displayTitle} · ${uiState.totalSavedProfiles} PROFILES",
            actions = {
                PremiumButton(
                    text = if (uiState.engineState.canStop) "Disconnect" else "Add Profile",
                    icon = Icons.Outlined.Add,
                    isSecondary = uiState.engineState.canStop,
                    onClick = {
                        if (uiState.engineState.canStop) {
                            viewModel.onToggleConnectionClicked()
                        } else {
                            onNavigateToAddProfile()
                        }
                    }
                )
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            if (uiState.actionBannerMessage != null) {
                val isError = uiState.engineState == EngineState.ERROR || uiState.engineState == EngineState.NO_INTERNET
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isError) NexoraColors.StatusErrorCrimson.copy(alpha = 0.14f)
                            else colorScheme.primaryContainer
                        )
                        .border(
                            width = 1.dp,
                            color = if (isError) NexoraColors.StatusErrorCrimson else colorScheme.primary,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = uiState.actionBannerMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isError) NexoraColors.StatusErrorCrimson else colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Dismiss notification",
                        tint = colorScheme.secondary,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clickable { viewModel.onDismissBanner() }
                    )
                }
            }

            // Real Engine Connection Status Card
            ConnectionStatusCard(
                engineState = uiState.engineState,
                sessionDuration = uiState.sessionDurationFormatted,
                statusDetail = statusDescription
            )

            // Centerpiece Connect / Disconnect Orb Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                ConnectButton(
                    statusLabel = if (uiState.engineState.canStop) "DISCONNECT" else uiState.engineState.displayTitle,
                    engineSubLabel = uiState.engineState.statusBadge,
                    onClick = viewModel::onToggleConnectionClicked
                )
            }

            // Selected Profile, Protocol & Live Latency Summary
            SectionHeader(
                title = "SELECTED PROFILE & LATENCY",
                trailingMetadata = selectedProfile?.protocol?.badgeLabel ?: "NONE SELECTED"
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TrafficMetricCard(
                    label = "LATENCY",
                    value = uiState.latencyFormatted,
                    caption = if (selectedProfile != null) {
                        "${selectedProfile.serverAddress}:${selectedProfile.port} · Tap to ping"
                    } else {
                        "Select profile to ping"
                    },
                    icon = Icons.Outlined.Speed,
                    accentColor = NexoraColors.CyanHighlight,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = selectedProfile != null) {
                            viewModel.onRefreshLatencyClicked()
                        }
                )
                TrafficMetricCard(
                    label = "SELECTED PROTOCOL",
                    value = selectedProfile?.protocol?.displayName ?: "N/A",
                    caption = selectedProfile?.displayName ?: "Tap Servers to select",
                    icon = Icons.Outlined.Security,
                    accentColor = NexoraColors.ElectricPurpleBright,
                    modifier = Modifier.weight(1f)
                )
            }

            if (selectedProfile != null) {
                RealProfileCard(
                    profile = selectedProfile,
                    onSelect = onNavigateToServers,
                    onEdit = onNavigateToServers,
                    onDuplicate = onNavigateToServers,
                    onCopyLink = onNavigateToServers,
                    onDelete = onNavigateToServers,
                    showActions = false
                )
            } else {
                ServerCard(
                    profile = null,
                    onClick = onNavigateToAddProfile
                )
            }

            // Real Engine Traffic Statistics (Displays N/A when no libbox session is reporting)
            SectionHeader(
                title = "ENGINE TRAFFIC TELEMETRY",
                trailingMetadata = if (uiState.engineState.isTunnelRunning) "LIVE LIBBOX STREAM" else "NO ACTIVE SESSION"
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TrafficMetricCard(
                    label = "DOWNLOAD SPEED",
                    value = uiState.downloadSpeedFormatted,
                    caption = "Downlink Rate",
                    icon = Icons.Outlined.ArrowDownward,
                    accentColor = NexoraColors.CyanHighlight,
                    modifier = Modifier.weight(1f)
                )
                TrafficMetricCard(
                    label = "UPLOAD SPEED",
                    value = uiState.uploadSpeedFormatted,
                    caption = "Uplink Rate",
                    icon = Icons.Outlined.ArrowUpward,
                    accentColor = NexoraColors.ElectricPurpleBright,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TrafficMetricCard(
                    label = "TOTAL DOWNLOADED",
                    value = uiState.totalDownloadedFormatted,
                    caption = "Session RX Bytes",
                    icon = Icons.Outlined.SwapVert,
                    accentColor = NexoraColors.CyanHighlight,
                    modifier = Modifier.weight(1f)
                )
                TrafficMetricCard(
                    label = "TOTAL UPLOADED",
                    value = uiState.totalUploadedFormatted,
                    caption = "Session TX Bytes",
                    icon = Icons.Outlined.SwapVert,
                    accentColor = NexoraColors.ElectricPurpleBright,
                    modifier = Modifier.weight(1f)
                )
            }

            // Quick Actions
            SectionHeader(
                title = "ENGINE & PROFILE CONTROLS",
                trailingMetadata = "QUICK ACCESS"
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PremiumButton(
                    text = "Servers (${uiState.totalSavedProfiles})",
                    icon = Icons.Outlined.Dns,
                    onClick = onNavigateToServers,
                    modifier = Modifier.weight(1f)
                )
                PremiumButton(
                    text = "Diagnostics",
                    icon = Icons.Outlined.NetworkCheck,
                    onClick = onNavigateToTools,
                    isSecondary = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
