package com.nexora.x.feature.subscriptions

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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexora.x.core.design.theme.NexoraColors
import com.nexora.x.core.ui.components.EmptyState
import com.nexora.x.core.ui.components.NexoraTopBar
import com.nexora.x.core.ui.components.PremiumButton
import com.nexora.x.core.ui.components.SectionHeader
import com.nexora.x.core.ui.components.TrafficMetricCard

/**
 * NEXORA X Subscriptions Screen.
 * Implements visual structure for:
 * - Subscription list & Empty state
 * - Add subscription button
 * - Update subscription action
 * - Expiration information & Profile count summary cards
 */
@Composable
fun SubscriptionsScreen(
    viewModel: SubscriptionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NexoraColors.DeepNavyBackground)
    ) {
        NexoraTopBar(
            title = "SUBSCRIPTIONS",
            subtitle = "REMOTE GROUP FEEDS · PHASE 2 READY",
            actions = {
                PremiumButton(
                    text = "Update",
                    icon = Icons.Outlined.Refresh,
                    isSecondary = true,
                    onClick = viewModel::onUpdateAllClicked
                )
                PremiumButton(
                    text = "Add URL",
                    icon = Icons.Outlined.Add,
                    onClick = viewModel::onAddSubscriptionClicked
                )
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.actionBannerMessage != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NexoraColors.ElectricPurpleSubtle)
                        .border(1.dp, NexoraColors.ElectricPurple, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = uiState.actionBannerMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NexoraColors.TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Dismiss notification",
                        tint = NexoraColors.CyanHighlight,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clickable { viewModel.onDismissBanner() }
                    )
                }
            }

            // Expiration Information & Profile Count Summary
            SectionHeader(
                title = "SUBSCRIPTION TELEMETRY",
                trailingMetadata = "PHASE 1 PLACEHOLDERS"
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TrafficMetricCard(
                    label = "PROFILE COUNT",
                    value = "0 Nodes",
                    caption = uiState.totalProfileCountLabel,
                    icon = Icons.Outlined.FolderSpecial,
                    accentColor = NexoraColors.CyanHighlight,
                    modifier = Modifier.weight(1f)
                )
                TrafficMetricCard(
                    label = "EXPIRATION INFO",
                    value = "— Days",
                    caption = uiState.expirationSummaryLabel,
                    icon = Icons.Outlined.EventAvailable,
                    accentColor = NexoraColors.ElectricPurpleBright,
                    modifier = Modifier.weight(1f)
                )
            }

            // Subscription List or Empty State
            SectionHeader(
                title = "CONFIGURED SUBSCRIPTION FEEDS",
                trailingMetadata = "${uiState.subscriptions.size} GROUPS"
            )
            if (uiState.subscriptions.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.RssFeed,
                    title = "No Subscription Feeds Added",
                    description = "Remote Base64, Clash YAML, and V2Ray subscription URL parsing is architected for Phase 2.",
                    phaseNotice = "PHASE 2 ROADMAP: Auto-update intervals, Subscription-Userinfo headers & quota expiration tracking.",
                    primaryActionLabel = "Register Subscription URL (Phase 2)",
                    onPrimaryAction = viewModel::onAddSubscriptionClicked
                )
            }
        }
    }
}
