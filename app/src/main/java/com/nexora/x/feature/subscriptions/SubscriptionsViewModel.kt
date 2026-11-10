package com.nexora.x.feature.subscriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexora.x.domain.model.SubscriptionItem
import com.nexora.x.domain.repository.NexoraRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class SubscriptionsUiState(
    val subscriptions: List<SubscriptionItem> = emptyList(),
    val totalProfileCountLabel: String = "0 Profiles",
    val expirationSummaryLabel: String = "No Active Subscriptions",
    val actionBannerMessage: String? = null
)

@HiltViewModel
class SubscriptionsViewModel @Inject constructor(
    private val repository: NexoraRepository
) : ViewModel() {

    val uiState: StateFlow<SubscriptionsUiState> = combine(
        repository.subscriptions,
        repository.lastActionBanner
    ) { list, banner ->
        SubscriptionsUiState(
            subscriptions = list,
            totalProfileCountLabel = "${list.size} Subscriptions · 0 Profiles",
            expirationSummaryLabel = "Awaiting Phase 2 Subscription Sync",
            actionBannerMessage = banner
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SubscriptionsUiState()
    )

    fun onAddSubscriptionClicked() {
        repository.requestAddSubscriptionAction()
    }

    fun onUpdateAllClicked() {
        repository.requestUpdateSubscriptionsAction()
    }

    fun onDismissBanner() {
        repository.dismissActionBanner()
    }
}
