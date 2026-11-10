package com.nexora.x.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexora.x.core.design.theme.NexoraColors
import com.nexora.x.core.navigation.NexoraDestination

/**
 * Reusable 5-tab PremiumBottomNavigation bar for NEXORA X:
 * HOME, SERVERS, SUBSCRIPTIONS, TOOLS, SETTINGS.
 */
@Composable
fun PremiumBottomNavigation(
    destinations: List<NexoraDestination>,
    currentDestination: NexoraDestination,
    onNavigate: (NexoraDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(NexoraColors.BlackSurface)
            .navigationBarsPadding()
    ) {
        HorizontalDivider(
            thickness = 1.dp,
            color = NexoraColors.HairlineBorder
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            destinations.forEach { destination ->
                val isSelected = destination == currentDestination
                val itemTint = if (isSelected) {
                    NexoraColors.CyanHighlight
                } else {
                    NexoraColors.TextMuted
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) NexoraColors.ElectricPurpleSubtle
                            else NexoraColors.BlackSurface
                        )
                        .then(
                            if (isSelected) {
                                Modifier.border(
                                    width = 1.dp,
                                    color = NexoraColors.ElectricPurple.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            } else {
                                Modifier
                            }
                        )
                        .clickable { onNavigate(destination) }
                        .padding(vertical = 8.dp)
                        .semantics {
                            role = Role.Tab
                            selected = isSelected
                            contentDescription = "${destination.label} tab"
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = null,
                        tint = itemTint,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = destination.label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isSelected) NexoraColors.TextPrimary else NexoraColors.TextMuted
                    )
                }
            }
        }
    }
}
