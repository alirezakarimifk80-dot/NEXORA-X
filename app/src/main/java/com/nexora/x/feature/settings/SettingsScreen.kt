package com.nexora.x.feature.settings

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
import androidx.compose.material.icons.outlined.Close
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
import com.nexora.x.core.ui.components.NexoraTopBar
import com.nexora.x.core.ui.components.SectionHeader
import com.nexora.x.core.ui.components.SettingsItem
import com.nexora.x.domain.model.SettingsCategory

/**
 * NEXORA X Settings Screen.
 * Organized into four required sections:
 * - GENERAL (Language, Theme, App appearance)
 * - CONNECTION (Auto connect, DNS settings, Routing settings, Per-app proxy)
 * - ADVANCED (TUN settings, Network diagnostics, Connection logs)
 * - ABOUT (Application version, Developer information, Open-source licenses)
 * Unimplemented settings are visibly marked as COMING SOON.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NexoraColors.DeepNavyBackground)
    ) {
        NexoraTopBar(
            title = "SETTINGS",
            subtitle = "SYSTEM CONFIGURATION & ARCHITECTURE"
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

            SettingsCategory.entries.forEach { category ->
                val items = uiState.groupedSettings[category].orEmpty()
                if (items.isNotEmpty()) {
                    SectionHeader(
                        title = category.title,
                        trailingMetadata = "${items.size} OPTIONS"
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items.forEach { option ->
                            SettingsItem(
                                option = option,
                                onClick = viewModel::onSettingClicked
                            )
                        }
                    }
                }
            }
        }
    }
}
