package com.nexora.x.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.nexora.x.core.model.Profile
import com.nexora.x.domain.model.ServerProfile

/**
 * Reusable Server Profile Card component supporting both:
 * 1. Real Phase 2 Room-backed [Profile] with protocol badges, server address:port, transport/TLS tags,
 *    selected indicator, and Edit / Duplicate / Copy URI / Delete actions.
 * 2. Phase 1 legacy [ServerProfile] or empty selection slot on Home/Servers.
 */
@Composable
fun ServerCard(
    profile: ServerProfile?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val isSelected = profile?.isSelected == true
    val borderColor = if (isSelected) {
        colorScheme.primary
    } else {
        colorScheme.outline
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(colorScheme.surfaceVariant)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = MaterialTheme.shapes.medium
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
            .semantics {
                contentDescription = if (profile != null) {
                    "Server ${profile.name}, Protocol ${profile.protocol.label}, Latency ${profile.latencyLabel}"
                } else {
                    "No Server Selected, Tap to select or add profile"
                }
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(colorScheme.surface)
                    .border(1.dp, colorScheme.outline, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Dns,
                    contentDescription = null,
                    tint = if (profile != null) colorScheme.secondary else NexoraColors.TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = profile?.name ?: "No Server Profile Selected",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = colorScheme.onSurface
                )
                Text(
                    text = if (profile != null) {
                        "${profile.protocol.tag} · ${profile.transport} · ${profile.security}"
                    } else {
                        "UNCONFIGURED · Tap to add or select a saved profile"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = profile?.latencyLabel ?: "— ms",
                style = MaterialTheme.typography.labelLarge,
                color = colorScheme.secondary
            )
            Text(
                text = if (isSelected) "SELECTED" else "SLOT IDLE",
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) colorScheme.primary else NexoraColors.TextMuted
            )
        }
    }
}

/**
 * Phase 2 Room-backed Profile Card with full management actions (Select, Edit, Duplicate, Copy Link, Delete)
 * and light/dark theme compatibility.
 */
@Composable
fun RealProfileCard(
    profile: Profile,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onCopyLink: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    showActions: Boolean = true
) {
    val colorScheme = MaterialTheme.colorScheme
    val borderColor = if (profile.isSelected) {
        colorScheme.primary
    } else {
        colorScheme.outline
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(colorScheme.surface)
            .border(
                width = if (profile.isSelected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = MaterialTheme.shapes.medium
            )
            .clickable(onClick = onSelect)
            .padding(16.dp)
            .semantics {
                contentDescription =
                    "Profile ${profile.displayName}, Protocol ${profile.protocol.displayName}, Endpoint ${profile.serverAddress}:${profile.port}"
            },
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
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
                        text = profile.protocol.badgeLabel,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = colorScheme.secondary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(colorScheme.secondaryContainer)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                    Text(
                        text = profile.displayName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = colorScheme.onSurface
                    )
                }

                Text(
                    text = "${profile.serverAddress}:${profile.port} · ${profile.transportConfig.networkType.uppercase()} · ${profile.tlsConfig.securityMode.uppercase()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onSurfaceVariant
                )
            }

            if (profile.isSelected) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = "Selected Profile",
                        tint = colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "SELECTED",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = colorScheme.secondary
                    )
                }
            }
        }

        if (profile.unsupportedWarnings.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(NexoraColors.StatusPendingAmber.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = "Unsupported parameter warning",
                    tint = NexoraColors.StatusPendingAmber,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = profile.unsupportedWarnings.first(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = NexoraColors.StatusPendingAmber
                )
            }
        }

        if (showActions) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (profile.tlsConfig.sni.isNotBlank()) {
                        "SNI: ${profile.tlsConfig.sni}"
                    } else {
                        "Cipher: ${profile.securityMethod}"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = NexoraColors.TextMuted
                )

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(
                        onClick = onCopyLink,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "Copy profile link",
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDuplicate,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FileCopy,
                            contentDescription = "Duplicate profile",
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit profile",
                            tint = colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Delete profile",
                            tint = NexoraColors.StatusErrorCrimson,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
