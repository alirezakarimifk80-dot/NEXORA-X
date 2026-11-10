package com.nexora.x.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexora.x.core.design.theme.NexoraColors

/**
 * Reusable SectionHeader component with optional right-aligned metadata label.
 */
@Composable
fun SectionHeader(
    title: String,
    trailingMetadata: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = NexoraColors.CyanHighlight
        )
        if (trailingMetadata != null) {
            Text(
                text = trailingMetadata,
                style = MaterialTheme.typography.labelMedium,
                color = NexoraColors.TextMuted
            )
        }
    }
}
