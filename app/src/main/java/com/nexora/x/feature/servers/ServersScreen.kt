package com.nexora.x.feature.servers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.PersistableBundle
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexora.x.core.design.theme.NexoraColors
import com.nexora.x.core.model.ProfileSortOrder
import com.nexora.x.core.model.ProfileType
import com.nexora.x.core.ui.components.EmptyState
import com.nexora.x.core.ui.components.NexoraTopBar
import com.nexora.x.core.ui.components.PremiumButton
import com.nexora.x.core.ui.components.RealProfileCard
import com.nexora.x.core.ui.components.SectionHeader

/**
 * NEXORA X Phase 2 Servers Screen.
 * Features:
 * - Real Room-backed profile list
 * - Protocol badges (VLESS, VMess, Trojan, Shadowsocks)
 * - Server address & port display
 * - Selected profile indicator
 * - Search & Sorting (Recently Updated, Name A-Z, Protocol, Port)
 * - Add profile button -> navigates to AddProfileScreen
 * - Edit profile -> navigates to AddProfileScreen(profileId)
 * - Delete profile with confirmation dialog
 * - Duplicate profile
 * - Copy profile URI link to clipboard
 * - Empty, Loading, and Error states
 * - Works in both Light and Dark themes
 */
@Composable
fun ServersScreen(
    onNavigateToAddProfile: () -> Unit = {},
    onNavigateToEditProfile: (String) -> Unit = {},
    viewModel: ServersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    // Delete Confirmation Modal
    val pendingDelete = uiState.pendingDeleteProfile
    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = viewModel::onCancelDeleteProfile,
            title = {
                Text(
                    text = "Delete Profile?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently remove '${pendingDelete.displayName}' (${pendingDelete.serverAddress}:${pendingDelete.port}) from local storage?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::onConfirmDeleteProfile) {
                    Text(
                        text = "Delete",
                        color = NexoraColors.StatusErrorCrimson,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onCancelDeleteProfile) {
                    Text(text = "Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        NexoraTopBar(
            title = "SERVERS",
            subtitle = "${uiState.totalSavedCount} SAVED PROFILES · ROOM DB",
            actions = {
                PremiumButton(
                    text = "Add Profile",
                    icon = Icons.Outlined.Add,
                    onClick = onNavigateToAddProfile
                )
            }
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
                        .background(colorScheme.primaryContainer)
                        .border(1.dp, colorScheme.primary, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = uiState.actionBannerMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Dismiss notification",
                        tint = colorScheme.secondary,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clickable { viewModel.onDismissBanner() }
                    )
                }
            }

            if (uiState.errorMessage != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NexoraColors.StatusErrorCrimson.copy(alpha = 0.12f))
                        .border(1.dp, NexoraColors.StatusErrorCrimson, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = uiState.errorMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NexoraColors.StatusErrorCrimson
                    )
                }
            }

            // Search Field
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "Search profile name, server address, or SNI...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search profiles",
                        tint = colorScheme.secondary
                    )
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            // Protocol Filter Bar
            SectionHeader(
                title = "PROTOCOL FILTER",
                trailingMetadata = uiState.selectedProtocolFilter?.displayName ?: "All Protocols"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val allSelected = uiState.selectedProtocolFilter == null
                Text(
                    text = "All Protocols",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = if (allSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (allSelected) NexoraColors.TextPrimary else colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (allSelected) colorScheme.primary else colorScheme.surface)
                        .border(1.dp, colorScheme.outline, RoundedCornerShape(10.dp))
                        .clickable { viewModel.onProtocolFilterSelected(null) }
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                )

                ProfileType.entries.forEach { protocol ->
                    val isSelected = protocol == uiState.selectedProtocolFilter
                    Text(
                        text = protocol.displayName,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isSelected) NexoraColors.TextPrimary else colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) colorScheme.primary else colorScheme.surface)
                            .border(1.dp, colorScheme.outline, RoundedCornerShape(10.dp))
                            .clickable { viewModel.onProtocolFilterSelected(protocol) }
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                    )
                }
            }

            // Sorting Bar
            SectionHeader(
                title = "SORT PROFILES",
                trailingMetadata = uiState.sortOrder.label
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProfileSortOrder.entries.forEach { order ->
                    val isSelected = order == uiState.sortOrder
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) colorScheme.secondaryContainer else colorScheme.surface)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) colorScheme.secondary else colorScheme.outline,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { viewModel.onSortOrderSelected(order) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Sort,
                            contentDescription = null,
                            tint = if (isSelected) colorScheme.secondary else colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = order.label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isSelected) colorScheme.secondary else colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Loading / Empty / Populated Profile List
            SectionHeader(
                title = "SAVED PROFILES",
                trailingMetadata = "${uiState.filteredProfiles.size} SHOWN"
            )

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = colorScheme.primary)
                    }
                }
                uiState.filteredProfiles.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Outlined.Dns,
                        title = if (uiState.totalSavedCount == 0) {
                            "No Saved Profiles Yet"
                        } else {
                            "No Profiles Match Current Filter"
                        },
                        description = if (uiState.totalSavedCount == 0) {
                            "Import a real VLESS, VMess, Trojan, or Shadowsocks configuration link or create a profile manually."
                        } else {
                            "Try clearing the search query or switching the protocol filter to 'All Protocols'."
                        },
                        phaseNotice = "PHASE 2 ACTIVE: Real Room database persistence & multi-protocol URI parser.",
                        primaryActionLabel = "Add or Import Profile",
                        onPrimaryAction = onNavigateToAddProfile
                    )
                }
                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        uiState.filteredProfiles.forEach { profile ->
                            RealProfileCard(
                                profile = profile,
                                onSelect = { viewModel.onSelectProfile(profile) },
                                onEdit = { onNavigateToEditProfile(profile.id) },
                                onDuplicate = { viewModel.onDuplicateProfile(profile) },
                                onCopyLink = {
                                    val uri = viewModel.buildShareableUri(profile)
                                    val systemClipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    if (systemClipboard != null) {
                                        val clip = ClipData.newPlainText("NEXORA X Profile Link", uri)
                                        clip.description.extras = PersistableBundle().apply {
                                            putBoolean("android.content.extra.IS_SENSITIVE", true)
                                        }
                                        systemClipboard.setPrimaryClip(clip)
                                    } else {
                                        clipboardManager.setText(AnnotatedString(uri))
                                    }
                                },
                                onDelete = { viewModel.onRequestDeleteProfile(profile) }
                            )
                        }
                    }
                }
            }
        }
    }
}
