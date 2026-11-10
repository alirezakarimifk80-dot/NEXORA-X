package com.nexora.x.core.common

/**
 * Sealed hierarchy representing asynchronous resource state without fabricated values.
 */
sealed interface ResourceState<out T> {
    data object Idle : ResourceState<Nothing>
    data object Loading : ResourceState<Nothing>
    data class PendingPhase(val phaseNumber: Int, val reason: String) : ResourceState<Nothing>
    data class Success<T>(val data: T) : ResourceState<T>
    data class Error(val message: String) : ResourceState<Nothing>
}
