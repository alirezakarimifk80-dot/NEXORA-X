package com.nexora.x.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexora.x.core.design.theme.NexoraColors

/**
 * Reusable telemetry metric card for Download Speed, Upload Speed, Latency, and Traffic counters.
 * Uses monospace tabular typography to prevent layout jitter.
 */
@Composable
fun TrafficMetricCard(
    label: String,
    value: String,
    caption: String,
    icon: ImageVector,
    accentColor: Color = NexoraColors.CyanHighlight,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(NexoraColors.BlackSurface)
            .border(
                width = 1.dp,
                color = NexoraColors.HairlineBorder,
                shape = MaterialTheme.shapes.medium
            )
            .padding(16.dp)
            .semantics {
                contentDescription = "$label: $value ($caption)"
            },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = NexoraColors.TextSecondary
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = NexoraColors.TextPrimary
        )

        Text(
            text = caption,
            style = MaterialTheme.typography.labelMedium,
            color = NexoraColors.TextMuted
        )
    }
}
