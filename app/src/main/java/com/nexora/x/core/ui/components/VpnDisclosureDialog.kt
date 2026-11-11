package com.nexora.x.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nexora.x.core.design.theme.NexoraColors

/**
 * Google Play Store VpnService Policy Prominent Disclosure & Consent Dialog.
 *
 * Google Play's VpnService policy requires apps using `android.net.VpnService` to present
 * an explicit, prominent in-app disclosure BEFORE invoking `VpnService.prepare()` system consent.
 *
 * This dialog clearly explains:
 * 1. Why NEXORA X uses Android `VpnService` (to establish a local device-level TUN interface
 *    and route device network traffic through the user's self-configured proxy server).
 * 2. How traffic is encrypted between the Android device and the user-configured remote endpoint
 *    (VLESS, VMess, Trojan, or Shadowsocks via `sing-box` `libbox v1.10.7`).
 * 3. What data is stored locally on the device (imported profiles in Room SQLite and settings in
 *    DataStore, excluded from cloud auto-backup) and that NEXORA X collects or shares zero
 *    browsing activity, DNS queries, or personal data with the application publisher.
 * 4. How the user can disconnect at any time via the Home screen or the persistent foreground
 *    notification ("Stop VPN").
 */
@Composable
fun VpnDisclosureDialog(
    onAcceptAndContinue: () -> Unit,
    onDecline: () -> Unit,
    isReadOnlyReview: Boolean = false
) {
    Dialog(
        onDismissRequest = onDecline,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = isReadOnlyReview,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = 1.dp,
                    color = NexoraColors.CyanHighlight.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(16.dp)
                ),
            color = NexoraColors.MidnightSurface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Security,
                        contentDescription = "VPN Service Disclosure",
                        tint = NexoraColors.CyanHighlight
                    )
                    Column {
                        Text(
                            text = "VPN SERVICE PROMINENT DISCLOSURE",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NexoraColors.TextPrimary,
                            modifier = Modifier.semantics { heading() }
                        )
                        Text(
                            text = "Required before enabling Android VpnService",
                            style = MaterialTheme.typography.labelSmall,
                            color = NexoraColors.CyanHighlight
                        )
                    }
                }

                Text(
                    text = "NEXORA X is a user-configured VPN client utility powered by the open-source sing-box (libbox v1.10.7) engine. Before connecting, please review how NEXORA X uses Android's VpnService permission:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NexoraColors.TextSecondary
                )

                DisclosureSectionItem(
                    icon = Icons.Outlined.VpnKey,
                    title = "1. Why NEXORA X Uses Android VpnService",
                    description = "NEXORA X uses android.net.VpnService to create a local virtual network interface (TUN) on your device. When you tap Connect, device network traffic is captured by this local TUN interface and routed exclusively to the VLESS, VMess, Trojan, or Shadowsocks server endpoint that you imported and selected."
                )

                DisclosureSectionItem(
                    icon = Icons.Outlined.Lock,
                    title = "2. Encryption & Destination Server",
                    description = "Traffic between your device and your configured remote server is encrypted according to the protocol settings in your selected profile (such as TLS, Reality, XTLS, or AEAD ciphers). Because you supply your own server configuration, your remote server operator handles your egress traffic."
                )

                DisclosureSectionItem(
                    icon = Icons.Outlined.Storage,
                    title = "3. Local-Only Data & Zero Telemetry",
                    description = "Your imported server profiles and preferences are stored strictly locally on your device (excluded from cloud auto-backup). NEXORA X does NOT operate backend servers, does NOT log or inspect your browsing history or DNS queries, and does NOT sell or transmit personal or diagnostic data to the app publisher or third parties."
                )

                DisclosureSectionItem(
                    icon = Icons.Outlined.CheckCircle,
                    title = "4. Full User Control & Instant Disconnect",
                    description = "NEXORA X only establishes a VPN tunnel when you explicitly initiate a connection. While active, a persistent Android foreground notification displays live status and a one-tap 'Stop VPN' button so you can disconnect at any time."
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (isReadOnlyReview) {
                    PremiumButton(
                        text = "Close Disclosure",
                        icon = Icons.Outlined.Close,
                        onClick = onDecline,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PremiumButton(
                            text = "Decline",
                            icon = Icons.Outlined.Close,
                            isSecondary = true,
                            onClick = onDecline,
                            modifier = Modifier.weight(1f)
                        )
                        PremiumButton(
                            text = "Accept & Continue",
                            icon = Icons.Outlined.CheckCircle,
                            onClick = onAcceptAndContinue,
                            modifier = Modifier.weight(1.3f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DisclosureSectionItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(NexoraColors.DeepNavyBackground.copy(alpha = 0.7f))
            .border(
                width = 1.dp,
                color = NexoraColors.BorderSubtle,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NexoraColors.CyanHighlight
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = NexoraColors.TextPrimary
            )
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = NexoraColors.TextSecondary
        )
    }
}
