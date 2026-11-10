package com.nexora.x.domain.model

enum class ToolAvailability(val label: String) {
    READY_LOCAL_INSPECTION("READY · LOCAL"),
    PENDING_CORE_ENGINE("UNAVAILABLE · PHASE 3 CORE")
}

/**
 * Domain model representing a networking diagnostic utility on the Tools screen.
 * Strictly distinguishes unavailable tools from local architectural inspection.
 */
data class NetworkToolItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val availability: ToolAvailability,
    val metricPlaceholder: String,
    val technicalDetail: String
)
