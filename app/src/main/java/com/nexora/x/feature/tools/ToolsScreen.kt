package com.nexora.x.feature.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexora.x.core.design.theme.NexoraColors
import com.nexora.x.core.ui.components.NexoraTopBar
import com.nexora.x.core.ui.components.PremiumButton
import com.nexora.x.core.ui.components.SectionHeader
import com.nexora.x.domain.model.NetworkToolItem
import com.nexora.x.domain.model.ToolAvailability

/**
 * NEXORA X Phase 3 Diagnostics & Network Tools Screen.
 *
 * Displays:
 * - Engine version (`sing-box 1.10.7`)
 * - Engine initialization state & native `libbox.aar` presence
 * - Current VPN state (`DISCONNECTED`, `PREPARING`, `REQUESTING_PERMISSION`, `STARTING`, `CONNECTED`, `STOPPING`, `ERROR`)
 * - Service state (`STOPPED`, `STARTING_FOREGROUND`, `RUNNING_FOREGROUND`, `STOPPED_ON_ERROR`)
 * - Last non-sensitive error (passed through [com.nexora.x.core.engine.LogRedactor])
 * - Configuration validation result & redacted diagnostic log buffer
 */
@Composable
fun ToolsScreen(
    viewModel: ToolsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val diag = uiState.engineDiagnostics
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        NexoraTopBar(
            title = "DIAGNOSTICS & TOOLS",
            subtitle = "SING-BOX LIBBOX ENGINE · REDACTED LOGS",
            actions = {
                PremiumButton(
                    text = "Validate Config",
                    icon = Icons.Outlined.CheckCircle,
                    onClick = viewModel::onValidateSelectedProfileClicked
                )
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (uiState.actionBannerMessage != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colorScheme.primaryContainer)
                        .border(1.dp, colorScheme.primary, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = uiState.actionBannerMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurface,
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

            // Phase 3 Native sing-box Engine Diagnostics Panel
            SectionHeader(
                title = "SING-BOX ENGINE DIAGNOSTICS",
                trailingMetadata = diag.currentVpnState.name
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(colorScheme.surface)
                    .border(1.dp, colorScheme.outline, MaterialTheme.shapes.medium)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DiagnosticRow("ENGINE VERSION", diag.engineVersion)
                DiagnosticRow(
                    "LIBBOX.AAR BINARY",
                    if (diag.isLibboxBinaryLoaded) "LOADED (JNI READY)" else "MISSING IN app/libs/libbox.aar"
                )
                DiagnosticRow(
                    "INITIALIZATION STATE",
                    if (diag.isEngineInitialized) "INITIALIZED" else "NOT INITIALIZED"
                )
                DiagnosticRow("CURRENT VPN STATE", diag.currentVpnState.displayTitle)
                DiagnosticRow("ANDROID SERVICE STATE", diag.serviceState)
                DiagnosticRow(
                    "CONFIG VALIDATION",
                    uiState.generatedConfigPreviewRedacted ?: diag.lastValidationSummary
                )
                DiagnosticRow(
                    "LAST NON-SENSITIVE ERROR",
                    diag.lastRedactedError ?: "None"
                )
            }

            // Redacted Engine Event Ring Buffer
            SectionHeader(
                title = "REDACTED ENGINE EVENT LOG",
                trailingMetadata = "SECRETS SCRUBBED"
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(colorScheme.surface)
                    .border(1.dp, colorScheme.outline, MaterialTheme.shapes.medium)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (diag.recentDiagnosticLogs.isEmpty()) {
                    Text(
                        text = "No engine lifecycle events recorded in this session yet.",
                        style = MaterialTheme.typography.labelMedium,
                        color = NexoraColors.TextMuted
                    )
                } else {
                    diag.recentDiagnosticLogs.take(10).forEach { line ->
                        Text(
                            text = "• $line",
                            style = MaterialTheme.typography.labelMedium,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            SectionHeader(
                title = "NETWORKING UTILITIES",
                trailingMetadata = "${uiState.tools.size} MODULES"
            )

            uiState.tools.forEach { tool ->
                NetworkToolCard(
                    tool = tool,
                    onClick = { viewModel.onToolClicked(tool.id) }
                )
            }
        }
    }
}

@Composable
private fun DiagnosticRow(
    label: String,
    value: String
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colorScheme.background)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = NexoraColors.TextMuted,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = colorScheme.onSurface
        )
    }
}

@Composable
private fun NetworkToolCard(
    tool: NetworkToolItem,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isReady = tool.availability == ToolAvailability.READY_LOCAL_INSPECTION
    val badgeColor = if (isReady) {
        colorScheme.secondary
    } else {
        NexoraColors.StatusPendingAmber
    }

    Column(
        modifier = modifierWithBorder(isReady),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = tool.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = colorScheme.onSurface
                )
                Text(
                    text = tool.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = tool.availability.label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = badgeColor
            )
        }
    }
}

@Composable
private fun modifierWithBorder(isReady: Boolean): Modifier {
    val colorScheme = MaterialTheme.colorScheme
    return Modifier
        .fillMaxWidth()
        .clip(MaterialTheme.shapes.medium)
        .background(colorScheme.surface)
        .border(
            width = 1.dp,
            color = if (isReady) colorScheme.secondary.copy(alpha = 0.4f) else colorScheme.outline,
            shape = MaterialTheme.shapes.medium
        )
        .padding(16.dp)
}
