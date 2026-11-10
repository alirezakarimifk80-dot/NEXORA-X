package com.nexora.x.domain.model

import com.nexora.x.core.common.AppConstants

/**
 * Domain model representing dashboard telemetry placeholders.
 * Phase 1 rule: Never generate fake speed, ping, or traffic values.
 */
data class TelemetrySnapshot(
    val downloadSpeed: String = AppConstants.PLACEHOLDER_SPEED,
    val uploadSpeed: String = AppConstants.PLACEHOLDER_SPEED,
    val latency: String = AppConstants.PLACEHOLDER_LATENCY,
    val sessionDuration: String = AppConstants.PLACEHOLDER_DURATION,
    val totalDownloadTraffic: String = AppConstants.PLACEHOLDER_TRAFFIC,
    val totalUploadTraffic: String = AppConstants.PLACEHOLDER_TRAFFIC,
    val coreStatusNote: String = "Awaiting Phase 3 Core Engine"
)
