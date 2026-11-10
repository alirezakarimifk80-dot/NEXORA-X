package com.nexora.x.core.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexora.x.core.design.theme.NexoraColors

/**
 * Large animated centerpiece connection button for the NEXORA X Home Dashboard.
 * Phase 1 requirement: Does not claim to connect; clearly indicates ENGINE PENDING.
 */
@Composable
fun ConnectButton(
    statusLabel: String,
    engineSubLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "NexoraOrbBreathing")
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OrbRingScale"
    )

    Box(
        modifier = modifier.size(196.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer ambient halo ring
        Box(
            modifier = Modifier
                .size(192.dp)
                .scale(ringScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            NexoraColors.ElectricPurple.copy(alpha = 0.18f),
                            NexoraColors.CyanHighlight.copy(alpha = 0.05f),
                            NexoraColors.DeepNavyBackground.copy(alpha = 0.0f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = NexoraColors.ElectricPurple.copy(alpha = 0.35f),
                    shape = CircleShape
                )
        )

        // Inner tactile orb button
        Box(
            modifier = Modifier
                .size(152.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            NexoraColors.ElevatedCardSurface,
                            NexoraColors.BlackSurface
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            NexoraColors.ElectricPurple,
                            NexoraColors.CyanHighlight,
                            NexoraColors.ElectricPurple
                        )
                    ),
                    shape = CircleShape
                )
                .clickable(onClick = onClick)
                .semantics {
                    role = Role.Button
                    contentDescription = "Connection Engine Trigger: $statusLabel, $engineSubLabel"
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.PowerSettingsNew,
                    contentDescription = null,
                    tint = NexoraColors.CyanHighlight,
                    modifier = Modifier.size(34.dp)
                )
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = NexoraColors.TextPrimary
                )
                Text(
                    text = engineSubLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = NexoraColors.StatusPendingAmber
                )
            }
        }
    }
}
