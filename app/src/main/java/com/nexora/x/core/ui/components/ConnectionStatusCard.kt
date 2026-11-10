package com.nexora.x.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.nexora.x.core.engine.EngineState
import com.nexora.x.domain.model.ConnectionState

/**
 * Displays the current tunnel connection status and engine state.
 * Overloaded to support both the Phase 3 [EngineState] state machine and the legacy [ConnectionState].
 */
@Composable
fun ConnectionStatusCard(
    engineState: EngineState,
    sessionDuration: String,
    statusDetail: String,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val badgeColor = when (engineState) {
        EngineState.CONNECTED -> NexoraColors.StatusNominalEmerald
        EngineState.ERROR -> NexoraColors.StatusErrorCrimson
        EngineState.PREPARING,
        EngineState.REQUESTING_PERMISSION,
        EngineState.STARTING,
        EngineState.STOPPING -> NexoraColors.StatusPendingAmber
        EngineState.DISCONNECTED -> colorScheme.secondary
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(colorScheme.surface)
            .border(
                width = 1.dp,
                color = if (engineState == EngineState.ERROR) NexoraColors.StatusErrorCrimson else colorScheme.outline,
                shape = MaterialTheme.shapes.large
            )
            .padding(20.dp)
            .semantics {
                contentDescription =
                    "Connection Status ${engineState.displayTitle}, ${engineState.statusBadge}"
            },
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "TUNNEL STATUS",
                    style = MaterialTheme.typography.labelMedium,
                    color = NexoraColors.TextMuted
                )
                Text(
                    text = engineState.displayTitle,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (engineState == EngineState.ERROR) {
                        NexoraColors.StatusErrorCrimson
                    } else {
                        colorScheme.onSurface
                    }
                )
            }

            Text(
                text = engineState.statusBadge,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = badgeColor,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(badgeColor.copy(alpha = 0.12f))
                    .border(
                        width = 1.dp,
                        color = badgeColor.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        Text(
            text = statusDetail,
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(colorScheme.background)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SESSION DURATION",
                style = MaterialTheme.typography.labelMedium,
                color = NexoraColors.TextMuted
            )
            Text(
                text = sessionDuration,
                style = MaterialTheme.typography.labelLarge,
                color = colorScheme.secondary
            )
        }
    }
}

@Composable
fun ConnectionStatusCard(
    connectionState: ConnectionState,
    sessionDuration: String,
    modifier: Modifier = Modifier
) {
    ConnectionStatusCard(
        engineState = EngineState.DISCONNECTED,
        sessionDuration = sessionDuration,
        statusDetail = connectionState.description,
        modifier = modifier
    )
}
