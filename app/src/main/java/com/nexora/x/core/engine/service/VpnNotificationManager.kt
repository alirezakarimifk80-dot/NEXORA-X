package com.nexora.x.core.engine.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.nexora.x.MainActivity
import com.nexora.x.R
import com.nexora.x.core.engine.EngineState
import com.nexora.x.core.engine.EngineTrafficStatistics

/**
 * Manages the persistent Foreground Service Notification while [NexoraVpnService] is active.
 *
 * Displays:
 * - NEXORA X
 * - Actual connection state
 * - Selected profile display name (sanitized; never credentials or raw links)
 * - Actual traffic speeds if reported by `libbox`, or `N/A` when unavailable
 * - Stop VPN action button (`ACTION_STOP_VPN`) that genuinely stops the service and engine
 */
object VpnNotificationManager {

    const val NOTIFICATION_CHANNEL_ID = "nexora_x_vpn_session_channel"
    const val NOTIFICATION_ID = 10042

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "NEXORA X VPN Tunnel Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Persistent status notification for active NEXORA X sing-box VPN sessions."
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun buildForegroundNotification(
        context: Context,
        state: EngineState,
        profileName: String?,
        traffic: EngineTrafficStatistics
    ): Notification {
        ensureNotificationChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopVpnIntent = Intent(context, NexoraVpnService::class.java).apply {
            action = NexoraVpnService.ACTION_STOP_VPN
        }
        val stopVpnPendingIntent = PendingIntent.getService(
            context,
            1,
            stopVpnIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val safeProfileLabel = profileName?.takeIf { it.isNotBlank() } ?: "Profile"
        val title = "NEXORA X · ${state.displayTitle}"

        val speedLine = if (traffic.downlinkSpeedBytesPerSec != null && traffic.uplinkSpeedBytesPerSec != null) {
            "↓ ${formatBytesPerSec(traffic.downlinkSpeedBytesPerSec)} · ↑ ${formatBytesPerSec(traffic.uplinkSpeedBytesPerSec)}"
        } else {
            "Speed: N/A"
        }

        val contentText = "$safeProfileLabel · $speedLine"

        return NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(contentText)
            .setOngoing(state == EngineState.STARTING || state == EngineState.CONNECTED)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                android.R.drawable.ic_media_pause,
                "Stop VPN",
                stopVpnPendingIntent
            )
            .build()
    }

    fun formatBytesPerSec(bytesPerSec: Long?): String {
        if (bytesPerSec == null || bytesPerSec < 0L) return "N/A"
        return "${formatByteCount(bytesPerSec)}/s"
    }

    fun formatByteCount(bytes: Long?): String {
        if (bytes == null || bytes < 0L) return "N/A"
        if (bytes < 1024L) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024.0) return String.format("%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024.0) return String.format("%.2f MB", mb)
        val gb = mb / 1024.0
        return String.format("%.2f GB", gb)
    }
}
