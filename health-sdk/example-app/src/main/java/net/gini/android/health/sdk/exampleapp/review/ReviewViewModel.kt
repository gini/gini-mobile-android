package net.gini.android.health.sdk.exampleapp.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.gini.android.health.sdk.GiniHealth

class ReviewViewModel(val giniHealth: GiniHealth) : ViewModel() {
    val giniPaymentModule = giniHealth.giniInternalPaymentModule
    val paymentProviderAppsFlow = giniPaymentModule.paymentComponent.paymentProviderAppsFlow

    fun loadPaymentProviderApps() {
        viewModelScope.launch {
            giniPaymentModule.paymentComponent.loadPaymentProviderApps()
        }
    }
}
