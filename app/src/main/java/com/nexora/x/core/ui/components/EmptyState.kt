package com.nexora.x.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nexora.x.core.design.theme.NexoraColors

/**
 * Reusable EmptyState component for Servers and Subscriptions screens in Phase 1.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    phaseNotice: String,
    primaryActionLabel: String? = null,
    onPrimaryAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(NexoraColors.BlackSurface)
            .border(
                width = 1.dp,
                color = NexoraColors.HairlineBorder,
                shape = MaterialTheme.shapes.large
            )
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(NexoraColors.ElectricPurpleSubtle)
                .border(1.dp, NexoraColors.ElectricPurple.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = NexoraColors.CyanHighlight,
                modifier = Modifier.size(26.dp)
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = NexoraColors.TextPrimary,
            textAlign = TextAlign.Center
        )

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = NexoraColors.TextSecondary,
            textAlign = TextAlign.Center
        )

        Text(
            text = phaseNotice,
            style = MaterialTheme.typography.labelMedium,
            color = NexoraColors.StatusPendingAmber,
            textAlign = TextAlign.Center
        )

        if (primaryActionLabel != null && onPrimaryAction != null) {
            Spacer(modifier = Modifier.height(6.dp))
            PremiumButton(
                text = primaryActionLabel,
                onClick = onPrimaryAction
            )
        }
    }
}
