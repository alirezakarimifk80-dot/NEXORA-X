package com.nexora.x.domain.model

/**
 * Domain model representing a remote Subscription group.
 * Actual subscription parsing and HTTP synchronization are scheduled for Phase 2.
 */
data class SubscriptionItem(
    val id: String,
    val name: String,
    val endpointMask: String,
    val profileCountLabel: String,
    val expirationLabel: String,
    val lastUpdatedLabel: String,
    val statusNote: String
)
