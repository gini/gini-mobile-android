package net.gini.android.merchant.sdk.exampleapp

import androidx.lifecycle.ViewModel
import net.gini.android.merchant.sdk.integratedFlow.PaymentFlowConfiguration

class MainViewModel: ViewModel() {

    private var flowConfiguration: PaymentFlowConfiguration? = null

    fun saveConfiguration(flowConfig: PaymentFlowConfiguration) {
        flowConfiguration = flowConfig
    }

    fun getFlowConfiguration() = flowConfiguration
}
