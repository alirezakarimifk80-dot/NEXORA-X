package com.nexora.x.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexora.x.core.design.theme.NexoraColors
import com.nexora.x.core.model.ProfileType
import com.nexora.x.core.ui.components.NexoraTopBar
import com.nexora.x.core.ui.components.PremiumButton
import com.nexora.x.core.ui.components.SectionHeader

/**
 * NEXORA X Phase 2 Add / Edit Profile Screen.
 * Supports:
 * - Import from clipboard (`vless://`, `vmess://`, `trojan://`, `ss://`, or multi-line plain text)
 * - Paste configuration & live parser preview
 * - Manual configuration entry (Name, Protocol, Host, Port, UUID/Password, Transport, TLS/Reality)
 * - Honest QR import entry point (clearly states camera scanner is not yet active)
 * - Form validation, unsupported parameter warnings, Save and Cancel actions
 */
@Composable
fun AddProfileScreen(
    editingProfileId: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: AddProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current
    val colorScheme = MaterialTheme.colorScheme

    LaunchedEffect(editingProfileId) {
        viewModel.loadProfileForEdit(editingProfileId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        NexoraTopBar(
            title = if (uiState.editingProfileId != null) "EDIT PROFILE" else "ADD PROFILE",
            subtitle = "VLESS · VMESS · TROJAN · SHADOWSOCKS",
            actions = {
                PremiumButton(
                    text = "Cancel",
                    icon = Icons.Outlined.Close,
                    isSecondary = true,
                    onClick = onNavigateBack
                )
                PremiumButton(
                    text = "Save",
                    icon = Icons.Outlined.Save,
                    onClick = { viewModel.onSaveClicked(onSuccess = onNavigateBack) }
                )
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Entry Mode Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AddProfileEntryMode.entries.forEach { mode ->
                    val selected = uiState.entryMode == mode
                    Text(
                        text = mode.label,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (selected) NexoraColors.TextPrimary else colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) colorScheme.primary else colorScheme.surface)
                            .border(
                                width = 1.dp,
                                color = if (selected) colorScheme.secondary else colorScheme.outline,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { viewModel.onEntryModeChanged(mode) }
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                    )
                }
            }

            // Status / QR Notice Banner
            if (uiState.statusBanner != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colorScheme.primaryContainer)
                        .border(1.dp, colorScheme.primary, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.QrCodeScanner,
                        contentDescription = null,
                        tint = colorScheme.secondary
                    )
                    Text(
                        text = uiState.statusBanner ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurface
                    )
                }
            }

            // Validation Errors
            if (uiState.validationErrors.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NexoraColors.StatusErrorCrimson.copy(alpha = 0.12f))
                        .border(1.dp, NexoraColors.StatusErrorCrimson, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    uiState.validationErrors.forEach { err ->
                        Text(
                            text = "• $err",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = NexoraColors.StatusErrorCrimson
                        )
                    }
                }
            }

            // Unsupported Parameter Warnings
            if (uiState.warnings.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NexoraColors.StatusPendingAmber.copy(alpha = 0.12f))
                        .border(1.dp, NexoraColors.StatusPendingAmber, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WarningAmber,
                            contentDescription = "Unsupported parameter warning",
                            tint = NexoraColors.StatusPendingAmber
                        )
                        Text(
                            text = "UNSUPPORTED PARAMETER WARNINGS PRESERVED",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = NexoraColors.StatusPendingAmber
                        )
                    }
                    uiState.warnings.forEach { warning ->
                        Text(
                            text = "• $warning",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurface
                        )
                    }
                }
            }

            // Clipboard / Link Import Box
            if (uiState.entryMode != AddProfileEntryMode.MANUAL_FORM) {
                SectionHeader(
                    title = "IMPORT URI OR PLAIN-TEXT CONFIG",
                    trailingMetadata = "vless / vmess / trojan / ss"
                )
                OutlinedTextField(
                    value = uiState.rawImportText,
                    onValueChange = viewModel::onRawImportTextChanged,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6,
                    placeholder = {
                        Text(
                            text = "Paste vless://, vmess://, trojan://, ss:// link or multi-line plain-text block...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PremiumButton(
                        text = "Import Clipboard",
                        icon = Icons.Outlined.ContentPaste,
                        isSecondary = true,
                        onClick = {
                            val clip = clipboardManager.getText()?.text
                            viewModel.onPasteFromClipboard(clip)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    PremiumButton(
                        text = "Parse Into Form",
                        onClick = { viewModel.parseAndPopulateManualForm() },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Protocol Selector
            SectionHeader(
                title = "PROTOCOL SELECTION",
                trailingMetadata = uiState.protocol.displayName
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProfileType.entries.forEach { proto ->
                    val isSelected = uiState.protocol == proto
                    Text(
                        text = proto.displayName,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isSelected) NexoraColors.TextPrimary else colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) colorScheme.primary else colorScheme.surface)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) colorScheme.secondary else colorScheme.outline,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { viewModel.onProtocolChanged(proto) }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }

            // Core Server & Credential Fields
            SectionHeader(
                title = "ENDPOINT & CREDENTIAL",
                trailingMetadata = "REQUIRED"
            )
            OutlinedTextField(
                value = uiState.displayName,
                onValueChange = viewModel::onDisplayNameChanged,
                label = { Text("Profile Display Name") },
                placeholder = { Text("e.g., Tokyo VLESS Reality 01") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = uiState.serverAddress,
                    onValueChange = viewModel::onServerAddressChanged,
                    label = { Text("Server Address / Host") },
                    placeholder = { Text("edge.example.com") },
                    singleLine = true,
                    modifier = Modifier.weight(2f)
                )
                OutlinedTextField(
                    value = uiState.portText,
                    onValueChange = viewModel::onPortChanged,
                    label = { Text("Port") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = uiState.credential,
                onValueChange = viewModel::onCredentialChanged,
                label = {
                    Text(
                        when (uiState.protocol) {
                            ProfileType.VLESS, ProfileType.VMESS -> "UUID"
                            ProfileType.TROJAN, ProfileType.SHADOWSOCKS -> "Password"
                        }
                    )
                },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.securityMethod,
                onValueChange = viewModel::onSecurityMethodChanged,
                label = { Text("Cipher / Encryption Method") },
                placeholder = { Text("none / auto / aes-256-gcm / chacha20-ietf-poly1305") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Transport Configuration
            SectionHeader(
                title = "TRANSPORT CONFIGURATION",
                trailingMetadata = uiState.networkType.uppercase()
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("tcp", "ws", "grpc", "http", "h2", "quic").forEach { net ->
                    val selected = uiState.networkType == net
                    Text(
                        text = net.uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) NexoraColors.TextPrimary else colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) colorScheme.primary else colorScheme.surface)
                            .border(1.dp, colorScheme.outline, RoundedCornerShape(8.dp))
                            .clickable { viewModel.onNetworkTypeChanged(net) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = uiState.transportHost,
                    onValueChange = viewModel::onTransportHostChanged,
                    label = { Text("WS / HTTP Host Header") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = uiState.transportPath,
                    onValueChange = viewModel::onTransportPathChanged,
                    label = { Text("WS Path / Endpoint") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            if (uiState.networkType == "grpc") {
                OutlinedTextField(
                    value = uiState.grpcServiceName,
                    onValueChange = viewModel::onGrpcServiceNameChanged,
                    label = { Text("gRPC Service Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (uiState.protocol == ProfileType.VLESS) {
                OutlinedTextField(
                    value = uiState.flow,
                    onValueChange = viewModel::onFlowChanged,
                    label = { Text("XTLS Flow (e.g., xtls-rprx-vision)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // TLS / Reality Configuration
            SectionHeader(
                title = "TLS & REALITY SECURITY",
                trailingMetadata = uiState.tlsSecurityMode.uppercase()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("none", "tls", "reality").forEach { mode ->
                    val selected = uiState.tlsSecurityMode == mode
                    Text(
                        text = mode.uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) NexoraColors.TextPrimary else colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) colorScheme.primary else colorScheme.surface)
                            .border(1.dp, colorScheme.outline, RoundedCornerShape(8.dp))
                            .clickable { viewModel.onTlsSecurityModeChanged(mode) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

            if (uiState.tlsSecurityMode != "none") {
                OutlinedTextField(
                    value = uiState.sni,
                    onValueChange = viewModel::onSniChanged,
                    label = { Text("SNI (Server Name Indication)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.alpn,
                        onValueChange = viewModel::onAlpnChanged,
                        label = { Text("ALPN (h2,http/1.1)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = uiState.fingerprint,
                        onValueChange = viewModel::onFingerprintChanged,
                        label = { Text("uTLS Fingerprint") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (uiState.tlsSecurityMode == "reality") {
                    OutlinedTextField(
                        value = uiState.realityPublicKey,
                        onValueChange = viewModel::onRealityPublicKeyChanged,
                        label = { Text("Reality Public Key (pbk)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uiState.realityShortId,
                        onValueChange = viewModel::onRealityShortIdChanged,
                        label = { Text("Reality Short ID (sid)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Allow Insecure Peer Certificate (Skip Verify)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurface
                    )
                    Switch(
                        checked = uiState.allowInsecure,
                        onCheckedChange = viewModel::onAllowInsecureChanged
                    )
                }

                if (uiState.allowInsecure) {
                    Text(
                        text = "Security Warning: Disabling TLS peer certificate verification ('insecure': true) allows untrusted or self-signed certificates and exposes traffic to active man-in-the-middle (MITM) interception.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NexoraColors.StatusPendingAmber,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
