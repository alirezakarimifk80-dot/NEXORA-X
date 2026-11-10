package com.nexora.x.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexora.x.core.design.theme.NexoraColors
import com.nexora.x.domain.model.SettingImplementationStatus
import com.nexora.x.domain.model.SettingsOption

/**
 * Reusable SettingsItem row component.
 * Clearly distinguishes active Phase 1 settings from those marked Coming Soon (Phase 2/3).
 */
@Composable
fun SettingsItem(
    option: SettingsOption,
    onClick: (SettingsOption) -> Unit,
    modifier: Modifier = Modifier
) {
    val isAvailable = option.status == SettingImplementationStatus.ACTIVE_PHASE_1
    val statusColor = if (isAvailable) {
        NexoraColors.CyanHighlight
    } else {
        NexoraColors.StatusPendingAmber
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(NexoraColors.BlackSurface)
            .border(
                width = 1.dp,
                color = NexoraColors.HairlineBorder,
                shape = MaterialTheme.shapes.medium
            )
            .clickable { onClick(option) }
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .semantics {
                contentDescription = "${option.title}: ${option.currentValue}, ${option.status.badgeText}"
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = option.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = NexoraColors.TextPrimary
                )
                Text(
                    text = "· ${option.status.badgeText}",
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor
                )
            }
            Text(
                text = option.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = NexoraColors.TextSecondary
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = option.currentValue,
                style = MaterialTheme.typography.labelMedium,
                color = if (isAvailable) NexoraColors.TextPrimary else NexoraColors.TextMuted
            )
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = NexoraColors.TextMuted
            )
        }
    }
}
