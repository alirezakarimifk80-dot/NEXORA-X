package com.nexora.x.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexora.x.core.design.theme.NexoraColors

/**
 * Reusable PremiumButton with electric purple gradient or secondary obsidian styling.
 */
@Composable
fun PremiumButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isSecondary: Boolean = false
) {
    val backgroundModifier = if (isSecondary) {
        Modifier.background(NexoraColors.ElevatedCardSurface)
    } else {
        Modifier.background(
            Brush.horizontalGradient(
                colors = listOf(
                    NexoraColors.ElectricPurple,
                    NexoraColors.ElectricPurpleBright
                )
            )
        )
    }

    val borderColor = if (isSecondary) {
        NexoraColors.SubtleHighlightBorder
    } else {
        NexoraColors.CyanHighlight.copy(alpha = 0.4f)
    }

    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .then(backgroundModifier)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = MaterialTheme.shapes.small
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp)
            .semantics {
                role = Role.Button
                contentDescription = text
            },
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSecondary) NexoraColors.CyanHighlight else NexoraColors.TextPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = NexoraColors.TextPrimary
        )
    }
}
